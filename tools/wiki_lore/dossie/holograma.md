# Holograma comparativo: Tiranossauro × Titanovenator

Especificação para o cartão da base e da luta (decisão do Felipe em 2026-10-04: por enquanto só
especificar; o Titanovenator ainda não é criatura do jogo).

## Quando aparece

Na aba do Analisador onde fica o dossiê (`tools/wiki_lore/dossie/01-titanovenator.txt`), destravado
quando o Titanovenator morre. Antes disso, a entrada fica trancada ("???"), como as fichas da DINO
FILE.

## O que mostra

- Os dois modelos lado a lado, na mesma plataforma de holograma da DINO FILE
  (`AnalyzerScreen.renderViewport`), na **mesma escala**: a diferença de tamanho tem que ser a real
  dos modelos, sem encaixar cada um no próprio quadro.
- Os dois girando juntos, devagar, com o mesmo ângulo, para comparar o perfil.
- Grade de fundo com marcas de altura, para dar a medida sem número.
- Rótulos embaixo de cada um: TIRANOSSAURO e TITANOVENATOR.
- Cor: ciano do holograma para o Tiranossauro e âmbar para o Titanovenator, as duas
  translúcidas e com as cores da pele desligadas. O texto do dossiê já descreve a pele.
- Abaixo do holograma, a tabela "Comparação com o tiranossauro" do dossiê.

## Dados

- Os modelos são os das entidades (`iceagesurvival:tyrannosaurus` e a futura do Titanovenator),
  criados como os modelos da DINO FILE (`AnalyzerScreen.model`).
- A escala sai da altura da caixa de cada entidade; o maior dos dois preenche o quadro e o outro
  fica em proporção.

## Fora do escopo

Animação de ataque, sons e números de jogo (vida, dano) no holograma.
