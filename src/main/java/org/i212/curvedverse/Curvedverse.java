package org.i212.curvedverse;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.i212.curvedverse.dimension.CurvedverseDimensionRegistry;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;

@Mod(Curvedverse.MODID)
public class Curvedverse {
    public static final String MODID = "curvedverse";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Curvedverse(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.register(this);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            org.i212.curvedverse.util.ifs.DragonCurveStrategy dragonCurve = new org.i212.curvedverse.util.ifs.DragonCurveStrategy();
            dragonCurve.bake();
            org.i212.curvedverse.util.ifs.IFSStrategy.register("DragonCurve", dragonCurve);
            LOGGER.info("Registered and baked DragonCurve strategy");
        });
        LOGGER.info("Curvedverse common setup complete");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Curvedverse server starting");
        
        MinecraftServer server = event.getServer();
        CurvedverseDimensionRegistry registry = null;

        try {
            LOGGER.info("Initializing Curvedverse Dimension Registry...");
            registry = CurvedverseDimensionRegistry.getInstance(server);
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize dimension registry", e);
            return;
        }
        
        try {
            LOGGER.info("Scanning player data for custom dimensions...");
            Path playerDataDir = server.getWorldPath(LevelResource.PLAYER_DATA_DIR);
            File[] playerFiles = playerDataDir.toFile().listFiles((dir, name) -> name.endsWith(".dat"));
            
            if (playerFiles != null) {
                for (File playerFile : playerFiles) {
                    try {
                        CompoundTag playerNbt;
                        try (FileInputStream fis = new FileInputStream(playerFile)) {
                             playerNbt = NbtIo.readCompressed(fis, net.minecraft.nbt.NbtAccounter.unlimitedHeap());
                        }
                        loadPlayerDimension(playerNbt, registry);
                    } catch (Exception e) {
                        LOGGER.warn("Failed to read player data: {}", playerFile.getName(), e);
                    }
                }
            }
            
            File levelDat = server.getWorldPath(LevelResource.LEVEL_DATA_FILE).toFile();
            if (levelDat.exists()) {
                try {
                    CompoundTag levelNbt;
                    try (FileInputStream fis = new FileInputStream(levelDat)) {
                        levelNbt = NbtIo.readCompressed(fis, net.minecraft.nbt.NbtAccounter.unlimitedHeap());
                    }
                    if (levelNbt.contains("Data")) {
                        CompoundTag data = levelNbt.getCompound("Data");
                        if (data.contains("Player")) {
                            loadPlayerDimension(data.getCompound("Player"), registry);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to check level.dat for player dimension", e);
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("Error during player dimension scan", e);
        }
    }

    private void loadPlayerDimension(CompoundTag playerNbt, CurvedverseDimensionRegistry registry) {
        if (playerNbt.contains("Dimension")) {
            String dimString = playerNbt.getString("Dimension");
            if (!dimString.isEmpty()) {
                ResourceLocation dimId = ResourceLocation.tryParse(dimString);
                if (dimId != null) {
                    registry.loadDimension(dimId);
                }
            }
        }
    }
}
