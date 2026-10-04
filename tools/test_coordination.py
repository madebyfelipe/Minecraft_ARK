#!/usr/bin/env python3
"""Contrato público da coordenação: processos reais e banco isolado por teste.

Executar da raiz: python3 -m unittest discover -s tools -p test_coordination.py -v
Não abre nem modifica o banco de trabalho do projeto.
"""

import concurrent.futures
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import threading
import unittest


CLI = Path(__file__).resolve().with_name("coordination.py")


class CoordinationContractTest(unittest.TestCase):
    def test_review_archive_and_short_list_preserve_evidence(self):
        self.add('ARCHIVE', mode='read')
        self.claim('ARCHIVE')
        self.finish('ARCHIVE')
        self.command('review', 'ARCHIVE', '--actor', 'codex', '--note', 'Verificação independente')
        self.command('archive', 'ARCHIVE', '--actor', 'codex', '--summary', 'Conclusão curta')
        task = self.command('show', 'ARCHIVE')
        self.assertTrue(task['archived'])
        self.assertEqual(task['summary'], 'Trabalho entregue')
        self.assertEqual(task['evidence'], 'Verificação passou')
        self.assertFalse(any(t['id'] == 'ARCHIVE' for t in self.command('list')['tasks']))
        row = next(t for t in self.command('list', '--all')['tasks'] if t['id'] == 'ARCHIVE')
        self.assertNotIn('context', row)
        self.assertNotIn('evidence', row)
        self.rejected('claim', 'ARCHIVE', '--owner', 'outra', '--executor', 'codex')
        self.command('init')
        self.assertTrue(self.command('show', 'ARCHIVE')['archived'])

    def test_coordinator_cannot_authorize_write_or_review_manual(self):
        self.add('WRITE-DECISION', mode='write', status='entrada')
        self.rejected('ready', 'WRITE-DECISION', '--actor', 'codex', '--note', 'Sem decisão do Felipe')
        self.add('READ-DECISION', mode='read', status='pesquisa')
        self.command('ready', 'READ-DECISION', '--actor', 'codex', '--note', 'Pesquisa delimitada')
        self.assertEqual(self.command('show', 'READ-DECISION')['status'], 'pronto')
        packet = self.command('context', 'READ-DECISION')
        self.assertIn('Pesquisa delimitada', json.dumps(packet, ensure_ascii=False))
        self.assertNotIn('WRITE-DECISION', json.dumps(packet, ensure_ascii=False))
        self.rejected('review', 'MC-02', '--actor', 'codex', '--note', 'Não substitui validação manual')
        self.rejected('archive', 'READ-DECISION', '--actor', 'codex', '--summary', 'Ainda não concluída')

    def setUp(self):
        self.directory = tempfile.TemporaryDirectory(prefix="ias-coordination-test-")
        self.addCleanup(self.directory.cleanup)
        self.db = Path(self.directory.name) / "coordination.sqlite3"
        self.command("init")

    def run_command(self, *arguments):
        return subprocess.run(
            [sys.executable, str(CLI), "--db", str(self.db), *arguments],
            capture_output=True, text=True, timeout=20,
        )

    def command(self, *arguments):
        result = self.run_command(*arguments)
        self.assertEqual(result.returncode, 0, result.stderr or result.stdout)
        try:
            return json.loads(result.stdout)
        except json.JSONDecodeError:
            self.fail(f"Resposta não é JSON válido: {result.stdout!r}")

    def rejected(self, *arguments):
        result = self.run_command(*arguments)
        self.assertNotEqual(result.returncode, 0, result.stdout)
        self.assertTrue(result.stderr.strip(), "Erro deve explicar a falha em stderr")

    def add(self, identifier, mode="write", status="pronto", context=None):
        self.command(
            "add", identifier, "--title", f"Tarefa {identifier}",
            "--mode", mode, "--status", status,
            "--context", context or f"Contexto privado {identifier}",
            "--acceptance", f"Critério verificável {identifier}",
            "--refs", f"docs/{identifier}.md", "--files", f"src/{identifier}.java",
        )

    def claim(self, identifier, owner="sessao-a", executor="codex"):
        self.command("claim", identifier, "--owner", owner, "--executor", executor)

    def finish(self, identifier, owner="sessao-a"):
        self.command("finish", identifier, "--owner", owner,
                     "--summary", "Trabalho entregue", "--evidence", "Verificação passou")

    def exported(self):
        result = self.command("export")
        self.assertIsInstance(result, dict)
        self.assertIsInstance(result["tasks"], list)
        self.assertIsInstance(result["leases"], list)
        # Campos derivados do relógio não descrevem mutações persistidas.
        # Conserva timestamps do banco e todos os demais campos para comparar
        # o estado antes/depois de um comando rejeitado ou init repetido.
        def persisted(value):
            if isinstance(value, dict):
                return {key: persisted(item) for key, item in value.items()
                        if key not in {"generated_at", "heartbeat_age_seconds"}}
            if isinstance(value, list):
                return [persisted(item) for item in value]
            return value

        return persisted(result)

    def status(self, identifier):
        return next(task["status"] for task in self.exported()["tasks"]
                    if task["id"] == identifier)

    def simultaneous_claims(self, requests):
        # A barreira sincroniza o lançamento de processos independentes, sem
        # compartilhar conexão SQLite nem chamar funções internas da ferramenta.
        barrier = threading.Barrier(len(requests))

        def compete(request):
            identifier, owner = request
            barrier.wait(timeout=10)
            return self.run_command("claim", identifier, "--owner", owner,
                                    "--executor", "codex")

        with concurrent.futures.ThreadPoolExecutor(max_workers=len(requests)) as pool:
            return list(pool.map(compete, requests))

    def test_same_task_has_exactly_one_owner_in_competing_processes(self):
        self.add("unica")
        results = self.simultaneous_claims([("unica", "a"), ("unica", "b")])
        self.assertEqual(sum(result.returncode == 0 for result in results), 1)
        self.assertEqual(len(self.exported()["leases"]), 1)
        for result in results:
            if result.returncode:
                self.assertTrue(result.stderr.strip())

    def test_independent_write_tasks_share_global_exclusion(self):
        self.add("escrita-a")
        self.add("escrita-b")
        results = self.simultaneous_claims([("escrita-a", "a"), ("escrita-b", "b")])
        self.assertEqual(sum(result.returncode == 0 for result in results), 1)
        self.assertEqual(len(self.exported()["leases"]), 1)

    def test_independent_read_tasks_can_be_claimed_concurrently(self):
        self.add("leitura-a", mode="read")
        self.add("leitura-b", mode="read")
        results = self.simultaneous_claims([("leitura-a", "a"), ("leitura-b", "b")])
        self.assertTrue(all(result.returncode == 0 for result in results),
                        [result.stderr for result in results])
        self.assertEqual(len(self.exported()["leases"]), 2)

    def test_read_lease_can_coexist_with_write_lease(self):
        self.add("escrita")
        self.add("leitura", mode="read")
        self.claim("escrita")
        self.claim("leitura", owner="sessao-b", executor="claude-work")
        self.assertEqual(len(self.exported()["leases"]), 2)

    def test_wrong_owner_cannot_change_or_end_lease(self):
        self.add("posse")
        self.claim("posse")
        before = self.exported()
        commands = [
            ("heartbeat", "posse", "--owner", "outra-sessao"),
            ("finish", "posse", "--owner", "outra-sessao", "--summary", "Fim",
             "--evidence", "Testado"),
            ("block", "posse", "--owner", "outra-sessao", "--reason", "Impedimento"),
            ("release", "posse", "--owner", "outra-sessao", "--reason", "Devolver"),
        ]
        for arguments in commands:
            with self.subTest(command=arguments[0]):
                self.rejected(*arguments)
                self.assertEqual(self.exported(), before)
        self.command("heartbeat", "posse", "--owner", "sessao-a")

    def test_owner_argument_is_required(self):
        self.add("posse")
        self.claim("posse")
        for arguments in [("heartbeat", "posse"),
                          ("finish", "posse", "--summary", "Fim", "--evidence", "Testado"),
                          ("block", "posse", "--reason", "Impedimento"),
                          ("release", "posse", "--reason", "Devolver")]:
            with self.subTest(command=arguments[0]):
                self.rejected(*arguments)
        self.assertEqual(len(self.exported()["leases"]), 1)

    def test_only_ready_tasks_can_be_claimed(self):
        for state in ["entrada", "pesquisa", "decisao"]:
            with self.subTest(state=state):
                self.add(state, mode="read", status=state)
                self.rejected("claim", state, "--owner", "a", "--executor", "codex")
                self.assertEqual(self.status(state), state)
        self.assertEqual(self.exported()["leases"], [])

    def test_manual_task_cannot_be_claimed_even_when_ready(self):
        self.add("manual", mode="manual")
        for executor in ["codex", "claude-work"]:
            with self.subTest(executor=executor):
                self.rejected("claim", "manual", "--owner", "a", "--executor", executor)
        self.assertEqual(self.status("manual"), "pronto")
        self.assertEqual(self.exported()["leases"], [])

    def test_finish_requires_nonempty_summary_and_evidence(self):
        self.add("entrega")
        self.claim("entrega")
        before = self.exported()
        invalid = [[], ["--summary", "Fim"], ["--evidence", "Testado"],
                   ["--summary", "", "--evidence", "Testado"],
                   ["--summary", "Fim", "--evidence", ""],
                   ["--summary", "  ", "--evidence", "Testado"],
                   ["--summary", "Fim", "--evidence", "\t "]]
        for extra in invalid:
            with self.subTest(extra=extra):
                self.rejected("finish", "entrega", "--owner", "sessao-a", *extra)
                self.assertEqual(self.exported(), before)

    def test_finish_releases_lease_and_waits_for_felipe_review(self):
        self.add("entrega")
        self.claim("entrega")
        self.finish("entrega")
        self.assertEqual(self.status("entrega"), "revisao")
        self.assertEqual(self.exported()["leases"], [])
        self.rejected("claim", "entrega", "--owner", "b", "--executor", "codex")
        self.rejected("approve", "entrega", "--actor", "codex", "--note", "Aprovado")
        self.assertEqual(self.status("entrega"), "revisao")
        self.command("approve", "entrega", "--actor", "felipe", "--note", "Conferido")
        self.assertEqual(self.status("entrega"), "concluido")
        self.rejected("claim", "entrega", "--owner", "b", "--executor", "codex")

    def test_felipe_approves_decision_to_ready_not_completed(self):
        self.add("escolha", status="decisao")
        self.command("approve", "escolha", "--actor", "felipe", "--note", "Decisão tomada")
        self.assertEqual(self.status("escolha"), "pronto")
        self.claim("escolha", executor="claude-work")

    def test_approval_cannot_skip_entry_or_blocked_tasks(self):
        self.add("entrada", status="entrada")
        self.add("bloqueio")
        self.claim("bloqueio")
        self.command("block", "bloqueio", "--owner", "sessao-a", "--reason", "Falta decisão")
        self.assertEqual(self.status("bloqueio"), "bloqueado")
        self.assertEqual(self.exported()["leases"], [])
        for identifier in ["entrada", "bloqueio"]:
            with self.subTest(identifier=identifier):
                before = self.status(identifier)
                self.rejected("approve", identifier, "--actor", "felipe", "--note", "Pular")
                self.assertEqual(self.status(identifier), before)

    def test_release_returns_to_ready_and_allows_new_owner(self):
        self.add("devolvida")
        self.claim("devolvida")
        self.command("release", "devolvida", "--owner", "sessao-a", "--reason", "Pausa")
        self.assertEqual(self.status("devolvida"), "pronto")
        self.assertEqual(self.exported()["leases"], [])
        self.claim("devolvida", owner="sessao-b")

    def test_only_felipe_can_reclaim_with_an_explicit_reason(self):
        self.add("orfa")
        self.claim("orfa")
        before = self.exported()
        invalid = [
            ["--actor", "codex", "--reason", "Recuperar"],
            ["--actor", "claude-work", "--reason", "Recuperar"],
            ["--actor", "felipe"],
            ["--actor", "felipe", "--reason", "  "],
        ]
        for extra in invalid:
            with self.subTest(extra=extra):
                self.rejected("reclaim", "orfa", *extra)
                self.assertEqual(self.exported(), before)
        self.command("reclaim", "orfa", "--actor", "felipe", "--reason", "Sessão encerrada")
        self.assertEqual(self.status("orfa"), "pronto")
        self.assertEqual(self.exported()["leases"], [])
        self.claim("orfa", owner="nova-sessao")

    def test_init_is_idempotent_and_preserves_task_and_active_lease(self):
        self.add("persistente")
        self.claim("persistente", executor="claude-work")
        before = self.exported()
        self.command("init")
        self.command("init")
        self.assertEqual(self.exported(), before)
        self.rejected("claim", "persistente", "--owner", "outra", "--executor", "codex")
        self.command("heartbeat", "persistente", "--owner", "sessao-a")

    def test_duplicate_add_cannot_overwrite_existing_task(self):
        self.add("original")
        before = self.exported()
        self.rejected("add", "original", "--title", "Sobrescrever", "--mode", "read",
                      "--status", "entrada", "--context", "Mudado", "--acceptance", "Mudado")
        self.assertEqual(self.exported(), before)

    def test_context_contains_only_requested_card(self):
        self.add("selecionada", context="MARCADOR_CONTEXTO_SELECIONADO")
        self.add("outra", context="MARCADOR_CONTEXTO_OUTRO")
        result = self.run_command("context", "selecionada")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("MARCADOR_CONTEXTO_SELECIONADO", result.stdout)
        self.assertIn("Critério verificável selecionada", result.stdout)
        self.assertNotIn("MARCADOR_CONTEXTO_OUTRO", result.stdout)
        self.assertNotIn("Critério verificável outra", result.stdout)

    def test_export_and_read_commands_are_valid_json(self):
        self.add("inspecao", mode="read")
        self.claim("inspecao")
        exported = self.exported()
        cards = [card for card in exported["tasks"] if card["id"] == "inspecao"]
        self.assertEqual(len(cards), 1)
        self.assertEqual(len(exported["leases"]), 1)
        self.command("list")
        self.command("show", "inspecao")
        self.command("events")


if __name__ == "__main__":
    unittest.main()
