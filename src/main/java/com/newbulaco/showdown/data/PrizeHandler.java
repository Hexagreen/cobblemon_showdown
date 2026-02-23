package com.newbulaco.showdown.data;

import com.newbulaco.showdown.util.MessageUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrizeHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(PrizeHandler.class);

    public static boolean giveItem(ServerPlayer player, String itemId, int amount) {
        Item item = getItem(itemId);
        if (item == null) {
            LOGGER.error("Could not find item: {}", itemId);
            MessageUtil.error(player, "Prize distribution failed: unknown item " + itemId);
            return false;
        }

        int remaining = amount;
        int maxStackSize = item.getMaxStackSize();

        while (remaining > 0) {
            int stackAmount = Math.min(remaining, maxStackSize);
            ItemStack stack = new ItemStack(item, stackAmount);

            if (!player.getInventory().add(stack)) {
                // inventory full, drop on ground instead
                player.drop(stack, false);
                LOGGER.info("Dropped {} items on ground for {} (inventory full)", stackAmount, player.getName().getString());
            }

            remaining -= stackAmount;
        }

        LOGGER.info("Gave {}x {} to {}", amount, itemId, player.getName().getString());
        return true;
    }

    public static void transferBetItems(ServerPlayer winner, ServerPlayer loser,
                                         String itemId, int amount) {
        Item item = getItem(itemId);
        if (item == null) {
            LOGGER.error("Could not find bet item: {}", itemId);
            return;
        }

        int taken = takeItems(loser, item, amount);
        if (taken < amount) {
            LOGGER.warn("Could only take {} of {} bet items from {}",
                    taken, amount, loser.getName().getString());
        }

        if (taken > 0) {
            giveItem(winner, itemId, taken);
            MessageUtil.success(winner, "You won " + taken + "x " + getItemDisplayName(itemId) + " from the battle!");
            MessageUtil.warning(loser, "You lost " + taken + "x " + getItemDisplayName(itemId) + " from the battle.");
        }
    }

    public static int takeItems(ServerPlayer player, Item item, int amount) {
        int remaining = amount;

        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == item) {
                int toTake = Math.min(remaining, stack.getCount());
                stack.shrink(toTake);
                remaining -= toTake;

                if (stack.isEmpty()) {
                    player.getInventory().setItem(i, ItemStack.EMPTY);
                }
            }
        }

        return amount - remaining;
    }

    public static boolean hasItems(ServerPlayer player, String itemId, int amount) {
        Item item = getItem(itemId);
        if (item == null) {
            return false;
        }

        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == item) {
                count += stack.getCount();
                if (count >= amount) {
                    return true;
                }
            }
        }

        return count >= amount;
    }

    public static int countItems(ServerPlayer player, String itemId) {
        Item item = getItem(itemId);
        if (item == null) {
            return 0;
        }

        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == item) {
                count += stack.getCount();
            }
        }

        return count;
    }

    public static Item getItem(String itemId) {
        ResourceLocation location = new ResourceLocation(itemId);
        return ForgeRegistries.ITEMS.getValue(location);
    }

    public static String getItemDisplayName(String itemId) {
        Item item = getItem(itemId);
        if (item == null) {
            return itemId;
        }
        return item.getDescription().getString();
    }

    public static boolean isValidItem(String itemId) {
        return getItem(itemId) != null;
    }

    // expected format: "item_id,amount"
    public static ItemBet parseBet(String betString) {
        if (betString == null || betString.isEmpty()) {
            return null;
        }

        String[] parts = betString.split(",");
        if (parts.length != 2) {
            return null;
        }

        try {
            String itemId = parts[0].trim();
            int amount = Integer.parseInt(parts[1].trim());

            if (!isValidItem(itemId) || amount <= 0) {
                return null;
            }

            return new ItemBet(itemId, amount);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static class ItemBet {
        private final String itemId;
        private final int amount;

        public ItemBet(String itemId, int amount) {
            this.itemId = itemId;
            this.amount = amount;
        }

        public String getItemId() {
            return itemId;
        }

        public int getAmount() {
            return amount;
        }

        @Override
        public String toString() {
            return amount + "x " + getItemDisplayName(itemId);
        }
    }
}
