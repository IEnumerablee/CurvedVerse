package org.i212.curvedverse.dimension;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.server.MinecraftServer;
import org.i212.curvedverse.util.ComplexNumber;

import java.util.Collections;
import java.util.List;

public class InterpolatedDimensionMetadata extends DimensionMetadata {
    public static final String TYPE = "interpolated";

    private final double temperature;
    private final double humidity;
    private final double hostility;
    private final double pollution;
    private final String biomeId;
    private final List<String> resources;
    private ComplexNumber complexPos;

    public InterpolatedDimensionMetadata(String coordinate, ResourceLocation dimensionId,
                             double temperature, double humidity, double hostility, double pollution,
                             ResourceLocation biomeId, List<String> resources) {
        super(coordinate, dimensionId, TYPE);
        this.temperature = temperature;
        this.humidity = humidity;
        this.hostility = hostility;
        this.pollution = pollution;
        this.biomeId = biomeId != null ? biomeId.toString() : null;
        this.resources = resources != null ? resources : Collections.emptyList();
        parseCoordinates(coordinate);
    }

    public InterpolatedDimensionMetadata(String coordinate, String dimensionId,
                             double temperature, double humidity, double hostility, double pollution,
                             String biomeId, List<String> resources) {
        super(coordinate, dimensionId, TYPE);
        this.temperature = temperature;
        this.humidity = humidity;
        this.hostility = hostility;
        this.pollution = pollution;
        this.biomeId = biomeId;
        this.resources = resources != null ? resources : Collections.emptyList();
        parseCoordinates(coordinate);
    }

    private void parseCoordinates(String coordinate) {
        try {
            this.complexPos = ComplexNumber.fromString(coordinate);
        } catch (Exception e) {
            this.complexPos = new ComplexNumber(0, 0);
        }
    }

    public ComplexNumber getComplexPos() { return complexPos; }

    public double getReal() { return complexPos.getReal(); }
    public double getImaginary() { return complexPos.getImaginary(); }

    public double getTemperature() { return temperature; }
    public double getHumidity() { return humidity; }
    public double getHostility() { return hostility; }
    public double getPollution() { return pollution; }

    public ResourceLocation getBiomeId() {
        return biomeId != null ? ResourceLocation.parse(biomeId) : null;
    }

    public List<String> getResources() {
        return resources;
    }

    @Override
    public ChunkGenerator createChunkGenerator(MinecraftServer server) {
        ChunkGenerator overworldGenerator = server.overworld().getChunkSource().getGenerator();

        if (overworldGenerator instanceof NoiseBasedChunkGenerator noiseGenerator) {
            Holder<NoiseGeneratorSettings> settingsHolder = noiseGenerator.generatorSettings();

            if (settingsHolder.isBound()) {
                NoiseGeneratorSettings original = settingsHolder.value();
                NoiseRouter router = buildRouter(original.noiseRouter());

                // temperature [0..1]: чем выше — тем выше горы (minY сдвигается вниз, height растёт)
                // humidity [0..1]: практически не меняет высоту, но влажность → больше эрозии → более плоский ландшафт
                int extraDepth = (int) ((temperature - 0.5) * 64);   // от -32 до +32 блоков глубины
                int newMinY  = original.noiseSettings().minY()  - Math.max(0, extraDepth);
                int newHeight = original.noiseSettings().height() + Math.abs(extraDepth);

                // Горизонтальный масштаб шума: hostility делает рельеф мельче/крупнее
                int newHorizontal = Math.max(1,
                    (int) (original.noiseSettings().noiseSizeHorizontal() * (1.0 + (hostility - 0.5) * 0.6)));
                // Вертикальный масштаб: humidity даёт более плоские или более острые горы
                int newVertical   = Math.max(1,
                    (int) (original.noiseSettings().noiseSizeVertical()   * (1.0 + (0.5 - humidity)   * 0.4)));

                NoiseSettings modifiedNoise = new NoiseSettings(newMinY, newHeight, newHorizontal, newVertical);

                // pollution [0..1]: высокое → мелкое море (пустынные пейзажи), низкое → глубокий океан
                int newSeaLevel = (int) (original.seaLevel() + (pollution - 0.5) * 40);

                // aquifers: при высокой враждебности отключаем предсказуемые водоёмы
                boolean aquifers = hostility < 0.65;

                NoiseGeneratorSettings modified = new NoiseGeneratorSettings(
                    modifiedNoise,
                    original.defaultBlock(),
                    original.defaultFluid(),
                    router,
                    original.surfaceRule(),
                    original.spawnTarget(),
                    newSeaLevel,
                    original.disableMobGeneration(),
                    aquifers,
                    original.oreVeinsEnabled(),
                    original.useLegacyRandomSource()
                );

                return new NoiseBasedChunkGenerator(
                    noiseGenerator.getBiomeSource(),
                    Holder.direct(modified)
                );
            }
        }

        return super.createChunkGenerator(server);
    }

    /**
     * Строит модифицированный NoiseRouter на основе параметров метадаты.
     *
     * <ul>
     *   <li><b>temperature</b> – сдвигает базовую глубину ({@code depth}):
     *       высокая температура поднимает рельеф вверх, делая горный мир.</li>
     *   <li><b>humidity</b> – масштабирует {@code erosion}:
     *       высокая влажность усиливает эрозию → ландшафт становится более пологим.</li>
     *   <li><b>hostility</b> – масштабирует {@code finalDensity}:
     *       высокая враждебность делает рельеф более хаотичным и резким.</li>
     *   <li><b>pollution</b> – сдвигает {@code continents}:
     *       высокое загрязнение смещает мир в сторону суши, низкое — в сторону океана.</li>
     * </ul>
     */
    private NoiseRouter buildRouter(NoiseRouter base) {
        // --- temperature → depth ---
        // depth определяет «базовую высоту» в точке, сдвигаем его константой.
        // temperature = 0.0 → опускаем на -0.5, temperature = 1.0 → поднимаем на +0.5
        double depthShift = (temperature - 0.5); // [-0.5 .. +0.5]
        DensityFunction depth = DensityFunctions.add(base.depth(), DensityFunctions.constant(depthShift));

        // --- humidity → erosion (масштабирование) ---
        // erosion отвечает за «изглаженность» рельефа.
        // humidity = 1.0 → сильная эрозия (умножаем на ~1.5), humidity = 0.0 → слабая (умножаем на ~0.5)
        double erosionScale = 0.5 + humidity;          // [0.5 .. 1.5]
        DensityFunction erosion = DensityFunctions.mul(base.erosion(), DensityFunctions.constant(erosionScale));

        // --- hostility → finalDensity (масштабирование хаоса) ---
        // finalDensity — итоговая плотность блока, её масштабирование делает горы выше/острее.
        // hostility = 0.0 → сглаживаем (*0.5), hostility = 1.0 → хаотично (*1.8)
        double densityScale = 0.5 + hostility * 1.3;   // [0.5 .. 1.8]
        DensityFunction finalDensity = DensityFunctions.mul(base.finalDensity(), DensityFunctions.constant(densityScale));

        // --- pollution → continents (смещение) ---
        // continents: >0 = суша, <0 = океан.
        // pollution = 1.0 → +0.4 (мир почти без океанов), pollution = 0.0 → -0.4 (мир-океан)
        double continentsShift = (pollution - 0.5) * 0.8; // [-0.4 .. +0.4]
        DensityFunction continents = DensityFunctions.add(base.continents(), DensityFunctions.constant(continentsShift));

        return new NoiseRouter(
            base.barrierNoise(),
            base.fluidLevelFloodednessNoise(),
            base.fluidLevelSpreadNoise(),
            base.lavaNoise(),
            base.temperature(),
            base.vegetation(),
            continents,         // <- pollution
            erosion,            // <- humidity
            depth,              // <- temperature
            base.ridges(),
            base.initialDensityWithoutJaggedness(),
            finalDensity,       // <- hostility
            base.veinToggle(),
            base.veinRidged(),
            base.veinGap()
        );
    }
}
