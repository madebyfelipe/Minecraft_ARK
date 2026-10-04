#!/usr/bin/env python3
"""Kanban local: estado SQLite e terminais de Claude/Codex, somente no loopback."""
import argparse
import codecs
import errno
import fcntl
import json
import os
from pathlib import Path
import pty
import re
import secrets
import shutil
import signal
import subprocess
import struct
import sys
import threading
import termios
import time
import uuid
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

ROOT = Path(__file__).resolve().parent.parent
TOKEN = secrets.token_urlsafe(32)
RUNS = {}
RUN_LOCK = threading.Lock()
ANSI = re.compile(r'\x1b(?:\[[0-?]*[ -/]*[@-~]|\][^\x07]*(?:\x07|\x1b\\)|[()][A-Za-z0-9]|[@-_0-9])')


def cli(*args):
    result = subprocess.run([sys.executable, str(ROOT/'tools/coordination.py'), *args],
                            cwd=ROOT, capture_output=True, text=True, timeout=20)
    if result.returncode:
        try:
            message = json.loads(result.stderr).get('error', 'Operação recusada')
        except ValueError:
            message = result.stderr.strip()[-1000:]
        raise ValueError(message)
    return json.loads(result.stdout)


class Session:
    def __init__(self, task, executor, owner, command):
        self.id = owner
        self.task = task
        self.executor = executor
        self.output = ''
        self.exit_code = None
        self.exit_reason = None
        self.lock = threading.Lock()
        self.master, slave = pty.openpty()
        # PTYs novas têm dimensão 0x0; TUIs passam a quebrar um caractere por linha.
        fcntl.ioctl(slave, termios.TIOCSWINSZ, struct.pack('HHHH', 32, 110, 0, 0))
        env = os.environ.copy()
        env['TERM'] = 'dumb'
        env.pop('CLAUDE_CODE_CHILD_SESSION', None)
        java = Path.home()/'.gradle/jdks/eclipse_adoptium-17-amd64-linux/jdk-17.0.20.1+1'
        if (java/'bin/java').is_file():
            env.setdefault('JAVA_HOME', str(java))
            env['PATH'] = str(java/'bin')+os.pathsep+env.get('PATH', '')
        try:
            self.process = subprocess.Popen(command, cwd=ROOT, stdin=slave, stdout=slave,
                                            stderr=slave, env=env, start_new_session=True)
        except Exception:
            os.close(self.master)
            raise
        finally:
            os.close(slave)
        threading.Thread(target=self.read, daemon=True).start()
        threading.Thread(target=self.keepalive, daemon=True).start()

    def snapshot(self):
        with self.lock:
            return {'id': self.id, 'task_id': self.task, 'executor': self.executor,
                    'exit_code': self.exit_code, 'exit_reason': self.exit_reason,
                    'output': ANSI.sub('', self.output).replace('\r', '').replace('\x0f', '')[-24000:]}

    def read(self):
        decoder = codecs.getincrementaldecoder('utf-8')(errors='replace')
        try:
            while True:
                data = os.read(self.master, 4096)
                if not data:
                    break
                with self.lock:
                    self.output = (self.output+decoder.decode(data))[-48000:]
        except OSError as exc:
            if exc.errno != errno.EIO:
                with self.lock:
                    self.output += '\nErro de leitura do terminal.'
        finally:
            code = self.process.wait()
            with self.lock:
                self.exit_code = code
            os.close(self.master)
            try:
                lease = cli('show', self.task).get('lease')
                if lease and lease['owner'] == self.id:
                    cli('block', self.task, '--owner', self.id, '--reason',
                        f'Sessão terminou (código {code}) sem registrar handoff; revisar antes de retomar.')
            except ValueError:
                pass  # Uma reserva órfã continua visível; nunca furtá-la automaticamente.

    def keepalive(self):
        renewed = time.monotonic()
        while self.process.poll() is None:
            time.sleep(1)
            if self.process.poll() is not None:
                break
            try:
                task = cli('show', self.task)
                if task['status'] != 'executando' or not task.get('lease') or task['lease']['owner'] != self.id:
                    with self.lock:
                        self.exit_reason = 'handoff' if task['status'] in ('revisao', 'concluido') else 'reserva encerrada'
                    os.killpg(self.process.pid, signal.SIGTERM)
                    break
                if time.monotonic()-renewed >= 20:
                    cli('heartbeat', self.task, '--owner', self.id)
                    renewed = time.monotonic()
            except ValueError:
                # Não continuar um terminal fora da reserva/branch autorizada.
                if self.process.poll() is None:
                    os.killpg(self.process.pid, signal.SIGTERM)
                break


def launch(task_id, executor):
    if executor not in ('codex', 'claude-work'):
        raise ValueError('Executor inválido')
    task = cli('show', task_id)
    if task['status'] != 'pronto' or task['mode'] == 'manual':
        raise ValueError('Só tarefas prontas de leitura/escrita iniciam sessões.')
    if executor == 'claude-work':
        executable = shutil.which('claude') or str(Path.home()/'.local/bin/claude')
        auth = subprocess.run([executable, 'auth', 'status'], capture_output=True, text=True, timeout=15)
        if auth.returncode or json.loads(auth.stdout).get('email') != 'redektm.co@gmail.com':
            raise ValueError('Claude não está conectado à conta de trabalho confirmada. Nenhuma sessão aberta.')
    else:
        executable = shutil.which('codex') or '/usr/lib/chatgpt/resources/codex'
    if not Path(executable).is_file():
        raise ValueError('Executável do agente não encontrado')
    owner = 'kanban-'+str(uuid.uuid4())
    cli('claim', task_id, '--owner', owner, '--executor', executor)
    try:
        packet = cli('context', task_id)
        prompt = ('Você foi iniciado pelo kanban local do Minecraft. Leia coordination/START.md e '
                  'coordination/PROJECT.md. Sua tarefa JÁ ESTÁ RESERVADA; não reserve novamente. '
                  f'Dono obrigatório: {owner}. Codex coordena, Claude trabalho executa. '
                  'Execute apenas o escopo autorizado abaixo. Se faltar decisão, registre block com a pergunta '
                  'e pergunte ao Felipe. Ao terminar registre finish com evidência; não aprove sua própria entrega. '
                  'Não trocar branch nem iniciar outro trabalho. Use heartbeat nas transições.\n'+
                  json.dumps(packet, ensure_ascii=False, indent=2))
        command = ([executable, '--ax-screen-reader', '--model', 'claude-opus-5-5', '--name', task_id, prompt]
                   if executor == 'claude-work' else
                   [executable, '--no-daemon', '--no-alt-screen', '-C', str(ROOT), prompt])
        session = Session(task_id, executor, owner, command)
        with RUN_LOCK:
            RUNS[owner] = session
        return session.snapshot()
    except Exception:
        cli('release', task_id, '--owner', owner, '--reason', 'Não foi possível abrir a sessão')
        raise


class Handler(BaseHTTPRequestHandler):
    def log_message(self, fmt, *args):
        pass

    def respond(self, code, data, mime='application/json; charset=utf-8'):
        body = data if isinstance(data, bytes) else json.dumps(data, ensure_ascii=False).encode()
        self.send_response(code)
        self.send_header('Content-Type', mime)
        self.send_header('Cache-Control', 'no-store')
        self.send_header('X-Content-Type-Options', 'nosniff')
        self.send_header('Referrer-Policy', 'no-referrer')
        self.send_header('Content-Security-Policy', "default-src 'none'; script-src 'self'; style-src 'unsafe-inline'; connect-src 'self'; form-action 'none'; frame-ancestors 'none'; base-uri 'none'")
        self.end_headers()
        self.wfile.write(body)

    def valid_host(self):
        return self.headers.get('Host') == f'127.0.0.1:{self.server.server_port}'

    def do_GET(self):
        if not self.valid_host():
            self.respond(403, {'error': 'Host recusado'})
            return
        try:
            if self.path == '/':
                page = (ROOT/'coordination/board.html').read_text().replace('__TOKEN__', TOKEN)
                self.respond(200, page.encode(), 'text/html; charset=utf-8')
            elif self.path == '/app.js':
                self.respond(200, (ROOT/'coordination/board.js').read_bytes(), 'text/javascript; charset=utf-8')
            elif self.path == '/api/board':
                data = cli('export')
                with RUN_LOCK:
                    data['sessions'] = [session.snapshot() for session in RUNS.values()]
                self.respond(200, data)
            else:
                self.respond(404, {'error': 'Rota não encontrada'})
        except (ValueError, OSError, subprocess.SubprocessError) as exc:
            self.respond(400, {'error': str(exc)})

    def do_POST(self):
        expected = f'http://127.0.0.1:{self.server.server_port}'
        if (not self.valid_host() or self.headers.get('Origin') != expected or
                not secrets.compare_digest(self.headers.get('X-Coordinator-Token', ''), TOKEN)):
            self.respond(403, {'error': 'Pedido de outra origem recusado'})
            return
        try:
            size = int(self.headers.get('Content-Length', '0'))
            if not 0 < size <= 10000:
                raise ValueError('Tamanho de pedido inválido')
            data = json.loads(self.rfile.read(size))
            if self.path == '/api/launch':
                result = launch(data['task_id'], data['executor'])
            elif self.path == '/api/approve':
                result = cli('approve', data['task_id'], '--actor', 'felipe', '--note', data['note'])
            elif self.path == '/api/ready':
                result = cli('ready', data['task_id'], '--actor', 'felipe', '--note', data['note'])
            elif self.path == '/api/reclaim':
                with RUN_LOCK:
                    if any(s.task == data['task_id'] and s.process.poll() is None for s in RUNS.values()):
                        raise ValueError('Pare a sessão ativa antes de recuperar a reserva.')
                result = cli('reclaim', data['task_id'], '--actor', 'felipe', '--reason', data['note'])
            elif self.path == '/api/task':
                task_id = 'REQ-'+uuid.uuid4().hex[:10]
                result = cli('add', task_id, '--title', data['title'], '--mode', 'read',
                             '--status', 'entrada', '--context', data['context'],
                             '--acceptance', data['acceptance'])
            elif self.path in ('/api/input', '/api/stop'):
                with RUN_LOCK:
                    session = RUNS.get(data['session_id'])
                if not session or session.process.poll() is not None:
                    raise ValueError('Sessão não está ativa')
                if self.path == '/api/stop':
                    with session.lock:
                        session.exit_reason = 'parada solicitada'
                    os.killpg(session.process.pid, signal.SIGTERM)
                else:
                    task = cli('show', session.task)
                    if task['status'] != 'executando' or not task.get('lease') or task['lease']['owner'] != session.id:
                        raise ValueError('A sessão não possui mais a reserva. Abra um novo pacote.')
                    content = data.get('text')
                    if not isinstance(content, str) or not content or len(content) > 4000:
                        raise ValueError('Entrada inválida')
                    os.write(session.master, (content+'\r').encode())
                result = {'ok': True}
            else:
                self.respond(404, {'error': 'Rota não encontrada'})
                return
            self.respond(200, result)
        except (ValueError, KeyError, TypeError, OSError, subprocess.SubprocessError) as exc:
            self.respond(400, {'error': str(exc)})


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--port', type=int, default=8765)
    args = parser.parse_args()
    cli('init')
    server = ThreadingHTTPServer(('127.0.0.1', args.port), Handler)
    print(f'Kanban: http://127.0.0.1:{server.server_port}/', flush=True)
    print('Estado local. Só abrir sessão pelo botão ou terminal já coordenado.', flush=True)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()
        # Não fingir concluir/reclamar tarefas ao fechar o painel.
        with RUN_LOCK:
            sessions = list(RUNS.values())
        for session in sessions:
            if session.process.poll() is None:
                os.killpg(session.process.pid, signal.SIGTERM)


if __name__ == '__main__':
    main()
