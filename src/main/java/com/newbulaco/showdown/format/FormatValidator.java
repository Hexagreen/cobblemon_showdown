package com.newbulaco.showdown.format;

import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * validates player parties against format rules.
 * checks party size, species clause, banned content, and whitelist restrictions.
 */
public class FormatValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(FormatValidator.class);

    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;

        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return Collections.unmodifiableList(errors);
        }

        public String getErrorMessage() {
            if (valid) return "Party is valid";
            return String.join("\n", errors);
        }
    }

    public List<String> validateParty(PlayerPartyStore party, Format format) {
        List<String> errors = new ArrayList<>();

        if (party == null) {
            errors.add("Party data is null");
            return errors;
        }

        if (format == null) {
            errors.add("Format is null");
            return errors;
        }

        List<Pokemon> pokemonList = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            Pokemon pokemon = party.get(i);
            if (pokemon != null) {
                pokemonList.add(pokemon);
            }
        }

        int requiredSize = format.getPartySize();
        if (pokemonList.size() < requiredSize) {
            errors.add("Party requires at least " + requiredSize + " Pokemon (you have " + pokemonList.size() + ")");
        }

        int healthyCount = 0;
        for (Pokemon pokemon : pokemonList) {
            if (pokemon.getCurrentHealth() > 0) {
                healthyCount++;
            }
        }
        if (healthyCount < requiredSize) {
            errors.add("Need at least " + requiredSize + " non-fainted Pokemon");
        }

        Set<String> seenSpecies = new HashSet<>();

        for (Pokemon pokemon : pokemonList) {
            String speciesName = pokemon.getSpecies().getName().toLowerCase();

            if (format.hasSpeciesClause()) {
                if (seenSpecies.contains(speciesName)) {
                    errors.add("Species Clause violation: duplicate " + pokemon.getSpecies().getName());
                }
                seenSpecies.add(speciesName);
            }

            if (isSpeciesBanned(speciesName, format)) {
                errors.add("Banned Pokemon: " + pokemon.getSpecies().getName());
            }

            if (pokemon.getAbility() != null) {
                String abilityName = pokemon.getAbility().getName().toLowerCase();
                if (isAbilityBanned(abilityName, format)) {
                    errors.add(pokemon.getSpecies().getName() + " has banned ability: " + pokemon.getAbility().getName());
                }
            }

            ItemStack heldItem = pokemon.heldItem();
            if (!heldItem.isEmpty()) {
                String itemName = heldItem.getItem().toString().toLowerCase();
                if (isItemBanned(itemName, format)) {
                    errors.add(pokemon.getSpecies().getName() + " has banned item: " + heldItem.getHoverName().getString());
                }
            }

            for (var move : pokemon.getMoveSet()) {
                if (move != null) {
                    String moveName = move.getName().toLowerCase();
                    if (isMoveBanned(moveName, format)) {
                        errors.add(pokemon.getSpecies().getName() + " has banned move: " + move.getName());
                    }
                }
            }
        }

        LOGGER.debug("Validated party against format {}: {} errors", format.getName(), errors.size());
        return errors;
    }

    public boolean validatePartySize(int partySize, Format format) {
        return partySize == format.getPartySize();
    }

    public boolean isSpeciesBanned(String speciesId, Format format) {
        if (speciesId == null) return false;

        String normalized = normalizeId(speciesId);

        // whitelist mode inverts the check: only listed species are allowed
        if (format.isBanAllExceptWhitelist()) {
            List<String> whitelist = format.getWhitelistSpecies();
            return whitelist.stream()
                    .map(this::normalizeId)
                    .noneMatch(id -> id.equals(normalized));
        }

        return format.getBans().getPokemon().stream()
                .map(this::normalizeId)
                .anyMatch(id -> id.equals(normalized));
    }

    public boolean isMoveBanned(String moveId, Format format) {
        if (moveId == null) return false;

        String normalized = normalizeId(moveId);
        return format.getBans().getMoves().stream()
                .map(this::normalizeId)
                .anyMatch(id -> id.equals(normalized));
    }

    public boolean isAbilityBanned(String abilityId, Format format) {
        if (abilityId == null) return false;

        String normalized = normalizeId(abilityId);
        return format.getBans().getAbilities().stream()
                .map(this::normalizeId)
                .anyMatch(id -> id.equals(normalized));
    }

    public boolean isItemBanned(String itemId, Format format) {
        if (itemId == null) return false;

        String normalized = normalizeId(itemId);
        return format.getBans().getItems().stream()
                .map(this::normalizeId)
                .anyMatch(id -> id.equals(normalized));
    }

    private String normalizeId(String id) {
        if (id == null) return "";
        return id.toLowerCase()
                .replaceAll("[\\s-]", "_")
                .replaceAll("_+", "_");
    }

    public String formatValidationErrors(List<String> errors) {
        if (errors.isEmpty()) {
            return "Your party is valid for this format!";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Your party is invalid for this format:\n");
        for (int i = 0; i < errors.size(); i++) {
            sb.append("  ").append(i + 1).append(". ").append(errors.get(i)).append("\n");
        }
        return sb.toString();
    }

    public String getFormatSummary(Format format) {
        StringBuilder sb = new StringBuilder();
        sb.append("Format: ").append(format.getName()).append("\n");

        if (format.getDescription() != null && !format.getDescription().isEmpty()) {
            sb.append(format.getDescription()).append("\n\n");
        }

        sb.append("Party Size: ").append(format.getPartySize()).append("\n");
        sb.append("Best Of: ").append(format.getBestOf()).append("\n");

        if (format.getSetLevel() != null) {
            sb.append("Set Level: ").append(format.getSetLevel()).append("\n");
        }

        if (format.hasBattleTimer()) {
            sb.append("Battle Timer: Enabled\n");
        }

        if (format.hasTeamPreview()) {
            sb.append("Team Preview: Enabled\n");
        }

        if (format.hasSpeciesClause()) {
            sb.append("Species Clause: Active (no duplicate species)\n");
        }

        Format.FormatBans bans = format.getBans();
        if (!bans.getPokemon().isEmpty()) {
            sb.append("\nBanned Pokemon: ").append(bans.getPokemon().size()).append("\n");
        }
        if (!bans.getMoves().isEmpty()) {
            sb.append("Banned Moves: ").append(bans.getMoves().size()).append("\n");
        }
        if (!bans.getAbilities().isEmpty()) {
            sb.append("Banned Abilities: ").append(bans.getAbilities().size()).append("\n");
        }
        if (!bans.getItems().isEmpty()) {
            sb.append("Banned Items: ").append(bans.getItems().size()).append("\n");
        }

        if (format.isBanAllExceptWhitelist()) {
            sb.append("\nWhitelist Mode: Only ")
              .append(format.getWhitelistSpecies().size())
              .append(" species allowed\n");
        }

        return sb.toString();
    }
}
