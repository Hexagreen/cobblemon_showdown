package com.newbulaco.showdown.gui;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.CobblemonItems;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.item.PokemonItem;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.newbulaco.showdown.format.Format;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * layout: player's Pokemon on left (col 0), opponent's on right (col 8),
 * selected Pokemon in battle order in center arena, manual ready button
 */
public class PartySelectionMenuProvider implements MenuProvider {
    private final ServerPlayer selector;
    private final ServerPlayer opponent;
    private final PartySelectionSession session;
    private final Format format;
    private final int maxSelections;

    private PlayerPartyStore party;
    private PartySelectionMenu openedMenu;
    // LinkedHashSet preserves insertion order -- first clicked = lead Pokemon
    private final Set<Integer> selectedSlots = new LinkedHashSet<>();
    private int opponentSelectedCount = 0;
    private boolean opponentReady = false;
    private boolean playerReady = false;
    private boolean guiModifierFlag = false;

    // center arena slots where selected Pokemon appear in battle order
    // row 1: 11, 12, 13, 14 / row 2: 20, 21 (up to 6 Pokemon)
    private static final int[] BATTLE_ORDER_SLOTS = {11, 12, 13, 14, 20, 21};

    private static final int READY_BUTTON_SLOT = 31;
    private static final int FORMAT_INFO_SLOT = 22;
    private static final int TIMER_SLOT = 40;
    private static final int PLAYER_STATUS_SLOT = 29;
    private static final int OPPONENT_STATUS_SLOT = 33;

    public PartySelectionMenuProvider(PartySelectionSession session, ServerPlayer selector,
                                       ServerPlayer opponent, Format format) {
        this.session = session;
        this.selector = selector;
        this.opponent = opponent;
        this.format = format;
        this.maxSelections = format.getPartySize();
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.literal("⚔ Battle Arena - Select " + maxSelections + " Pokemon ⚔");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        PartySelectionMenu menu = new PartySelectionMenu(this, containerId, playerInventory);
        setupMenuContents(menu);
        this.openedMenu = menu;
        return menu;
    }

    private void setupMenuContents(PartySelectionMenu menu) {
        party = Cobblemon.INSTANCE.getStorage().getParty(selector);
        PlayerPartyStore opponentParty = Cobblemon.INSTANCE.getStorage().getParty(opponent);

        setupBackground(menu);

        // player's Pokemon on the left column (col 0)
        for (int i = 0; i < 6; i++) {
            int itemSlot = i * 9;
            Pokemon pokemon = party.get(i);
            if (pokemon != null) {
                ItemStack pokemonItem = createPokemonDisplayItem(pokemon, i);
                menu.setItem(itemSlot, menu.getStateId(), pokemonItem);
            } else {
                ItemStack empty = new ItemStack(Items.LIGHT_GRAY_STAINED_GLASS_PANE);
                empty.setHoverName(Component.literal(ChatFormatting.GRAY + "Empty Slot"));
                menu.setItem(itemSlot, menu.getStateId(), empty);
            }
        }

        // opponent's Pokemon on the right column (col 8)
        for (int i = 0; i < 6; i++) {
            int itemSlot = (i * 9) + 8;
            Pokemon pokemon = opponentParty.get(i);
            if (pokemon != null) {
                if (format.hasTeamPreview()) {
                    ItemStack pokemonItem = createOpponentPokemonDisplayItem(pokemon, opponent.getName().getString());
                    menu.setItem(itemSlot, menu.getStateId(), pokemonItem);
                } else {
                    ItemStack pokeballItem = new ItemStack(CobblemonItems.POKE_BALL.asItem());
                    pokeballItem.setHoverName(Component.literal(ChatFormatting.GOLD +
                        opponent.getName().getString() + "'s Pokemon"));
                    menu.setItem(itemSlot, menu.getStateId(), pokeballItem);
                }
            } else {
                ItemStack empty = new ItemStack(Items.LIGHT_GRAY_STAINED_GLASS_PANE);
                empty.setHoverName(Component.literal(ChatFormatting.GRAY + "Empty Slot"));
                menu.setItem(itemSlot, menu.getStateId(), empty);
            }
        }

        setupBattleOrderDisplay(menu);
        setupFormatInfo(menu);
        setupReadyButton(menu);
        setupStatusDisplays(menu);
    }

    private void setupBackground(PartySelectionMenu menu) {
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                int slot = row * 9 + col;
                ItemStack filler;

                if (col == 1) {
                    filler = guiModifierFlag ?
                        new ItemStack(Items.LIGHT_BLUE_STAINED_GLASS_PANE) :
                        new ItemStack(Items.CYAN_STAINED_GLASS_PANE);
                    filler.setHoverName(Component.literal(ChatFormatting.AQUA + "Your Side"));
                } else if (col == 7) {
                    filler = guiModifierFlag ?
                        new ItemStack(Items.YELLOW_STAINED_GLASS_PANE) :
                        new ItemStack(Items.ORANGE_STAINED_GLASS_PANE);
                    filler.setHoverName(Component.literal(ChatFormatting.GOLD + opponent.getName().getString() + "'s Side"));
                } else if (col >= 2 && col <= 6) {
                    filler = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
                    filler.setHoverName(Component.literal(ChatFormatting.DARK_GRAY + "Battle Arena"));
                } else {
                    continue;
                }

                menu.setItem(slot, menu.getStateId(), filler);
            }
        }
    }

    private void setupBattleOrderDisplay(PartySelectionMenu menu) {
        ItemStack arenaTitle = new ItemStack(Items.NETHER_STAR);
        arenaTitle.setHoverName(Component.literal(ChatFormatting.GOLD + "★ " + ChatFormatting.WHITE + "Battle Order" + ChatFormatting.GOLD + " ★"));
        ListTag titleLore = new ListTag();
        titleLore.add(StringTag.valueOf(Component.Serializer.toJson(
            Component.literal(ChatFormatting.GRAY + "Your selected Pokemon appear below"))));
        titleLore.add(StringTag.valueOf(Component.Serializer.toJson(
            Component.literal(ChatFormatting.GRAY + "First selected = Lead Pokemon"))));
        arenaTitle.getOrCreateTagElement("display").put("Lore", titleLore);
        menu.setItem(4, menu.getStateId(), arenaTitle);

        List<Integer> selectedList = new ArrayList<>(selectedSlots);
        for (int i = 0; i < BATTLE_ORDER_SLOTS.length && i < maxSelections; i++) {
            int displaySlot = BATTLE_ORDER_SLOTS[i];

            if (i < selectedList.size()) {
                int partySlot = selectedList.get(i);
                Pokemon pokemon = party.get(partySlot);
                if (pokemon != null) {
                    ItemStack pokemonItem = createBattleOrderItem(pokemon, i + 1, partySlot);
                    menu.setItem(displaySlot, menu.getStateId(), pokemonItem);
                }
            } else {
                ItemStack emptySlot = new ItemStack(Items.BLACK_STAINED_GLASS_PANE);
                String slotLabel = (i == 0) ? "LEAD" : "#" + (i + 1);
                emptySlot.setHoverName(Component.literal(ChatFormatting.DARK_GRAY + "[" + slotLabel + "] Empty"));
                ListTag lore = new ListTag();
                lore.add(StringTag.valueOf(Component.Serializer.toJson(
                    Component.literal(ChatFormatting.GRAY + "Select a Pokemon from the left"))));
                emptySlot.getOrCreateTagElement("display").put("Lore", lore);
                menu.setItem(displaySlot, menu.getStateId(), emptySlot);
            }
        }
    }

    private ItemStack createBattleOrderItem(Pokemon pokemon, int order, int partySlot) {
        Pokemon displayPokemon = pokemon;
        if (format.getSetLevel() > 0) {
            BattlePokemon copy = BattlePokemon.Companion.safeCopyOf(pokemon);
            copy.getEffectedPokemon().setLevel(format.getSetLevel());
            displayPokemon = copy.getEffectedPokemon();
        }

        ItemStack item = PokemonItem.from(displayPokemon, 1);

        String orderLabel = (order == 1) ? "★ LEAD" : "#" + order;
        ChatFormatting color = (order == 1) ? ChatFormatting.GOLD : ChatFormatting.GREEN;
        item.setHoverName(Component.literal(color + "[" + orderLabel + "] " + ChatFormatting.WHITE + displayPokemon.getDisplayName().getString()));

        ListTag lore = new ListTag();
        lore.add(StringTag.valueOf(Component.Serializer.toJson(
            Component.literal(ChatFormatting.YELLOW + "Click to remove from battle order"))));
        lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(""))));

        StringBuilder typeStr = new StringBuilder();
        typeStr.append(displayPokemon.getPrimaryType().getDisplayName().getString());
        if (displayPokemon.getSecondaryType() != null) {
            typeStr.append("/").append(displayPokemon.getSecondaryType().getDisplayName().getString());
        }
        lore.add(StringTag.valueOf(Component.Serializer.toJson(
            Component.literal(ChatFormatting.GRAY + "Type: " + ChatFormatting.WHITE + typeStr))));

        item.getOrCreateTag().putInt("partySlot", partySlot);
        item.getOrCreateTagElement("display").put("Lore", lore);
        return item;
    }

    private void setupFormatInfo(PartySelectionMenu menu) {
        ItemStack formatItem = new ItemStack(Items.BOOK);
        formatItem.setHoverName(Component.literal(ChatFormatting.AQUA + "Format: " + ChatFormatting.WHITE + format.getName()));

        ListTag lore = new ListTag();
        lore.add(StringTag.valueOf(Component.Serializer.toJson(
            Component.literal(ChatFormatting.GRAY + "Party Size: " + ChatFormatting.WHITE + format.getPartySize()))));

        if (format.getSetLevel() > 0) {
            lore.add(StringTag.valueOf(Component.Serializer.toJson(
                Component.literal(ChatFormatting.GRAY + "Level Cap: " + ChatFormatting.WHITE + format.getSetLevel()))));
        }

        if (format.getBestOf() > 1) {
            lore.add(StringTag.valueOf(Component.Serializer.toJson(
                Component.literal(ChatFormatting.GRAY + "Best of: " + ChatFormatting.WHITE + format.getBestOf()))));
        }

        if (format.hasSpeciesClause()) {
            lore.add(StringTag.valueOf(Component.Serializer.toJson(
                Component.literal(ChatFormatting.YELLOW + "Species Clause Active"))));
        }

        if (format.hasBattleTimer()) {
            lore.add(StringTag.valueOf(Component.Serializer.toJson(
                Component.literal(ChatFormatting.YELLOW + "Battle Timer Active"))));
        }

        if (format.hasTeamPreview()) {
            lore.add(StringTag.valueOf(Component.Serializer.toJson(
                Component.literal(ChatFormatting.GREEN + "Team Preview Enabled"))));
        }

        if (format.getBans() != null) {
            var bans = format.getBans();
            int totalBans = 0;
            if (bans.getPokemon() != null) totalBans += bans.getPokemon().size();
            if (bans.getMoves() != null) totalBans += bans.getMoves().size();
            if (bans.getAbilities() != null) totalBans += bans.getAbilities().size();
            if (bans.getItems() != null) totalBans += bans.getItems().size();

            if (totalBans > 0) {
                lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(""))));
                lore.add(StringTag.valueOf(Component.Serializer.toJson(
                    Component.literal(ChatFormatting.RED + "Bans: " + totalBans + " total restrictions"))));
            }
        }

        formatItem.getOrCreateTagElement("display").put("Lore", lore);
        menu.setItem(FORMAT_INFO_SLOT, menu.getStateId(), formatItem);
    }

    private void setupReadyButton(PartySelectionMenu menu) {
        ItemStack readyButton;
        boolean canReady = selectedSlots.size() == maxSelections;

        if (playerReady) {
            readyButton = new ItemStack(Items.LIME_CONCRETE);
            readyButton.setHoverName(Component.literal(ChatFormatting.GREEN + "✓ READY!"));
            ListTag lore = new ListTag();
            lore.add(StringTag.valueOf(Component.Serializer.toJson(
                Component.literal(ChatFormatting.GRAY + "Waiting for opponent..."))));
            lore.add(StringTag.valueOf(Component.Serializer.toJson(
                Component.literal(ChatFormatting.YELLOW + "Click to cancel ready"))));
            readyButton.getOrCreateTagElement("display").put("Lore", lore);
        } else if (canReady) {
            readyButton = new ItemStack(Items.EMERALD_BLOCK);
            readyButton.setHoverName(Component.literal(ChatFormatting.GREEN + "▶ CLICK WHEN READY"));
            ListTag lore = new ListTag();
            lore.add(StringTag.valueOf(Component.Serializer.toJson(
                Component.literal(ChatFormatting.WHITE + "You have selected " + maxSelections + " Pokemon"))));
            lore.add(StringTag.valueOf(Component.Serializer.toJson(
                Component.literal(ChatFormatting.YELLOW + "Click to confirm your selection!"))));
            readyButton.getOrCreateTagElement("display").put("Lore", lore);
        } else {
            readyButton = new ItemStack(Items.BARRIER);
            readyButton.setHoverName(Component.literal(ChatFormatting.RED + "✗ NOT READY"));
            ListTag lore = new ListTag();
            lore.add(StringTag.valueOf(Component.Serializer.toJson(
                Component.literal(ChatFormatting.WHITE + "Selected: " + selectedSlots.size() + "/" + maxSelections))));
            lore.add(StringTag.valueOf(Component.Serializer.toJson(
                Component.literal(ChatFormatting.GRAY + "Select more Pokemon to continue"))));
            readyButton.getOrCreateTagElement("display").put("Lore", lore);
        }

        menu.setItem(READY_BUTTON_SLOT, menu.getStateId(), readyButton);
    }

    private void setupStatusDisplays(PartySelectionMenu menu) {
        int timeLeft = (int) Math.ceil(
            ((session.getCreationTime() + PartySelectionSession.SELECTION_TIMEOUT_MILLIS) - System.currentTimeMillis()) / 1000f);

        ItemStack timerItem = new ItemStack(Items.CLOCK);
        ChatFormatting timerColor = timeLeft > 30 ? ChatFormatting.GREEN : (timeLeft > 10 ? ChatFormatting.YELLOW : ChatFormatting.RED);
        timerItem.setHoverName(Component.literal(timerColor + "⏱ " + timeLeft + " seconds remaining"));
        menu.setItem(TIMER_SLOT, menu.getStateId(), timerItem);

        ItemStack playerStatus = new ItemStack(playerReady ? Items.LIME_DYE : Items.LIGHT_BLUE_DYE);
        String playerStatusText = playerReady ? ChatFormatting.GREEN + "READY ✓" : ChatFormatting.WHITE.toString() + selectedSlots.size() + "/" + maxSelections;
        playerStatus.setHoverName(Component.literal(ChatFormatting.AQUA + "You: " + playerStatusText));
        menu.setItem(PLAYER_STATUS_SLOT, menu.getStateId(), playerStatus);

        ItemStack opponentStatus = new ItemStack(opponentReady ? Items.LIME_DYE : Items.ORANGE_DYE);
        String opponentStatusText = opponentReady ? ChatFormatting.GREEN + "READY ✓" : ChatFormatting.WHITE.toString() + opponentSelectedCount + "/" + maxSelections;
        opponentStatus.setHoverName(Component.literal(ChatFormatting.GOLD + opponent.getName().getString() + ": " + opponentStatusText));
        menu.setItem(OPPONENT_STATUS_SLOT, menu.getStateId(), opponentStatus);
    }

    private ItemStack createOpponentPokemonDisplayItem(Pokemon pokemon, String ownerName) {
        Pokemon displayPokemon = pokemon;
        if (format.getSetLevel() > 0) {
            BattlePokemon copy = BattlePokemon.Companion.safeCopyOf(pokemon);
            copy.getEffectedPokemon().setLevel(format.getSetLevel());
            displayPokemon = copy.getEffectedPokemon();
        }

        ItemStack item = PokemonItem.from(displayPokemon, 1);

        String levelStr = format.getSetLevel() > 0 ?
            " (Lv." + format.getSetLevel() + ")" : " (Lv." + pokemon.getLevel() + ")";
        item.setHoverName(Component.literal(ChatFormatting.GOLD + ownerName + "'s " +
            displayPokemon.getDisplayName().getString() + levelStr));

        ListTag loreTag = new ListTag();
        loreTag.add(StringTag.valueOf(Component.Serializer.toJson(
            Component.literal(ChatFormatting.DARK_PURPLE + "Opponent's Pokemon"))));
        loreTag.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(""))));

        StringBuilder typeStr = new StringBuilder();
        typeStr.append(displayPokemon.getPrimaryType().getDisplayName().getString());
        if (displayPokemon.getSecondaryType() != null) {
            typeStr.append("/").append(displayPokemon.getSecondaryType().getDisplayName().getString());
        }
        loreTag.add(StringTag.valueOf(Component.Serializer.toJson(
            Component.literal(ChatFormatting.GRAY + "Type: " + ChatFormatting.WHITE + typeStr))));

        if (displayPokemon.getAbility() != null) {
            String abilityName = displayPokemon.getAbility().getDisplayName();
            if (!abilityName.isEmpty()) {
                loreTag.add(StringTag.valueOf(Component.Serializer.toJson(
                    Component.literal(ChatFormatting.GRAY + "Ability: " + ChatFormatting.GOLD + abilityName))));
            }
        }

        if (!displayPokemon.heldItem().isEmpty()) {
            loreTag.add(StringTag.valueOf(Component.Serializer.toJson(
                Component.literal(ChatFormatting.GRAY + "Item: " + ChatFormatting.AQUA +
                    displayPokemon.heldItem().getHoverName().getString()))));
        }

        loreTag.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(""))));
        loreTag.add(StringTag.valueOf(Component.Serializer.toJson(
            Component.literal(ChatFormatting.GRAY + "Moves:"))));
        displayPokemon.getMoveSet().getMoves().forEach(move -> {
            loreTag.add(StringTag.valueOf(Component.Serializer.toJson(
                Component.literal(ChatFormatting.WHITE + "  - " + move.getDisplayName().getString()))));
        });

        item.getOrCreateTagElement("display").put("Lore", loreTag);
        return item;
    }

    private ItemStack createPokemonDisplayItem(Pokemon pokemon, int slot) {
        Pokemon displayPokemon = pokemon;
        if (format.getSetLevel() > 0) {
            BattlePokemon copy = BattlePokemon.Companion.safeCopyOf(pokemon);
            copy.getEffectedPokemon().setLevel(format.getSetLevel());
            displayPokemon = copy.getEffectedPokemon();
        }

        ItemStack item = PokemonItem.from(displayPokemon, 1);

        int selectionOrder = getSelectionOrder(slot);
        boolean isSelected = selectionOrder > 0;

        ChatFormatting color = isSelected ? ChatFormatting.GREEN : ChatFormatting.AQUA;
        String levelStr = format.getSetLevel() > 0 ?
            " (Lv." + format.getSetLevel() + ")" : " (Lv." + pokemon.getLevel() + ")";
        String orderStr = isSelected ? " [#" + selectionOrder + "]" : "";
        String leadStr = (selectionOrder == 1) ? " ★LEAD" : "";
        item.setHoverName(Component.literal(color + displayPokemon.getDisplayName().getString() + levelStr + orderStr + leadStr));

        ListTag loreTag = new ListTag();

        if (isSelected) {
            if (selectionOrder == 1) {
                loreTag.add(StringTag.valueOf(Component.Serializer.toJson(
                    Component.literal(ChatFormatting.GOLD + "★ LEAD POKEMON"))));
            } else {
                loreTag.add(StringTag.valueOf(Component.Serializer.toJson(
                    Component.literal(ChatFormatting.GREEN + "Battle Position #" + selectionOrder))));
            }
            loreTag.add(StringTag.valueOf(Component.Serializer.toJson(
                Component.literal(ChatFormatting.YELLOW + "Click to deselect"))));
        } else if (selectedSlots.size() >= maxSelections) {
            loreTag.add(StringTag.valueOf(Component.Serializer.toJson(
                Component.literal(ChatFormatting.RED + "Max selections reached!"))));
            loreTag.add(StringTag.valueOf(Component.Serializer.toJson(
                Component.literal(ChatFormatting.GRAY + "Deselect another Pokemon first"))));
        } else {
            String hint = selectedSlots.isEmpty() ? "Click to select as LEAD" : "Click to add to team";
            loreTag.add(StringTag.valueOf(Component.Serializer.toJson(
                Component.literal(ChatFormatting.YELLOW + hint))));
        }

        loreTag.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(""))));

        StringBuilder typeStr = new StringBuilder();
        typeStr.append(displayPokemon.getPrimaryType().getDisplayName().getString());
        if (displayPokemon.getSecondaryType() != null) {
            typeStr.append("/").append(displayPokemon.getSecondaryType().getDisplayName().getString());
        }
        loreTag.add(StringTag.valueOf(Component.Serializer.toJson(
            Component.literal(ChatFormatting.GRAY + "Type: " + ChatFormatting.WHITE + typeStr))));

        int healthPercent = (int) ((displayPokemon.getCurrentHealth() / (float) displayPokemon.getHp()) * 100);
        ChatFormatting hpColor = healthPercent > 50 ? ChatFormatting.GREEN : (healthPercent > 25 ? ChatFormatting.YELLOW : ChatFormatting.RED);
        loreTag.add(StringTag.valueOf(Component.Serializer.toJson(
            Component.literal(ChatFormatting.GRAY + "HP: " + hpColor +
                displayPokemon.getCurrentHealth() + "/" + displayPokemon.getHp() + " (" + healthPercent + "%)"))));

        if (displayPokemon.getAbility() != null) {
            String abilityName = displayPokemon.getAbility().getDisplayName();
            if (!abilityName.isEmpty()) {
                loreTag.add(StringTag.valueOf(Component.Serializer.toJson(
                    Component.literal(ChatFormatting.GRAY + "Ability: " + ChatFormatting.GOLD + abilityName))));
            }
        }

        if (displayPokemon.getNature() != null) {
            loreTag.add(StringTag.valueOf(Component.Serializer.toJson(
                Component.literal(ChatFormatting.GRAY + "Nature: " + ChatFormatting.LIGHT_PURPLE +
                    displayPokemon.getNature().getDisplayName()))));
        }

        if (!displayPokemon.heldItem().isEmpty()) {
            loreTag.add(StringTag.valueOf(Component.Serializer.toJson(
                Component.literal(ChatFormatting.GRAY + "Item: " + ChatFormatting.AQUA +
                    displayPokemon.heldItem().getHoverName().getString()))));
        }

        loreTag.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(""))));
        loreTag.add(StringTag.valueOf(Component.Serializer.toJson(
            Component.literal(ChatFormatting.GRAY + "Moves:"))));
        displayPokemon.getMoveSet().getMoves().forEach(move -> {
            loreTag.add(StringTag.valueOf(Component.Serializer.toJson(
                Component.literal(ChatFormatting.WHITE + "  - " + move.getDisplayName().getString()))));
        });

        item.getOrCreateTagElement("display").put("Lore", loreTag);
        return item;
    }

    // returns 1-based selection order, or 0 if not selected
    private int getSelectionOrder(int slot) {
        int order = 1;
        for (Integer selected : selectedSlots) {
            if (selected.equals(slot)) {
                return order;
            }
            order++;
        }
        return 0;
    }

    public void onSlotClick(PartySelectionMenu menu, int slot) {
        // left column = player Pokemon slot
        if (slot % 9 == 0 && slot < 54) {
            int partyIndex = slot / 9;
            onSelectPokemonSlot(menu, partyIndex);
            return;
        }

        for (int i = 0; i < BATTLE_ORDER_SLOTS.length; i++) {
            if (slot == BATTLE_ORDER_SLOTS[i]) {
                onBattleOrderSlotClick(menu, slot);
                return;
            }
        }

        if (slot == READY_BUTTON_SLOT) {
            onReadyButtonClick();
            return;
        }
    }

    public void onSelectPokemonSlot(PartySelectionMenu menu, int partySlot) {
        Pokemon pokemon = party.get(partySlot);
        if (pokemon == null) return;

        if (playerReady) {
            selector.sendSystemMessage(Component.literal("You've already readied up! Click the ready button to cancel.")
                .withStyle(ChatFormatting.RED));
            return;
        }

        if (pokemon.getCurrentHealth() <= 0) {
            selector.sendSystemMessage(Component.literal("Cannot select a fainted Pokemon!")
                .withStyle(ChatFormatting.RED));
            return;
        }

        if (selectedSlots.contains(partySlot)) {
            selectedSlots.remove(partySlot);
        } else if (selectedSlots.size() < maxSelections) {
            selectedSlots.add(partySlot);
        } else {
            selector.sendSystemMessage(Component.literal("Maximum Pokemon selected! Deselect one to change.")
                .withStyle(ChatFormatting.YELLOW));
            return;
        }

        setupMenuContents(menu);

        session.onPokemonSelected(this);
    }

    private void onBattleOrderSlotClick(PartySelectionMenu menu, int slot) {
        if (playerReady) {
            selector.sendSystemMessage(Component.literal("You've already readied up! Click the ready button to cancel.")
                .withStyle(ChatFormatting.RED));
            return;
        }

        int orderIndex = -1;
        for (int i = 0; i < BATTLE_ORDER_SLOTS.length; i++) {
            if (BATTLE_ORDER_SLOTS[i] == slot) {
                orderIndex = i;
                break;
            }
        }

        if (orderIndex >= 0) {
            List<Integer> selectedList = new ArrayList<>(selectedSlots);
            if (orderIndex < selectedList.size()) {
                int partySlot = selectedList.get(orderIndex);
                selectedSlots.remove(partySlot);
                setupMenuContents(menu);
                session.onPokemonSelected(this);
            }
        }
    }

    private void onReadyButtonClick() {
        if (selectedSlots.size() != maxSelections) {
            selector.sendSystemMessage(Component.literal("Select " + maxSelections + " Pokemon before readying up!")
                .withStyle(ChatFormatting.RED));
            return;
        }

        playerReady = !playerReady;

        if (playerReady) {
            selector.sendSystemMessage(Component.literal("You are now ready! Waiting for opponent...")
                .withStyle(ChatFormatting.GREEN));
        } else {
            selector.sendSystemMessage(Component.literal("Ready cancelled. You can modify your selection.")
                .withStyle(ChatFormatting.YELLOW));
        }

        if (openedMenu != null) {
            setupMenuContents(openedMenu);
        }

        session.onPlayerReadyChanged(this, playerReady);
    }

    public void updateOpponentCount(int count) {
        this.opponentSelectedCount = count;
        if (openedMenu != null) {
            setupStatusDisplays(openedMenu);
        }
    }

    public void updateOpponentReady(boolean ready) {
        this.opponentReady = ready;
        if (openedMenu != null) {
            setupStatusDisplays(openedMenu);
        }
    }

    // called each tick to animate the border colors and refresh timer
    public void timedGuiUpdate() {
        guiModifierFlag = !guiModifierFlag;
        if (openedMenu != null) {
            setupBackground(openedMenu);
            setupBattleOrderDisplay(openedMenu);
            setupFormatInfo(openedMenu);
            setupReadyButton(openedMenu);
            setupStatusDisplays(openedMenu);
        }
    }

    public void forceCloseMenu() {
        if (openedMenu != null) {
            openedMenu.invalidateMenu();
        }
    }

    public void onPlayerCloseContainer() {
        session.onPlayerCloseMenu(selector);
    }

    public List<Integer> getSelectedSlots() {
        return new ArrayList<>(selectedSlots);
    }

    public boolean isSelectionComplete() {
        return selectedSlots.size() == maxSelections && playerReady;
    }

    public boolean isPlayerReady() {
        return playerReady;
    }

    public ServerPlayer getSelector() {
        return selector;
    }
}
