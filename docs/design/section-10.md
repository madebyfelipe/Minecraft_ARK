## 10. Arquitetura

```
dev.madebyfelipe.iceagesurvival
├── IceAgeSurvival          entrypoint, registro nos event buses
├── core/                   lógica pura, sem classes do Minecraft (testável por JUnit)
│   ├── stats/              atributos, escala por nível
│   ├── genetics/           genoma, herança 50/50, mutações
│   ├── taming/             torpor, eficiência, afinidade
│   └── temperature/        curva de frio, proteção, ritmo de congelamento
├── species/                definição de espécie (Codec) + registry de datapack
├── entity/                 classe base de criatura, goals, dados sincronizados
├── temperature/            leitura do ambiente e congelamento do jogador
├── item/                   flechas tranquilizantes, rifle, implante
├── network/                payloads (comandos, UI)
├── registry/               DeferredRegisters, tags usadas em código, capabilities
├── config/                 configuração comum e de servidor
└── client/                 renderers, HUD, telas (só client)
```

Princípios:

- **Servidor decide tudo.** Cliente envia intenção (payload), servidor valida dono, distância e estado.
- **`core/` não importa `net.minecraft`.** A entidade é uma casca fina que alimenta e consome a lógica pura.
- **Dados antes de código.** Número de balanceamento vive no JSON da espécie ou na config, nunca espalhado em classes.
- **Sem mixins** enquanto um evento ou API do Forge resolver.

### Espécie por dados

Registry de datapack `iceagesurvival:species`, arquivos em `data/<namespace>/iceagesurvival/species/<nome>.json`, validados por Codec no carregamento e sincronizados para o cliente. O formato é definido na Etapa 2 a partir do exemplo do brief (stats base, taming, comportamento, montaria).

Limite conhecido: o *tipo de entidade* (modelo, hitbox, registro) continua sendo código — uma espécie nova precisa de um `EntityType` registrado e de assets. O JSON define tudo o que é balanceamento e comportamento.

### Persistência

- Dados da criatura (nível, genes, torpor, dono, afinidade, comando atual): NBT da entidade via Codec.
- Campos que o cliente precisa ver (torpor, nível, inconsciente, dono): `SynchedEntityData`.
- Implante: Data Component no item, carregando o snapshot serializado da criatura.

### Networking

Forge 1.20.1: `SimpleChannel` em `network/ModPayloads.java`. O cliente envia intenção e o servidor valida. Um payload por intenção do jogador; nada de payload genérico. As referências antigas a `CustomPacketPayload`/`StreamCodec` eram do alvo anterior e não descrevem o código atual.
