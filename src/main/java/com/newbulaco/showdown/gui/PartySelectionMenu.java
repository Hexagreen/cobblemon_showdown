package com.newbulaco.showdown.gui;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import org.jetbrains.annotations.NotNull;

// players select which Pokemon to use when their party size exceeds the format size
public class PartySelectionMenu extends ChestMenu {
    private final PartySelectionMenuProvider menuProvider;
    private boolean isValid = true;

    public PartySelectionMenu(PartySelectionMenuProvider menuProvider, int containerId, Inventory playerInventory) {
        super(MenuType.GENERIC_9x6, containerId, playerInventory, new SimpleContainer(9 * 6), 6);
        this.menuProvider = menuProvider;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        menuProvider.onPlayerCloseContainer();
    }

    public void invalidateMenu() {
        this.isValid = false;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return super.stillValid(player) && isValid;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (isValidMenuSlot(slotId) && slotId != -999) {
            // all click handling delegated to provider for selection, battle order, and ready logic
            menuProvider.onSlotClick(this, slotId);
        }
    }

    private boolean isValidMenuSlot(int slotId) {
        return slotId >= -999 && slotId < 54;
    }
}
