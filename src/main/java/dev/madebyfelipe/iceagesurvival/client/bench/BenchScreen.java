package dev.madebyfelipe.iceagesurvival.client.bench;

import dev.madebyfelipe.iceagesurvival.defense.bench.BenchMenu;
import dev.madebyfelipe.iceagesurvival.client.TechStyle;
import dev.madebyfelipe.iceagesurvival.defense.bench.BenchRecipe;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Tela de bancada: grade com as receitas da bancada (como o cortador de pedra) sobre o inventário. O que dá para
 * fazer com o inventário fica com borda verde; o resto, apagado. Clique fabrica uma vez, Shift + clique até encher
 * uma pilha; o servidor confere e paga. Desenhada em código, no visual de aparelho das outras telas
 * ({@link TechStyle}, sem textura de GUI).
 */
public class BenchScreen extends AbstractContainerScreen<BenchMenu> {
    private static final int SUBTLE = TechStyle.SUBTLE;
    private static final int CRAFTABLE = 0xFF143020;
    private static final int DIM = 0xA0060C0A;

    private static final int GRID_X = 8;
    private static final int GRID_Y = 17;
    private static final int CELL = 18;
    private static final int COLUMNS = 8;
    private static final int ROWS = 3;
    private static final int SCROLL_X = GRID_X + COLUMNS * CELL + 4;

    /** Primeira linha visível da grade. */
    private int firstRow;

    public BenchScreen(BenchMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = imageHeight - 94;
    }

    private int totalRows() {
        return (menu.recipes().size() + COLUMNS - 1) / COLUMNS;
    }

    /** Índice da receita sob o mouse, ou -1. */
    private int recipeAt(double mouseX, double mouseY) {
        int x = (int) Math.floor(mouseX) - leftPos - GRID_X;
        int y = (int) Math.floor(mouseY) - topPos - GRID_Y;
        if (x < 0 || y < 0 || x >= COLUMNS * CELL || y >= ROWS * CELL) {
            return -1;
        }
        int index = (firstRow + y / CELL) * COLUMNS + x / CELL;
        return index < menu.recipes().size() ? index : -1;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        TechStyle.frame(graphics, leftPos, topPos, leftPos + imageWidth, topPos + imageHeight);
        for (Slot slot : menu.slots) {
            TechStyle.slot(graphics, leftPos + slot.x, topPos + slot.y);
        }
        List<BenchRecipe> recipes = menu.recipes();
        if (recipes.isEmpty()) {
            graphics.drawString(font, Component.translatable("gui.iceagesurvival.bench.empty"),
                    leftPos + GRID_X, topPos + GRID_Y + 4, SUBTLE, false);
            return;
        }
        int hovered = recipeAt(mouseX, mouseY);
        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                int index = (firstRow + row) * COLUMNS + column;
                if (index >= recipes.size()) {
                    break;
                }
                BenchRecipe recipe = recipes.get(index);
                boolean craftable = menu.canCraft(recipe);
                int x = leftPos + GRID_X + column * CELL;
                int y = topPos + GRID_Y + row * CELL;
                graphics.fill(x, y, x + CELL, y + CELL, index == hovered ? TechStyle.BRIGHT
                        : craftable ? TechStyle.ACCENT : TechStyle.SLOT_EDGE);
                graphics.fill(x + 1, y + 1, x + CELL - 1, y + CELL - 1, craftable ? CRAFTABLE : TechStyle.SLOT);
                graphics.renderItem(recipe.result(), x + 1, y + 1);
                graphics.renderItemDecorations(font, recipe.result(), x + 1, y + 1);
                if (!craftable) {
                    // Por cima do item e do número: o que falta ingrediente fica apagado.
                    graphics.pose().pushPose();
                    graphics.pose().translate(0, 0, 300);
                    graphics.fill(x + 1, y + 1, x + CELL - 1, y + CELL - 1, DIM);
                    graphics.pose().popPose();
                }
            }
        }
        int rows = totalRows();
        if (rows > ROWS) {
            int height = ROWS * CELL;
            int thumb = Math.max(6, height * ROWS / rows);
            int offset = (height - thumb) * firstRow / (rows - ROWS);
            graphics.fill(leftPos + SCROLL_X + 2, topPos + GRID_Y, leftPos + SCROLL_X + 4, topPos + GRID_Y + height,
                    TechStyle.SLOT_EDGE);
            graphics.fill(leftPos + SCROLL_X + 1, topPos + GRID_Y + offset, leftPos + SCROLL_X + 5,
                    topPos + GRID_Y + offset + thumb, TechStyle.BRIGHT);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        TechStyle.titleLine(graphics, font, title, titleLabelX, titleLabelY, imageWidth - 8);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, SUBTLE, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        int index = recipeAt(mouseX, mouseY);
        if (index >= 0 && menu.getCarried().isEmpty()) {
            graphics.renderComponentTooltip(font, tooltip(menu.recipes().get(index)), mouseX, mouseY);
        } else {
            renderTooltip(graphics, mouseX, mouseY);
        }
    }

    /** Nome do resultado e, por ingrediente, "nome: tem/precisa" em verde ou vermelho. */
    private List<Component> tooltip(BenchRecipe recipe) {
        List<Component> lines = new ArrayList<>();
        ItemStack result = recipe.result();
        Component name = result.getHoverName();
        lines.add(result.getCount() > 1 ? Component.literal(result.getCount() + "× ").append(name) : name);
        List<ItemStack> items = menu.playerItems();
        for (BenchRecipe.Cost cost : recipe.costs()) {
            int have = BenchRecipe.available(items, cost);
            lines.add(Component.translatable("gui.iceagesurvival.bench.ingredient", displayName(cost), have, cost.count())
                    .withStyle(have >= cost.count() ? ChatFormatting.GREEN : ChatFormatting.RED));
        }
        lines.add(Component.translatable("gui.iceagesurvival.bench.shift").withStyle(ChatFormatting.DARK_GRAY));
        return lines;
    }

    /** Um item que serve ao ingrediente; numa tag, troca a cada segundo. */
    private static Component displayName(BenchRecipe.Cost cost) {
        ItemStack[] options = cost.ingredient().getItems();
        if (options.length == 0) {
            return Component.literal("?");
        }
        return options[(int) (System.currentTimeMillis() / 1000 % options.length)].getHoverName();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int index = recipeAt(mouseX, mouseY);
        if (button == 0 && index >= 0 && minecraft != null && minecraft.gameMode != null) {
            if (menu.canCraft(menu.recipes().get(index))) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, BenchMenu.buttonId(index, hasShiftDown()));
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int rows = totalRows();
        if (rows > ROWS) {
            firstRow = Mth.clamp(firstRow - (int) Math.signum(delta), 0, rows - ROWS);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
}
