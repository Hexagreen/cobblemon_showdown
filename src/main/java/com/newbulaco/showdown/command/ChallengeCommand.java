package com.newbulaco.showdown.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.newbulaco.showdown.CobblemonShowdown;
import com.newbulaco.showdown.config.ShowdownConfig;
import com.newbulaco.showdown.challenge.Challenge;
import com.newbulaco.showdown.challenge.ChallengeManager;
import com.newbulaco.showdown.data.HistoryStorage;
import com.newbulaco.showdown.data.PlayerHistory;
import com.newbulaco.showdown.battle.BattleManager;
import com.newbulaco.showdown.data.PrizeHandler;
import com.newbulaco.showdown.format.Format;
import com.newbulaco.showdown.format.FormatManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ChallengeCommand {

    public static void register(LiteralArgumentBuilder<CommandSourceStack> parent) {
        parent.then(Commands.literal("challenge")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ChallengeCommand::showFormats)
                        .then(Commands.argument("format", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    FormatManager fm = CobblemonShowdown.getFormatManager();
                                    if (fm != null) {
                                        for (String formatId : fm.getFormatIds()) {
                                            if (formatId.toLowerCase().startsWith(builder.getRemainingLowerCase())) {
                                                builder.suggest(formatId);
                                            }
                                        }
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(ChallengeCommand::challengePlayer)
                                .then(Commands.argument("item_bet", StringArgumentType.greedyString())
                                        .executes(ChallengeCommand::challengePlayerWithBet)))));

        parent.then(Commands.literal("accept")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ChallengeCommand::acceptChallenge)));

        parent.then(Commands.literal("deny")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ChallengeCommand::denyChallenge)));

        parent.then(Commands.literal("history")
                .executes(ChallengeCommand::showOwnHistory)
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ChallengeCommand::showPlayerHistory)));
    }

    private static int showFormats(CommandContext<CommandSourceStack> context) {
        FormatManager formatManager = CobblemonShowdown.getFormatManager();
        if (formatManager == null) {
            sendError(context.getSource(), "Format manager not initialized");
            return 0;
        }

        Collection<String> formatIds = formatManager.getFormatIds();
        if (formatIds.isEmpty()) {
            sendError(context.getSource(), "No formats available");
            return 0;
        }

        sendInfo(context.getSource(), "Available formats:");
        for (String formatId : formatIds) {
            Format format = formatManager.getFormat(formatId);
            if (format != null) {
                MutableComponent formatLine = Component.literal("  ")
                        .append(Component.literal(formatId)
                                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)
                                .withStyle(style -> style
                                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                                Component.literal(format.getDescription() != null ?
                                                        format.getDescription() : "No description")))
                                        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                                                "/showdown challenge @p " + formatId))))
                        .append(Component.literal(" - " + format.getName())
                                .withStyle(ChatFormatting.GRAY));
                context.getSource().sendSuccess(() -> formatLine, false);
            }
        }
        sendInfo(context.getSource(), "Usage: /showdown challenge <player> <format>");
        return 1;
    }

    private static int challengePlayer(CommandContext<CommandSourceStack> context) {
        return challengePlayerInternal(context, null);
    }

    private static int challengePlayerWithBet(CommandContext<CommandSourceStack> context) {
        String betString = StringArgumentType.getString(context, "item_bet");
        Challenge.ItemBet itemBet = parseItemBet(betString);

        if (itemBet == null) {
            sendError(context.getSource(), "Invalid item bet format. Use: item_bet:<item>,<amount>");
            sendInfo(context.getSource(), "Example: item_bet:minecraft:diamond,5");
            return 0;
        }

        return challengePlayerInternal(context, itemBet);
    }

    private static int challengePlayerInternal(CommandContext<CommandSourceStack> context, Challenge.ItemBet itemBet) {
        try {
            ServerPlayer challenger = context.getSource().getPlayerOrException();
            ServerPlayer challenged = EntityArgument.getPlayer(context, "player");
            String formatId = StringArgumentType.getString(context, "format");

            if (challenger.getUUID().equals(challenged.getUUID())) {
                sendError(context.getSource(), "You cannot challenge yourself!");
                return 0;
            }

            if (ShowdownConfig.isChallengeRadiusEnabled()) {
                double distance = challenger.distanceTo(challenged);
                double maxDistance = ShowdownConfig.getChallengeRadius();
                if (distance > maxDistance) {
                    sendError(context.getSource(), "You must be within " + (int) maxDistance +
                            " blocks of " + challenged.getName().getString() + " to challenge them!");
                    sendInfo(context.getSource(), "Current distance: " + String.format("%.1f", distance) + " blocks");
                    return 0;
                }
            }

            FormatManager formatManager = CobblemonShowdown.getFormatManager();
            if (formatManager == null || !formatManager.hasFormat(formatId)) {
                sendError(context.getSource(), "Format '" + formatId + "' does not exist");
                Collection<String> formatIds = formatManager != null ? formatManager.getFormatIds() : Collections.emptyList();
                if (!formatIds.isEmpty()) {
                    sendInfo(context.getSource(), "Available formats: " + String.join(", ", formatIds));
                }
                return 0;
            }

            Format format = formatManager.getFormat(formatId);
            ChallengeManager challengeManager = CobblemonShowdown.getChallengeManager();

            if (challengeManager.hasChallenge(challenged.getUUID())) {
                sendError(context.getSource(), challenged.getName().getString() + " already has a pending challenge");
                return 0;
            }

            boolean created = challengeManager.createChallenge(
                    challenger.getUUID(),
                    challenged.getUUID(),
                    formatId,
                    itemBet
            );

            if (!created) {
                sendError(context.getSource(), "Failed to create challenge");
                return 0;
            }

            sendSuccess(context.getSource(), "Challenge sent to " + challenged.getName().getString());
            if (itemBet != null) {
                sendInfo(context.getSource(), "Item bet: " + itemBet);
            }

            MutableComponent message = Component.literal("⚔ ")
                    .withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(challenger.getName().getString())
                            .withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(" challenged you to "))
                    .append(Component.literal(format.getName())
                            .withStyle(ChatFormatting.AQUA))
                    .append(Component.literal("!"));

            if (itemBet != null) {
                message.append(Component.literal("\n  Item Bet: ")
                        .withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(String.valueOf(itemBet))
                                .withStyle(ChatFormatting.GOLD));
            }

            message.append(Component.literal("\n  "));

            MutableComponent acceptButton = Component.literal("[Accept]")
                    .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
                    .withStyle(style -> style
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    Component.literal("Click to accept")))
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                    "/showdown accept " + challenger.getName().getString())));

            MutableComponent denyButton = Component.literal("[Deny]")
                    .withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
                    .withStyle(style -> style
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    Component.literal("Click to deny")))
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                    "/showdown deny " + challenger.getName().getString())));

            message.append(acceptButton).append(Component.literal("  ")).append(denyButton);

            message.append(Component.literal("\n  Expires in 60 seconds")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));

            challenged.sendSystemMessage(message);

            return 1;

        } catch (Exception e) {
            sendError(context.getSource(), "Error creating challenge: " + e.getMessage());
            return 0;
        }
    }

    private static int acceptChallenge(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer accepter = context.getSource().getPlayerOrException();
            ServerPlayer challenger = EntityArgument.getPlayer(context, "player");

            ChallengeManager challengeManager = CobblemonShowdown.getChallengeManager();
            Challenge challenge = challengeManager.getChallengeFrom(challenger.getUUID(), accepter.getUUID());

            if (challenge == null) {
                sendError(context.getSource(), "No pending challenge from " + challenger.getName().getString());
                return 0;
            }

            if (challenge.isExpired()) {
                challengeManager.removeChallenge(accepter.getUUID());
                sendError(context.getSource(), "Challenge has expired");
                return 0;
            }

            FormatManager formatManager = CobblemonShowdown.getFormatManager();
            Format format = formatManager.getFormat(challenge.getFormatId());

            if (format == null) {
                sendError(context.getSource(), "Format '" + challenge.getFormatId() + "' no longer exists!");
                challengeManager.removeChallenge(accepter.getUUID());
                return 0;
            }

            PrizeHandler.ItemBet prizeBet = null;
            if (challenge.getItemBet() != null) {
                prizeBet = new PrizeHandler.ItemBet(
                        challenge.getItemBet().getItemId(),
                        challenge.getItemBet().getAmount()
                );
            }

            BattleManager battleManager = CobblemonShowdown.getBattleManager();
            if (battleManager == null) {
                sendError(context.getSource(), "Battle system not available!");
                return 0;
            }

            challengeManager.acceptChallenge(accepter.getUUID());

            boolean started = battleManager.startChallengeBattle(challenger, accepter, format, prizeBet);

            if (started) {
                sendSuccess(context.getSource(), "Challenge accepted! Battle starting...");
                MutableComponent challengerMsg = Component.literal(accepter.getName().getString())
                        .withStyle(ChatFormatting.YELLOW)
                        .append(Component.literal(" accepted your challenge!")
                                .withStyle(ChatFormatting.GREEN));
                challenger.sendSystemMessage(challengerMsg);
            } else {
                sendError(context.getSource(), "Failed to start battle - check your party!");
            }

            return started ? 1 : 0;

        } catch (Exception e) {
            sendError(context.getSource(), "Error accepting challenge: " + e.getMessage());
            return 0;
        }
    }

    private static int denyChallenge(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer denier = context.getSource().getPlayerOrException();
            ServerPlayer challenger = EntityArgument.getPlayer(context, "player");

            ChallengeManager challengeManager = CobblemonShowdown.getChallengeManager();
            Challenge challenge = challengeManager.getChallengeFrom(challenger.getUUID(), denier.getUUID());

            if (challenge == null) {
                sendError(context.getSource(), "No pending challenge from " + challenger.getName().getString());
                return 0;
            }

            challengeManager.removeChallenge(denier.getUUID());
            sendSuccess(context.getSource(), "Challenge denied");

            MutableComponent challengerMsg = Component.literal(denier.getName().getString())
                    .withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal(" denied your challenge")
                            .withStyle(ChatFormatting.RED));
            challenger.sendSystemMessage(challengerMsg);

            return 1;

        } catch (Exception e) {
            sendError(context.getSource(), "Error denying challenge: " + e.getMessage());
            return 0;
        }
    }

    private static int showOwnHistory(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            return showHistoryForPlayer(context.getSource(), player);
        } catch (Exception e) {
            sendError(context.getSource(), "Error showing history: " + e.getMessage());
            return 0;
        }
    }

    private static int showPlayerHistory(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            return showHistoryForPlayer(context.getSource(), target);
        } catch (Exception e) {
            sendError(context.getSource(), "Error showing history: " + e.getMessage());
            return 0;
        }
    }

    private static int showHistoryForPlayer(CommandSourceStack source, ServerPlayer player) {
        HistoryStorage storage = CobblemonShowdown.getHistoryStorage();
        if (storage == null) {
            sendError(source, "History storage not initialized");
            return 0;
        }

        PlayerHistory history = storage.getHistory(player.getUUID());

        MutableComponent message = Component.literal("Battle History: ")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                .append(Component.literal(player.getName().getString())
                        .withStyle(ChatFormatting.YELLOW));

        message.append(Component.literal("\n\nStats:")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));

        message.append(Component.literal("\n  Wins: ")
                .withStyle(ChatFormatting.GRAY))
                .append(Component.literal(String.valueOf(history.getStats().getWins()))
                        .withStyle(ChatFormatting.GREEN));

        message.append(Component.literal("\n  Losses: ")
                .withStyle(ChatFormatting.GRAY))
                .append(Component.literal(String.valueOf(history.getStats().getLosses()))
                        .withStyle(ChatFormatting.RED));

        message.append(Component.literal("\n  Total: ")
                .withStyle(ChatFormatting.GRAY))
                .append(Component.literal(String.valueOf(history.getTotalMatches()))
                        .withStyle(ChatFormatting.WHITE));

        if (history.getTotalMatches() > 0) {
            message.append(Component.literal("\n  Win Rate: ")
                    .withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(String.format("%.1f%%", history.getWinRate()))
                            .withStyle(ChatFormatting.YELLOW));
        }

        List<PlayerHistory.MatchRecord> matches = history.getMatches();
        if (!matches.isEmpty()) {
            message.append(Component.literal("\n\nRecent Matches:")
                    .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));

            int count = Math.min(5, matches.size());
            for (int i = matches.size() - 1; i >= matches.size() - count; i--) {
                PlayerHistory.MatchRecord match = matches.get(i);

                ChatFormatting resultColor = match.isWin() ? ChatFormatting.GREEN : ChatFormatting.RED;
                String resultText = match.isWin() ? "W" : "L";

                message.append(Component.literal("\n  [" + resultText + "] ")
                        .withStyle(resultColor, ChatFormatting.BOLD))
                        .append(Component.literal(match.getFormatId())
                                .withStyle(ChatFormatting.GRAY));

            }
        }

        source.sendSuccess(() -> message, false);
        return 1;
    }

    private static Challenge.ItemBet parseItemBet(String betString) {
        if (!betString.startsWith("item_bet:")) return null;

        String[] parts = betString.substring(9).split(",");
        if (parts.length != 2) return null;

        try {
            String itemId = parts[0].trim();
            int amount = Integer.parseInt(parts[1].trim());
            if (amount <= 0 || amount > 64) return null;
            return new Challenge.ItemBet(itemId, amount);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static void sendSuccess(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message).withStyle(ChatFormatting.GREEN), false);
    }

    private static void sendError(CommandSourceStack source, String message) {
        source.sendFailure(Component.literal(message).withStyle(ChatFormatting.RED));
    }

    private static void sendInfo(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message).withStyle(ChatFormatting.GRAY), false);
    }
}
