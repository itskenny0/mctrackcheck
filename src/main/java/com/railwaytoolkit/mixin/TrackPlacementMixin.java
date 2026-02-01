package com.railwaytoolkit.mixin;

import com.railwaytoolkit.client.CurvatureDisplay;
import com.railwaytoolkit.client.MaxRadiusHighlight;
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
     * Inject at the end of clientTick to update our displays.
     * We use a different approach now - instead of redirecting the message,
     * we'll let Create display its message and then immediately display ours after.
     */
    @Inject(method = "clientTick", at = @At("TAIL"))
    private static void onClientTickEnd(CallbackInfo ci) {
        // Update curvature display (this will overwrite Create's message with combined info)
        CurvatureDisplay.displayCurvatureInfo();

        // Update max radius highlight
        MaxRadiusHighlight.updateHighlight();
    }
}
