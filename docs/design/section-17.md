## 17. Genética

- Cada atributo é herdado de um dos pais, 50/50, independentemente dos outros.
- Mutações raras:
  - **Ataque:** sem teto.
  - **Velocidade:** teto de +30%.
  - **Vida:** só sobe em linhagens que carregam o gene de mutação de vida.
  - Demais atributos: não escalam por mutação.
- O genoma é lógica pura em `core/genetics`, coberta por testes JUnit com RNG semeado.

Implementado na Etapa 8 (`Genome`, `Genetics`). Pontos **e** contagem de mutações de cada atributo vêm juntos de um dos pais. A cada cria, 3 tentativas de mutação de 2,5% (`mutationAttempts`, `mutationChance`); cada uma sorteia entre ataque, velocidade e — se a linhagem tiver o gene — vida. Ataque e vida ganham 2 pontos (e 2 níveis) por mutação; velocidade ganha 3% por mutação até 10 (+30%), e passado o teto a mutação se perde. O gene de vida passa se um dos pais o tiver, surge sozinho em 1% das crias (`healthGeneChance`) e 5% das selvagens já o carregam (`wildHealthGeneChance`). A tela de status mostra sexo, mutações, o gene, gestação e crescimento.

