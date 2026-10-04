"""Integração HTTP local sem iniciar modelos reais nem alterar o banco do projeto."""
import importlib.util
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import threading
import time
import unittest
from unittest.mock import patch, Mock
from urllib.error import HTTPError
from urllib.request import Request, urlopen

ROOT = Path(__file__).resolve().parent.parent
spec = importlib.util.spec_from_file_location('kanban_server', ROOT/'tools/coordination_server.py')
server_module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(server_module)


class ServerContractTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.db = str(Path(self.temp.name)/'state.sqlite3')
        def cli(*args):
            result = subprocess.run([sys.executable, str(ROOT/'tools/coordination.py'),
                                     '--db', self.db, *args], capture_output=True, text=True)
            if result.returncode:
                raise ValueError(json.loads(result.stderr)['error'])
            return json.loads(result.stdout)
        self.cli = cli
        self.patch = patch.object(server_module, 'cli', cli)
        self.patch.start()
        cli('init')
        server_module.RUNS.clear()
        self.server = server_module.ThreadingHTTPServer(('127.0.0.1', 0), server_module.Handler)
        self.origin = f'http://127.0.0.1:{self.server.server_port}'
        self.thread = threading.Thread(target=self.server.serve_forever, daemon=True)
        self.thread.start()

    def tearDown(self):
        self.server.shutdown()
        self.server.server_close()
        self.thread.join()
        server_module.RUNS.clear()
        self.patch.stop()
        self.temp.cleanup()

    def request(self, path, data=None, headers=None):
        base = {'Origin': self.origin, 'X-Coordinator-Token': server_module.TOKEN}
        if headers:
            base.update(headers)
        request = Request(self.origin+path, data=json.dumps(data).encode() if data else None,
                          headers=base)
        try:
            response = urlopen(request, timeout=5)
        except HTTPError as exc:
            response = exc
        with response:
            body = response.read()
            return response.status, body, response.headers

    def add(self, id='TEST', mode='read'):
        self.cli('add', id, '--title', 'Teste', '--mode', mode, '--status', 'pronto',
                 '--context', 'Teste de contrato', '--acceptance', 'Não executar modelos reais')

    def test_page_has_csp_and_no_external_resources(self):
        code, body, headers = self.request('/')
        self.assertEqual(code, 200)
        self.assertIn("frame-ancestors 'none'", headers['Content-Security-Policy'])
        self.assertIn(server_module.TOKEN.encode(), body)
        self.assertNotIn(b'__TOKEN__', body)

    def test_board_reads_real_shared_state(self):
        self.add()
        code, body, _ = self.request('/api/board')
        self.assertEqual(code, 200)
        data = json.loads(body)
        self.assertTrue(any(t['id'] == 'TEST' for t in data['tasks']))
        self.assertEqual(data['sessions'], [])

    def test_wrong_origin_token_and_host_cannot_mutate(self):
        self.add()
        for headers in [{'Origin': 'https://untrusted.invalid'},
                        {'X-Coordinator-Token': 'wrong'}, {'Host': 'untrusted.invalid'}]:
            with self.subTest(headers=headers):
                code, _, _ = self.request('/api/launch', {'task_id': 'TEST', 'executor': 'codex'}, headers)
                self.assertEqual(code, 403)
        self.assertIsNone(self.cli('show', 'TEST')['lease'])

    def test_inbox_task_is_not_automatically_authorized(self):
        code, body, _ = self.request('/api/task', {'title': 'Pedido', 'context': 'Contexto', 'acceptance': 'Critério'})
        self.assertEqual(code, 200)
        task = json.loads(body)
        self.assertEqual(task['status'], 'entrada')
        self.assertIsNone(task['lease'])

    def test_finished_session_cannot_receive_new_work_without_lease(self):
        self.add()
        session = Mock()
        session.task = 'TEST'
        session.id = 'session-test'
        session.master = 999
        session.process.poll.return_value = None
        server_module.RUNS[session.id] = session
        with patch.object(server_module.os, 'write') as write:
            code, _, _ = self.request('/api/input', {'session_id': session.id, 'text': 'Altere outro arquivo'})
            self.assertEqual(code, 400)
            write.assert_not_called()

    def test_active_terminal_cannot_be_reclaimed(self):
        self.add()
        self.cli('claim', 'TEST', '--owner', 'session-test', '--executor', 'codex')
        session = Mock()
        session.task = 'TEST'
        session.process.poll.return_value = None
        server_module.RUNS['session-test'] = session
        code, _, _ = self.request('/api/reclaim', {'task_id': 'TEST', 'note': 'Ainda rodando'})
        self.assertEqual(code, 400)
        self.assertEqual(self.cli('show', 'TEST')['lease']['owner'], 'session-test')

    def test_wrong_claude_account_does_not_reserve_or_launch(self):
        self.add()
        task = self.cli('show', 'TEST')
        with patch.object(server_module, 'cli', return_value=task) as calls, \
             patch.object(server_module.shutil, 'which', return_value=sys.executable), \
             patch.object(server_module.subprocess, 'run', return_value=Mock(returncode=0, stdout='{"email":"pessoal@example.invalid"}')), \
             patch.object(server_module, 'Session') as launcher:
            with self.assertRaises(ValueError):
                server_module.launch('TEST', 'claude-work')
            self.assertEqual(calls.call_count, 1)
            launcher.assert_not_called()

    def test_real_pty_round_trip_without_an_ai_process(self):
        self.add()
        owner = 'pty-test'
        self.cli('claim', 'TEST', '--owner', owner, '--executor', 'codex')
        session = server_module.Session('TEST', 'codex', owner,
                                       [sys.executable, '-u', '-c', "import os; print('READY'); print('COLS:'+str(os.get_terminal_size().columns)); print('ECHO:'+input())"])
        server_module.RUNS[owner] = session
        deadline = time.monotonic()+5
        while 'READY' not in session.snapshot()['output'] and time.monotonic() < deadline:
            time.sleep(.05)
        code, _, _ = self.request('/api/input', {'session_id': owner, 'text': 'teste'})
        self.assertEqual(code, 200)
        while session.snapshot()['exit_code'] is None and time.monotonic() < deadline:
            time.sleep(.05)
        self.assertIn('ECHO:teste', session.snapshot()['output'])
        self.assertIn('COLS:110', session.snapshot()['output'])
        deadline = time.monotonic()+5
        while self.cli('show', 'TEST')['status'] == 'executando' and time.monotonic() < deadline:
            time.sleep(.05)
        self.assertEqual(self.cli('show', 'TEST')['status'], 'bloqueado')


if __name__ == '__main__':
    unittest.main()
