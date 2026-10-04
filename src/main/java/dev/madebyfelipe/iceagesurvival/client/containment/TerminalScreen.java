package dev.madebyfelipe.iceagesurvival.client.containment;

import dev.madebyfelipe.iceagesurvival.core.containment.ContainmentShell;
import dev.madebyfelipe.iceagesurvival.network.ModPayloads;
import dev.madebyfelipe.iceagesurvival.network.TerminalCommandPayload;
import dev.madebyfelipe.iceagesurvival.network.TerminalScreenPayload;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

/**
 * A tela do console da contenção, como um monitor de fósforo verde: a saída rola linha a linha (a inicialização mais
 * devagar), o prompt fica embaixo com o cursor piscando e a senha sai em asteriscos. Enter manda a linha para o
 * servidor ({@link TerminalCommandPayload}), que responde com {@link TerminalScreenPayload}; as setas recuperam
 * comandos anteriores; a roda do mouse e Page Up/Down rolam; Esc ou {@code exit} fecham.
 */
public class TerminalScreen extends Screen {
    private static final int MAX_WIDTH = 440;
    private static final int MAX_HEIGHT = 264;
    private static final int LINE = 10;
    private static final int PAD = 10;
    private static final int HEADER = 16;
    private static final int MAX_SCROLLBACK = 400;
    private static final int MAX_HISTORY = 32;
    /** Sem resposta do servidor neste tempo, o prompt volta mesmo assim. */
    private static final long REPLY_TIMEOUT_MILLIS = 3000;

    private static final int BEZEL = 0xFF1B2124;
    private static final int BEZEL_EDGE = 0xFF3A4449;
    private static final int GLASS = 0xFF040B07;
    private static final int PHOSPHOR = 0xFF5AE678;
    private static final int PHOSPHOR_BRIGHT = 0xFFB4FFC6;
    private static final int PHOSPHOR_DIM = 0xFF2E7A44;
    private static final int ALERT = 0xFFFFB21E;
    private static final Style MONO = Style.EMPTY.withFont(Minecraft.UNIFORM_FONT);

    private final BlockPos terminal;
    private final List<String> scrollback = new ArrayList<>();
    private final Deque<String> pending = new ArrayDeque<>();
    private final List<String> history = new ArrayList<>();
    private final long openedAt = Util.getMillis();
    private String prompt;
    private boolean secret;
    private boolean closeWhenDone;
    private boolean booting = true;
    private String input = "";
    private int historyIndex = -1;
    private int scroll;
    private long awaitingSince = -1;
    private int left;
    private int top;
    private int right;
    private int bottom;

    private TerminalScreen(BlockPos terminal, List<String> boot, String prompt, boolean secret) {
        super(Component.translatable("block.iceagesurvival.containment_console"));
        this.terminal = terminal;
        this.prompt = prompt;
        this.secret = secret;
        pending.addAll(boot);
    }

    /** O que o servidor mandou: abre a tela, ou alimenta a que já está aberta neste console. */
    public static void receive(TerminalScreenPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (payload.open()) {
            minecraft.setScreen(new TerminalScreen(payload.terminal(), payload.lines(), payload.prompt(),
                    payload.secret()));
            return;
        }
        if (minecraft.screen instanceof TerminalScreen screen && screen.terminal.equals(payload.terminal())) {
            screen.apply(payload);
        }
    }

    private void apply(TerminalScreenPayload payload) {
        if (payload.clear()) {
            scrollback.clear();
            pending.clear();
        }
        pending.addAll(payload.lines());
        prompt = payload.prompt();
        secret = payload.secret();
        closeWhenDone |= payload.close();
        awaitingSince = -1;
        scroll = 0;
    }

    @Override
    protected void init() {
        int width = Math.min(MAX_WIDTH, this.width - 16);
        int height = Math.min(MAX_HEIGHT, this.height - 16);
        left = (this.width - width) / 2;
        top = (this.height - height) / 2;
        right = left + width;
        bottom = top + height;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private boolean ready() {
        return pending.isEmpty() && (awaitingSince < 0 || Util.getMillis() - awaitingSince > REPLY_TIMEOUT_MILLIS);
    }

    /** A saída sai aos poucos: uma linha a cada tick na inicialização, três depois. */
    @Override
    public void tick() {
        int lines = booting ? 1 : 3;
        for (int i = 0; i < lines && !pending.isEmpty(); i++) {
            push(pending.poll());
        }
        if (pending.isEmpty()) {
            booting = false;
            if (closeWhenDone) {
                onClose();
            }
        }
    }

    private void push(String line) {
        scrollback.add(line);
        while (scrollback.size() > MAX_SCROLLBACK) {
            scrollback.remove(0);
        }
    }

    // ---- Entrada ----

    @Override
    public boolean charTyped(char typed, int modifiers) {
        if (!ready() || !SharedConstants.isAllowedChatCharacter(typed) || input.length() >= ContainmentShell.MAX_LINE) {
            return false;
        }
        input += typed;
        scroll = 0;
        click(1.9F);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        switch (keyCode) {
            case GLFW.GLFW_KEY_ESCAPE -> {
                onClose();
                return true;
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                if (ready()) {
                    submit();
                }
                return true;
            }
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (!input.isEmpty()) {
                    input = hasControlDown() ? "" : input.substring(0, input.length() - 1);
                    click(1.6F);
                }
                return true;
            }
            case GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_DOWN -> {
                recall(keyCode == GLFW.GLFW_KEY_UP ? 1 : -1);
                return true;
            }
            case GLFW.GLFW_KEY_PAGE_UP, GLFW.GLFW_KEY_PAGE_DOWN -> {
                scrollBy(keyCode == GLFW.GLFW_KEY_PAGE_UP ? visibleLines() / 2 : -visibleLines() / 2);
                return true;
            }
            case GLFW.GLFW_KEY_L -> {
                if (hasControlDown()) {
                    scrollback.clear();
                    return true;
                }
            }
            default -> {
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scrollBy((int) Math.signum(delta) * 3);
        return true;
    }

    private void scrollBy(int lines) {
        scroll = Mth.clamp(scroll + lines, 0, Math.max(0, wrapped().size() - visibleLines()));
    }

    private void recall(int step) {
        if (secret || history.isEmpty()) {
            return;
        }
        historyIndex = Mth.clamp(historyIndex + step, -1, history.size() - 1);
        input = historyIndex < 0 ? "" : history.get(history.size() - 1 - historyIndex);
    }

    private void submit() {
        String line = input;
        push(prompt + (secret ? "*".repeat(line.length()) : line));
        if (!secret && !line.isBlank() && (history.isEmpty() || !history.get(history.size() - 1).equals(line))) {
            history.add(line);
            if (history.size() > MAX_HISTORY) {
                history.remove(0);
            }
        }
        historyIndex = -1;
        input = "";
        scroll = 0;
        awaitingSince = Util.getMillis();
        ModPayloads.sendToServer(new TerminalCommandPayload(terminal, line));
        click(1.2F);
    }

    private void click(float pitch) {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_HAT.value(), pitch, 0.12F));
    }

    // ---- Desenho ----

    private int textLeft() {
        return left + PAD + 2;
    }

    private int textWidth() {
        return right - PAD - 2 - textLeft();
    }

    private int visibleLines() {
        return Math.max(1, (bottom - PAD - 2 - (top + HEADER + 6)) / LINE - 1);
    }

    /** O texto da tela já quebrado na largura, com a linha do prompt no fim. */
    private List<FormattedCharSequence> wrapped() {
        List<FormattedCharSequence> out = new ArrayList<>();
        for (String line : scrollback) {
            if (line.isEmpty()) {
                out.add(FormattedCharSequence.EMPTY);
            } else {
                out.addAll(font.split(Component.literal(line).withStyle(MONO), textWidth()));
            }
        }
        return out;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        // Gabinete do monitor e o vidro.
        graphics.fill(left - 6, top - 6, right + 6, bottom + 6, BEZEL_EDGE);
        graphics.fill(left - 5, top - 5, right + 5, bottom + 5, BEZEL);
        graphics.fill(left, top, right, bottom, GLASS);
        graphics.fill(right - 18, bottom + 1, right - 12, bottom + 4, Util.getMillis() / 600 % 2 == 0 ? 0xFF3AFF6A
                : 0xFF1C6A30);

        long now = Util.getMillis();
        float warm = Mth.clamp((now - openedAt) / 500.0F, 0.0F, 1.0F);
        int header = top + 4;
        graphics.drawString(font, Component.literal("CONTENÇÃO // CONSOLE DE OPERAÇÃO").withStyle(MONO), textLeft(),
                header, fade(PHOSPHOR_BRIGHT, warm), false);
        Component hint = Component.literal("[ESC] sair").withStyle(MONO);
        graphics.drawString(font, hint, right - PAD - font.width(hint), header, fade(PHOSPHOR_DIM, warm), false);
        graphics.fill(left + PAD, top + HEADER, right - PAD, top + HEADER + 1, fade(PHOSPHOR_DIM, warm));

        List<FormattedCharSequence> lines = wrapped();
        int visible = visibleLines();
        int end = Math.max(0, lines.size() - scroll);
        int start = Math.max(0, end - visible);
        int y = top + HEADER + 6;
        graphics.enableScissor(left + 1, top + HEADER + 2, right - 1, bottom - 1);
        for (int i = start; i < end; i++) {
            graphics.drawString(font, lines.get(i), textLeft(), y, fade(colorOf(lines.get(i)), warm), false);
            y += LINE;
        }
        if (scroll == 0) {
            renderPrompt(graphics, y, warm);
        }
        graphics.disableScissor();
        if (scroll > 0) {
            Component more = Component.literal("-- " + scroll + " linhas abaixo --").withStyle(MONO);
            graphics.drawString(font, more, right - PAD - font.width(more), bottom - PAD - 2, ALERT, false);
        }
        renderGlass(graphics, now);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderPrompt(GuiGraphics graphics, int y, float warm) {
        if (!pending.isEmpty() || awaitingSince >= 0 && Util.getMillis() - awaitingSince <= REPLY_TIMEOUT_MILLIS) {
            if (Util.getMillis() / 250 % 2 == 0) {
                graphics.fill(textLeft(), y + 1, textLeft() + 5, y + 9, fade(PHOSPHOR, warm));
            }
            return;
        }
        String shown = secret ? "*".repeat(input.length()) : input;
        Component text = Component.literal(prompt + shown).withStyle(MONO);
        int x = textLeft();
        int maxWidth = textWidth() - 6;
        if (font.width(text) > maxWidth) {
            String clipped = font.plainSubstrByWidth(prompt + shown, maxWidth, true);
            text = Component.literal(clipped).withStyle(MONO);
        }
        graphics.drawString(font, text, x, y, fade(PHOSPHOR_BRIGHT, warm), false);
        if (Util.getMillis() / 500 % 2 == 0) {
            int cursor = x + font.width(text) + 1;
            graphics.fill(cursor, y + 1, cursor + 5, y + 9, fade(PHOSPHOR, warm));
        }
    }

    /** Alertas (ATENÇÃO, COLAPSO, negado) saem em âmbar; o resto em verde. */
    private int colorOf(FormattedCharSequence line) {
        StringBuilder text = new StringBuilder();
        line.accept((index, style, codePoint) -> {
            text.appendCodePoint(codePoint);
            return true;
        });
        String plain = text.toString();
        if (plain.contains("ATENÇÃO") || plain.contains("COLAPSO") || plain.contains("ALERTA")
                || plain.contains("negad") || plain.contains("incorretos")) {
            return ALERT;
        }
        if (plain.endsWith("$") || plain.contains("@" + ContainmentShell.HOST + ":")) {
            return PHOSPHOR_BRIGHT;
        }
        return PHOSPHOR;
    }

    /** Linhas de varredura, a faixa clara descendo e o brilho do vidro nos cantos. */
    private void renderGlass(GuiGraphics graphics, long now) {
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 100.0F);
        for (int y = top + 1; y < bottom; y += 2) {
            graphics.fill(left, y, right, y + 1, 0x22000000);
        }
        int band = top + (int) (now / 18 % Math.max(1, bottom - top));
        graphics.fill(left, band, right, Math.min(bottom, band + 3), 0x0E9CFFB4);
        int flicker = (int) (6 + 4 * Mth.sin(now / 70.0F));
        graphics.fill(left, top, right, bottom, flicker << 24 | 0x5AE678);
        graphics.fill(left, top, right, top + 2, 0x30000000);
        graphics.fill(left, bottom - 2, right, bottom, 0x30000000);
        graphics.pose().popPose();
    }

    /** Esmaece a cor; nunca abaixo de 8 de alfa, porque a fonte trata alfa quase zero como opaco. */
    private static int fade(int color, float amount) {
        int alpha = Math.max(8, Math.round(((color >>> 24) & 0xFF) * amount));
        return alpha << 24 | color & 0xFFFFFF;
    }
}
