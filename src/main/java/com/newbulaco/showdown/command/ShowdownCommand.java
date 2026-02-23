package com.newbulaco.showdown.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.newbulaco.showdown.CobblemonShowdown;
import com.newbulaco.showdown.battle.BattleManager;
import com.newbulaco.showdown.battle.TeamPreview;
import com.newbulaco.showdown.format.Format;
import com.newbulaco.showdown.format.FormatManager;
import com.newbulaco.showdown.util.MessageUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

public class ShowdownCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> showdownCommand = Commands.literal("showdown");

        FormatCommand.register(showdownCommand);
        ChallengeCommand.register(showdownCommand);

        DtCommand.register(showdownCommand);

        showdownCommand.then(Commands.literal("preview_select")
                .then(Commands.argument("slot", IntegerArgumentType.integer(0, 5))
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            int slot = IntegerArgumentType.getInteger(ctx, "slot");
                            return TeamPreview.selectLead(player, slot) ? 1 : 0;
                        })));

        showdownCommand.then(Commands.literal("spectate")
                .then(Commands.argument("player", StringArgumentType.word())
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            String targetName = StringArgumentType.getString(ctx, "player");

                            BattleManager battleManager = CobblemonShowdown.getBattleManager();
                            if (battleManager == null) {
                                MessageUtil.error(player, "Battle system not available");
                                return 0;
                            }

                            return battleManager.spectatePlayer(player, targetName) ? 1 : 0;
                        })));

        showdownCommand.then(Commands.literal("unspectate")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();

                    BattleManager battleManager = CobblemonShowdown.getBattleManager();
                    if (battleManager == null) {
                        MessageUtil.error(player, "Battle system not available");
                        return 0;
                    }

                    return battleManager.removeSpectator(player) ? 1 : 0;
                }));

        showdownCommand.then(Commands.literal("abort")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();

                    BattleManager battleManager = CobblemonShowdown.getBattleManager();
                    if (battleManager == null) {
                        MessageUtil.error(player, "Battle system not available");
                        return 0;
                    }

                    return battleManager.abortBattle(player) ? 1 : 0;
                }));

        showdownCommand.then(Commands.literal("formats")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    FormatManager formatManager = CobblemonShowdown.getFormatManager();

                    if (formatManager == null) {
                        MessageUtil.error(player, "Format manager not available");
                        return 0;
                    }

                    Collection<String> formatIds = formatManager.getFormatIds();
                    if (formatIds.isEmpty()) {
                        MessageUtil.info(player, "No formats available");
                        return 1;
                    }

                    MessageUtil.info(player, "Available formats:");
                    for (String formatId : formatIds) {
                        Format format = formatManager.getFormat(formatId);
                        if (format != null) {
                            player.sendSystemMessage(Component.literal("  §b" + formatId + " §7- " + format.getName()));
                        }
                    }
                    return 1;
                }));

        showdownCommand.then(Commands.literal("help")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    showHelp(player);
                    return 1;
                }));

        showdownCommand.executes(ctx -> {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            showHelp(player);
            return 1;
        });

        dispatcher.register(showdownCommand);
    }

    private static void showHelp(ServerPlayer player) {
        MutableComponent help = Component.literal("=== Cobblemon Showdown Help ===")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        player.sendSystemMessage(help);

        player.sendSystemMessage(Component.literal(""));

        player.sendSystemMessage(Component.literal("Challenge Commands:")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        sendHelpLine(player, "/showdown challenge <player> <format>", "Challenge a player to battle");
        sendHelpLine(player, "/showdown challenge <player> <format> item_bet:<item>,<amount>", "Challenge with item wager");
        sendHelpLine(player, "/showdown accept <player>", "Accept a pending challenge");
        sendHelpLine(player, "/showdown deny <player>", "Deny a pending challenge");
        sendHelpLine(player, "/showdown abort", "Forfeit your current battle or series");

        player.sendSystemMessage(Component.literal(""));

        player.sendSystemMessage(Component.literal("Info Commands:")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        sendHelpLine(player, "/showdown formats", "List all available battle formats");
        sendHelpLine(player, "/showdown format_party_test <format>", "Test if your party is valid");
        sendHelpLine(player, "/showdown history [player]", "View match history and stats");

        player.sendSystemMessage(Component.literal(""));

        player.sendSystemMessage(Component.literal("Spectating Commands:")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        sendHelpLine(player, "/showdown spectate <player>", "Watch a player's battle");
        sendHelpLine(player, "/showdown unspectate", "Stop spectating");

        if (player.hasPermissions(2)) {
            player.sendSystemMessage(Component.literal(""));
            player.sendSystemMessage(Component.literal("Staff Commands (OP 2+):")
                    .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
            sendHelpLine(player, "/showdown format <id> export", "Export format as JSON");
        }

        if (player.hasPermissions(3)) {
            player.sendSystemMessage(Component.literal(""));
            player.sendSystemMessage(Component.literal("Admin Commands (OP 3+):")
                    .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
            sendHelpLine(player, "/showdown format <id> create", "Create a new format");
            sendHelpLine(player, "/showdown format <id> edit", "Edit format settings");
            sendHelpLine(player, "/showdown format <id> delete", "Delete a format");
            sendHelpLine(player, "/showdown format <id> import <json>", "Import format from JSON");
        }
    }

    private static void sendHelpLine(ServerPlayer player, String command, String description) {
        MutableComponent line = Component.literal("  " + command)
                .withStyle(ChatFormatting.GREEN)
                .append(Component.literal(" - " + description)
                        .withStyle(ChatFormatting.GRAY));
        player.sendSystemMessage(line);
    }
}
