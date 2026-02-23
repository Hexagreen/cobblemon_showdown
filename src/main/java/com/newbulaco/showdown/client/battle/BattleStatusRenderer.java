package com.newbulaco.showdown.client.battle;

import com.cobblemon.mod.common.client.CobblemonClient;
import com.cobblemon.mod.common.client.battle.ActiveClientBattlePokemon;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BattleStatusRenderer {

    // layout constants (matching BattleOverlay)
    private static final int HORIZONTAL_INSET = 6;
    private static final int VERTICAL_INSET = 10;
    private static final int VERTICAL_SPACING = 72;
    private static final int COMPACT_VERTICAL_SPACING = 50;
    private static final int TILE_WIDTH = 152;
    private static final int COMPACT_TILE_WIDTH = 100;
    private static final int PORTRAIT_DIAMETER = 54;
    private static final int COMPACT_PORTRAIT_DIAMETER = 38;
    private static final int PORTRAIT_OFFSET_Y = 4;
    private static final int COMPACT_PORTRAIT_OFFSET_Y = 3;
    private static final int INFO_OFFSET_X = 4;
    private static final int COMPACT_INFO_OFFSET_X = 3;

    private static final float SCALE = 0.5f;
    private static final int BG_COLOR = 0x88333333;
    private static final int BORDER_COLOR = 0xFF1A1A1A;

    public static void renderPokemonStatus(GuiGraphics context, ActiveClientBattlePokemon activePokemon, boolean left, int rank) {
        if (activePokemon == null || activePokemon.getBattlePokemon() == null) return;

        var mc = Minecraft.getInstance();
        var battle = CobblemonClient.INSTANCE.getBattle();
        if (battle == null) return;

        boolean isCompact = battle.getBattleFormat().getBattleType().getSlotsPerActor() > 1;

        int portraitDiameter = isCompact ? COMPACT_PORTRAIT_DIAMETER : PORTRAIT_DIAMETER;
        int portraitOffsetY = isCompact ? COMPACT_PORTRAIT_OFFSET_Y : PORTRAIT_OFFSET_Y;
        int infoOffsetX = isCompact ? COMPACT_INFO_OFFSET_X : INFO_OFFSET_X;
        int tileWidth = isCompact ? COMPACT_TILE_WIDTH : TILE_WIDTH;
        int verticalSpacing = isCompact ? COMPACT_VERTICAL_SPACING : VERTICAL_SPACING;

        String pokemonId = getPokemonIdentifier(activePokemon);
        PokemonBattleStatus status = BattleStatusTracker.getPokemonStatus(pokemonId);

        if (!status.hasAnyStatus()) return;

        float baseX = activePokemon.getXDisplacement();
        float baseY = VERTICAL_INSET + rank * verticalSpacing;

        baseX += left ? infoOffsetX + portraitDiameter + 2 : 0;
        baseY += portraitOffsetY + portraitDiameter + 2;

        List<PokemonBattleStatus.StatDisplay> stats = status.getStatDisplays();
        List<PokemonBattleStatus.EffectDisplay> effects = status.getEffectDisplays();

        float x = baseX;
        float y = baseY;
        int maxWidth = tileWidth - portraitDiameter - 10;

        for (PokemonBattleStatus.StatDisplay stat : stats) {
            int textWidth = (int)(mc.font.width(stat.text()) * SCALE);
            if (x - baseX + textWidth > maxWidth && x > baseX) {
                x = baseX;
                y += mc.font.lineHeight * SCALE + 3;
            }
            x = drawStatusTag(context, mc.font, stat.text(), (int)x, (int)y, SCALE, stat.color()) + 3;
        }

        if (!stats.isEmpty() && !effects.isEmpty()) {
            x = baseX;
            y += mc.font.lineHeight * SCALE + 3;
        }

        for (PokemonBattleStatus.EffectDisplay effect : effects) {
            int textWidth = (int)(mc.font.width(effect.text()) * SCALE);
            if (x - baseX + textWidth > maxWidth && x > baseX) {
                x = baseX;
                y += mc.font.lineHeight * SCALE + 3;
            }
            x = drawStatusTag(context, mc.font, effect.text(), (int)x, (int)y, SCALE, effect.color()) + 3;
        }
    }

    public static void renderFieldStatus(GuiGraphics context) {
        var mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int centerX = screenWidth / 2;
        int y = VERTICAL_INSET + 25;

        List<String> fieldEffects = new ArrayList<>();

        String weather = BattleStatusTracker.getWeather();
        if (!weather.isEmpty()) {
            int turns = BattleStatusTracker.getWeatherTurns();
            String text = formatFieldEffect(weather);
            if (turns > 0) text += " (" + turns + ")";
            fieldEffects.add(text);
        }

        String terrain = BattleStatusTracker.getTerrain();
        if (!terrain.isEmpty()) {
            int turns = BattleStatusTracker.getTerrainTurns();
            String text = formatFieldEffect(terrain);
            if (turns > 0) text += " (" + turns + ")";
            fieldEffects.add(text);
        }

        for (Map.Entry<String, Integer> room : BattleStatusTracker.getRooms().entrySet()) {
            String text = formatFieldEffect(room.getKey()) + " (" + room.getValue() + ")";
            fieldEffects.add(text);
        }

        // check both server packets and message parsing for Wheel of Dharma adaptation
        String allyAdapted = getAdaptedTypeFromVolatiles(true);
        String enemyAdapted = getAdaptedTypeFromVolatiles(false);

        // fallback to message parsing if server packets didn't provide data
        if (allyAdapted.isEmpty()) {
            allyAdapted = BattleStatusTracker.getAllyAdaptedType();
        }
        if (enemyAdapted.isEmpty()) {
            enemyAdapted = BattleStatusTracker.getEnemyAdaptedType();
        }

        if (!allyAdapted.isEmpty()) {
            fieldEffects.add("Ally Adapted: " + allyAdapted);
        }
        if (!enemyAdapted.isEmpty()) {
            fieldEffects.add("Foe Adapted: " + enemyAdapted);
        }

        for (String effect : fieldEffects) {
            int textWidth = (int)(mc.font.width(effect) * SCALE);
            int x = centerX - textWidth / 2;
            drawStatusTag(context, mc.font, effect, x, y, SCALE, ChatFormatting.WHITE);
            y += mc.font.lineHeight * SCALE + 4;
        }

        renderHazards(context, true);
        renderHazards(context, false);
    }

    private static void renderHazards(GuiGraphics context, boolean isAlly) {
        var mc = Minecraft.getInstance();
        Map<String, Integer> hazards = isAlly ? BattleStatusTracker.getAllyHazards() : BattleStatusTracker.getEnemyHazards();

        if (hazards.isEmpty()) return;

        int y = VERTICAL_INSET + 120;
        int x = isAlly ? HORIZONTAL_INSET : mc.getWindow().getGuiScaledWidth() - HORIZONTAL_INSET - 60;

        for (Map.Entry<String, Integer> hazard : hazards.entrySet()) {
            String text = formatFieldEffect(hazard.getKey());
            if (hazard.getValue() > 1) text += " x" + hazard.getValue();

            if (!isAlly) {
                int textWidth = (int)(mc.font.width(text) * SCALE);
                x = mc.getWindow().getGuiScaledWidth() - HORIZONTAL_INSET - textWidth - 2;
            }

            drawStatusTag(context, mc.font, text, x, y, SCALE, ChatFormatting.GOLD);
            y += mc.font.lineHeight * SCALE + 3;
        }
    }

    private static int drawStatusTag(GuiGraphics ctx, Font font, String text, int x, int y, float scale, ChatFormatting color) {
        int margin = 1;
        int textWidth = (int)(font.width(text) * scale);
        int textHeight = (int)(font.lineHeight * scale);

        ctx.fill(x - margin, y - margin, x + textWidth + margin, y + textHeight + margin, BG_COLOR);
        ctx.renderOutline(x - margin - 1, y - margin - 1, textWidth + margin * 2 + 2, textHeight + margin * 2 + 2, BORDER_COLOR);

        ctx.pose().pushPose();
        ctx.pose().translate(x, y, 0);
        ctx.pose().scale(scale, scale, 1);
        ctx.drawString(font, text, 0, 0, color.getColor(), true);
        ctx.pose().popPose();

        return x + textWidth + margin;
    }

    private static String getPokemonIdentifier(ActiveClientBattlePokemon pokemon) {
        if (pokemon.getBattlePokemon() == null) return "";

        String ownerName = pokemon.getBattlePokemon().getActor().getDisplayName().getString();
        String pokemonName = pokemon.getBattlePokemon().getDisplayName().getString();
        return ownerName + "/" + pokemonName;
    }

    private static String getAdaptedTypeFromVolatiles(boolean isAlly) {
        var effects = isAlly
            ? ClientVolatileEffectManager.getInstance().getAllyEffects()
            : ClientVolatileEffectManager.getInstance().getOpponentEffects();

        for (String effectId : effects) {
            if (effectId.toLowerCase().startsWith("wheelofdharmaadapted")) {
                String type = effectId.substring("wheelofdharmaadapted".length());
                if (!type.isEmpty()) {
                    return Character.toUpperCase(type.charAt(0)) + type.substring(1).toLowerCase();
                }
            }
        }
        return "";
    }

    private static String formatFieldEffect(String effect) {
        String[] words = effect.replace("_", " ").split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1).toLowerCase())
                      .append(" ");
            }
        }
        return result.toString().trim();
    }
}
