package com.wildrose.minigameswr;

import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;

import java.util.Random;

/**
 * Generates completely empty (void) chunks.
 * Registered via {@link MiniGamesWR#getDefaultWorldGenerator} only for the
 * world whose name matches {@code void-generator.world-name} in config.yml.
 */
public class VoidChunkGenerator extends ChunkGenerator {

    @Override
    public ChunkData generateChunkData(World world, Random random, int chunkX, int chunkZ, BiomeGrid biome) {
        // Return an empty ChunkData — no blocks placed, resulting in a void chunk.
        return createChunkData(world);
    }
}
