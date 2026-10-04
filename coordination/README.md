# Quadro e processo local

Abrir: `python3 tools/coordination_server.py --port 8766` e
`http://127.0.0.1:8766/`. Python 3 e SQLite da biblioteca padrão; sem instalação.
O link publicado no Sites continua sendo uma cópia, não o estado local.

O painel lê o banco real, recebe pedidos na Entrada e mostra decisões, reservas,
resultados e bloqueios. Só cartões prontos iniciam sessões. O botão abre uma CLI
real num terminal local supervisionado; não é um link para o aplicativo desktop.
O terminal aparece abaixo do quadro para acompanhar e responder perguntas/permissões.
Fechar a aba não fecha o servidor. Parar o servidor encerra seus terminais;
reservas órfãs após uma falha permanecem visíveis para recuperação explícita.

## Sessões abertas por você

Abra Claude Code ou Codex neste checkout atualizado. As entradas `CLAUDE.md` e
`AGENTS.md` mandam seguir START, listar o estado e reservar antes de editar.
Sessões já abertas precisam receber: “Leia coordination/START.md e registre sua
tarefa antes de continuar; não troque de branch nem escreva enquanto houver outro dono.”
O painel mostra a reserva de sessões manuais, mas seus terminais continuam no aplicativo
em que você as abriu. Não há descoberta automática de conversas antigas.

## Comandos de agente

```sh
python3 tools/coordination.py init
python3 tools/coordination.py list
python3 tools/coordination.py context ID
python3 tools/coordination.py claim ID --owner IDENTIFICADOR_UNICO --executor claude-work
python3 tools/coordination.py heartbeat ID --owner IDENTIFICADOR_UNICO
python3 tools/coordination.py finish ID --owner IDENTIFICADOR_UNICO --summary 'resultado' --evidence 'comando e resultado dos checks, limites e próximo passo'
python3 tools/coordination.py block ID --owner IDENTIFICADOR_UNICO --reason 'pergunta para Felipe ou impedimento'
```

Codex cria pacotes usando `add --help` e o template. `ready` exige nota de
autorização; Codex pode preparar leitura, Felipe autoriza escrita/validação.
`review --actor codex --note ...` fecha entrega técnica verificada;
`approve --actor felipe --note ...` registra decisões e validações manuais.
`archive --actor codex --summary ...` resume as concluídas sem apagar evidência.
`events` dá os últimos registros; `show ID` abre detalhes; `export` gera a fotografia completa.

## Recuperação

Pare primeiro a sessão antiga. Felipe usa `reclaim ID --actor felipe --reason ...`
após verificar que ela não está trabalhando. Reservas não expiram por tempo.
`ready ID --actor felipe --note ...` reabre pacote bloqueado após resolver seu impedimento.
Faça backup de `.coordination/state.sqlite3` com o servidor parado ou usando o
backup SQLite; não versionar DB, prompts de sessão ou logs. Estado está limitado a
este computador e checkout; outras cópias do repo precisam de outro mecanismo compartilhado.

É coordenação cooperativa: o protocolo não bloqueia um editor/programa que ignore
as regras e não substitui o sandbox/permissões de cada agente. Felipe continua
decidindo gameplay; agentes não usam `--actor felipe` para inventar aprovação.
Nenhum launcher troca conta automaticamente ou desativa permissões.

## Verificação do processo

`python3 -m unittest discover -s tools -p 'test_coordination*.py' -v` verifica
concorrência, estados, handoff, endpoints e terminal simulado sem gastar modelos.
Checks do mod continuam `./gradlew build` e `./gradlew runGameTestServer`.
Depois de marcos, avaliar contexto lido, repetições e tempo nos eventos, propor
mudança ao Felipe e atualizar o processo. Não carregar todo o histórico para isso.
