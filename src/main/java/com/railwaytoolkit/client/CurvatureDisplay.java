package com.railwaytoolkit.client;

import com.railwaytoolkit.config.RailwayToolkitConfig;
import com.railwaytoolkit.config.RailwayToolkitConfig.CurvatureRating;
import com.railwaytoolkit.mixin.accessor.PlacementInfoAccessor;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackPlacement;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/**
 * Handles the display of curvature radius and slope information
 * during track placement.
 */
public class CurvatureDisplay {

    private static long lastDisplayTime = 0;
    private static final long DISPLAY_COOLDOWN_MS = 50; // Prevent flickering

    /**
     * Called from the mixin after Create's clientTick processes track placement.
     */
    public static void onTrackPlacementTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player == null) {
            return;
        }

        // Check if we're in track placement mode by accessing Create's cached placement info
        TrackPlacement.PlacementInfo info = TrackPlacement.cached;

        if (info == null) {
            return;
        }

        // Cast to accessor to access package-private fields
        PlacementInfoAccessor accessor = (PlacementInfoAccessor) (Object) info;

        // Only display if there's a valid curve being placed
        BezierConnection curve = accessor.getCurve();
        if (curve == null) {
            return;
        }

        // Get curve properties
        double radius = curve.getRadius();
        double slope = calculateSlope(accessor);

        // Check for enforcement mode (Ctrl+Alt held)
        boolean enforcing = isEnforcementKeyHeld();
        double enforcementRadius = RailwayToolkitConfig.getEnforcementMinRadius();

        // Build display message
        MutableComponent message = buildDisplayMessage(radius, slope, enforcing, enforcementRadius);

        if (message != null) {
            // Throttle display updates
            long now = System.currentTimeMillis();
            if (now - lastDisplayTime > DISPLAY_COOLDOWN_MS) {
                player.displayClientMessage(message, true);
                lastDisplayTime = now;
            }
        }
    }

    /**
     * Calculate the slope/grade of the track placement.
     * Returns the grade as a percentage.
     */
    private static double calculateSlope(PlacementInfoAccessor accessor) {
        Vec3 end1 = accessor.getEnd1();
        Vec3 end2 = accessor.getEnd2();

        if (end1 == null || end2 == null) {
            return 0;
        }

        double horizontalDistance = Math.sqrt(
                Math.pow(end2.x - end1.x, 2) +
                Math.pow(end2.z - end1.z, 2)
        );

        if (horizontalDistance < 0.001) {
            return 0;
        }

        double verticalChange = end2.y - end1.y;
        return (verticalChange / horizontalDistance) * 100.0;
    }

    /**
     * Check if the enforcement modifier keys are being held (Ctrl+Alt).
     */
    public static boolean isEnforcementKeyHeld() {
        if (!RailwayToolkitConfig.CLIENT.enableEnforcement.get()) {
            return false;
        }

        long window = Minecraft.getInstance().getWindow().getWindow();
        boolean ctrlHeld = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS ||
                          GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
        boolean altHeld = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS ||
                         GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS;

        return ctrlHeld && altHeld;
    }

    /**
     * Build the display message showing curvature information.
     */
    private static MutableComponent buildDisplayMessage(double radius, double slope,
                                                         boolean enforcing, double enforcementRadius) {
        RailwayToolkitConfig.ClientConfig config = RailwayToolkitConfig.CLIENT;

        MutableComponent message = Component.empty();
        boolean hasContent = false;

        // Show curvature radius
        if (config.showCurvatureRadius.get() && radius > 0) {
            String radiusText;
            if (config.showDecimalPlaces.get()) {
                radiusText = String.format("R: %.1f", radius);
            } else {
                radiusText = String.format("R: %d", Math.round(radius));
            }
            message.append(Component.literal(radiusText).withStyle(ChatFormatting.WHITE));
            hasContent = true;
        }

        // Show curvature rating
        if (config.showCurvatureRating.get() && radius > 0) {
            CurvatureRating rating = RailwayToolkitConfig.getRating(radius);

            if (hasContent) {
                message.append(Component.literal(" | ").withStyle(ChatFormatting.GRAY));
            }

            ChatFormatting ratingColor = switch (rating) {
                case MAINLINE -> ChatFormatting.GREEN;
                case YARD -> ChatFormatting.YELLOW;
                case TOO_TIGHT -> ChatFormatting.GOLD;
                case INVALID -> ChatFormatting.RED;
            };

            message.append(Component.literal(rating.getDisplayName()).withStyle(ratingColor));
            hasContent = true;
        }

        // Show slope
        if (config.showSlope.get() && Math.abs(slope) > 0.1) {
            if (hasContent) {
                message.append(Component.literal(" | ").withStyle(ChatFormatting.GRAY));
            }

            String slopeText = String.format("Grade: %.1f%%", slope);
            ChatFormatting slopeColor = Math.abs(slope) > 10 ? ChatFormatting.RED :
                                        Math.abs(slope) > 5 ? ChatFormatting.YELLOW :
                                        ChatFormatting.GREEN;
            message.append(Component.literal(slopeText).withStyle(slopeColor));
            hasContent = true;
        }

        // Show enforcement status
        if (enforcing) {
            if (hasContent) {
                message.append(Component.literal(" | ").withStyle(ChatFormatting.GRAY));
            }

            boolean meetsRequirement = radius >= enforcementRadius;
            String enfLevel = config.enforcementLevel.get();
            String enfText = meetsRequirement ?
                    String.format("[%s OK]", enfLevel) :
                    String.format("[%s: Need R>=%.0f]", enfLevel, enforcementRadius);

            ChatFormatting enfColor = meetsRequirement ? ChatFormatting.GREEN : ChatFormatting.RED;
            message.append(Component.literal(enfText).withStyle(enfColor));
            hasContent = true;
        }

        return hasContent ? message : null;
    }

    /**
     * Check if current track placement should be blocked due to enforcement.
     * This is called separately to potentially cancel placement.
     */
    public static boolean shouldBlockPlacement(double radius) {
        if (!isEnforcementKeyHeld()) {
            return false;
        }

        double enforcementRadius = RailwayToolkitConfig.getEnforcementMinRadius();
        return radius > 0 && radius < enforcementRadius;
    }
}
