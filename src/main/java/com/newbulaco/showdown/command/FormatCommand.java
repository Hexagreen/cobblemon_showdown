package com.newbulaco.showdown.command;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.newbulaco.showdown.CobblemonShowdown;
import com.newbulaco.showdown.format.Format;
import com.newbulaco.showdown.format.FormatManager;
import com.newbulaco.showdown.format.FormatValidator;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public class FormatCommand {

    // TODO: integrate with Forge permission API
    private static final int OP_EXPORT = 2;
    private static final int OP_CREATE = 3;

    private static final SuggestionProvider<CommandSourceStack> FORMAT_SUGGESTIONS = (context, builder) -> {
        FormatManager formatManager = CobblemonShowdown.getFormatManager();
        if (formatManager != null) {
            return SharedSuggestionProvider.suggest(
                    formatManager.getAllFormats().keySet(),
                    builder
            );
        }
        return builder.buildFuture();
    };

    public static void register(LiteralArgumentBuilder<CommandSourceStack> parent) {
        parent.then(Commands.literal("format")
                .then(Commands.argument("formatId", StringArgumentType.word())
                        .suggests(FORMAT_SUGGESTIONS)
                        .then(Commands.literal("create")
                                .requires(source -> source.hasPermission(OP_CREATE))
                                .executes(FormatCommand::createFormat))
                        .then(Commands.literal("edit")
                                .requires(source -> source.hasPermission(OP_CREATE))
                                .executes(FormatCommand::editFormat))
                        .then(Commands.literal("duplicate")
                                .requires(source -> source.hasPermission(OP_CREATE))
                                .then(Commands.argument("newId", StringArgumentType.word())
                                        .executes(FormatCommand::duplicateFormat)))
                        .then(Commands.literal("delete")
                                .requires(source -> source.hasPermission(OP_CREATE))
                                .executes(FormatCommand::deleteFormat))
                        .then(Commands.literal("export")
                                .requires(source -> source.hasPermission(OP_EXPORT))
                                .executes(FormatCommand::exportFormat))
                        .then(Commands.literal("import")
                                .requires(source -> source.hasPermission(OP_CREATE))
                                .then(Commands.argument("json", StringArgumentType.greedyString())
                                        .executes(FormatCommand::importFormat)))
                        .executes(FormatCommand::showFormatInfo))
                .then(Commands.literal("list")
                        .executes(FormatCommand::listFormats)))
                .then(Commands.literal("format_party_test")
                        .then(Commands.argument("formatId", StringArgumentType.word())
                                .suggests(FORMAT_SUGGESTIONS)
                                .executes(FormatCommand::testParty)));
    }

    private static int showFormatInfo(CommandContext<CommandSourceStack> context) {
        String formatId = StringArgumentType.getString(context, "formatId");
        FormatManager formatManager = CobblemonShowdown.getFormatManager();

        if (formatManager == null) {
            sendError(context.getSource(), "Format manager not initialized");
            return 0;
        }

        Format format = formatManager.getFormat(formatId);
        if (format == null) {
            sendError(context.getSource(), "Format '" + formatId + "' does not exist");
            return 0;
        }

        FormatValidator validator = new FormatValidator();
        String summary = validator.getFormatSummary(format);

        sendSuccess(context.getSource(), Component.literal(summary));
        return 1;
    }

    private static int listFormats(CommandContext<CommandSourceStack> context) {
        FormatManager formatManager = CobblemonShowdown.getFormatManager();

        if (formatManager == null) {
            sendError(context.getSource(), "Format manager not initialized");
            return 0;
        }

        var formats = formatManager.getAllFormats();
        if (formats.isEmpty()) {
            sendWarning(context.getSource(), "No formats available");
            return 0;
        }

        MutableComponent message = Component.literal("Available Formats (" + formats.size() + "):\n")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);

        for (var entry : formats.entrySet()) {
            String id = entry.getKey();
            Format format = entry.getValue();

            MutableComponent formatLine = Component.literal("  • ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(format.getName())
                            .withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(" (" + id + ")")
                            .withStyle(ChatFormatting.DARK_GRAY));

            formatLine.withStyle(style -> style
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            Component.literal("Click for details")))
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                            "/showdown format " + id)));

            message.append("\n").append(formatLine);
        }

        sendSuccess(context.getSource(), message);
        return formats.size();
    }

    private static int createFormat(CommandContext<CommandSourceStack> context) {
        String formatId = StringArgumentType.getString(context, "formatId");
        FormatManager formatManager = CobblemonShowdown.getFormatManager();

        if (formatManager == null) {
            sendError(context.getSource(), "Format manager not initialized");
            return 0;
        }

        if (formatManager.hasFormat(formatId)) {
            sendError(context.getSource(), "Format '" + formatId + "' already exists");
            return 0;
        }

        Format newFormat = new Format(formatId.replace("_", " "));
        newFormat.setDescription("Created via command");

        if (formatManager.saveFormat(formatId, newFormat)) {
            sendSuccess(context.getSource(), "Created format: " + formatId);
            sendInfo(context.getSource(), "Edit the JSON file in config/cobblemon_showdown/formats/" + formatId + ".json");
            return 1;
        } else {
            sendError(context.getSource(), "Failed to create format");
            return 0;
        }
    }

    private static int editFormat(CommandContext<CommandSourceStack> context) {
        String formatId = StringArgumentType.getString(context, "formatId");
        FormatManager formatManager = CobblemonShowdown.getFormatManager();

        if (formatManager == null) {
            sendError(context.getSource(), "Format manager not initialized");
            return 0;
        }

        if (!formatManager.hasFormat(formatId)) {
            sendError(context.getSource(), "Format '" + formatId + "' does not exist");
            return 0;
        }

        sendInfo(context.getSource(), "Edit the format JSON file:");
        sendInfo(context.getSource(), "config/cobblemon_showdown/formats/" + formatId + ".json");
        sendInfo(context.getSource(), "Changes will be automatically reloaded");
        return 1;
    }

    private static int duplicateFormat(CommandContext<CommandSourceStack> context) {
        String sourceId = StringArgumentType.getString(context, "formatId");
        String targetId = StringArgumentType.getString(context, "newId");
        FormatManager formatManager = CobblemonShowdown.getFormatManager();

        if (formatManager == null) {
            sendError(context.getSource(), "Format manager not initialized");
            return 0;
        }

        if (!formatManager.hasFormat(sourceId)) {
            sendError(context.getSource(), "Source format '" + sourceId + "' does not exist");
            return 0;
        }

        if (formatManager.hasFormat(targetId)) {
            sendError(context.getSource(), "Target format '" + targetId + "' already exists");
            return 0;
        }

        if (formatManager.duplicateFormat(sourceId, targetId)) {
            sendSuccess(context.getSource(), "Duplicated " + sourceId + " → " + targetId);
            return 1;
        } else {
            sendError(context.getSource(), "Failed to duplicate format");
            return 0;
        }
    }

    private static int deleteFormat(CommandContext<CommandSourceStack> context) {
        String formatId = StringArgumentType.getString(context, "formatId");
        FormatManager formatManager = CobblemonShowdown.getFormatManager();

        if (formatManager == null) {
            sendError(context.getSource(), "Format manager not initialized");
            return 0;
        }

        if (!formatManager.hasFormat(formatId)) {
            sendError(context.getSource(), "Format '" + formatId + "' does not exist");
            return 0;
        }

        if (formatManager.deleteFormat(formatId)) {
            sendSuccess(context.getSource(), "Deleted format: " + formatId);
            return 1;
        } else {
            sendError(context.getSource(), "Failed to delete format");
            return 0;
        }
    }

    private static int exportFormat(CommandContext<CommandSourceStack> context) {
        String formatId = StringArgumentType.getString(context, "formatId");
        FormatManager formatManager = CobblemonShowdown.getFormatManager();

        if (formatManager == null) {
            sendError(context.getSource(), "Format manager not initialized");
            return 0;
        }

        String json = formatManager.exportFormat(formatId);
        if (json == null) {
            sendError(context.getSource(), "Format '" + formatId + "' does not exist");
            return 0;
        }

        MutableComponent message = Component.literal("Format Export: " + formatId)
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);

        MutableComponent jsonComponent = Component.literal(json)
                .withStyle(ChatFormatting.GRAY)
                .withStyle(style -> style
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.literal("Click to copy")))
                        .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, json)));

        context.getSource().sendSuccess(() -> message, false);
        context.getSource().sendSuccess(() -> jsonComponent, false);

        sendInfo(context.getSource(), "Click the JSON above to copy to clipboard");
        return 1;
    }

    private static int importFormat(CommandContext<CommandSourceStack> context) {
        String formatId = StringArgumentType.getString(context, "formatId");
        String json = StringArgumentType.getString(context, "json");
        FormatManager formatManager = CobblemonShowdown.getFormatManager();

        if (formatManager == null) {
            sendError(context.getSource(), "Format manager not initialized");
            return 0;
        }

        if (formatManager.importFormat(formatId, json)) {
            sendSuccess(context.getSource(), "Imported format: " + formatId);
            return 1;
        } else {
            sendError(context.getSource(), "Failed to import format (invalid JSON?)");
            return 0;
        }
    }

    private static int testParty(CommandContext<CommandSourceStack> context) {
        String formatId = StringArgumentType.getString(context, "formatId");
        FormatManager formatManager = CobblemonShowdown.getFormatManager();

        if (formatManager == null) {
            sendError(context.getSource(), "Format manager not initialized");
            return 0;
        }

        Format format = formatManager.getFormat(formatId);
        if (format == null) {
            sendError(context.getSource(), "Format '" + formatId + "' does not exist");
            return 0;
        }

        try {
            ServerPlayer player = context.getSource().getPlayerOrException();

            PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
            if (party == null) {
                sendError(context.getSource(), "Could not access your party");
                return 0;
            }

            FormatValidator validator = new FormatValidator();
            List<String> errors = validator.validateParty(party, format);

            sendInfo(context.getSource(), "Testing against format: " + format.getName());

            if (errors.isEmpty()) {
                sendSuccess(context.getSource(), "Your party is valid for this format!");
            } else {
                sendError(context.getSource(), "Your party has " + errors.size() + " issue(s):");
                for (String error : errors) {
                    sendWarning(context.getSource(), "  - " + error);
                }
            }

            return errors.isEmpty() ? 1 : 0;
        } catch (Exception e) {
            sendError(context.getSource(), "Error validating party: " + e.getMessage());
            return 0;
        }
    }

    private static void sendSuccess(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message).withStyle(ChatFormatting.GREEN), false);
    }

    private static void sendSuccess(CommandSourceStack source, Component message) {
        source.sendSuccess(() -> message, false);
    }

    private static void sendError(CommandSourceStack source, String message) {
        source.sendFailure(Component.literal(message).withStyle(ChatFormatting.RED));
    }

    private static void sendWarning(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message).withStyle(ChatFormatting.YELLOW), false);
    }

    private static void sendInfo(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message).withStyle(ChatFormatting.GRAY), false);
    }
}
