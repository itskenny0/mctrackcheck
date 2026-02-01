package com.railwaytoolkit.mixin.accessor;

import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackPlacement;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Mixin accessor to access package-private fields in TrackPlacement.PlacementInfo
 */
@Mixin(value = TrackPlacement.PlacementInfo.class, remap = false)
public interface PlacementInfoAccessor {

    @Accessor("curve")
    BezierConnection getCurve();

    @Accessor("curve")
    void setCurve(BezierConnection curve);

    @Accessor("valid")
    boolean isValid();

    @Accessor("valid")
    void setValid(boolean valid);

    @Accessor("end1")
    Vec3 getEnd1();

    @Accessor("end2")
    Vec3 getEnd2();

    @Accessor("message")
    String getMessage();
}
