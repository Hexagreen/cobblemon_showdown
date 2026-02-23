# Cobblemon Showdown API Guide

This guide explains how other mods can integrate with Cobblemon Showdown to add custom content that appears in the battle UI and /dt command.

## Overview

The Showdown API allows you to register:
- **Custom abilities** with display names, descriptions, and battle mechanics
- **Custom moves** with full stats (type, power, accuracy, PP, priority) and effects
- **Move modifications** to change existing moves (power, accuracy, effects)
- **Custom field conditions** (weather, terrain, rooms)
- **Custom volatile effects** (temporary status on individual Pokemon)
- **Custom side conditions** (hazards, screens, team effects)
- **Helper JavaScript** for utility functions needed by your content

Registered content will automatically appear in:
- Battle overlay UI (volatile effects, field conditions, side conditions)
- `/showdown dt` command lookups
- Move tooltips and effect displays
- Showdown battle engine (when Showdown JS is provided)

## Getting Started

### Dependency Setup

Add Cobblemon Showdown as a dependency in your `build.gradle`:

```gradle
dependencies {
    implementation fg.deobf("com.newbulaco:cobblemon_showdown:1.0.0")
}
```

And in your `mods.toml`:

```toml
[[dependencies.yourmod]]
    modId="cobblemon_showdown"
    mandatory=false
    versionRange="[1.0.0,)"
    ordering="AFTER"
    side="BOTH"
```

### Registration

Register your custom content during `FMLCommonSetupEvent`:

```java
import com.newbulaco.showdown.api.ShowdownAPI;
import com.newbulaco.showdown.api.content.*;

@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModSetup {

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            registerCustomContent();
        });
    }

    private static void registerCustomContent() {
        // register abilities, moves, etc.
    }
}
```

## Registering Custom Abilities

```java
// basic ability (UI display only)
ShowdownAPI.registerAbility(new CustomAbility.Builder("wheelofdharma")
    .displayName("Wheel of Dharma")
    .description("After being hit by a super-effective move, gains immunity to that type.")
    .modId("cobblemon_newbmons")
    .build());

// ability with battle mechanics
ShowdownAPI.registerAbility(new CustomAbility.Builder("wheelofdharma")
    .displayName("Wheel of Dharma")
    .description("After being hit by a super-effective move, gains immunity to that type.")
    .num(9001)  // custom abilities use 9000+
    .showdownJs("""
        onDamagingHit(damage, target, source, move) {
            if (target.getMoveHitData(move).typeMod > 0) {
                target.addVolatile('wheelofdharmaadapted' + move.type.toLowerCase());
                this.add('-ability', target, 'Wheel of Dharma');
                this.add('-message', 'Mahoraga adapted: ' + move.type + ' immunity!');
            }
        }
        """)
    .modId("cobblemon_newbmons")
    .build());
```

**Builder methods:**
- `displayName(String)` - name shown in UI (defaults to formatted ID)
- `description(String)` - full description text
- `num(int)` - Showdown numeric ID (use 9000+ for custom)
- `showdownJs(String)` - JavaScript code for battle mechanics
- `modId(String)` - your mod's ID for attribution

## Registering Custom Moves

```java
// basic move (UI display only)
ShowdownAPI.registerMove(new CustomMove.Builder("havocsword")
    .displayName("Havoc Sword")
    .type("Dark")
    .category("Physical")
    .power(100)
    .accuracy(100)
    .pp(5)
    .description("Power is 1.5x if the user has a status condition.")
    .modId("cobblemon_newbmons")
    .build());

// move with battle mechanics
ShowdownAPI.registerMove(new CustomMove.Builder("havocsword")
    .displayName("Havoc Sword")
    .type("Dark")
    .category("Physical")
    .power(100)
    .accuracy(100)
    .pp(5)
    .num(9001)  // custom moves use 9000+
    .target("normal")
    .flag("contact")
    .flag("slicing")
    .showdownJs("""
        onBasePower(basePower, pokemon, target) {
            if (pokemon.status) {
                return this.chainModify(1.5);
            }
        }
        """)
    .description("Power is 1.5x if the user has a status condition.")
    .modId("cobblemon_newbmons")
    .build());
```

**Builder methods:**
- `displayName(String)` - name shown in UI
- `type(String)` - elemental type (Normal, Fire, Water, etc.)
- `category(String)` - Physical, Special, or Status
- `power(int)` - base power (0 for status moves)
- `accuracy(int)` - accuracy percentage (0 for moves that can't miss)
- `pp(int)` - power points
- `priority(int)` - move priority (-7 to +5)
- `num(int)` - Showdown numeric ID (use 9000+ for custom)
- `target(String)` - targeting type (normal, any, adjacentFoe, allAdjacent, etc.)
- `flag(String)` - add a flag (contact, protect, mirror, slicing, etc.)
- `showdownJs(String)` - JavaScript code for effects
- `description(String)` - full description text
- `modId(String)` - your mod's ID

## Registering Move Modifications

Modify existing moves without replacing them entirely:

```java
// change octazooka's stats, description, and add an effect
ShowdownAPI.registerMoveModification(new MoveModification.Builder("octazooka")
    .power(80)
    .accuracy(100)
    .description("The user attacks by spraying ink in the target's face. " +
                 "This lowers the target's evasiveness and sets Ink Slick on their side.")
    .showdownJs("""
        onHit(target, source, move) {
            target.side.addSideCondition('inkslick', source, move);
        }
        """)
    .modId("cobblemon_newbmons")
    .build());
```

**Builder methods:**
- `power(int)` - new base power (null to keep original)
- `accuracy(int)` - new accuracy (null to keep original)
- `pp(int)` - new PP (null to keep original)
- `priority(int)` - new priority (null to keep original)
- `description(String)` - new description for /dt and UI
- `showdownJs(String)` - JavaScript to add/override effects
- `modId(String)` - your mod's ID

## Registering Ability Modifications

Modify existing abilities (reworked abilities) without replacing them entirely:

```java
// rework cute charm to steal items on switch-in
ShowdownAPI.registerAbilityModification(new AbilityModification.Builder("cutecharm")
    .description("On switch-in, if this Pokemon has no held item, " +
                 "it steals the opposing Pokemon's held item.")
    .shortDesc("On switch-in without item, steals foe's item.")
    .showdownJs("""
        onStart(pokemon) {
            if (pokemon.item) return;
            const target = pokemon.side.foe.active[0];
            if (!target || !target.item) return;
            const item = target.takeItem(pokemon);
            if (item) {
                this.add('-item', pokemon, item, '[from] ability: Cute Charm');
                pokemon.setItem(item);
            }
        }
        """)
    .modId("cobblemon_newbmons")
    .build());
```

**Builder methods:**
- `description(String)` - new description for /dt and UI
- `shortDesc(String)` - short description for compact displays
- `showdownJs(String)` - JavaScript to override battle logic
- `modId(String)` - your mod's ID

## Registering Custom Field Conditions

Field conditions include weather, terrain, and rooms.

```java
// custom weather
ShowdownAPI.registerFieldCondition(new CustomFieldCondition.Builder("bloodmoon")
    .displayName("Blood Moon")
    .weather()
    .color(0xFFAA0000)  // dark red
    .defaultDuration(5)
    .description("A crimson moon rises, empowering Dark-type moves.")
    .modId("yourmod")
    .build());

// custom terrain
ShowdownAPI.registerFieldCondition(new CustomFieldCondition.Builder("cursedterrain")
    .displayName("Cursed Terrain")
    .terrain()
    .color(0xFF880088)  // purple
    .defaultDuration(5)
    .description("Grounded Pokemon take residual damage each turn.")
    .modId("yourmod")
    .build());

// custom room
ShowdownAPI.registerFieldCondition(new CustomFieldCondition.Builder("gravityroom")
    .displayName("Gravity Room")
    .room()
    .color(0xFF444488)  // dark blue
    .defaultDuration(5)
    .description("All Pokemon are grounded and accuracy is boosted.")
    .modId("yourmod")
    .build());
```

**Builder methods:**
- `displayName(String)` - name shown in UI
- `weather()` / `terrain()` / `room()` - set the condition type
- `color(int)` - ARGB color for UI display
- `defaultDuration(int)` - default turn count
- `description(String)` - full description text
- `modId(String)` - your mod's ID

## Registering Custom Volatile Effects

Volatile effects are temporary conditions on a single Pokemon.

```java
// basic volatile (UI display only)
ShowdownAPI.registerVolatileEffect(new CustomVolatileEffect.Builder("phantomguard")
    .displayName("Phantom Guard")
    .color(0xFF8888FF)  // light purple
    .description("Protected from the next attack.")
    .modId("yourmod")
    .build());

// volatile with battle mechanics
ShowdownAPI.registerVolatileEffect(new CustomVolatileEffect.Builder("wheelofdharmaadaptedfire")
    .displayName("Adapted: Fire")
    .color(0xFFFF6600)
    .showdownJs("""
        onTryHit(target, source, move) {
            if (move.type === 'Fire') {
                this.add('-immune', target, '[from] ability: Wheel of Dharma');
                return null;
            }
        },
        noCopy: true
        """)
    .modId("cobblemon_newbmons")
    .build());

// hidden volatile (won't show in UI)
ShowdownAPI.registerVolatileEffect(new CustomVolatileEffect.Builder("internalcounter")
    .displayName("Internal Counter")
    .hidden()
    .modId("yourmod")
    .build());
```

**Builder methods:**
- `displayName(String)` - name shown in UI
- `color(int)` - ARGB color for UI display
- `showInUI(boolean)` - whether to display in battle UI
- `hidden()` - shortcut for `showInUI(false)`
- `showdownJs(String)` - JavaScript condition definition
- `description(String)` - full description text
- `modId(String)` - your mod's ID

## Registering Custom Side Conditions

Side conditions affect an entire team (hazards, screens, etc.).

```java
// basic side condition (UI display only)
ShowdownAPI.registerSideCondition(new CustomSideCondition.Builder("cursedspikes")
    .displayName("Cursed Spikes")
    .hazard()
    .color(0xFF880088)  // purple
    .stackable(3)
    .description("Damages and may curse Pokemon switching in.")
    .modId("yourmod")
    .build());

// side condition with battle mechanics
ShowdownAPI.registerSideCondition(new CustomSideCondition.Builder("inkslick")
    .displayName("Ink Slick")
    .hazard()
    .color(0xFF333333)
    .showdownJs("""
        onSwitchIn(pokemon) {
            if (pokemon.isGrounded()) {
                this.boost({evasion: -1}, pokemon, pokemon.side.foe.active[0], this.effect);
            }
        }
        """)
    .description("Lowers evasion of grounded Pokemon switching in.")
    .modId("cobblemon_newbmons")
    .build());
```

**Builder methods:**
- `displayName(String)` - name shown in UI
- `hazard()` / `screen()` / `category(Category)` - set the condition type
- `color(int)` - ARGB color for UI display
- `stackable(int maxStacks)` - make the condition stackable
- `showdownJs(String)` - JavaScript side condition definition
- `description(String)` - full description text
- `modId(String)` - your mod's ID

## Registering Helper JavaScript

Register utility JavaScript that will be available to your abilities and moves:

```java
// register a constant
ShowdownAPI.registerHelperJs("""
    const ALL_TYPES = ['Normal', 'Fire', 'Water', 'Electric', 'Grass', 'Ice',
                       'Fighting', 'Poison', 'Ground', 'Flying', 'Psychic', 'Bug',
                       'Rock', 'Ghost', 'Dragon', 'Dark', 'Steel', 'Fairy'];
    """);

// register a utility function
ShowdownAPI.registerHelperJs("""
    function isMahoraga(pokemon) {
        const species = pokemon.species;
        const name = typeof species === 'string' ? species :
                     (species.name || species.id || species.baseSpecies || '');
        return name.toLowerCase().includes('mahoraga');
    }
    """);
```

Helper JS is injected at the beginning of both abilities.js and moves.js.

## Querying Registered Content

You can also query registered content:

```java
// check if custom ability exists
if (ShowdownAPI.hasCustomAbility("wheelofdharma")) {
    CustomAbility ability = ShowdownAPI.getAbility("wheelofdharma");
    String name = ability.getDisplayName();
}

// get display names (falls back to formatted ID if not registered)
String abilityName = ShowdownAPI.getAbilityDisplayName("someid");
String moveName = ShowdownAPI.getMoveDisplayName("someid");
String fieldName = ShowdownAPI.getFieldConditionDisplayName("someid");

// get all registered content
Collection<CustomAbility> allAbilities = ShowdownAPI.getAllAbilities();
Collection<CustomMove> allMoves = ShowdownAPI.getAllMoves();
```

## Generating Showdown Scripts

When you register content with `showdownJs`, use `ShowdownScriptBuilder` to generate the JavaScript files:

```java
import com.newbulaco.showdown.api.ShowdownScriptBuilder;

// in your mod initialization, after registering content
Path showdownModDir = Paths.get("showdown/data/mods/cobblemon");
ShowdownScriptBuilder.buildAndWriteScripts(showdownModDir);
```

This generates:
- `abilities.js` - all custom abilities with Showdown JS
- `moves.js` - all custom moves and modifications with Showdown JS
- `conditions.js` - all volatile effects and side conditions with Showdown JS

The generated files follow Showdown's mod format and will be merged with base data.

## Battle Integration Notes

For full battle integration:
1. Register content with `showdownJs` for battle mechanics
2. Use `ShowdownScriptBuilder.buildAndWriteScripts()` to generate JS files
3. Ensure the JS files are written before Showdown boots (use a mixin on `GraalShowdownService.openConnection()`)

Content without `showdownJs` will still appear in UI and DT command, but won't have battle effects.

Make sure your effect IDs match between the Showdown battle logic and the API registration.

## Complete Example

```java
@Mod("mymonmod")
public class MyMonMod {
    public static final String MODID = "mymonmod";

    public MyMonMod() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
    }

    private void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // register custom ability
            ShowdownAPI.registerAbility(new CustomAbility.Builder("shadowform")
                .displayName("Shadow Form")
                .description("This Pokemon takes 50% less damage from Physical moves.")
                .modId(MODID)
                .build());

            // register signature move
            ShowdownAPI.registerMove(new CustomMove.Builder("shadowstrike")
                .displayName("Shadow Strike")
                .type("Ghost")
                .category("Physical")
                .power(80)
                .accuracy(100)
                .pp(15)
                .description("Has increased critical hit ratio.")
                .modId(MODID)
                .build());

            // register volatile effect for the ability
            ShowdownAPI.registerVolatileEffect(new CustomVolatileEffect.Builder("shadowform")
                .displayName("Shadow Form")
                .color(0xFF444466)
                .modId(MODID)
                .build());
        });
    }
}
```
