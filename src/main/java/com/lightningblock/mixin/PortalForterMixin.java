package com.lightningblock.mixin;

import com.lightningblock.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.PortalForcer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes vanilla PortalForcer recognise Lightning Portal blocks as valid portal
 * destinations during dimension travel.  Without this, returning from the
 * Nether would fail to find the overworld Lightning Portal and create a
 * random vanilla portal instead.
 */
@Mixin(PortalForcer.class)
public abstract class PortalForterMixin {

    @Shadow
    protected ServerLevel level;

    @Inject(method = "lambda$findPortalAround$5", at = @At("HEAD"), cancellable = true)
    private void lightningblock$acceptLightningPortal(BlockState state, BlockPos pos,
                                                        CallbackInfoReturnable<Boolean> cir) {
        if (this.level.getBlockState(pos).is(ModBlocks.LIGHTNING_PORTAL.get())) {
            cir.setReturnValue(true);
        }
    }
}
