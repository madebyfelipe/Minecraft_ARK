package dev.madebyfelipe.iceagesurvival.client;

import dev.madebyfelipe.iceagesurvival.command.CreatureCommands;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.item.CryoCapsuleItem;
import dev.madebyfelipe.iceagesurvival.network.CryoCapsulePayload;
import dev.madebyfelipe.iceagesurvival.network.ModPayloads;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Aba "Cápsulas" do menu da tecla O: as cápsulas criogênicas cheias do inventário, com "Soltar" (à frente do
 * jogador), e as criaturas do jogador por perto, com "Guardar" (na primeira cápsula vazia). O servidor decide;
 * a tela só lê o inventário e as entidades que o cliente já conhece.
 */
public class CryoCapsuleScreen extends Screen {
    private static final int WIDTH = 284;
    private static final int ROW_HEIGHT = 22;
    private static final int HEADER = 28;
    private static final int FOOTER = 8;
    private static final int BUTTON_WIDTH = 62;
    private static final int BUTTON_HEIGHT = 16;
    private static final int TAB_WIDTH = 54;
    private static final int TAB_HEIGHT = 12;
    private static final int REFRESH_TICKS = 10;

    /** Uma linha: cápsula cheia (espaço do inventário) ou criatura por perto (id da entidade). */
    private record Row(boolean capsule, int target, String name, int level, Component detail) {
    }

    private List<Row> rows = List.of();
    private int emptyCapsules;
    private int scroll;
    private int left;
    private int top;
    private int visibleRows;
    private int ticks;

    public CryoCapsuleScreen() {
        super(Component.translatable("iceagesurvival.cryo.title"));
    }

    /**
     * As duas abas do menu da tecla O, no canto direito do cabeçalho; a da tela aberta fica acesa e desligada.
     */
    static List<Button> tabs(int right, int top, boolean capsules) {
        int x = right - 8 - 2 * TAB_WIDTH - 4;
        Button locate = new TechButton(x, top + 6, TAB_WIDTH, TAB_HEIGHT,
                Component.translatable("iceagesurvival.locator.tab"), TechStyle.BRIGHT, true,
                b -> CreatureLocatorScreen.open());
        Button cryo = new TechButton(x + TAB_WIDTH + 4, top + 6, TAB_WIDTH, TAB_HEIGHT,
                Component.translatable("iceagesurvival.cryo.tab"), TechStyle.BRIGHT, true,
                b -> Minecraft.getInstance().setScreen(new CryoCapsuleScreen()));
        locate.active = capsules;
        cryo.active = !capsules;
        return List.of(locate, cryo);
    }

    /** Largura que as abas tomam do cabeçalho, para o resto não passar por cima. */
    static int tabsWidth() {
        return 2 * TAB_WIDTH + 4;
    }

    @Override
    protected void init() {
        visibleRows = Math.max(1, Math.min(8, (height - 40 - HEADER - FOOTER) / ROW_HEIGHT));
        left = (width - WIDTH) / 2;
        top = (height - (HEADER + visibleRows * ROW_HEIGHT + FOOTER)) / 2;
        rows = collect();
        rebuild();
    }

    /** O que mostrar agora: as cápsulas cheias primeiro, depois as criaturas da mais perto à mais longe. */
    private List<Row> collect() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        List<Row> result = new ArrayList<>();
        emptyCapsules = 0;
        if (player == null || minecraft.level == null) {
            return result;
        }
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (CryoCapsuleItem.isEmptyCapsule(stack)) {
                emptyCapsules++;
            } else if (CryoCapsuleItem.holdsCreature(stack)) {
                Component species = CryoCapsuleItem.creatureType(stack)
                        .<Component>map(type -> Component.translatable("entity." + type.getNamespace() + "." + type.getPath()))
                        .orElse(Component.literal("?"));
                result.add(new Row(true, slot, CryoCapsuleItem.creatureName(stack), CryoCapsuleItem.creatureLevel(stack),
                        species));
            }
        }
        double range = CreatureCommands.COMMAND_RANGE;
        minecraft.level.getEntitiesOfClass(PrehistoricCreature.class, player.getBoundingBox().inflate(range),
                        creature -> creature.isAlive() && creature.isTame() && !creature.isCorpse()
                                && player.getUUID().equals(creature.getOwnerUUID())
                                && creature.distanceToSqr(player) <= range * range)
                .stream()
                .sorted(Comparator.comparingDouble(creature -> creature.distanceToSqr(player)))
                .forEach(creature -> result.add(new Row(false, creature.getId(), creature.getName().getString(),
                        creature.creatureLevel(), Component.literal(String.format(Locale.ROOT, "%.0f m",
                                creature.distanceTo(player))))));
        return result;
    }

    /** Refaz os botões das linhas visíveis e as abas. */
    private void rebuild() {
        scroll = Math.max(0, Math.min(scroll, rows.size() - visibleRows));
        clearWidgets();
        tabs(left + WIDTH, top, true).forEach(this::addRenderableWidget);
        for (int index = 0; index < visibleRows && scroll + index < rows.size(); index++) {
            Row row = rows.get(scroll + index);
            int y = top + HEADER + index * ROW_HEIGHT + (ROW_HEIGHT - BUTTON_HEIGHT) / 2;
            Button button = new TechButton(left + WIDTH - 8 - BUTTON_WIDTH, y, BUTTON_WIDTH, BUTTON_HEIGHT,
                    Component.translatable(row.capsule() ? "iceagesurvival.cryo.release" : "iceagesurvival.cryo.store"),
                    row.capsule() ? RadarHud.TARGET : RadarHud.BRIGHT,
                    b -> ModPayloads.sendToServer(new CryoCapsulePayload(!row.capsule(), row.target())));
            button.active = row.capsule() || emptyCapsules > 0;
            addRenderableWidget(button);
        }
    }

    @Override
    public void tick() {
        if (++ticks % REFRESH_TICKS != 0) {
            return;
        }
        int emptyBefore = emptyCapsules;
        List<Row> next = collect();
        // Só refaz os botões quando muda o que está listado; a distância muda sozinha no desenho.
        if (emptyBefore != emptyCapsules || !sameTargets(next, rows)) {
            rows = next;
            rebuild();
        } else {
            rows = next;
        }
    }

    private static boolean sameTargets(List<Row> a, List<Row> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (a.get(i).capsule() != b.get(i).capsule() || a.get(i).target() != b.get(i).target()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int max = Math.max(0, rows.size() - visibleRows);
        int next = Math.max(0, Math.min(max, scroll - (int) Math.signum(delta)));
        if (next != scroll) {
            scroll = next;
            rebuild();
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (minecraft != null && minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int right = left + WIDTH;
        int bottom = top + HEADER + visibleRows * ROW_HEIGHT + FOOTER;
        TechStyle.frame(graphics, left, top, right, bottom);
        int headerRight = right - 8 - tabsWidth() - 6;
        TechStyle.header(graphics, font, title, left + 8, top + 8, headerRight, 12);
        Component empty = Component.translatable("iceagesurvival.cryo.empty_count", emptyCapsules);
        graphics.drawString(font, empty, headerRight - font.width(empty), top + 8, RadarHud.SUBTLE);

        if (rows.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("iceagesurvival.cryo.none"),
                    left + WIDTH / 2, top + HEADER + 8, RadarHud.SUBTLE);
        }
        int textRight = right - 14 - BUTTON_WIDTH;
        for (int index = 0; index < visibleRows && scroll + index < rows.size(); index++) {
            Row row = rows.get(scroll + index);
            int y = top + HEADER + index * ROW_HEIGHT;
            if (mouseX >= left && mouseX < right && mouseY >= y && mouseY < y + ROW_HEIGHT) {
                graphics.fill(left + 1, y, right - 1, y + ROW_HEIGHT, TechStyle.HOVER);
            }
            if (row.capsule()) {
                graphics.fill(left + 1, y + 2, left + 3, y + ROW_HEIGHT - 2, RadarHud.ACCENT);
            }
            int x = left + 10;
            Component level = Component.translatable("iceagesurvival.hud.level", row.level());
            String name = font.plainSubstrByWidth(row.name(), textRight - x - font.width(level) - 6);
            graphics.drawString(font, name, x, y + 3, RadarHud.TEXT);
            graphics.drawString(font, level, x + font.width(name) + 6, y + 3, RadarHud.TARGET);
            Component detail = row.capsule() ? row.detail()
                    : Component.translatable("iceagesurvival.cryo.nearby").append(" · ").append(row.detail());
            graphics.drawString(font, detail, x, y + 12, RadarHud.SUBTLE);
            if (index > 0) {
                graphics.fill(left + 10, y, right - 8, y + 1, HudShapes.fade(RadarHud.ACCENT, 0.15F));
            }
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
