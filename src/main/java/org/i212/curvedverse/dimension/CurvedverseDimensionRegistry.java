package org.i212.curvedverse.dimension;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import net.commoble.infiniverse.api.InfiniverseAPI;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CurvedverseDimensionRegistry extends SavedData {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(DimensionMetadata.class, (JsonDeserializer<DimensionMetadata>) (json, typeOfT, context) -> {
                JsonObject jsonObject = json.getAsJsonObject();
                String type = jsonObject.get("type").getAsString();
                if (InterpolatedDimensionMetadata.TYPE.equals(type)) {
                    return context.deserialize(json, InterpolatedDimensionMetadata.class);
                } else if (SimpleDimensionMetadata.TYPE.equals(type)) {
                    return context.deserialize(json, SimpleDimensionMetadata.class);
                }
                return null;
            })
            .create();
    private static final String SAVE_FILENAME = "curvedverse_dimensions.json";
    
    private final Map<String, DimensionMetadata> dimensions = new ConcurrentHashMap<>();
    private final MinecraftServer server;


    private CurvedverseDimensionRegistry(MinecraftServer server) {
        this.server = server;
        loadFromJson();
    }

    public static CurvedverseDimensionRegistry getInstance(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        
        return overworld.getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(
                () -> new CurvedverseDimensionRegistry(server),
                (tag, registryAccess) -> {
                     CurvedverseDimensionRegistry registry = new CurvedverseDimensionRegistry(server);
                     return registry;
                },
                null
            ),
            "curvedverse_dimensions"
        );
    }

    private void loadFromJson() {
        Path savePath = server.getWorldPath(LevelResource.ROOT).resolve(SAVE_FILENAME);
        File file = savePath.toFile();

        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                Type type = new TypeToken<Map<String, DimensionMetadata>>(){}.getType();
                Map<String, DimensionMetadata> loaded = GSON.fromJson(reader, type);
                if (loaded != null) {
                    dimensions.clear();
                    dimensions.putAll(loaded);
                    LOGGER.info("Loaded {} dimensions from {}", dimensions.size(), SAVE_FILENAME);
                    
                    for (DimensionMetadata meta : dimensions.values()) {
                        restoreDimension(meta);
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Failed to load dimension registry from JSON", e);
            }
        }
    }

    private void saveToJson() {
        Path savePath = server.getWorldPath(LevelResource.ROOT).resolve(SAVE_FILENAME);
        File file = savePath.toFile();
        File tempFile = new File(file.getAbsolutePath() + ".tmp");

        try (FileWriter writer = new FileWriter(tempFile)) {
            GSON.toJson(dimensions, writer);
            writer.flush();
        } catch (IOException e) {
            LOGGER.error("Failed to save dimension registry to temporary JSON", e);
            return;
        }

        try {
            if (file.exists() && !file.delete()) {
                LOGGER.error("Failed to delete old dimension registry file");
            }
            if (!tempFile.renameTo(file)) {
                LOGGER.error("Failed to rename temporary dimension registry file to {}", SAVE_FILENAME);
            } else {
                LOGGER.info("Saved {} dimensions to {}", dimensions.size(), SAVE_FILENAME);
            }
        } catch (Exception e) {
            LOGGER.error("Error finalizing dimension registry save", e);
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        return tag;
    }

    public void registerDimension(DimensionMetadata metadata) {
        dimensions.put(metadata.getCoordinate(), metadata);
        saveToJson();
        setDirty();
    }
    
    public DimensionMetadata getDimensionMetadata(String key) {
        return dimensions.get(key);
    }
    
    public DimensionMetadata getDimensionMetadata(ResourceLocation id) {
        for (DimensionMetadata meta : dimensions.values()) {
            if (meta.getDimensionId().equals(id)) {
                return meta;
            }
        }
        return null;
    }

    public ServerLevel loadDimension(String key) {
        DimensionMetadata meta = getDimensionMetadata(key);
        if (meta == null) {
            LOGGER.warn("Dimension metadata not found for key {}", key);
            return null;
        }
        return restoreDimension(meta);
    }
    
    public ServerLevel loadDimension(ResourceLocation id) {
        DimensionMetadata meta = getDimensionMetadata(id);
        if (meta == null) {
            return null;
        }
        return restoreDimension(meta);
    }

    private ServerLevel restoreDimension(DimensionMetadata meta) {
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, meta.getDimensionId());
        
        try {
            return InfiniverseAPI.get().getOrCreateLevel(server, key, () -> {
                ChunkGenerator generator = meta.createChunkGenerator(server);
                 ResourceKey<DimensionType> typeKey = ResourceKey.create(Registries.DIMENSION_TYPE, ResourceLocation.fromNamespaceAndPath("curvedverse", "default"));
                 var typeHolder = server.registryAccess().registryOrThrow(Registries.DIMENSION_TYPE).getHolderOrThrow(typeKey);
                 
                 return new LevelStem(typeHolder, generator);
            });
        } catch (Exception e) {
            LOGGER.error("Error restoring dimension {}", meta.getDimensionId(), e);
            return null;
        }
    }

    public void unloadDimension(String key) {
        DimensionMetadata meta = getDimensionMetadata(key);
        if (meta != null) {
            ResourceKey<Level> levelKey = ResourceKey.create(Registries.DIMENSION, meta.getDimensionId());
            InfiniverseAPI.get().markDimensionForUnregistration(server, levelKey);
        }
    }
    
    public Collection<DimensionMetadata> getAllDimensions() {
        return Collections.unmodifiableCollection(dimensions.values());
    }

    public ServerLevel createAndRegister(DimensionMetadata meta) {
        LOGGER.info("Attempting to create dimension {} of type {} at key {}",
            meta.getDimensionId(), meta.getType(), meta.getCoordinate());

        ServerLevel level = restoreDimension(meta);

        if (level != null) {
            LOGGER.info("Successfully created dimension {}", meta.getDimensionId());
            registerDimension(meta);
            // Лишняя подстраховка, но registerDimension уже вызывает saveToJson()
        } else {
            LOGGER.error("Failed to create dimension {}", meta.getDimensionId());
        }
        return level;
    }
}
