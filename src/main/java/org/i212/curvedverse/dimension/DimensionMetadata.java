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

public abstract class DimensionMetadata {
    protected final String coordinate;
    protected final String dimensionId;
    protected final String type;

    public DimensionMetadata(String coordinate, ResourceLocation dimensionId, String type) {
        this.coordinate = coordinate;
        this.dimensionId = dimensionId.toString();
        this.type = type;
    }
    
    public DimensionMetadata(String coordinate, String dimensionId, String type) {
        this.coordinate = coordinate;
        this.dimensionId = dimensionId;
        this.type = type;
    }

    public String getCoordinate() {
        return coordinate;
    }

    public ResourceLocation getDimensionId() {
        return ResourceLocation.parse(dimensionId);
    }

    public String getType() {
        return type;
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
