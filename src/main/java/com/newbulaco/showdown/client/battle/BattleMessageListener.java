package com.newbulaco.showdown.client.battle;

import com.cobblemon.mod.common.client.CobblemonClient;
import com.cobblemon.mod.common.client.battle.ClientBattle;
import com.newbulaco.showdown.CobblemonShowdown;
import com.newbulaco.showdown.api.ShowdownAPI;
import com.newbulaco.showdown.api.content.CustomFieldCondition;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod.EventBusSubscriber(modid = CobblemonShowdown.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class BattleMessageListener {
    private static final Logger LOGGER = LoggerFactory.getLogger("CobblemonShowdown");

    private static ClientBattle lastBattle = null;
    private static boolean subscribed = false;

    // Wheel of Dharma messages can be split across multiple lines
    private static boolean waitingForAdaptedType = false;
    private static boolean isAllyAdaptation = true;

    // used to determine side for Wheel of Dharma activation
    private static String lastWheelOfDharmaOwner = null;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        try {
            ClientBattle battle = CobblemonClient.INSTANCE.getBattle();

            if (battle != null && battle != lastBattle) {
                lastBattle = battle;
                subscribed = false;
                BattleStatusTracker.clearAll();
                LOGGER.info("[Showdown] Battle started - subscribing to message queue");

                // lambda must return Unit for Kotlin interop
                battle.getMessages().subscribe(message -> {
                    onBattleMessage(message);
                    return kotlin.Unit.INSTANCE;
                });
                subscribed = true;
                LOGGER.info("[Showdown] Subscribed to battle messages");
            }

            if (battle == null && lastBattle != null) {
                lastBattle = null;
                subscribed = false;
                BattleStatusTracker.clearAll();
                ClientVolatileEffectManager.getInstance().clearAll();
                ClientStatChangeManager.getInstance().clearAll();
                waitingForAdaptedType = false;
                isAllyAdaptation = true;
                lastWheelOfDharmaOwner = null;
            }

        } catch (Exception e) {
            // ignore errors
        }
    }

    private static void onBattleMessage(FormattedCharSequence message) {
        try {
            StringBuilder sb = new StringBuilder();
            message.accept((index, style, codePoint) -> {
                sb.appendCodePoint(codePoint);
                return true;
            });
            String rawText = sb.toString();
            String text = rawText.toLowerCase();

            if (text.contains("mahoraga") || text.contains("adapted") ||
                text.contains("wheel") || text.contains("dharma") ||
                text.contains("immunity")) {
                LOGGER.debug("[Showdown] Battle message (Mahoraga): {}", rawText);
            }

            parseWeather(text);
            parseTerrain(text);
            parseRooms(text);
            parseHazards(text);
            parseWheelOfDharma(text);

            // stat changes are now read directly from ClientBattlePokemon.statChanges
            // volatile effects are now synced from server via VolatileEffectPacket

        } catch (Exception e) {
            // ignore parsing errors
        }
    }

    private static void parseWeather(String text) {
        if (text.contains("sunlight") || text.contains("sunny") || text.contains("sun turned harsh")) {
            if (text.contains("harsh sunlight") || text.contains("extremely harsh")) {
                BattleStatusTracker.setWeather("Harsh Sun", -1);
            } else {
                BattleStatusTracker.setWeather("Sun", 5);
            }
        } else if (text.contains("started to rain") || text.contains("rain continues") || text.contains("heavy rain")) {
            if (text.contains("heavy rain")) {
                BattleStatusTracker.setWeather("Heavy Rain", -1);
            } else {
                BattleStatusTracker.setWeather("Rain", 5);
            }
        } else if (text.contains("sandstorm") && (text.contains("kicked up") || text.contains("raging"))) {
            BattleStatusTracker.setWeather("Sandstorm", 5);
        } else if (text.contains("hail") && (text.contains("started") || text.contains("continues"))) {
            BattleStatusTracker.setWeather("Hail", 5);
        } else if (text.contains("snow") && (text.contains("started") || text.contains("continues"))) {
            BattleStatusTracker.setWeather("Snow", 5);
        }

        if (text.contains("sunlight faded") || text.contains("rain stopped") ||
            text.contains("sandstorm subsided") || text.contains("hail stopped") ||
            text.contains("snow stopped") || text.contains("weather cleared")) {
            BattleStatusTracker.setWeather("", 0);
        }
    }

    private static void parseTerrain(String text) {
        if (text.contains("electric terrain")) {
            if (text.contains("faded") || text.contains("disappeared")) {
                BattleStatusTracker.setTerrain("", 0);
            } else {
                BattleStatusTracker.setTerrain("Electric Terrain", 5);
            }
        } else if (text.contains("grassy terrain")) {
            if (text.contains("faded") || text.contains("disappeared")) {
                BattleStatusTracker.setTerrain("", 0);
            } else {
                BattleStatusTracker.setTerrain("Grassy Terrain", 5);
            }
        } else if (text.contains("psychic terrain")) {
            if (text.contains("faded") || text.contains("disappeared")) {
                BattleStatusTracker.setTerrain("", 0);
            } else {
                BattleStatusTracker.setTerrain("Psychic Terrain", 5);
            }
        } else if (text.contains("misty terrain")) {
            if (text.contains("faded") || text.contains("disappeared")) {
                BattleStatusTracker.setTerrain("", 0);
            } else {
                BattleStatusTracker.setTerrain("Misty Terrain", 5);
            }
        } else {
            for (CustomFieldCondition fc : ShowdownAPI.getAllFieldConditions()) {
                if (fc.getType() != CustomFieldCondition.Type.TERRAIN) continue;
                String name = fc.getDisplayName().toLowerCase();
                if (text.contains(name)) {
                    if (text.contains("faded") || text.contains("disappeared")) {
                        BattleStatusTracker.setTerrain("", 0);
                    } else {
                        BattleStatusTracker.setTerrain(fc.getDisplayName(), fc.getDefaultDuration());
                    }
                    break;
                }
            }
        }
    }

    private static void parseRooms(String text) {
        if (text.contains("trick room")) {
            if (text.contains("twisted") || text.contains("created")) {
                BattleStatusTracker.setRoom("Trick Room", 5);
            } else if (text.contains("wore off") || text.contains("ended")) {
                BattleStatusTracker.removeRoom("Trick Room");
            }
        }
        if (text.contains("magic room")) {
            if (text.contains("created")) {
                BattleStatusTracker.setRoom("Magic Room", 5);
            } else if (text.contains("wore off") || text.contains("ended")) {
                BattleStatusTracker.removeRoom("Magic Room");
            }
        }
        if (text.contains("wonder room")) {
            if (text.contains("created")) {
                BattleStatusTracker.setRoom("Wonder Room", 5);
            } else if (text.contains("wore off") || text.contains("ended")) {
                BattleStatusTracker.removeRoom("Wonder Room");
            }
        }
    }

    private static void parseHazards(String text) {
        if (text.contains("stealth rock") || text.contains("pointed stones")) {
            if (text.contains("float") || text.contains("set")) {
                if (text.contains("your") || text.contains("ally")) {
                    BattleStatusTracker.addAllyHazard("Stealth Rock", 1);
                } else {
                    BattleStatusTracker.addEnemyHazard("Stealth Rock", 1);
                }
            } else if (text.contains("disappeared") || text.contains("blown away")) {
                BattleStatusTracker.removeAllyHazard("Stealth Rock");
                BattleStatusTracker.removeEnemyHazard("Stealth Rock");
            }
        }

        if (text.contains("spikes") && !text.contains("toxic spikes")) {
            if (text.contains("scattered") || text.contains("set")) {
                if (text.contains("your") || text.contains("ally")) {
                    int current = BattleStatusTracker.getAllyHazards().getOrDefault("Spikes", 0);
                    BattleStatusTracker.addAllyHazard("Spikes", Math.min(current + 1, 3));
                } else {
                    int current = BattleStatusTracker.getEnemyHazards().getOrDefault("Spikes", 0);
                    BattleStatusTracker.addEnemyHazard("Spikes", Math.min(current + 1, 3));
                }
            }
        }

        if (text.contains("toxic spikes")) {
            if (text.contains("scattered") || text.contains("set")) {
                if (text.contains("your") || text.contains("ally")) {
                    int current = BattleStatusTracker.getAllyHazards().getOrDefault("Toxic Spikes", 0);
                    BattleStatusTracker.addAllyHazard("Toxic Spikes", Math.min(current + 1, 2));
                } else {
                    int current = BattleStatusTracker.getEnemyHazards().getOrDefault("Toxic Spikes", 0);
                    BattleStatusTracker.addEnemyHazard("Toxic Spikes", Math.min(current + 1, 2));
                }
            }
        }

        if (text.contains("sticky web")) {
            if (text.contains("spread") || text.contains("set")) {
                if (text.contains("your") || text.contains("ally")) {
                    BattleStatusTracker.addAllyHazard("Sticky Web", 1);
                } else {
                    BattleStatusTracker.addEnemyHazard("Sticky Web", 1);
                }
            }
        }
    }

    /**
     * new format: "-activate|Pokemon|ability: Wheel of Dharma|[adaptedtype]Fighting"
     * old format (split): "Mahoraga adapted:" then "Fighting immunity!"
     *
     * side is determined by comparing the pokemon's owner name to the local player.
     */
    private static void parseWheelOfDharma(String text) {
        if (text.contains("mahoraga") && text.contains("'s")) {
            int apostropheIdx = text.indexOf("'s mahoraga");
            if (apostropheIdx == -1) apostropheIdx = text.indexOf("'s Mahoraga");
            if (apostropheIdx > 0) {
                String beforeApostrophe = text.substring(0, apostropheIdx);
                String[] words = beforeApostrophe.split(" ");
                if (words.length > 0) {
                    lastWheelOfDharmaOwner = words[words.length - 1];
                }
            }
        }

        // [adaptedtype] format from -activate protocol message
        if (text.contains("[adaptedtype]")) {
            int typeIdx = text.indexOf("[adaptedtype]");
            String adaptedType = text.substring(typeIdx + "[adaptedtype]".length()).trim();

            if (adaptedType.contains("|")) {
                adaptedType = adaptedType.substring(0, adaptedType.indexOf("|"));
            }
            if (adaptedType.contains(" ")) {
                adaptedType = adaptedType.substring(0, adaptedType.indexOf(" "));
            }

            if (!adaptedType.isEmpty()) {
                adaptedType = Character.toUpperCase(adaptedType.charAt(0)) +
                              adaptedType.substring(1).toLowerCase();

                boolean isAlly = isOwnerLocalPlayer(lastWheelOfDharmaOwner);
                if (isAlly) {
                    LOGGER.info("[Showdown] Wheel of Dharma: Ally adapted to {}", adaptedType);
                    BattleStatusTracker.setAllyAdaptedType(adaptedType);
                } else {
                    LOGGER.info("[Showdown] Wheel of Dharma: Enemy adapted to {}", adaptedType);
                    BattleStatusTracker.setEnemyAdaptedType(adaptedType);
                }
            }
            return;
        }

        // old format: waiting for the type from a previous "adapted:" message
        if (waitingForAdaptedType) {
            if (text.contains("immunity")) {
                int immunityIdx = text.indexOf(" immunity");
                if (immunityIdx == -1) immunityIdx = text.indexOf("immunity");

                if (immunityIdx > 0) {
                    String adaptedType = text.substring(0, immunityIdx).trim();

                    if (!adaptedType.isEmpty()) {
                        adaptedType = Character.toUpperCase(adaptedType.charAt(0)) +
                                      adaptedType.substring(1).toLowerCase();

                        if (isAllyAdaptation) {
                            LOGGER.info("[Showdown] Wheel of Dharma: Ally adapted to {}", adaptedType);
                            BattleStatusTracker.setAllyAdaptedType(adaptedType);
                        } else {
                            LOGGER.info("[Showdown] Wheel of Dharma: Enemy adapted to {}", adaptedType);
                            BattleStatusTracker.setEnemyAdaptedType(adaptedType);
                        }
                    }
                }
                waitingForAdaptedType = false;
                return;
            }

            if (!text.contains("adapted")) {
                waitingForAdaptedType = false;
            }
        }

        // old "adapted:" format (split messages)
        if (text.contains("adapted:")) {
            isAllyAdaptation = isOwnerLocalPlayer(lastWheelOfDharmaOwner);

            int colonIdx = text.indexOf("adapted:");
            int typeStart = colonIdx + "adapted:".length();

            int immunityIdx = text.indexOf(" immunity", typeStart);
            if (immunityIdx > typeStart) {
                String adaptedType = text.substring(typeStart, immunityIdx).trim();
                if (!adaptedType.isEmpty()) {
                    adaptedType = Character.toUpperCase(adaptedType.charAt(0)) +
                                  adaptedType.substring(1).toLowerCase();

                    if (isAllyAdaptation) {
                        LOGGER.info("[Showdown] Wheel of Dharma: Ally adapted to {}", adaptedType);
                        BattleStatusTracker.setAllyAdaptedType(adaptedType);
                    } else {
                        LOGGER.info("[Showdown] Wheel of Dharma: Enemy adapted to {}", adaptedType);
                        BattleStatusTracker.setEnemyAdaptedType(adaptedType);
                    }
                }
            } else {
                // type will be in next message (message was split)
                waitingForAdaptedType = true;
            }
            return;
        }

        if (text.contains("wheel of dharma") && (text.contains("suppressed") || text.contains("neutraliz"))) {
            LOGGER.info("[Showdown] Wheel of Dharma suppressed - clearing adapted types");
            BattleStatusTracker.clearAdaptedTypes();
        }
    }

    private static boolean isOwnerLocalPlayer(String ownerName) {
        if (ownerName == null || ownerName.isEmpty()) {
            return true; // default to ally if unknown
        }

        var mc = Minecraft.getInstance();
        if (mc.player != null) {
            String localName = mc.player.getName().getString().toLowerCase();
            return ownerName.toLowerCase().equals(localName);
        }

        return true; // default to ally if can't determine
    }
}
