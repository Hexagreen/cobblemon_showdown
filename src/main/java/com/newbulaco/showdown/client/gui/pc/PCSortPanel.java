package com.newbulaco.showdown.client.gui.pc;

import com.cobblemon.mod.common.client.storage.ClientPC;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;

public class PCSortPanel {
    private static final Logger LOGGER = LoggerFactory.getLogger("CobblemonShowdown");

    private static final int TOGGLE_WIDTH = 32;
    private static final int TOGGLE_HEIGHT = 14;
    private static final int PANEL_WIDTH = 90;
    private static final int PANEL_HEIGHT = 155;
    private static final int BUTTON_HEIGHT = 12;
    private static final int BUTTON_SPACING = 1;

    private final int x;
    private final int y;
    private final ClientPC pc;
    private final IntSupplier currentBoxSupplier;

    private boolean expanded = false;
    private PCSortManager sortManager;

    private Button toggleButton;
    private Button ascDescButton;
    private Button sortBoxButton;
    private Button sortAllButton;
    private List<Button> criteriaButtons = new ArrayList<>();

    public PCSortPanel(int x, int y, ClientPC pc, IntSupplier currentBoxSupplier) {
        this.x = x;
        this.y = y;
        this.pc = pc;
        this.currentBoxSupplier = currentBoxSupplier;
        this.sortManager = new PCSortManager(pc, currentBoxSupplier.getAsInt());

        initButtons();
    }

    private void initButtons() {
        var font = Minecraft.getInstance().font;

        toggleButton = Button.builder(
            Component.literal("Sort").withStyle(ChatFormatting.WHITE),
            btn -> toggleExpanded()
        ).bounds(x, y, TOGGLE_WIDTH, TOGGLE_HEIGHT).build();
        toggleButton.setTooltip(Tooltip.create(Component.literal("Click to show sort options")));

        ascDescButton = Button.builder(
            Component.literal("▲").withStyle(ChatFormatting.GREEN),
            btn -> {
                sortManager.toggleAscending();
                updateAscDescButton();
            }
        ).bounds(x, y + TOGGLE_HEIGHT + 2, 20, BUTTON_HEIGHT).build();
        ascDescButton.setTooltip(Tooltip.create(Component.literal("Toggle ascending/descending")));

        sortBoxButton = Button.builder(
            Component.literal("Box").withStyle(ChatFormatting.AQUA),
            btn -> sortCurrentBox()
        ).bounds(x + 22, y + TOGGLE_HEIGHT + 2, 30, BUTTON_HEIGHT).build();
        sortBoxButton.setTooltip(Tooltip.create(Component.literal("Sort current box")));

        sortAllButton = Button.builder(
            Component.literal("All").withStyle(ChatFormatting.GOLD),
            btn -> sortAllBoxes()
        ).bounds(x + 54, y + TOGGLE_HEIGHT + 2, 30, BUTTON_HEIGHT).build();
        sortAllButton.setTooltip(Tooltip.create(Component.literal("Sort all boxes")));

        int buttonY = y + TOGGLE_HEIGHT + 2 + BUTTON_HEIGHT + BUTTON_SPACING + 2;
        for (PCSortManager.SortCriteria criteria : PCSortManager.SortCriteria.values()) {
            final PCSortManager.SortCriteria c = criteria;
            Button btn = Button.builder(
                Component.literal(getShortName(criteria)),
                b -> selectCriteria(c)
            ).bounds(x, buttonY, PANEL_WIDTH - 6, BUTTON_HEIGHT).build();
            btn.setTooltip(Tooltip.create(Component.literal("Sort by " + criteria.getDisplayName())));
            criteriaButtons.add(btn);
            buttonY += BUTTON_HEIGHT + BUTTON_SPACING;
        }

        updateCriteriaButtonStyles();
    }

    private String getShortName(PCSortManager.SortCriteria criteria) {
        switch (criteria) {
            case IV_TOTAL: return "IV Total";
            case BST: return "Base Stats";
            case DEX_NUMBER: return "Dex #";
            case NAME: return "Name";
            case LEVEL: return "Level";
            case GENDER: return "Gender";
            case SHINY: return "Shiny";
            case CATCH_DATE: return "Catch Date";
            case FRIENDSHIP: return "Friendship";
            case NATURE: return "Nature";
            default: return criteria.getDisplayName();
        }
    }

    private void toggleExpanded() {
        expanded = !expanded;
        if (expanded) {
            toggleButton.setMessage(Component.literal("Sort ▼").withStyle(ChatFormatting.WHITE));
        } else {
            toggleButton.setMessage(Component.literal("Sort").withStyle(ChatFormatting.WHITE));
        }
    }

    private void updateAscDescButton() {
        if (sortManager.isAscending()) {
            ascDescButton.setMessage(Component.literal("▲").withStyle(ChatFormatting.GREEN));
            ascDescButton.setTooltip(Tooltip.create(Component.literal("Ascending (click to change)")));
        } else {
            ascDescButton.setMessage(Component.literal("▼").withStyle(ChatFormatting.RED));
            ascDescButton.setTooltip(Tooltip.create(Component.literal("Descending (click to change)")));
        }
    }

    private void selectCriteria(PCSortManager.SortCriteria criteria) {
        sortManager.setCriteria(criteria);
        updateCriteriaButtonStyles();
    }

    private void updateCriteriaButtonStyles() {
        PCSortManager.SortCriteria current = sortManager.getCriteria();
        int index = 0;
        for (PCSortManager.SortCriteria criteria : PCSortManager.SortCriteria.values()) {
            Button btn = criteriaButtons.get(index);
            if (criteria == current) {
                btn.setMessage(Component.literal("● " + getShortName(criteria)).withStyle(ChatFormatting.YELLOW));
            } else {
                btn.setMessage(Component.literal("  " + getShortName(criteria)).withStyle(ChatFormatting.GRAY));
            }
            index++;
        }
    }

    private void sortCurrentBox() {
        if (sortManager.isSorting()) return;

        // save current settings before creating new manager, otherwise defaults are used
        PCSortManager.SortCriteria savedCriteria = sortManager.getCriteria();
        boolean savedAscending = sortManager.isAscending();

        sortManager = new PCSortManager(pc, currentBoxSupplier.getAsInt());
        sortManager.setCriteria(savedCriteria);
        sortManager.setAscending(savedAscending);

        sortManager.sortCurrentBox().thenRun(() -> {
            LOGGER.info("[Showdown] Box sort completed");
        });
    }

    private void sortAllBoxes() {
        if (sortManager.isSorting()) return;

        sortManager.sortAllBoxes().thenRun(() -> {
            LOGGER.info("[Showdown] All boxes sort completed");
        });
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // high z-level to render above PC GUI elements (3D model, text, etc.)
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 400);

        toggleButton.render(graphics, mouseX, mouseY, delta);

        if (!expanded) {
            graphics.pose().popPose();
            return;
        }

        int panelX = x - 2;
        int panelY = y + TOGGLE_HEIGHT;
        int panelHeight = PANEL_HEIGHT;

        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + panelHeight, 0xDD000000);
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + 1, 0xFF444444);
        graphics.fill(panelX, panelY + panelHeight - 1, panelX + PANEL_WIDTH, panelY + panelHeight, 0xFF444444);
        graphics.fill(panelX, panelY, panelX + 1, panelY + panelHeight, 0xFF444444);
        graphics.fill(panelX + PANEL_WIDTH - 1, panelY, panelX + PANEL_WIDTH, panelY + panelHeight, 0xFF444444);

        if (sortManager.isSorting()) {
            graphics.drawString(Minecraft.getInstance().font,
                "Sorting...", x + 2, y + TOGGLE_HEIGHT + 4, 0xFFFFFF00);
            graphics.pose().popPose();
            return;
        }

        ascDescButton.render(graphics, mouseX, mouseY, delta);
        sortBoxButton.render(graphics, mouseX, mouseY, delta);
        sortAllButton.render(graphics, mouseX, mouseY, delta);

        for (Button btn : criteriaButtons) {
            btn.render(graphics, mouseX, mouseY, delta);
        }

        graphics.pose().popPose();
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (toggleButton.isMouseOver(mouseX, mouseY)) {
            toggleButton.onClick(mouseX, mouseY);
            return true;
        }

        if (!expanded) return false;

        if (ascDescButton.isMouseOver(mouseX, mouseY)) {
            ascDescButton.onClick(mouseX, mouseY);
            return true;
        }
        if (sortBoxButton.isMouseOver(mouseX, mouseY)) {
            sortBoxButton.onClick(mouseX, mouseY);
            return true;
        }
        if (sortAllButton.isMouseOver(mouseX, mouseY)) {
            sortAllButton.onClick(mouseX, mouseY);
            return true;
        }

        for (Button btn : criteriaButtons) {
            if (btn.isMouseOver(mouseX, mouseY)) {
                btn.onClick(mouseX, mouseY);
                return true;
            }
        }

        return false;
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        if (toggleButton.isMouseOver(mouseX, mouseY)) return true;

        if (expanded) {
            int panelX = x - 2;
            int panelY = y + TOGGLE_HEIGHT;
            return mouseX >= panelX && mouseX <= panelX + PANEL_WIDTH
                && mouseY >= panelY && mouseY <= panelY + PANEL_HEIGHT;
        }

        return false;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
        if (expanded) {
            toggleButton.setMessage(Component.literal("Sort ▼").withStyle(ChatFormatting.WHITE));
        } else {
            toggleButton.setMessage(Component.literal("Sort").withStyle(ChatFormatting.WHITE));
        }
    }
}
