package com.railwaytoolkit.client;

import com.railwaytoolkit.config.RailwayToolkitConfig;
import com.railwaytoolkit.config.RailwayToolkitConfig.CurvatureRating;
import com.railwaytoolkit.mixin.accessor.PlacementInfoAccessor;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackPlacement;
import net.createmod.catnip.data.Couple;
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

    /**
     * Called from the mixin at the end of clientTick to display our curvature info.
     * This overwrites Create's message but includes all relevant information.
     */
    public static void displayCurvatureInfo() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player == null) {
            return;
        }

        TrackPlacement.PlacementInfo info = TrackPlacement.cached;
        if (info == null) {
            return;
        }

        PlacementInfoAccessor accessor = (PlacementInfoAccessor) (Object) info;
        BezierConnection curve = accessor.getCurve();

        double radius = curve != null ? calculateMinimumRadius(curve) : 0;
        double slope = calculateSlope(accessor);

        boolean enforcing = isEnforcementKeyHeld();
        double enforcementRadius = EnforcementHandler.getCurrentMinRadius();

        MutableComponent curvatureInfo = buildCurvatureInfo(radius, slope, enforcing, enforcementRadius);

        if (curvatureInfo != null) {
            // Build combined message with Create's status
            MutableComponent combined = Component.empty();

            // Add Create's status first
            if (accessor.isValid()) {
                combined.append(Component.literal("Can Connect").withStyle(ChatFormatting.GREEN));
            } else {
                String msg = accessor.getMessage();
                if (msg != null && !msg.equals("track.second_point")) {
                    combined.append(Component.literal("Invalid").withStyle(ChatFormatting.RED));
                } else {
                    combined.append(Component.literal("Select End").withStyle(ChatFormatting.WHITE));
                }
            }

            combined.append(Component.literal(" | ").withStyle(ChatFormatting.DARK_GRAY));
            combined.append(curvatureInfo);

            player.displayClientMessage(combined, true);
        }
    }

    /**
     * Called from the mixin to append our curvature info to Create's message.
     * @deprecated Use displayCurvatureInfo() instead
     */
    @Deprecated
    public static Component appendCurvatureInfo(Component createMessage) {
        TrackPlacement.PlacementInfo info = TrackPlacement.cached;

        if (info == null) {
            return createMessage;
        }

        // Cast to accessor to access package-private fields
        PlacementInfoAccessor accessor = (PlacementInfoAccessor) (Object) info;

        // Get curve - may be null for straight tracks
        BezierConnection curve = accessor.getCurve();

        // Get curve properties - use our calculation for minimum radius
        double radius = curve != null ? calculateMinimumRadius(curve) : 0;
        double slope = calculateSlope(accessor);

        // Check for enforcement mode (Alt held)
        boolean enforcing = isEnforcementKeyHeld();
        double enforcementRadius = EnforcementHandler.getCurrentMinRadius();

        // Build our curvature info
        MutableComponent curvatureInfo = buildCurvatureInfo(radius, slope, enforcing, enforcementRadius);

        if (curvatureInfo == null) {
            return createMessage;
        }

        // Combine Create's message with our info
        MutableComponent combined = Component.empty();
        combined.append(createMessage);
        combined.append(Component.literal(" | ").withStyle(ChatFormatting.DARK_GRAY));
        combined.append(curvatureInfo);

        return combined;
    }

    /**
     * Calculate the minimum curvature radius of a Bezier curve.
     * This works for both regular curves and S-curves.
     *
     * For a cubic Bezier curve B(t), the curvature at t is:
     * κ(t) = |B'(t) × B''(t)| / |B'(t)|³
     *
     * The radius at t is R(t) = 1 / κ(t)
     */
    private static double calculateMinimumRadius(BezierConnection curve) {
        // First try Create's built-in radius (works for simple curves)
        double createRadius = curve.getRadius();
        if (createRadius > 0) {
            return createRadius;
        }

        // For S-curves and other cases, calculate from the Bezier curve
        Couple<Vec3> starts = curve.starts;
        Couple<Vec3> axes = curve.axes;

        Vec3 p0 = starts.getFirst();
        Vec3 p3 = starts.getSecond();

        double handleLength = curve.getHandleLength();
        Vec3 p1 = p0.add(axes.getFirst().normalize().scale(handleLength));
        Vec3 p2 = p3.add(axes.getSecond().normalize().scale(handleLength));

        // Sample the curve to find minimum radius
        double minRadius = Double.MAX_VALUE;
        int samples = 20;

        for (int i = 1; i < samples; i++) {
            double t = i / (double) samples;
            double radius = calculateRadiusAt(p0, p1, p2, p3, t);
            if (radius > 0 && radius < minRadius) {
                minRadius = radius;
            }
        }

        return minRadius == Double.MAX_VALUE ? 0 : minRadius;
    }

    /**
     * Calculate the curvature radius at a specific point on a cubic Bezier curve.
     */
    private static double calculateRadiusAt(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, double t) {
        // First derivative B'(t)
        Vec3 d1 = bezierDerivative(p0, p1, p2, p3, t);

        // Second derivative B''(t)
        Vec3 d2 = bezierSecondDerivative(p0, p1, p2, p3, t);

        // Curvature κ = |d1 × d2| / |d1|³
        Vec3 cross = d1.cross(d2);
        double crossMag = cross.length();
        double d1Mag = d1.length();

        if (d1Mag < 0.0001) {
            return 0;
        }

        double curvature = crossMag / (d1Mag * d1Mag * d1Mag);

        if (curvature < 0.0001) {
            return 0; // Essentially straight
        }

        return 1.0 / curvature;
    }

    /**
     * First derivative of a cubic Bezier curve at parameter t.
     */
    private static Vec3 bezierDerivative(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, double t) {
        double mt = 1 - t;
        // B'(t) = 3(1-t)²(P1-P0) + 6(1-t)t(P2-P1) + 3t²(P3-P2)
        return p1.subtract(p0).scale(3 * mt * mt)
                .add(p2.subtract(p1).scale(6 * mt * t))
                .add(p3.subtract(p2).scale(3 * t * t));
    }

    /**
     * Second derivative of a cubic Bezier curve at parameter t.
     */
    private static Vec3 bezierSecondDerivative(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, double t) {
        double mt = 1 - t;
        // B''(t) = 6(1-t)(P2-2P1+P0) + 6t(P3-2P2+P1)
        Vec3 term1 = p2.subtract(p1.scale(2)).add(p0);
        Vec3 term2 = p3.subtract(p2.scale(2)).add(p1);
        return term1.scale(6 * mt).add(term2.scale(6 * t));
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
     * Check if the enforcement modifier key is being held (Alt only).
     */
    public static boolean isEnforcementKeyHeld() {
        if (!RailwayToolkitConfig.CLIENT.enableEnforcement.get()) {
            return false;
        }

        long window = Minecraft.getInstance().getWindow().getWindow();
        boolean altHeld = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS ||
                         GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS;

        return altHeld;
    }

    /**
     * Build the curvature info portion of the display message.
     */
    private static MutableComponent buildCurvatureInfo(double radius, double slope,
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
                message.append(Component.literal(" ").withStyle(ChatFormatting.GRAY));
            }

            ChatFormatting ratingColor = switch (rating) {
                case MAINLINE -> ChatFormatting.GREEN;
                case YARD -> ChatFormatting.YELLOW;
                case TOO_TIGHT -> ChatFormatting.GOLD;
                case INVALID -> ChatFormatting.RED;
            };

            message.append(Component.literal("[" + rating.getDisplayName() + "]").withStyle(ratingColor));
            hasContent = true;
        }

        // Show slope
        if (config.showSlope.get() && Math.abs(slope) > 0.1) {
            if (hasContent) {
                message.append(Component.literal(" | ").withStyle(ChatFormatting.GRAY));
            }

            String slopeText = String.format("%.1f%%", slope);
            ChatFormatting slopeColor = Math.abs(slope) > 10 ? ChatFormatting.RED :
                                        Math.abs(slope) > 5 ? ChatFormatting.YELLOW :
                                        ChatFormatting.GREEN;
            message.append(Component.literal(slopeText).withStyle(slopeColor));
            hasContent = true;
        }

        // Show enforcement status when Alt is held
        if (enforcing) {
            if (hasContent) {
                message.append(Component.literal(" ").withStyle(ChatFormatting.GRAY));
            }

            String enfLevel = EnforcementHandler.getCurrentLevelName();
            if (radius > 0) {
                boolean meetsRequirement = radius >= enforcementRadius;
                String enfText = meetsRequirement ?
                        String.format("[%s OK]", enfLevel) :
                        String.format("[%s R>=%.0f]", enfLevel, enforcementRadius);
                ChatFormatting enfColor = meetsRequirement ? ChatFormatting.GREEN : ChatFormatting.RED;
                message.append(Component.literal(enfText).withStyle(enfColor));
            } else {
                // Straight track - always OK for enforcement, show scroll hint
                message.append(Component.literal("[" + enfLevel + " - Scroll]").withStyle(ChatFormatting.AQUA));
            }
            hasContent = true;
        }

        return hasContent ? message : null;
    }

    /**
     * Check if current track placement should be blocked due to enforcement.
     * This is called separately to potentially modify placement behavior.
     */
    public static boolean shouldBlockPlacement(double radius) {
        if (!isEnforcementKeyHeld()) {
            return false;
        }

        double enforcementRadius = EnforcementHandler.getCurrentMinRadius();
        return radius > 0 && radius < enforcementRadius;
    }

    /**
     * Get the minimum radius for a curve, used by enforcement mixin.
     */
    public static double getMinimumRadius(BezierConnection curve) {
        return curve != null ? calculateMinimumRadius(curve) : 0;
    }
}
