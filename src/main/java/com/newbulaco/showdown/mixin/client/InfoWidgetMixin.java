package com.newbulaco.showdown.mixin.client;

import com.cobblemon.mod.common.client.gui.summary.widgets.screens.info.InfoWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(InfoWidget.class)
public abstract class InfoWidgetMixin {

    // renderWidget has two ICONST_3 instructions:
    //   ordinal 0: dexNo.length < 3 (pokedex number padding)
    //   ordinal 1: maxLines = 3 (ability description line limit)
    @ModifyConstant(method = "renderWidget", constant = @Constant(intValue = 3, ordinal = 1))
    private int cobblemonShowdown$extendAbilityLines(int original) {
        return 5;
    }
}
