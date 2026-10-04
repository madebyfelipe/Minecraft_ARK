package dev.madebyfelipe.iceagesurvival.core.containment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.madebyfelipe.iceagesurvival.core.containment.ContainmentShell.Action;
import dev.madebyfelipe.iceagesurvival.core.containment.ContainmentShell.Mode;
import dev.madebyfelipe.iceagesurvival.core.containment.ContainmentShell.Reply;
import dev.madebyfelipe.iceagesurvival.core.containment.ContainmentShell.Session;
import dev.madebyfelipe.iceagesurvival.core.containment.ContainmentShell.Status;
import org.junit.jupiter.api.Test;

class ContainmentShellTest {
    private static final Status UP = new Status(true, false, 4, 4, true);
    private static final Status DOWN = new Status(false, false, 0, 4, false);

    private static Reply run(Session session, String line) {
        return ContainmentShell.run(session, line, UP);
    }

    private static boolean says(Reply reply, String text) {
        return reply.lines().stream().anyMatch(line -> line.contains(text));
    }

    private static Session operator() {
        Session session = new Session();
        run(session, "login operador");
        run(session, ContainmentShell.PASSWORD);
        return session;
    }

    @Test
    void aGuestCanLookButCannotShutTheFieldDown() {
        Session session = new Session();
        assertTrue(session.prompt().startsWith("convidado@contencao"));
        assertTrue(says(run(session, "help"), "campo"));
        assertTrue(says(run(session, "ls"), "campo.log"));
        assertTrue(says(run(session, "cat campo.log"), "reserva"));
        assertTrue(says(run(session, "cat especime.txt"), "permissão negada"));
        Reply denied = run(session, "campo desligar");
        assertTrue(says(denied, "permissão negada"));
        assertEquals(Action.NONE, denied.action());
        assertEquals(Mode.COMMAND, session.mode());
    }

    @Test
    void theHiddenHistoryOnlyShowsWithDashA() {
        Session session = new Session();
        assertFalse(says(run(session, "ls"), ".historico"));
        assertTrue(says(run(session, "ls -a"), ".historico"));
        assertTrue(says(run(session, "cat .historico"), "login operador"));
    }

    @Test
    void loginAsksForThePasswordWithHiddenInputAndAcceptsTheOneFromTheNotebook() {
        Session session = new Session();
        Reply asked = run(session, "login");
        assertEquals("usuário: ", asked.prompt());
        assertFalse(asked.secret());
        Reply password = run(session, "operador");
        assertTrue(password.secret(), "a senha devia ser digitada escondida");
        assertEquals("senha: ", password.prompt());
        Reply in = run(session, "LIMIAR-017");
        assertTrue(says(in, "acesso concedido"), "a senha não diferencia maiúsculas");
        assertTrue(session.operator());
        assertTrue(in.prompt().startsWith("operador@contencao"));
        assertFalse(in.secret());
    }

    @Test
    void aWrongPasswordOrUserKeepsTheGuest() {
        Session session = new Session();
        run(session, "login operador");
        assertTrue(says(run(session, "errada"), "incorretos"));
        assertFalse(session.operator());
        run(session, "login felipe");
        assertTrue(says(run(session, ContainmentShell.PASSWORD), "incorretos"));
        assertFalse(session.operator());
    }

    @Test
    void theOperatorShutsTheFieldDownOnlyAfterConfirming() {
        Session session = operator();
        Reply warning = run(session, "campo desligar");
        assertTrue(says(warning, "libera o espécime"));
        assertEquals(Action.NONE, warning.action());
        assertEquals(Mode.CONFIRM, session.mode());
        Reply cancelled = run(session, "n");
        assertEquals(Action.NONE, cancelled.action());
        assertTrue(says(cancelled, "cancelado"));

        run(session, "campo desligar");
        Reply done = run(session, "s");
        assertEquals(Action.SHUTDOWN, done.action());
        assertTrue(says(done, "COLAPSO"));
    }

    @Test
    void aFieldThatIsAlreadyDownCannotBeShutDownAgain() {
        Session session = operator();
        Reply reply = ContainmentShell.run(session, "campo desligar", DOWN);
        assertTrue(says(reply, "já está desligado"));
        assertEquals(Mode.COMMAND, session.mode());
        Reply collapsing = ContainmentShell.run(session, "campo desligar", new Status(false, true, 4, 4, true));
        assertTrue(says(collapsing, "colapso já começou"));
    }

    @Test
    void logoutReadingTheSpecimenFileAndOtherCommands() {
        Session session = operator();
        assertTrue(says(run(session, "cat especime.txt"), "ESPÉCIME 017"));
        assertEquals(Action.CLEAR, run(session, "clear").action());
        assertEquals(Action.EXIT, run(session, "exit").action());
        assertTrue(says(run(session, "shutdown"), "campo desligar"));
        assertTrue(says(run(session, "echo olá mundo"), "olá mundo"));
        assertTrue(says(run(session, "xyz"), "xyz: comando não encontrado"));
        assertTrue(says(run(session, "whoami"), "operador"));
        run(session, "logout");
        assertFalse(session.operator());
        assertTrue(says(run(session, "whoami"), "convidado"));
    }

    @Test
    void theBootAndStatusTellTheStateOfTheField() {
        assertTrue(ContainmentShell.boot(UP).stream().anyMatch(line -> line.contains("4/4")));
        assertTrue(says(run(new Session(), "status"), "ATIVO"));
        assertTrue(says(ContainmentShell.run(new Session(), "status", DOWN), "DESLIGADO"));
        assertTrue(says(ContainmentShell.run(new Session(), "status", new Status(false, true, 4, 4, true)),
                "EM COLAPSO"));
    }

    @Test
    void emptyAndOverlongLinesAreHarmless() {
        Session session = new Session();
        assertTrue(run(session, "").lines().isEmpty());
        assertTrue(run(session, null).lines().isEmpty());
        Reply reply = run(session, "x".repeat(500));
        assertTrue(reply.lines().get(0).length() < 200);
    }
}
