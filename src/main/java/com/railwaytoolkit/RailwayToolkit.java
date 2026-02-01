package com.railwaytoolkit;

import com.railwaytoolkit.config.RailwayToolkitConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(RailwayToolkit.MOD_ID)
public class RailwayToolkit {
    public static final String MOD_ID = "railwaytoolkit";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public RailwayToolkit(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Create Railway Toolkit initializing...");

        // Register config
        modContainer.registerConfig(ModConfig.Type.CLIENT, RailwayToolkitConfig.CLIENT_SPEC);

        LOGGER.info("Create Railway Toolkit initialized!");
    }
}
