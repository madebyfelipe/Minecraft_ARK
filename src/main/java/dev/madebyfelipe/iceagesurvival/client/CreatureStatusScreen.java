package dev.madebyfelipe.iceagesurvival.client;

import dev.madebyfelipe.iceagesurvival.core.command.Movement;
import dev.madebyfelipe.iceagesurvival.core.command.Stance;
import dev.madebyfelipe.iceagesurvival.core.command.Whistle;
import dev.madebyfelipe.iceagesurvival.core.stats.Stat;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.network.CreatureStatusPayload;
import dev.madebyfelipe.iceagesurvival.network.StatusRequestPayload;
import dev.madebyfelipe.iceagesurvival.network.WhistlePayload;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Tela de status de uma criatura do jogador: atributos com os pontos de cada um, afinidade,
 * ordens e dono, e botões de assobio só para ela. Os atributos vêm do servidor
 * ({@link CreatureStatusPayload}) e são pedidos de novo a cada segundo enquanto a tela está aberta.
 */
public class CreatureStatusScreen extends Screen {
    private static final int WIDTH = 276;
    private static final int HEIGHT = 196;
    private static final int PREVIEW_WIDTH = 96;
    private static final int REFRESH_TICKS = 20;
    private static final int ROW_HEIGHT = 12;

    private static final int PANEL = 0xE0101418;
    private static final int BORDER = 0xFF5FB36B;
    private static final int LABEL = 0xFFA0A8B0;
    private static final int VALUE = 0xFFFFFFFF;
    private static final int POINTS = 0xFF7FD17F;
    private static final int LEVEL = 0xFFFFC857;

    /** Criatura cujo status foi pedido e ainda não chegou; -1 se nenhuma. */
    private static int pendingId = -1;

    private final PrehistoricCreature creature;
    private CreatureStatusPayload status;
    private final Map<Whistle, Button> buttons = new EnumMap<>(Whistle.class);
    private int ticks;
    private int left;
    private int top;

    private CreatureStatusScreen(PrehistoricCreature creature, CreatureStatusPayload status) {
        super(creature.getName());
        this.creature = creature;
        this.status = status;
    }

    /** Pede ao servidor o status da criatura; a tela abre quando a resposta chegar. */
    public static void request(PrehistoricCreature creature) {
        pendingId = creature.getId();
        PacketDistributor.sendToServer(new StatusRequestPayload(creature.getId()));
    }

    /** Chegou um status: atualiza a tela aberta ou abre a que foi pedida. */
    public static void receive(CreatureStatusPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof CreatureStatusScreen open && open.creature.getId() == payload.creatureId()) {
            open.status = payload;
        } else if (payload.creatureId() == pendingId && minecraft.level != null
                && minecraft.level.getEntity(payload.creatureId()) instanceof PrehistoricCreature creature) {
            minecraft.setScreen(new CreatureStatusScreen(creature, payload));
        }
        pendingId = -1;
    }

    @Override
    protected void init() {
        left = (width - WIDTH) / 2;
        top = (height - HEIGHT) / 2;
        buttons.clear();
        int y = top + HEIGHT - 48;
        addWhistleRow(y, Whistle.FOLLOW, Whistle.STAY);
        addWhistleRow(y + 22, Whistle.PASSIVE, Whistle.NEUTRAL, Whistle.DEFEND, Whistle.FLEE);
        updateButtons();
    }

    private void addWhistleRow(int y, Whistle... whistles) {
        int gap = 4;
        int buttonWidth = (WIDTH - 16 - gap * (whistles.length - 1)) / whistles.length;
        int x = left + 8;
        for (Whistle whistle : whistles) {
            Button button = Button.builder(Component.translatable("iceagesurvival.whistle." + whistle.id()),
                            b -> PacketDistributor.sendToServer(new WhistlePayload(whistle, creature.getId())))
                    .bounds(x, y, buttonWidth, 18)
                    .build();
            buttons.put(whistle, addRenderableWidget(button));
            x += buttonWidth + gap;
        }
    }

    /** O botão da ordem em vigor fica apagado, como uma aba selecionada. */
    private void updateButtons() {
        buttons.forEach((whistle, button) -> button.active =
                whistle.movement().map(movement -> movement != creature.movement()).orElse(true)
                        && whistle.stance().map(stance -> stance != creature.stance()).orElse(true));
    }

    @Override
    public void tick() {
        if (!creature.isAlive() || creature.isRemoved()) {
            onClose();
            return;
        }
        updateButtons();
        if (++ticks % REFRESH_TICKS == 0) {
            PacketDistributor.sendToServer(new StatusRequestPayload(creature.getId()));
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (CommandInput.isStatusKey(keyCode, scanCode)
                || minecraft != null && minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(left - 1, top - 1, left + WIDTH + 1, top + HEIGHT + 1, BORDER);
        graphics.fill(left, top, left + WIDTH, top + HEIGHT, PANEL);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        // Título: nome e nível.
        Component level = Component.translatable("iceagesurvival.hud.level", creature.creatureLevel());
        graphics.drawString(font, title, left + 8, top + 8, VALUE);
        graphics.drawString(font, level, left + WIDTH - 8 - font.width(level), top + 8, LEVEL);
        graphics.fill(left + 8, top + 20, left + WIDTH - 8, top + 21, 0x40FFFFFF);

        renderPreview(graphics, mouseX, mouseY);
        renderStats(graphics, left + PREVIEW_WIDTH + 8, top + 28);
    }

    private void renderPreview(GuiGraphics graphics, int mouseX, int mouseY) {
        int x1 = left + 8;
        int y1 = top + 26;
        int x2 = left + PREVIEW_WIDTH;
        int y2 = top + HEIGHT - 56;
        graphics.fill(x1, y1, x2, y2, 0x30FFFFFF);
        float size = Math.max(creature.getBbHeight(), creature.getBbWidth() * 1.4F);
        int scale = Math.max(4, Math.round((y2 - y1 - 16) / size));
        InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, x1, y1, x2, y2, scale, 0.0625F,
                mouseX, mouseY, creature);
    }

    private void renderStats(GuiGraphics graphics, int x, int y) {
        int right = left + WIDTH - 8;
        float maxHealth = (float) status.value(Stat.HEALTH);
        statRow(graphics, x, right, y, Stat.HEALTH,
                String.format(Locale.ROOT, "%.0f / %.0f", status.health(), maxHealth));
        CreatureHud.drawBar(graphics, x, y + 9, right - x, 2, status.health() / Math.max(maxHealth, 1.0F), 0xFFD04848);
        y += ROW_HEIGHT + 2;
        statRow(graphics, x, right, y, Stat.ATTACK, String.format(Locale.ROOT, "%.1f", status.value(Stat.ATTACK)));
        y += ROW_HEIGHT;
        double baseSpeed = status.baseValue(Stat.SPEED);
        statRow(graphics, x, right, y, Stat.SPEED, String.format(Locale.ROOT, "%.0f%%",
                baseSpeed > 0 ? status.value(Stat.SPEED) / baseSpeed * 100.0 : 100.0));
        y += ROW_HEIGHT;
        statRow(graphics, x, right, y, Stat.TORPOR,
                String.format(Locale.ROOT, "%.0f / %.0f", status.torpor(), status.value(Stat.TORPOR)));
        CreatureHud.drawBar(graphics, x, y + 9, right - x, 2,
                (float) (status.torpor() / Math.max(status.value(Stat.TORPOR), 1.0)), 0xFF9B59D0);
        y += ROW_HEIGHT + 2;
        statRow(graphics, x, right, y, Stat.ARMOR, String.format(Locale.ROOT, "%.1f", status.value(Stat.ARMOR)));
        y += ROW_HEIGHT + 4;

        graphics.drawString(font, Component.translatable("iceagesurvival.status.affinity"), x, y, LABEL);
        String affinity = String.format(Locale.ROOT, "%.0f / %.0f", status.affinity(), PrehistoricCreature.MAX_AFFINITY);
        graphics.drawString(font, affinity, right - font.width(affinity), y, VALUE);
        CreatureHud.drawBar(graphics, x, y + 9, right - x, 2, status.affinity() / PrehistoricCreature.MAX_AFFINITY, 0xFF5FB36B);
        y += ROW_HEIGHT + 4;

        infoRow(graphics, x, right, y, "iceagesurvival.status.orders", Component.translatable(
                "iceagesurvival.hud.orders", movementName(creature.movement()), stanceName(creature.stance())));
        y += ROW_HEIGHT;
        infoRow(graphics, x, right, y, "iceagesurvival.status.saddle", Component.translatable(
                creature.isSaddled() ? "iceagesurvival.status.yes" : "iceagesurvival.status.no"));
        y += ROW_HEIGHT;
        if (!status.ownerName().isEmpty()) {
            infoRow(graphics, x, right, y, "iceagesurvival.status.owner", Component.literal(status.ownerName()));
        }
    }

    private void statRow(GuiGraphics graphics, int x, int right, int y, Stat stat, String value) {
        graphics.drawString(font, Component.translatable("iceagesurvival.stat." + stat.id()), x, y, LABEL);
        int points = status.points(stat);
        String pointsText = points > 0 ? " +" + points : "";
        int valueX = right - font.width(value) - font.width(pointsText);
        graphics.drawString(font, value, valueX, y, VALUE);
        if (points > 0) {
            graphics.drawString(font, pointsText, right - font.width(pointsText), y, POINTS);
        }
    }

    private void infoRow(GuiGraphics graphics, int x, int right, int y, String labelKey, Component value) {
        graphics.drawString(font, Component.translatable(labelKey), x, y, LABEL);
        graphics.drawString(font, value, right - font.width(value), y, VALUE);
    }

    static Component movementName(Movement movement) {
        return Component.translatable("iceagesurvival.whistle." + movement.id());
    }

    static Component stanceName(Stance stance) {
        return Component.translatable("iceagesurvival.whistle." + stance.id());
    }
}
