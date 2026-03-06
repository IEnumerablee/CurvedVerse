package org.i212.curvedverse.util.attractor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import org.i212.curvedverse.util.ComplexNumber;
import org.i212.curvedverse.util.ifs.IFSStrategy;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class WorldAttractorManager extends SavedData {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final List<Attractor> attractors = new ArrayList<>();
    private final MinecraftServer server;

    public WorldAttractorManager(MinecraftServer server) {
        this.server = server;
        loadFromJson();
    }

    public static WorldAttractorManager get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(
                () -> new WorldAttractorManager(server),
                (tag, registryAccess) -> new WorldAttractorManager(server),
                null
            ),
            "curvedverse_attractors"
        );
    }

    public void generate(IFSStrategy strategy) {
        if (!attractors.isEmpty()) {
            return;
        }

        long seed = server.getWorldData().worldGenOptions().seed();
        LOGGER.info("Generating global attractors with seed {}", seed);

        List<ComplexNumber> points = strategy.getAttractors();
        if (points == null || points.isEmpty()) {
            if (strategy != null) {
                 strategy.bake();
                 points = strategy.getAttractors();
            }
            if (points == null || points.isEmpty()) {
                 LOGGER.error("Strategy has no attractors points to generate from!");
                 return;
            }
        }

        Random random = new Random(seed);
        List<ComplexNumber> shuffledPoints = new ArrayList<>(points);
        Collections.shuffle(shuffledPoints, random);

        String[] prefixes = {"Alpha", "Beta", "Gamma", "Delta", "Epsilon", "Zeta", "Eta", "Theta", "Iota", "Kappa", "Lambda", "Mu", "Nu", "Xi", "Omicron", "Pi", "Rho", "Sigma", "Tau", "Upsilon", "Phi", "Chi", "Psi", "Omega"};
        String[] roots = {"Aether", "Nether", "Void", "Flux", "Core", "Nexus", "Rift", "Spark", "Gloom", "Echo", "Mist", "Storm", "Frost", "Pyre", "Verdant", "Arid"};
        String[] suffixes = {"Station", "Outpost", "Node", "Singularity", "Point", "Zone", "Region", "Sector", "Expanse"};

        for (int i = 0; i < shuffledPoints.size(); i++) {
            ComplexNumber pos = shuffledPoints.get(i);

            double temp, hum;

            if (i == 0) {
                temp = -0.8 - (random.nextDouble() * 0.2);
                hum = random.nextGaussian() * 0.3;
            } else if (i == 1) {
                temp = 0.8 + (random.nextDouble() * 0.2);
                hum = random.nextGaussian() * 0.3;
            } else if (i == 2) {
                temp = random.nextGaussian() * 0.3;
                hum = 0.8 + (random.nextDouble() * 0.2);
            } else if (i == 3) {
                temp = random.nextGaussian() * 0.3;
                hum = -0.8 - (random.nextDouble() * 0.2);
            } else {
                temp = random.nextGaussian() * 0.4;
                hum = random.nextGaussian() * 0.4;
            }

            temp = Mth.clamp(temp, -1.0, 1.0);
            hum = Mth.clamp(hum, -1.0, 1.0);

            double host = random.nextDouble();
            double poll = random.nextDouble();
            int cha = random.nextInt(100) + 1;

            String name = prefixes[random.nextInt(prefixes.length)] + "-" + roots[random.nextInt(roots.length)];
            if (random.nextBoolean()) name += " " + suffixes[random.nextInt(suffixes.length)];

            attractors.add(new Attractor(pos, name, temp, hum, host, poll, cha));
        }

        setDirty();
        saveToJson();
    }

    public InterpolatedData interpolate(ComplexNumber point) {
        if (attractors.isEmpty()) {
            return new InterpolatedData(0, 0, 0, 0, Collections.emptyList());
        }

        double totalWeight = 0;
        double wTemp = 0;
        double wHum = 0;
        double wHost = 0;
        double wPoll = 0;

        List<Attractor> sortedByDist = new ArrayList<>(attractors);
        sortedByDist.sort(Comparator.comparingDouble(a -> distanceSquared(point, a.getPosition())));

        List<Integer> characters = sortedByDist.stream()
            .limit(3)
            .map(Attractor::getCharacter)
            .collect(Collectors.toList());

        for (Attractor a : attractors) {
            double d2 = distanceSquared(point, a.getPosition());
            if (d2 < 0.0001) d2 = 0.0001;

            double weight = 1.0 / d2;

            wTemp += a.getTemperature() * weight;
            wHum += a.getHumidity() * weight;
            wHost += a.getHostility() * weight;
            wPoll += a.getPollution() * weight;
            totalWeight += weight;
        }

        return new InterpolatedData(
            wTemp / totalWeight,
            wHum / totalWeight,
            wHost / totalWeight,
            wPoll / totalWeight,
            characters
        );
    }

    private double distanceSquared(ComplexNumber c1, ComplexNumber c2) {
        double dr = c1.getReal() - c2.getReal();
        double di = c1.getImaginary() - c2.getImaginary();
        return dr * dr + di * di;
    }

    private Path getJsonPath() {
        return server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
            .resolve("data")
            .resolve("curvedverse_attractors.json");
    }

    private void loadFromJson() {
        File file = getJsonPath().toFile();
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                Type type = new TypeToken<List<Attractor>>(){}.getType();
                List<Attractor> loaded = GSON.fromJson(reader, type);
                if (loaded != null) {
                    attractors.clear();
                    attractors.addAll(loaded);
                }
            } catch (IOException e) {
                LOGGER.error("Failed to load global attractors", e);
            }
        }
    }

    private void saveToJson() {
        File file = getJsonPath().toFile();
        file.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(attractors, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to save global attractors", e);
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        saveToJson();
        return tag;
    }
}






