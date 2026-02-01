package com.railwaytoolkit.mixin;

import com.railwaytoolkit.client.CurvatureDisplay;
import com.railwaytoolkit.mixin.accessor.PlacementInfoAccessor;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackPlacement;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to enforce curvature limits when Alt is held.
 */
@Mixin(value = TrackPlacement.class, remap = false)
public class TrackPlacementEnforcementMixin {

    /**
     * Inject at the end of tryConnect to potentially invalidate the placement
     * if enforcement is active and the curve doesn't meet requirements.
     */
    @Inject(method = "tryConnect", at = @At("RETURN"), cancellable = false)
    private static void onTryConnect(Level level, Player player, BlockPos pos2, BlockState state2,
                                     ItemStack stack, boolean girder, boolean maximiseTurn,
                                     CallbackInfoReturnable<TrackPlacement.PlacementInfo> cir) {
        if (!level.isClientSide()) {
            return;
        }

        TrackPlacement.PlacementInfo info = cir.getReturnValue();
        if (info == null) {
            return;
        }

        // Cast to accessor to access package-private fields
        PlacementInfoAccessor accessor = (PlacementInfoAccessor) (Object) info;
        BezierConnection curve = accessor.getCurve();

        if (curve == null) {
            return;
        }

        // Check if enforcement would block this placement (use our radius calculation for S-curves)
        double radius = CurvatureDisplay.getMinimumRadius(curve);
        if (CurvatureDisplay.shouldBlockPlacement(radius)) {
            // Mark the placement as invalid
            accessor.setValid(false);
            accessor.setCurve(null);
        }
    }
}
