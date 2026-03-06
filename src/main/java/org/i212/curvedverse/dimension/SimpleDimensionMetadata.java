package org.i212.curvedverse.dimension;

import net.minecraft.resources.ResourceLocation;

public class SimpleDimensionMetadata extends DimensionMetadata {
    public static final String TYPE = "simple";

    public SimpleDimensionMetadata(String coordinate, ResourceLocation dimensionId) {
        super(coordinate, dimensionId, TYPE);
    }
}

