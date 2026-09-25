package com.lightningblock.event;

import com.lightningblock.LightningBlockMod;
import com.lightningblock.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

/**
 * When a ruined portal (minecraft:ruined_portal) generates, there is a 1% chance
 * that one obsidian block in it is replaced with Lightning Ore (max one per portal).
 * The modified variant is conceptually named "lightningblock:ruined_portal_with_lightning".
 *
 * To avoid choking world generation, chunk-load events only enqueue positions;
 * a ServerTickEvent handler drains at most ONE chunk per tick.
 */
@Mod.EventBusSubscriber(modid = LightningBlockMod.MOD_ID)
public class RuinedPortalLightningEvent {

    private static final float REPLACE_CHANCE = 0.01f;
    private static final ResourceKey<Structure> RUINED_PORTAL_KEY =
            ResourceKey.create(Registries.STRUCTURE, new ResourceLocation("minecraft", "ruined_portal"));

    // Pending chunk positions to inspect, drained one per server tick
    private static final Queue<ChunkPos> PENDING = new LinkedList<>();
    // Track processed structure origins so each portal is checked at most once
    private static final Set<Long> PROCESSED_PORTALS = Collections.synchronizedSet(new HashSet<>());

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel)) return;
        ChunkAccess chunk = event.getChunk();
        synchronized (PENDING) {
            PENDING.add(chunk.getPos());
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        ServerLevel level = event.getServer().overworld();
        if (level == null) return;

        ChunkPos cp;
        synchronized (PENDING) {
            cp = PENDING.poll();
        }
        if (cp != null) {
            tryReplaceObsidian(level, cp);
        }
    }

    private static void tryReplaceObsidian(ServerLevel level, ChunkPos cp) {
        Registry<Structure> structureRegistry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        Optional<Structure> ruinedPortalOpt = structureRegistry.getOptional(RUINED_PORTAL_KEY);
        if (ruinedPortalOpt.isEmpty()) return;

        int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, cp.x * 16 + 8, cp.z * 16 + 8);
        BlockPos center = new BlockPos(cp.x * 16 + 8, surfaceY, cp.z * 16 + 8);

        StructureStart start;
        BoundingBox bb;
        try {
            start = level.structureManager().getStructureAt(center, ruinedPortalOpt.get());
            if (start == null || start.getPieces().isEmpty()) return;
            bb = start.getBoundingBox();
            if (bb == null) return;
        } catch (Exception e) {
            return; // structure not ready yet; another chunk in this portal will retry
        }
        long key = BlockPos.asLong(bb.minX(), bb.minY(), bb.minZ());
        if (!PROCESSED_PORTALS.add(key)) return;

        if (level.random.nextFloat() >= REPLACE_CHANCE) return;

        List<BlockPos> obsidianPositions = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(
                bb.minX(), bb.minY(), bb.minZ(),
                bb.maxX(), bb.maxY(), bb.maxZ())) {
            if (level.getBlockState(pos).is(Blocks.OBSIDIAN)) {
                obsidianPositions.add(pos.immutable());
            }
        }

        if (!obsidianPositions.isEmpty()) {
            BlockPos target = obsidianPositions.get(level.random.nextInt(obsidianPositions.size()));
            level.setBlock(target, ModBlocks.LIGHTNING_ORE.get().defaultBlockState(), 3);
        }
    }
}
