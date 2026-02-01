package com.railwaytoolkit.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class RailwayToolkitConfig {
    public static final ClientConfig CLIENT;
    public static final ModConfigSpec CLIENT_SPEC;

    static {
        final Pair<ClientConfig, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(ClientConfig::new);
        CLIENT = specPair.getLeft();
        CLIENT_SPEC = specPair.getRight();
    }

    public static class ClientConfig {
        // Curvature display settings
        public final ModConfigSpec.BooleanValue showCurvatureRadius;
        public final ModConfigSpec.BooleanValue showSlope;
        public final ModConfigSpec.BooleanValue showCurvatureRating;

        // Curvature thresholds (in blocks)
        public final ModConfigSpec.DoubleValue mainlineMinRadius;
        public final ModConfigSpec.DoubleValue yardMinRadius;
        public final ModConfigSpec.DoubleValue absoluteMinRadius;

        // Enforcement settings
        public final ModConfigSpec.BooleanValue enableEnforcement;
        public final ModConfigSpec.ConfigValue<String> enforcementLevel;

        // Display format
        public final ModConfigSpec.BooleanValue showDecimalPlaces;

        ClientConfig(ModConfigSpec.Builder builder) {
            builder.comment("Create Railway Toolkit Client Configuration")
                   .push("display");

            showCurvatureRadius = builder
                    .comment("Show the curvature radius while placing track")
                    .define("showCurvatureRadius", true);

            showSlope = builder
                    .comment("Show the slope/grade while placing track")
                    .define("showSlope", true);

            showCurvatureRating = builder
                    .comment("Show the curvature rating (Mainline/Yard/Too Tight)")
                    .define("showCurvatureRating", true);

            showDecimalPlaces = builder
                    .comment("Show decimal places in radius display")
                    .define("showDecimalPlaces", true);

            builder.pop();

            builder.comment("Curvature Rating Thresholds",
                           "Curves with radius >= mainlineMinRadius are rated as 'Mainline'",
                           "Curves with radius >= yardMinRadius but < mainlineMinRadius are rated as 'Yard'",
                           "Curves with radius < yardMinRadius but >= absoluteMinRadius are rated as 'Too Tight'",
                           "Curves with radius < absoluteMinRadius cannot be placed (Create's built-in limit)")
                   .push("thresholds");

            mainlineMinRadius = builder
                    .comment("Minimum radius for mainline-suitable curves (in blocks)",
                            "Recommended minimum for mainline operations is 60+ blocks")
                    .defineInRange("mainlineMinRadius", 60.0, 1.0, 1000.0);

            yardMinRadius = builder
                    .comment("Minimum radius for yard-suitable curves (in blocks)",
                            "Typical yard curves are tighter than mainline")
                    .defineInRange("yardMinRadius", 20.0, 1.0, 1000.0);

            absoluteMinRadius = builder
                    .comment("Absolute minimum radius before track becomes 'Too Tight' (in blocks)",
                            "Create's built-in minimum is approximately 7 blocks for 90-degree turns")
                    .defineInRange("absoluteMinRadius", 7.0, 1.0, 100.0);

            builder.pop();

            builder.comment("Enforcement Settings",
                           "When enabled and Alt is held, enforce minimum curvature limits")
                   .push("enforcement");

            enableEnforcement = builder
                    .comment("Enable the Alt key enforcement modifier")
                    .define("enableEnforcement", true);

            enforcementLevel = builder
                    .comment("Which limit to enforce when Alt is held",
                            "Valid values: MAINLINE, YARD, ABSOLUTE")
                    .define("enforcementLevel", "MAINLINE");

            builder.pop();
        }
    }

    public enum CurvatureRating {
        MAINLINE("Mainline", 0x00FF00),    // Green
        YARD("Yard", 0xFFFF00),            // Yellow
        TOO_TIGHT("Too Tight", 0xFF6600),  // Orange
        INVALID("Invalid", 0xFF0000);      // Red

        private final String displayName;
        private final int color;

        CurvatureRating(String displayName, int color) {
            this.displayName = displayName;
            this.color = color;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getColor() {
            return color;
        }
    }

    public static CurvatureRating getRating(double radius) {
        if (radius <= 0) {
            return CurvatureRating.INVALID;
        }
        if (radius >= CLIENT.mainlineMinRadius.get()) {
            return CurvatureRating.MAINLINE;
        }
        if (radius >= CLIENT.yardMinRadius.get()) {
            return CurvatureRating.YARD;
        }
        if (radius >= CLIENT.absoluteMinRadius.get()) {
            return CurvatureRating.TOO_TIGHT;
        }
        return CurvatureRating.INVALID;
    }

    public static double getEnforcementMinRadius() {
        String level = CLIENT.enforcementLevel.get().toUpperCase();
        return switch (level) {
            case "MAINLINE" -> CLIENT.mainlineMinRadius.get();
            case "YARD" -> CLIENT.yardMinRadius.get();
            case "ABSOLUTE" -> CLIENT.absoluteMinRadius.get();
            default -> CLIENT.mainlineMinRadius.get();
        };
    }
}
