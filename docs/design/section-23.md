## 23. Roadmap

| Etapa | Conteúdo | Estado |
|---|---|---|
| 0 | Pesquisa e este documento | ✅ 2026-09-30 |
| 1 | Workspace, Gradle, build, runServer | ✅ 2026-09-30 (`runClient` ainda não verificado) |
| 2 | Core: níveis, atributos, ownership, persistência, registry de espécies | ✅ 2026-09-30 |
| 3 | Domesticação com criatura de teste | ✅ 2026-09-30 (falta conferir no cliente) |
| 4 | Smilodon | ✅ 2026-09-30, conferido em jogo pelo Felipe |
| 5 | Mais criaturas, spawning | 🟡 Estegossauro, Pteranodonte, dodô e Elasmotério (início de jogo), migração de herbívoros, bônus de bando do Alossauro e ecologia dinâmica (fome, caçadas, estresse, disputas territoriais entre machos da mesma espécie — D24) implementados; caçador abandona presa quando uma investida mostra desvantagem; interações cobertas por GameTests; bandos com identidade e território (D26) e espreita de todo carnívoro (D27); falta conferir comportamento em jogo; Kelenken e a marca de cada caçador (D28), força pelo porte real, troféu de apex e desafio (D30); Ornitholestes e os hábitos por espécie — horário, camuflagem, carniça, sentinela (D34); Baryonyx pescador, anfíbio e montaria que nada, com modelo do Jurassic Reborn (D35–D37); spawn em todo bioma com o ideal ×3 (D41); Quetzalcoatlus, que engole a presa, decola num salto e sobe nas térmicas (D42); Megalania com peçonha, rastro e antídoto (D43); quebra de blocos por espécie (D44); carcaça (D45); sono pesado, ameaça maior, caça do solitário e spawn justo (D46); Anquilossauro com a clava da cauda, perna quebrada, duelo de flanco, mineração montada e esterco (D48) |
| 6 | Temperatura | ✅ 2026-09-30 em testes automáticos; falta sentir o frio em jogo e balancear a primeira hora |
| 7 | Montaria | 🟡 armazenamento por espécie, voo no estilo do Cobblemon, pulo sem barra de carga, montaria sem sela e More Hitboxes implementados; build e GameTests passam, falta conferir controles e hitboxes em jogo; slot de sela no inventário, fôlego de voo por nível e bico que segue o voo (D31); custos de voo por espécie, decolagem por salto e térmicas (D42); voo −40%, mergulho só íngreme e montaria voadora sem dano de queda nem sufocação (D50) |
| 8 | Reprodução e genética | ✅ 2026-09-30 em testes automáticos (genética em JUnit; acasalamento, gestação, ovo, incubadora, mesa química e estimulante em gametests); falta conferir em jogo |
| 9 | Worldgen | ✅ 2026-09-30 em teste automático (os biomas possíveis do preset são todos frios); falta criar um mundo e andar por ele |
| 10 | Endgame: base militar, postos com notas, apex secreto | 🟡 2026-10-04: postos (torre e complexo), base com a contenção, séries de registros e dossiê (D52, D53); Titanovenator como boss (D54), luta que exige mutação, console da contenção, holograma e recompensa nova (D57); falta sentir em jogo |

Revisão editorial do Analisador (2026-10-03): manual e fichas de `tools/wiki_lore/` revisados para uma leitura mais natural, preservando a voz do sobrevivente e o conteúdo da lore.

**Analisador permanente** (D56, 2026-10-04): slot próprio no inventário, tecla que traz o aparelho à mão, e mais lore nas NOTAS; falta sentir a tecla em jogo.

MVP = Etapas 1–4 + versão mínima de 6, 7 e 9 (mundo frio, temperatura básica, montar o Smilodon).

**Titanovenator** (estudo visual e boss, D54): o estudo local sobre o Rex do Revival (`art/titanovenator/README.md`)
virou o boss do endgame, com o modelo derivado em runtime; falta sentir em jogo.


## Processo de trabalho (2026-10-04)

Coordenação local compartilhada implantada em `coordination/`: Codex coordena e integra,
Claude trabalho executa com Opus 5.5. SQLite reserva tarefas atomicamente: uma escrita
por checkout, leituras paralelas; pesquisa → decisão → pronto → execução → revisão.
Felipe mantém decisões e validação manual; Codex revisa entregas técnicas.
Entradas curtas e design por seção; histórico preservado e consultado sob demanda.
Estudo do Titanovenator continua na branch própria, sem integração neste pacote.
