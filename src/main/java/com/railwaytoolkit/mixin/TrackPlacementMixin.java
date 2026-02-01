package com.railwaytoolkit.mixin;

import com.railwaytoolkit.client.CurvatureDisplay;
import com.simibubi.create.content.trains.track.TrackPlacement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to hook into Create's track placement client tick
 * to display curvature radius and slope information.
 */
@Mixin(value = TrackPlacement.class, remap = false)
public class TrackPlacementMixin {

    /**
     * Inject at the end of clientTick to display our curvature info
     * after Create has finished its processing.
     */
    @Inject(method = "clientTick", at = @At("TAIL"))
    private static void onClientTick(CallbackInfo ci) {
        CurvatureDisplay.onTrackPlacementTick();
    }
}
