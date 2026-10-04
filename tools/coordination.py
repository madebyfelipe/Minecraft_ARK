#!/usr/bin/env python3
"""Quadro local e reservas cooperativas; não inicia agentes nem executa tarefas."""

import argparse
from contextlib import contextmanager
from datetime import datetime, timezone
import json
from pathlib import Path
import re
import sqlite3
import subprocess
import sys

ROOT = Path(__file__).resolve().parent.parent
STATUSES = ('entrada', 'pesquisa', 'decisao', 'pronto', 'executando', 'revisao', 'bloqueado', 'concluido')
INITIAL_STATUSES = STATUSES[:4]
MODES = ('read', 'write', 'manual')
ID_PATTERN = re.compile(r'[A-Za-z0-9][A-Za-z0-9_.-]{0,99}\Z')


class CoordinationError(Exception):
    """Falha de validação ou conflito de reserva."""


def now():
    return datetime.now(timezone.utc).isoformat(timespec='seconds')


def string(value, name, limit=20000):
    if not isinstance(value, str) or not value.strip():
        raise CoordinationError(f'{name}: texto não vazio obrigatório')
    if len(value) > limit or '\x00' in value:
        raise CoordinationError(f'{name}: texto inválido ou maior que {limit} caracteres')
    return value


def identifier(value):
    string(value, 'id', 100)
    if not ID_PATTERN.fullmatch(value):
        raise CoordinationError('id: use letras, números, ponto, hífen ou sublinhado')
    return value


def git_state():
    def git(*args):
        try:
            result = subprocess.run(['git', '-C', str(ROOT), *args], check=True,
                                    capture_output=True, text=True, timeout=10)
            return result.stdout.strip()
        except (OSError, subprocess.SubprocessError) as exc:
            raise CoordinationError('Não foi possível consultar branch/commit do checkout') from exc
    return {'branch': git('rev-parse', '--abbrev-ref', 'HEAD'),
            'commit': git('rev-parse', 'HEAD')}


def validate_task(data, seed=False):
    if not isinstance(data, dict):
        raise CoordinationError('Cada tarefa precisa ser um objeto JSON')
    required = ('id', 'title', 'mode', 'status', 'context', 'acceptance')
    if any(key not in data for key in required):
        raise CoordinationError(f'Tarefa exige campos: {", ".join(required)}')
    result = {key: data[key] for key in required}
    result['id'] = identifier(result['id'])
    for key in ('title', 'context', 'acceptance'):
        string(result[key], key, 500 if key == 'title' else 20000)
    allowed_statuses = tuple(status for status in STATUSES if status != 'executando') if seed else INITIAL_STATUSES
    if result['mode'] not in MODES or result['status'] not in allowed_statuses:
        raise CoordinationError('mode/status inicial inválido')
    for key in ('refs', 'files'):
        values = data.get(key, [])
        if not isinstance(values, list) or len(values) > 100:
            raise CoordinationError(f'{key}: lista de até 100 textos obrigatória')
        result[key] = [string(value, key, 2000) for value in values]
    return result


def connect(path, initialize=False):
    if not path.is_absolute():
        raise CoordinationError('--db deve ser um caminho absoluto')
    if not initialize and not path.is_file():
        raise CoordinationError('Banco ausente; execute init primeiro')
    if initialize:
        path.parent.mkdir(parents=True, exist_ok=True)
    db = sqlite3.connect(path, timeout=10, isolation_level=None)
    db.row_factory = sqlite3.Row
    db.execute('PRAGMA busy_timeout=10000')
    db.execute('PRAGMA foreign_keys=ON')
    version = db.execute('PRAGMA user_version').fetchone()[0]
    if version not in (0, 1) or (not initialize and version != 1):
        db.close()
        raise CoordinationError('Versão de banco desconhecida; não foi modificada')
    return db


@contextmanager
def transaction(db, write=True):
    db.execute('BEGIN IMMEDIATE' if write else 'BEGIN')
    try:
        yield
        db.execute('COMMIT')
    except Exception:
        db.execute('ROLLBACK')
        raise


def schema(db):
    # Sem executescript: ele faria commit implícito fora da transação.
    statements = (
        '''CREATE TABLE IF NOT EXISTS tasks (
            id TEXT PRIMARY KEY, title TEXT NOT NULL,
            mode TEXT NOT NULL CHECK(mode IN ('read','write','manual')),
            status TEXT NOT NULL CHECK(status IN ('entrada','pesquisa','decisao','pronto','executando','revisao','bloqueado','concluido')),
            context TEXT NOT NULL, acceptance TEXT NOT NULL,
            refs TEXT NOT NULL, files TEXT NOT NULL, origin TEXT NOT NULL,
            summary TEXT, evidence TEXT, created_at TEXT NOT NULL, updated_at TEXT NOT NULL)''',
        '''CREATE TABLE IF NOT EXISTS leases (
            task_id TEXT PRIMARY KEY REFERENCES tasks(id), owner TEXT NOT NULL,
            executor TEXT NOT NULL CHECK(executor IN ('codex','claude-work')),
            branch TEXT NOT NULL, commit_sha TEXT NOT NULL,
            acquired_at TEXT NOT NULL, heartbeat_at TEXT NOT NULL)''',
        '''CREATE TABLE IF NOT EXISTS archives (
            task_id TEXT PRIMARY KEY REFERENCES tasks(id), summary TEXT NOT NULL, archived_at TEXT NOT NULL)''',
        '''CREATE TABLE IF NOT EXISTS events (
            sequence INTEGER PRIMARY KEY AUTOINCREMENT, task_id TEXT REFERENCES tasks(id),
            timestamp TEXT NOT NULL, action TEXT NOT NULL, owner TEXT, details TEXT NOT NULL)''',
    )
    for statement in statements:
        db.execute(statement)
    db.execute('PRAGMA user_version=1')


def event(db, task_id, action, owner, details):
    db.execute('INSERT INTO events(task_id,timestamp,action,owner,details) VALUES(?,?,?,?,?)',
               (task_id, now(), action, owner, json.dumps(details, ensure_ascii=False)))


def add_task(db, data, origin):
    stamp = now()
    db.execute('''INSERT INTO tasks(id,title,mode,status,context,acceptance,refs,files,origin,created_at,updated_at)
                  VALUES(?,?,?,?,?,?,?,?,?,?,?)''',
               (data['id'], data['title'], data['mode'], data['status'], data['context'],
                data['acceptance'], json.dumps(data['refs'], ensure_ascii=False),
                json.dumps(data['files'], ensure_ascii=False), origin, stamp, stamp))
    event(db, data['id'], 'add', None, {'origin': origin, 'status': data['status']})


def lease_json(row):
    if row is None:
        return None
    result = dict(row)
    result['heartbeat_age_seconds'] = max(0, int((datetime.now(timezone.utc) -
                                                datetime.fromisoformat(result['heartbeat_at'])).total_seconds()))
    result['automatic_expiration'] = False
    return result


def task_json(db, row):
    result = dict(row)
    for key in ('refs', 'files'):
        result[key] = json.loads(result[key])
    archive = db.execute('SELECT summary,archived_at FROM archives WHERE task_id=?', (result['id'],)).fetchone()
    result['archived'] = archive is not None
    result['archive_summary'] = archive['summary'] if archive else None
    result['archived_at'] = archive['archived_at'] if archive else None
    result['lease'] = lease_json(db.execute('SELECT * FROM leases WHERE task_id=?', (result['id'],)).fetchone())
    # Decisões recentes do próprio pacote acompanham o contexto mesmo depois
    # de saírem da janela global de eventos; nunca anexar cartões alheios.
    decisions = db.execute("""SELECT action,owner,timestamp,details FROM events
                              WHERE task_id=? AND action IN ('approve','ready','review','block','reclaim')
                              ORDER BY sequence DESC LIMIT 5""", (result['id'],)).fetchall()
    result['decisions'] = [{'action': row['action'], 'actor': row['owner'],
                            'timestamp': row['timestamp'], 'details': json.loads(row['details'])}
                           for row in reversed(decisions)]
    return result


def get_task(db, task_id):
    identifier(task_id)
    row = db.execute('SELECT * FROM tasks WHERE id=?', (task_id,)).fetchone()
    if row is None:
        raise CoordinationError(f'Tarefa inexistente: {task_id}')
    return row


def owned_lease(db, task_id, owner, check_branch=False):
    string(owner, 'owner', 200)
    lease = db.execute('SELECT * FROM leases WHERE task_id=?', (task_id,)).fetchone()
    if lease is None or lease['owner'] != owner:
        raise CoordinationError('Operação exige a reserva ativa do próprio owner')
    if check_branch and git_state()['branch'] != lease['branch']:
        raise CoordinationError('Branch mudou desde a reserva; heartbeat/finish recusado. Felipe deve recuperar a reserva explicitamente')
    return lease


def event_list(db, limit):
    if not 1 <= limit <= 1000:
        raise CoordinationError('limit deve estar entre 1 e 1000')
    rows = db.execute('SELECT * FROM events ORDER BY sequence DESC LIMIT ?', (limit,)).fetchall()
    result = []
    for row in rows:
        item = dict(row)
        item['details'] = json.loads(item['details'])
        result.append(item)
    return result


def snapshot(db):
    return {'generated_at': now(),
            'tasks': [task_json(db, row) for row in db.execute('SELECT * FROM tasks ORDER BY id').fetchall()],
            'leases': [lease_json(row) for row in db.execute('SELECT * FROM leases ORDER BY task_id').fetchall()],
            'events': event_list(db, 20)}


def mutate(db, args):
    command = args.command
    task = get_task(db, args.id)
    if command == 'claim':
        string(args.owner, 'owner', 200)
        if task['status'] != 'pronto' or task['mode'] == 'manual':
            raise CoordinationError('Só tarefa pronta read/write pode ser reservada')
        if db.execute('SELECT 1 FROM leases WHERE task_id=?', (args.id,)).fetchone():
            raise CoordinationError('Tarefa já tem reserva')
        if task['mode'] == 'write' and db.execute("SELECT 1 FROM leases JOIN tasks ON tasks.id=leases.task_id WHERE tasks.mode='write'").fetchone():
            raise CoordinationError('Checkout já tem uma tarefa write reservada; a idade não libera a reserva')
        state = git_state()
        stamp = now()
        db.execute('INSERT INTO leases VALUES(?,?,?,?,?,?,?)',
                   (args.id, args.owner, args.executor, state['branch'], state['commit'], stamp, stamp))
        target = 'executando'
        details = {'executor': args.executor, **state}
        actor = args.owner
    elif command == 'archive':
        string(args.summary, 'summary')
        if task['status'] != 'concluido':
            raise CoordinationError('archive exige tarefa concluída')
        if db.execute('SELECT 1 FROM archives WHERE task_id=?', (args.id,)).fetchone():
            raise CoordinationError('Tarefa já está arquivada')
        db.execute('INSERT INTO archives(task_id,summary,archived_at) VALUES(?,?,?)',
                   (args.id, args.summary, now()))
        target, actor, details = task['status'], args.actor, {'summary': args.summary}
    elif command == 'review':
        string(args.note, 'note')
        if task['status'] != 'revisao' or task['mode'] == 'manual':
            raise CoordinationError('review exige tarefa técnica read/write em revisão; validação manual depende de Felipe')
        string(task['summary'], 'summary da entrega')
        string(task['evidence'], 'evidence da entrega')
        if db.execute('SELECT 1 FROM leases WHERE task_id=?', (args.id,)).fetchone():
            raise CoordinationError('Tarefa ainda possui reserva ativa')
        target, actor, details = 'concluido', args.actor, {'note': args.note}
    elif command == 'ready':
        string(args.note, 'note')
        if task['status'] not in ('entrada', 'pesquisa', 'bloqueado'):
            raise CoordinationError('ready exige entrada, pesquisa ou bloqueado')
        if args.actor == 'codex' and task['mode'] != 'read':
            raise CoordinationError('Só Felipe autoriza tarefa write/manual; registre a origem da autorização na nota')
        if db.execute('SELECT 1 FROM leases WHERE task_id=?', (args.id,)).fetchone():
            raise CoordinationError('Tarefa ainda possui reserva ativa')
        target, actor, details = 'pronto', args.actor, {'note': args.note}
    elif command == 'approve':
        string(args.note, 'note')
        transitions = {'decisao': 'pronto', 'revisao': 'concluido'}
        if task['status'] not in transitions:
            raise CoordinationError('approve exige decisão ou revisão')
        if db.execute('SELECT 1 FROM leases WHERE task_id=?', (args.id,)).fetchone():
            raise CoordinationError('Tarefa ainda possui reserva ativa')
        target = transitions[task['status']]
        actor, details = args.actor, {'note': args.note}
    elif command == 'reclaim':
        string(args.reason, 'reason')
        lease = db.execute('SELECT * FROM leases WHERE task_id=?', (args.id,)).fetchone()
        if lease is None:
            raise CoordinationError('Tarefa não possui reserva para recuperar')
        db.execute('DELETE FROM leases WHERE task_id=?', (args.id,))
        target, actor = 'pronto', args.actor
        details = {'reason': args.reason, 'previous_lease': dict(lease)}
    else:
        lease = owned_lease(db, args.id, args.owner, command in ('heartbeat', 'finish'))
        actor = args.owner
        target = task['status']
        if command == 'heartbeat':
            db.execute('UPDATE leases SET heartbeat_at=? WHERE task_id=?', (now(), args.id))
            details = {'branch': lease['branch']}
        else:
            if command == 'finish':
                string(args.summary, 'summary')
                string(args.evidence, 'evidence')
                target = 'revisao'
                details = {'summary': args.summary, 'evidence': args.evidence}
                db.execute('UPDATE tasks SET summary=?,evidence=? WHERE id=?', (args.summary, args.evidence, args.id))
            else:
                string(args.reason, 'reason')
                target = 'bloqueado' if command == 'block' else 'pronto'
                details = {'reason': args.reason}
            details['released_lease'] = dict(lease)
            db.execute('DELETE FROM leases WHERE task_id=?', (args.id,))
    db.execute('UPDATE tasks SET status=?,updated_at=? WHERE id=?', (target, now(), args.id))
    event(db, args.id, command, actor, {'from': task['status'], 'to': target, **details})
    return task_json(db, get_task(db, args.id))


def parser():
    result = argparse.ArgumentParser(description=__doc__)
    result.add_argument('--db', type=Path, default=ROOT / '.coordination' / 'state.sqlite3')
    commands = result.add_subparsers(dest='command', required=True)
    commands.add_parser('init')
    commands.add_parser('list').add_argument('--all', action='store_true')
    for name in ('show', 'context', 'add', 'claim', 'heartbeat', 'finish', 'block', 'approve', 'release', 'reclaim', 'ready', 'archive', 'review'):
        command = commands.add_parser(name)
        command.add_argument('id')
        if name == 'add':
            for key in ('title', 'context', 'acceptance'):
                command.add_argument('--' + key, required=True)
            command.add_argument('--mode', choices=MODES, required=True)
            command.add_argument('--status', choices=INITIAL_STATUSES, required=True)
            for key in ('refs', 'files'):
                command.add_argument('--' + key, action='append', default=[])
        if name in ('claim', 'heartbeat', 'finish', 'block', 'release'):
            command.add_argument('--owner', required=True)
        if name == 'claim':
            command.add_argument('--executor', choices=('codex', 'claude-work'), required=True)
        if name == 'finish':
            command.add_argument('--summary', required=True)
            command.add_argument('--evidence', required=True)
        if name in ('block', 'release', 'reclaim'):
            command.add_argument('--reason', required=True)
        if name in ('approve', 'reclaim'):
            command.add_argument('--actor', choices=('felipe',), required=True)
        if name in ('archive', 'review'):
            command.add_argument('--actor', choices=('codex',), required=True)
        if name == 'archive':
            command.add_argument('--summary', required=True)
        if name == 'ready':
            command.add_argument('--actor', choices=('codex', 'felipe'), required=True)
        if name in ('approve', 'ready', 'review'):
            command.add_argument('--note', required=True)
    command = commands.add_parser('events')
    command.add_argument('--limit', type=int, default=20)
    command = commands.add_parser('export')
    command.add_argument('--output', type=Path)
    return result


def execute(db, args):
    command = args.command
    if command == 'init':
        seed_path = ROOT / 'coordination' / 'seed.json'
        seed = json.loads(seed_path.read_text(encoding='utf-8')) if seed_path.exists() else []
        if not isinstance(seed, list):
            raise CoordinationError('seed.json exige array de tarefas')
        seed = [validate_task(item, seed=True) for item in seed]
        if len({item['id'] for item in seed}) != len(seed):
            raise CoordinationError('seed.json contém IDs duplicados')
        with transaction(db):
            schema(db)
            added = []
            for item in seed:
                if not db.execute('SELECT 1 FROM tasks WHERE id=?', (item['id'],)).fetchone():
                    add_task(db, item, 'coordination/seed.json')
                    added.append(item['id'])
            if added:
                event(db, None, 'init', None, {'seed': 'coordination/seed.json', 'added': added})
        return {'db': str(args.db), 'added': added}
    if command == 'add':
        data = validate_task(vars(args))
        with transaction(db):
            if db.execute('SELECT 1 FROM tasks WHERE id=?', (args.id,)).fetchone():
                raise CoordinationError('ID já existe; tarefa existente não foi sobrescrita')
            add_task(db, data, 'CLI add')
            return task_json(db, get_task(db, args.id))
    if command in ('claim', 'heartbeat', 'finish', 'block', 'approve', 'release', 'reclaim', 'ready', 'archive', 'review'):
        with transaction(db):
            return mutate(db, args)
    with transaction(db, write=False):
        if command == 'list':
            query = '''SELECT tasks.id,tasks.title,tasks.mode,tasks.status FROM tasks
                       LEFT JOIN archives ON tasks.id=archives.task_id'''
            if not args.all:
                query += " WHERE tasks.status != 'concluido' AND archives.task_id IS NULL"
            query += ' ORDER BY tasks.id'
            summaries = []
            for row in db.execute(query).fetchall():
                item = dict(row)
                lease = db.execute('SELECT owner,executor,branch FROM leases WHERE task_id=?', (item['id'],)).fetchone()
                item['lease'] = dict(lease) if lease else None
                summaries.append(item)
            return {'tasks': summaries}
        if command == 'events':
            return {'events': event_list(db, args.limit)}
        if command == 'export':
            data = snapshot(db)
        else:
            data = task_json(db, get_task(db, args.id))
            if command == 'context':
                data = {'task': data, 'checkout': {'root': str(ROOT), **git_state()},
                        'next_steps': [
                            'Leia apenas as referências e arquivos desta tarefa; contexto e aceite estão acima.',
                            'Decisões de gameplay abertas exigem perguntar ao Felipe antes de implementar.',
                            'Só execute trabalho após claim de tarefa pronta read/write; manual depende do Felipe.',
                            'Use heartbeat durante a execução; branch deve permanecer a mesma da reserva.',
                            'Ao terminar, use finish com resumo e evidências; Codex usa review para entregas técnicas e Felipe usa approve para decisões/validações manuais.',
                            'Reservas não expiram: Felipe usa reclaim explícito para reservas órfãs.']}
    if command == 'export' and args.output:
        if args.output.resolve() == args.db.resolve():
            raise CoordinationError('output não pode sobrescrever o banco')
        args.output.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    return data


def main():
    args = parser().parse_args()
    db = None
    try:
        db = connect(args.db, args.command == 'init')
        print(json.dumps(execute(db, args), ensure_ascii=False, indent=2))
        return 0
    except (CoordinationError, sqlite3.Error, OSError, ValueError, TypeError) as exc:
        print(json.dumps({'error': str(exc)}, ensure_ascii=False), file=sys.stderr)
        return 1
    finally:
        if db is not None:
            db.close()


if __name__ == '__main__':
    sys.exit(main())
