package org.i212.curvedverse.test;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.DimensionTransition;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.i212.curvedverse.Curvedverse;
import org.i212.curvedverse.dimension.CurvedverseDimensionRegistry;
import org.i212.curvedverse.dimension.DimensionMetadata;

import java.util.Collections;
import java.util.Random;

@EventBusSubscriber(modid = Curvedverse.MODID)
public class CurvedverseCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("cv")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("create")
                .then(Commands.argument("key", StringArgumentType.string())
                        .executes(CurvedverseCommands::createDimension)))
            .then(Commands.literal("tp")
                .then(Commands.argument("key", StringArgumentType.string())
                        .executes(CurvedverseCommands::teleportToDimension)))
            .then(Commands.literal("list")
                .executes(CurvedverseCommands::listDimensions))
            .then(Commands.literal("unload")
                .then(Commands.argument("key", StringArgumentType.string())
                        .executes(CurvedverseCommands::unloadDimension)))
        );
    }

    private static int createDimension(CommandContext<CommandSourceStack> context) {
        String key = StringArgumentType.getString(context, "key");

        CommandSourceStack source = context.getSource();
        CurvedverseDimensionRegistry registry = CurvedverseDimensionRegistry.getInstance(source.getServer());
        
        // Check if exists
        if (registry.getDimensionMetadata(key) != null) {
            source.sendFailure(Component.literal("Dimension already exists with key: " + key));
            return 0;
        }

        // Randomize parameters for test
        Random random = new Random();
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Curvedverse.MODID, "dim_" + System.currentTimeMillis());
        double temp = random.nextDouble();
        double hum = random.nextDouble();
        double host = random.nextDouble();
        ResourceLocation biome = ResourceLocation.withDefaultNamespace("plains"); // Placeholder
        
        try {
            ServerLevel level = registry.createAndRegister(key, id, temp, hum, host, biome, Collections.emptyList());
            if (level != null) {
                source.sendSuccess(() -> Component.literal("Created dimension " + id + " with key: " + key), true);
                return 1;
            } else {
                source.sendFailure(Component.literal("Failed to create dimension (returned null)"));
            }
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error creating dimension: " + e.getMessage()));
            e.printStackTrace();
        }
        return 0;
    }

    private static int teleportToDimension(CommandContext<CommandSourceStack> context) {
        String key = StringArgumentType.getString(context, "key");

        CommandSourceStack source = context.getSource();
        CurvedverseDimensionRegistry registry = CurvedverseDimensionRegistry.getInstance(source.getServer());
        
        ServerLevel level = registry.loadDimension(key);
        if (level == null) {
            source.sendFailure(Component.literal("Dimension not found with key: " + key));
            return 0;
        }

        Entity entity = source.getEntity();
        if (entity != null) {
            DimensionTransition transition = new DimensionTransition(
                level,
                entity.position(),
                entity.getDeltaMovement(),
                entity.getYRot(),
                entity.getXRot(),
                DimensionTransition.DO_NOTHING
            );
            entity.changeDimension(transition);
            source.sendSuccess(() -> Component.literal("Teleported to dimension with key: " + key), true);
            return 1;
        } else {
            source.sendFailure(Component.literal("Command source is not an entity"));
        }
        return 0;
    }

    private static int listDimensions(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        CurvedverseDimensionRegistry registry = CurvedverseDimensionRegistry.getInstance(source.getServer());
        
        java.util.Collection<DimensionMetadata> all = registry.getAllDimensions();
        if (all.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No dimensions registered."), false);
            return 1;
        }
        
        source.sendSuccess(() -> Component.literal("Registered dimensions (" + all.size() + "):"), false);
        for (DimensionMetadata meta : all) {
            source.sendSuccess(() -> Component.literal("- " + meta.getDimensionId() + " at " + meta.getCoordinate()), false);
        }
        return all.size();
    }

    private static int unloadDimension(CommandContext<CommandSourceStack> context) {
        String key = StringArgumentType.getString(context, "key");

        CommandSourceStack source = context.getSource();
        CurvedverseDimensionRegistry registry = CurvedverseDimensionRegistry.getInstance(source.getServer());
        
        registry.unloadDimension(key);
        source.sendSuccess(() -> Component.literal("Unloaded dimension with key: " + key), true);
        return 1;
    }
}
