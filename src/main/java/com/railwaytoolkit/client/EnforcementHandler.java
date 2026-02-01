package com.railwaytoolkit.client;

import com.railwaytoolkit.RailwayToolkit;
import com.railwaytoolkit.config.RailwayToolkitConfig;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Handles enforcement level selection via scroll wheel when Alt is held.
 */
@EventBusSubscriber(modid = RailwayToolkit.MOD_ID, value = Dist.CLIENT)
public class EnforcementHandler {

    public enum EnforcementLevel {
        MAINLINE("Mainline", () -> RailwayToolkitConfig.CLIENT.mainlineMinRadius.get()),
        YARD("Yard", () -> RailwayToolkitConfig.CLIENT.yardMinRadius.get()),
        ABSOLUTE("Tight", () -> RailwayToolkitConfig.CLIENT.absoluteMinRadius.get());

        private final String displayName;
        private final java.util.function.Supplier<Double> radiusSupplier;

        EnforcementLevel(String displayName, java.util.function.Supplier<Double> radiusSupplier) {
            this.displayName = displayName;
            this.radiusSupplier = radiusSupplier;
        }

        public String getDisplayName() {
            return displayName;
        }

        public double getMinRadius() {
            return radiusSupplier.get();
        }

        public EnforcementLevel next() {
            EnforcementLevel[] values = values();
            return values[(ordinal() + 1) % values.length];
        }

        public EnforcementLevel previous() {
            EnforcementLevel[] values = values();
            return values[(ordinal() - 1 + values.length) % values.length];
        }
    }

    private static EnforcementLevel currentLevel = EnforcementLevel.MAINLINE;

    public static EnforcementLevel getCurrentLevel() {
        return currentLevel;
    }

    public static double getCurrentMinRadius() {
        return currentLevel.getMinRadius();
    }

    public static String getCurrentLevelName() {
        return currentLevel.getDisplayName();
    }

    /**
     * Handle scroll wheel events to cycle enforcement levels when Alt is held.
     */
    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (!RailwayToolkitConfig.CLIENT.enableEnforcement.get()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            return;
        }

        // Check if Alt is held
        long window = mc.getWindow().getWindow();
        boolean altHeld = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS ||
                         GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS;

        if (!altHeld) {
            return;
        }

        // Cycle through enforcement levels
        double scrollDelta = event.getScrollDeltaY();
        if (scrollDelta > 0) {
            currentLevel = currentLevel.previous();
            event.setCanceled(true);
        } else if (scrollDelta < 0) {
            currentLevel = currentLevel.next();
            event.setCanceled(true);
        }
    }
}
