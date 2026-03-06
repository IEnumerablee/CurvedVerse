package org.i212.curvedverse.dimension;

import com.mojang.serialization.JavaOps;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;

import java.util.Collections;
import java.util.List;

public class DimensionMetadata {
    private final String coordinate;
    private final String dimensionId;
    
    private final double temperature;
    private final double humidity;
    private final double hostility;
    private final String biomeId;
    private final List<String> resources;

    public DimensionMetadata(String coordinate, ResourceLocation dimensionId,
                             double temperature, double humidity, double hostility,
                             ResourceLocation biomeId, List<String> resources) {
        this.coordinate = coordinate;
        this.dimensionId = dimensionId.toString();
        this.temperature = temperature;
        this.humidity = humidity;
        this.hostility = hostility;
        this.biomeId = biomeId.toString();
        this.resources = resources != null ? resources : Collections.emptyList();
    }
    
    public DimensionMetadata(String coordinate, String dimensionId,
                             double temperature, double humidity, double hostility,
                             String biomeId, List<String> resources) {
        this.coordinate = coordinate;
        this.dimensionId = dimensionId;
        this.temperature = temperature;
        this.humidity = humidity;
        this.hostility = hostility;
        this.biomeId = biomeId;
        this.resources = resources != null ? resources : Collections.emptyList();
    }

    public String getCoordinate() {
        return coordinate;
    }

    public ResourceLocation getDimensionId() {
        return ResourceLocation.parse(dimensionId);
    }

    public double getTemperature() {
        return temperature;
    }

    public double getHumidity() {
        return humidity;
    }

    public double getHostility() {
        return hostility;
    }

    public ResourceLocation getBiomeId() {
        return ResourceLocation.parse(biomeId);
    }

    public List<String> getResources() {
        return resources;
    }

    public ChunkGenerator createChunkGenerator(MinecraftServer server) {
        ChunkGenerator overworldGenerator = server.overworld().getChunkSource().getGenerator();

        RegistryAccess registryAccess = server.registryAccess();

        RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, registryAccess);

        return ChunkGenerator.CODEC.encodeStart(ops, overworldGenerator)
                .flatMap(nbt -> ChunkGenerator.CODEC.parse(ops, nbt))
                .getOrThrow(error -> new IllegalStateException("Could not copy chunk generator: " + error));
    }

    public DimensionType getDimensionType(MinecraftServer server) {
        ResourceKey<DimensionType> typeKey = ResourceKey.create(Registries.DIMENSION_TYPE, ResourceLocation.fromNamespaceAndPath("curvedverse", "default"));
        DimensionType type = server.registryAccess().registryOrThrow(Registries.DIMENSION_TYPE).get(typeKey);
        return type;
    }
}
