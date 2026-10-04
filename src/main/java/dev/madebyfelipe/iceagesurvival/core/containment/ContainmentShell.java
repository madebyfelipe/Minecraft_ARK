package dev.madebyfelipe.iceagesurvival.core.containment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * O console de operação do campo de êxtase da base (D53), como um computador de verdade: um prompt, comandos no estilo
 * Unix com a saída em português e uma sessão por pessoa. Só {@code campo desligar} muda o mundo, e só para quem entrou
 * como {@link #USER} com a {@link #PASSWORD} (que está num dos registros da base) e confirmou.
 *
 * <p>Lógica pura, sem Minecraft (D10): o servidor guarda a {@link Session}, preenche o {@link Status} com o estado do
 * núcleo, manda cada linha digitada para {@link #run} e executa a {@link Action} da resposta.
 */
public final class ContainmentShell {
    public static final String HOST = "contencao";
    public static final String USER = "operador";
    public static final String GUEST = "convidado";
    /** A senha do operador; aparece no caderno da ala médica ({@code tools/wiki_lore/base/05-o-campo.txt}). */
    public static final String PASSWORD = "limiar-017";
    /** Tamanho máximo de uma linha digitada. */
    public static final int MAX_LINE = 96;

    /** O que o console espera da próxima linha. */
    public enum Mode { COMMAND, USERNAME, PASSWORD, CONFIRM }

    /** O que o servidor (ou a tela) faz depois de mostrar a resposta. */
    public enum Action { NONE, CLEAR, EXIT, SHUTDOWN }

    /** O estado da contenção, visto pelo console. */
    public record Status(boolean fieldUp, boolean collapsing, int emitters, int emittersTotal, boolean specimenHeld) {
    }

    /** A resposta a uma linha: o texto, a ação, e como fica o prompt (com a entrada escondida na senha). */
    public record Reply(List<String> lines, Action action, String prompt, boolean secret) {
    }

    /** A sessão de uma pessoa num console. */
    public static final class Session {
        private Mode mode = Mode.COMMAND;
        private boolean operator;
        private String pendingUser = "";

        public Mode mode() {
            return mode;
        }

        public boolean operator() {
            return operator;
        }

        public String prompt() {
            return switch (mode) {
                case USERNAME -> "usuário: ";
                case PASSWORD -> "senha: ";
                case CONFIRM -> "confirmar (s/n): ";
                case COMMAND -> (operator ? USER : GUEST) + "@" + HOST + ":~$ ";
            };
        }
    }

    /** Os arquivos da pasta do console; os que começam com ponto só aparecem no {@code ls -a}. */
    private static final Map<String, List<String>> FILES = Map.of(
            "leia-me.txt", List.of(
                    "CONSOLE DE OPERAÇÃO - CAMPO DE ÊXTASE, CÂMARA DO QUARTO NÍVEL",
                    "",
                    "Este terminal opera os emissores da câmara. Consulta é livre;",
                    "desligar o campo exige a conta do operador de plantão.",
                    "",
                    "  status          estado do campo e dos emissores",
                    "  campo desligar  derruba o campo (operador)",
                    "",
                    "Em caso de queda de energia os emissores passam para a",
                    "reserva sozinhos. NÃO desligue o campo com gente na câmara."),
            "campo.log", List.of(
                    "[00:00:12] emissores 1-4: sincronizados, tensão nominal",
                    "[00:41:07] espécime 017: batimento 1/min",
                    "[03:12:55] emissor 3: oscilação de fase, corrigida",
                    "[03:13:02] ALERTA: pico na linha principal (teste do quinto nível)",
                    "[03:13:02] emissores 1-4: reserva assumiu",
                    "[03:13:09] espécime 017: movimento detectado",
                    "[03:13:10] campo restabelecido em potência mínima",
                    "[??:??:??] relógio perdido; registro continua por contagem",
                    "[+0412 h] espécime 017: batimento 3/min",
                    "[+0412 h] reserva: autonomia indeterminada"),
            "especime.txt", List.of(
                    "ESPÉCIME 017 - CLASSIFICAÇÃO INTERNA",
                    "",
                    "Tiranossaurídeo de linhagem desconhecida, trazido no teste zero.",
                    "Altura na cernelha perto do dobro da de um tiranossauro adulto.",
                    "Couro espesso: ferimentos rasos fecham em minutos.",
                    "Sedativo não segura. Campo de êxtase obrigatório.",
                    "",
                    "Nota da ala médica: armas comuns não passam do couro. Se um dia",
                    "for preciso enfrentá-lo, só os animais mais fortes do vale."),
            ".historico", List.of(
                    "status",
                    "cat campo.log",
                    "login operador",
                    "status",
                    "logout"));
    private static final List<String> RESTRICTED = List.of("especime.txt");

    private ContainmentShell() {
    }

    /** O que aparece ao ligar a tela: a inicialização e o estado. */
    public static List<String> boot(Status status) {
        List<String> lines = new ArrayList<>(List.of(
                "BIOS militar v2.07  -  memória OK",
                "carregando sistema de operação da contenção...",
                "montando /campo ............ ok",
                "conexão com os emissores ... " + status.emitters() + "/" + status.emittersTotal(),
                "",
                "Bem-vindo ao console da câmara. Digite 'help' para ver os comandos.",
                ""));
        lines.addAll(statusLines(status));
        lines.add("");
        return lines;
    }

    public static Reply run(Session session, String rawLine, Status status) {
        String line = rawLine == null ? "" : rawLine.strip();
        if (line.length() > MAX_LINE) {
            line = line.substring(0, MAX_LINE);
        }
        return switch (session.mode) {
            case USERNAME -> username(session, line);
            case PASSWORD -> password(session, line);
            case CONFIRM -> confirm(session, line, status);
            case COMMAND -> command(session, line, status);
        };
    }

    private static Reply username(Session session, String line) {
        if (line.isEmpty()) {
            session.mode = Mode.COMMAND;
            return reply(session, List.of("login cancelado."));
        }
        session.pendingUser = line;
        session.mode = Mode.PASSWORD;
        return reply(session, List.of());
    }

    private static Reply password(Session session, String line) {
        session.mode = Mode.COMMAND;
        if (USER.equalsIgnoreCase(session.pendingUser) && PASSWORD.equalsIgnoreCase(line)) {
            session.operator = true;
            return reply(session, List.of("acesso concedido. Último acesso: data desconhecida.",
                    "Lembrete: o campo NÃO deve ser desligado com gente na câmara."));
        }
        return reply(session, List.of("usuário ou senha incorretos."));
    }

    private static Reply confirm(Session session, String line, Status status) {
        session.mode = Mode.COMMAND;
        String answer = line.toLowerCase(Locale.ROOT);
        if (!answer.equals("s") && !answer.equals("sim")) {
            return reply(session, List.of("cancelado. O campo continua ativo."));
        }
        if (!status.fieldUp()) {
            return reply(session, List.of("o campo já está desligado."));
        }
        return new Reply(List.of(
                "desligando emissor 1 ... ok",
                "desligando emissor 2 ... ok",
                "desligando emissor 3 ... ok",
                "desligando emissor 4 ... ok",
                "CAMPO EM COLAPSO. Espécime 017 liberado.",
                "Saia da câmara."), Action.SHUTDOWN, session.prompt(), false);
    }

    private static Reply command(Session session, String line, Status status) {
        if (line.isEmpty()) {
            return reply(session, List.of());
        }
        String[] words = line.split("\\s+");
        String name = words[0].toLowerCase(Locale.ROOT);
        String arg = words.length > 1 ? words[1].toLowerCase(Locale.ROOT) : "";
        return switch (name) {
            case "help", "ajuda", "?" -> reply(session, List.of(
                    "comandos:",
                    "  help              esta lista",
                    "  ls [-a]           lista os arquivos",
                    "  cat <arquivo>     mostra um arquivo",
                    "  status            estado do campo",
                    "  campo <status|desligar>",
                    "  login [usuário]   entra com outra conta",
                    "  logout            volta para convidado",
                    "  whoami, date, echo, clear, exit"));
            case "ls", "dir" -> reply(session, listing(arg.equals("-a") || arg.equals("-la") || arg.equals("-al")));
            case "cat", "more", "less", "type" -> cat(session, arg);
            case "status" -> reply(session, statusLines(status));
            case "campo" -> field(session, arg, status);
            case "login", "su" -> login(session, words);
            case "logout" -> {
                session.operator = false;
                yield reply(session, List.of("sessão encerrada."));
            }
            case "whoami" -> reply(session, List.of(session.operator ? USER : GUEST));
            case "date" -> reply(session, List.of("data do sistema perdida (bateria do relógio descarregada)."));
            case "echo" -> reply(session, List.of(line.length() > 4 ? line.substring(5).strip() : ""));
            case "clear", "cls" -> new Reply(List.of(), Action.CLEAR, session.prompt(), false);
            case "exit", "quit" -> new Reply(List.of("até logo."), Action.EXIT, session.prompt(), false);
            case "sudo" -> reply(session, List.of((session.operator ? USER : GUEST)
                    + " não está no arquivo sudoers. Este incidente será reportado."));
            case "shutdown", "poweroff", "reboot", "halt" -> reply(session, List.of(
                    name + ": desligar este console não desliga o campo. Use 'campo desligar'."));
            case "rm" -> reply(session, List.of("rm: sistema de arquivos montado somente para leitura."));
            case "cd" -> reply(session, List.of("cd: só existe esta pasta."));
            default -> reply(session, List.of(name + ": comando não encontrado"));
        };
    }

    private static Reply field(Session session, String arg, Status status) {
        return switch (arg) {
            case "status", "" -> reply(session, statusLines(status));
            case "desligar", "off", "down", "stop" -> {
                if (!session.operator) {
                    yield reply(session, List.of("campo: permissão negada. Entre como operador (login)."));
                }
                if (!status.fieldUp()) {
                    yield reply(session, List.of(status.collapsing() ? "campo: o colapso já começou."
                            : "campo: o campo já está desligado."));
                }
                session.mode = Mode.CONFIRM;
                yield reply(session, List.of("ATENÇÃO: desligar o campo libera o espécime 017.",
                        "Esta operação não pode ser desfeita."));
            }
            default -> reply(session, List.of("uso: campo <status|desligar>"));
        };
    }

    private static Reply login(Session session, String[] words) {
        if (words.length > 1) {
            session.pendingUser = words[1];
            session.mode = Mode.PASSWORD;
        } else {
            session.mode = Mode.USERNAME;
        }
        return reply(session, List.of());
    }

    private static Reply cat(Session session, String file) {
        if (file.isEmpty()) {
            return reply(session, List.of("uso: cat <arquivo>"));
        }
        List<String> content = FILES.get(file);
        if (content == null) {
            return reply(session, List.of("cat: " + file + ": arquivo não encontrado"));
        }
        if (RESTRICTED.contains(file) && !session.operator) {
            return reply(session, List.of("cat: " + file + ": permissão negada"));
        }
        return reply(session, content);
    }

    private static List<String> listing(boolean all) {
        List<String> names = new ArrayList<>(FILES.keySet());
        names.removeIf(name -> !all && name.startsWith("."));
        names.sort(String::compareTo);
        List<String> lines = new ArrayList<>();
        for (String name : names) {
            lines.add((RESTRICTED.contains(name) ? "-rw-------  " : "-rw-r--r--  ") + name);
        }
        return lines;
    }

    static List<String> statusLines(Status status) {
        String field = status.fieldUp() ? "ATIVO" : status.collapsing() ? "EM COLAPSO" : "DESLIGADO";
        return List.of(
                "campo de êxtase ...... " + field,
                "emissores ............ " + status.emitters() + "/" + status.emittersTotal() + " em linha",
                "espécime 017 ......... " + (status.specimenHeld() ? "CONTIDO" : "NÃO DETECTADO NO CAMPO"));
    }

    private static Reply reply(Session session, List<String> lines) {
        return new Reply(List.copyOf(lines), Action.NONE, session.prompt(), session.mode == Mode.PASSWORD);
    }
}
