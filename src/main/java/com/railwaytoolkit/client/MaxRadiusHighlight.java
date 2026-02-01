package com.railwaytoolkit.client;

import com.railwaytoolkit.config.RailwayToolkitConfig;
import com.railwaytoolkit.mixin.accessor.PlacementInfoAccessor;
import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackPlacement;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the yellow highlight showing where maximum curvature radius can be achieved.
 */
public class MaxRadiusHighlight {

    private static BlockPos lastHighlightPos = null;
    private static long lastUpdateTime = 0;
    private static final long UPDATE_INTERVAL_MS = 100;

    /**
     * Called from mixin to update the max radius highlight.
     */
    public static void updateHighlight() {
        // Throttle updates
        long now = System.currentTimeMillis();
        if (now - lastUpdateTime < UPDATE_INTERVAL_MS) {
            return;
        }
        lastUpdateTime = now;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player == null) {
            clearHighlight();
            return;
        }

        HitResult hitResult = mc.hitResult;
        if (hitResult == null || hitResult.getType() != HitResult.Type.BLOCK) {
            clearHighlight();
            return;
        }

        // Check if we're in track placement mode
        TrackPlacement.PlacementInfo info = TrackPlacement.cached;
        if (info == null) {
            clearHighlight();
            return;
        }

        PlacementInfoAccessor accessor = (PlacementInfoAccessor) (Object) info;
        BezierConnection curve = accessor.getCurve();

        // Only show for curves, not straight tracks or S-curves
        if (curve == null || curve.getRadius() <= 0) {
            clearHighlight();
            return;
        }

        // Get the current enforcement target radius
        double targetRadius = EnforcementHandler.getCurrentMinRadius();

        // Find the position that would give maximum radius
        BlockHitResult bhr = (BlockHitResult) hitResult;
        BlockPos basePos = bhr.getBlockPos();
        Level level = player.level();
        BlockState hitState = level.getBlockState(basePos);

        // Get the track item
        ItemStack stack = player.getMainHandItem();
        if (!stack.hasFoil()) {
            clearHighlight();
            return;
        }

        // Search for positions with best radius
        List<BlockPos> targetPositions = new ArrayList<>();
        double bestRadius = 0;
        BlockPos bestPos = null;

        for (int xOffset = -3; xOffset <= 3; xOffset++) {
            for (int zOffset = -3; zOffset <= 3; zOffset++) {
                if (xOffset == 0 && zOffset == 0) continue;

                BlockPos testPos = basePos.offset(xOffset, 0, zOffset);
                try {
                    TrackPlacement.PlacementInfo testInfo =
                            TrackPlacement.tryConnect(level, player, testPos, hitState, stack, false, true);

                    if (testInfo == null) continue;

                    PlacementInfoAccessor testAccessor = (PlacementInfoAccessor) (Object) testInfo;
                    if (!testAccessor.isValid()) continue;

                    BezierConnection testCurve = testAccessor.getCurve();
                    if (testCurve == null) continue;

                    double testRadius = testCurve.getRadius();
                    if (testRadius <= 0) continue;

                    // Track the best (maximum) radius position
                    if (testRadius > bestRadius) {
                        bestRadius = testRadius;
                        bestPos = testPos;
                    }

                    // Also collect positions that meet the target radius
                    if (testRadius >= targetRadius && testRadius < targetRadius * 1.2) {
                        targetPositions.add(testPos.below());
                    }
                } catch (Exception e) {
                    // Ignore errors in test placements
                }
            }
        }

        // Show highlight for positions meeting target radius, or the best position
        if (!targetPositions.isEmpty()) {
            Outliner.getInstance().showCluster("railwaytoolkit_target", targetPositions)
                    .withFaceTexture(AllSpecialTextures.THIN_CHECKERED)
                    .colored(0xFFFF00)  // Yellow
                    .lineWidth(0);
            lastHighlightPos = targetPositions.get(0);
        } else if (bestPos != null) {
            List<BlockPos> bestList = new ArrayList<>();
            bestList.add(bestPos.below());
            Outliner.getInstance().showCluster("railwaytoolkit_target", bestList)
                    .withFaceTexture(AllSpecialTextures.THIN_CHECKERED)
                    .colored(0xFFAA00)  // Orange for "best available"
                    .lineWidth(0);
            lastHighlightPos = bestPos;
        } else {
            clearHighlight();
        }
    }

    private static void clearHighlight() {
        if (lastHighlightPos != null) {
            Outliner.getInstance().remove("railwaytoolkit_target");
            lastHighlightPos = null;
        }
    }
}
