package org.i212.curvedverse.dimension;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.i212.curvedverse.util.ComplexNumber;
import org.i212.curvedverse.util.attractor.InterpolatedData;
import org.i212.curvedverse.util.attractor.WorldAttractorManager;
import org.i212.curvedverse.util.ifs.DragonCurveStrategy;
import org.i212.curvedverse.util.ifs.IFSStrategy;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ComplexWorldDimensionManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final MinecraftServer server;
    private final WorldAttractorManager attractorManager;
    private final CurvedverseDimensionRegistry dimensionRegistry;
    private final IFSStrategy defaultStrategy;

    public ComplexWorldDimensionManager(MinecraftServer server) {
        this.server = server;
        this.attractorManager = WorldAttractorManager.get(server);
        this.dimensionRegistry = CurvedverseDimensionRegistry.getInstance(server);
        this.defaultStrategy = new DragonCurveStrategy();
        this.defaultStrategy.bake();

        this.attractorManager.generate(defaultStrategy);
    }

    public ServerLevel getOrCreateStartDimension() {
        return getOrCreateDimensionAt(new ComplexNumber(0, 0));
    }

    public List<ComplexNumber> getTransitionPoints(ServerLevel currentLevel) {
        ResourceLocation id = currentLevel.dimension().location();
        DimensionMetadata meta = dimensionRegistry.getDimensionMetadata(id);

        ComplexNumber currentPoint;
        if (meta instanceof InterpolatedDimensionMetadata interpolated) {
            currentPoint = interpolated.getComplexPos();
        } else {
            currentPoint = new ComplexNumber(0, 0);
        }

        return getDragonCurvePaths(currentPoint);
    }

    public ServerLevel transitionToPoint(ComplexNumber targetPoint) {
        return getOrCreateDimensionAt(targetPoint);
    }

    public ServerLevel getOrCreateDimensionAt(ComplexNumber point) {
        String coordinateKey = point.toString();

        DimensionMetadata existing = dimensionRegistry.getDimensionMetadata(coordinateKey);
        if (existing != null) {
            return dimensionRegistry.loadDimension(coordinateKey);
        }

        InterpolatedData data = attractorManager.interpolate(point);

        String safeIdPos = coordinateKey
                .replace(".", "d")
                .replace("-", "m")
                .replace("_", "i");
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("curvedverse", "dim_" + safeIdPos);

        List<String> resources = data.getCharacters().stream()
                .map(String::valueOf)
                .collect(Collectors.toList());

        InterpolatedDimensionMetadata metadata = new InterpolatedDimensionMetadata(
            coordinateKey,
            id,
            data.getTemperature(),
            data.getHumidity(),
            data.getHostility(),
            data.getPollution(),
            ResourceLocation.withDefaultNamespace("plains"),
            resources
        );

        return dimensionRegistry.createAndRegister(metadata);
    }

    public List<ComplexNumber> getDragonCurvePaths(ComplexNumber currentPoint) {
        List<ComplexNumber> paths = new ArrayList<>();
        defaultStrategy.getTransformations().forEach(t -> {
            paths.add(t.apply(currentPoint));
        });
        return paths;
    }

    public MinecraftServer getServer() {
        return server;
    }
}
