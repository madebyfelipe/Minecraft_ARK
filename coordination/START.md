# Entrada de qualquer sessão

1. Leia `coordination/PROJECT.md`. Use `python3 tools/coordination.py init` e `list`.
   O estado fica em `.coordination/`, fora do Git; é comum às sessões neste checkout.
2. Se o pedido não tem cartão, Codex cria um pacote com objetivo, arquivos permitidos,
   referências e critério de conclusão. Pesquisa → decisão do Felipe → pronto.
   Não converter decisões em aprovação automática. Pedido claro do Felipe permite
   Codex criar cartão pronto diretamente, registrando a origem dessa autorização.
3. Escolha identificador único da sessão (`CODEX_THREAD_ID` ou UUID, nunca só "Claude").
   Leia `context ID`. Antes de editar, reserve:
   `python3 tools/coordination.py claim ID --owner SESSAO --executor codex` (ou `claude-work`).
   Se o cartão já foi reservado pelo lançador, use o dono informado no prompt; não reserve de novo.
4. Uma tarefa de escrita ocupa o checkout inteiro; pesquisa/revisão de leitura pode
   coexistir. Confira a branch registrada; não trocar branch ou fazer operações Git
   enquanto outra tarefa escreve. Leitura observa o trabalho em andamento, não um snapshot.
   Renove `heartbeat ID --owner SESSAO` nas transições e durante trabalhos longos.
5. Leia só os arquivos e referências do cartão. Subagentes recebem um pacote curto,
   autocontido, uma função, arquivos sem sobreposição e os critérios; nunca todo o histórico.
   Codex é responsável pela reserva de escrita enquanto seus subagentes trabalham.
6. Registre saída: `finish ID --owner SESSAO --summary 'o que mudou' --evidence 'checks e limites'`.
   Isso vai para revisão, não concluído. Codex verifica entregas técnicas e usa `review ID --actor codex --note 'checks verificados'`.
   Decisões de gameplay e validação manual continuam com o Felipe (`approve`).
   Use `block ... --reason 'pergunta ou impedimento'` se faltar informação.

Reservas não expiram sozinhas. Sessão interrompida: Felipe verifica se parou e usa
`reclaim ID --actor felipe --reason 'motivo'`; nunca furtar uma reserva por idade.
Nenhum lock impede programas que ignoram o protocolo: sessões já abertas precisam
receber estas instruções e registrar seu trabalho antes de coexistirem com novas sessões.

## Handoff e economia de contexto

O cartão registra resultado, evidência e próximo passo; eventos guardam detalhes.
Começar outra sessão não exige copiar a conversa inteira. Não repetir dumps ou logs:
resuma falha, comando, localização do detalhe e decisão necessária.
Depois de marcos, Codex revisa desperdício de contexto e propõe ajuste do processo ao Felipe.
Use `archive ID --actor codex --summary 'resultado e decisão válidos'` nas concluídas;
`list` mostra só o resumo de tarefas ativas. Preserve evidência e decisões, nunca apagar trabalho.
Regras completas de integração/licença/testes: `coordination/PROCESS.md`, sob demanda.
