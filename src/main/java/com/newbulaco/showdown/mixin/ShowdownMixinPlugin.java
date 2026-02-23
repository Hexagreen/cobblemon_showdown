package com.newbulaco.showdown.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

import java.util.List;
import java.util.Set;

public class ShowdownMixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger("CobblemonShowdown");
    private boolean cobblemonPresent = false;

    @Override
    public void onLoad(String mixinPackage) {
        LOGGER.info("[Showdown] Mixin plugin loaded for package: {}", mixinPackage);

        // use MixinService's bytecode provider to check class existence without loading it,
        // preventing conflicts with mods like ModernFix that need to mixin to core classes
        try {
            var bytecodeProvider = MixinService.getService().getBytecodeProvider();
            bytecodeProvider.getClassNode("com.cobblemon.mod.common.pokemon.Pokemon");
            cobblemonPresent = true;
            LOGGER.info("[Showdown] Cobblemon detected - client mixins will be enabled");
        } catch (Exception e) {
            cobblemonPresent = false;
            LOGGER.info("[Showdown] Cobblemon not found - client mixins will be disabled");
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        LOGGER.info("[Showdown] shouldApplyMixin called for target={}, mixin={}", targetClassName, mixinClassName);

        if (mixinClassName.contains("PCGUIMixin") || mixinClassName.contains("BattleSwitchPokemonSelectionMixin")) {
            if (!cobblemonPresent) {
                LOGGER.info("[Showdown] Skipping mixin {} - Cobblemon not present", mixinClassName);
                return false;
            }
            LOGGER.info("[Showdown] Applying mixin {} to {}", mixinClassName, targetClassName);
            return true;
        }

        // safe to always apply since it checks namespace, but needs cobblemon for party access
        if (mixinClassName.contains("ItemStackMixin")) {
            if (!cobblemonPresent) {
                LOGGER.info("[Showdown] Skipping ItemStackMixin - Cobblemon not present (party check would fail)");
                return false;
            }
            LOGGER.info("[Showdown] Applying ItemStackMixin for SimpleTMs tooltip enhancement");
            return true;
        }

        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
        LOGGER.info("[Showdown] acceptTargets - myTargets: {}", myTargets);
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        LOGGER.debug("[Showdown] preApply: {} -> {}", mixinClassName, targetClassName);
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        LOGGER.info("[Showdown] postApply: Successfully applied {} to {}", mixinClassName, targetClassName);
    }
}
