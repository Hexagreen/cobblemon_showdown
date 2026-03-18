package com.newbulaco.showdown.mixin.client;

import com.cobblemon.mod.common.client.gui.summary.widgets.screens.moves.MovesWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(MovesWidget.class)
public abstract class MovesWidgetMixin {

    // the only ICONST_5 in renderButton is maxLines = 5 for the move description
    @ModifyConstant(method = "renderWidget", constant = @Constant(intValue = 5, ordinal = 0))
    private int cobblemonShowdown$extendMoveDescriptionLines(int original) {
        return 7;
    }
}
