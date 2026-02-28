package com.wildrose.minigameswr.generator;

import org.bukkit.generator.ChunkGenerator;

/**
 * Void chunk generator – every chunk is generated as pure air.
 *
 * <p>All world-generation passes are disabled by returning {@code false}
 * from the {@code shouldGenerate*} hooks introduced in Paper/Bukkit 1.18.
 * The result is a world where no terrain, decorations, structures, or mobs
 * spawn naturally, suitable for minigame arenas.
 *
 * <p>Activate by pointing a world at this plugin in {@code bukkit.yml}:
 * <pre>
 * worlds:
 *   world:
 *     generator: MiniGamesWR
 * </pre>
 *
 * <p>See also {@link com.wildrose.minigameswr.listener.WorldLoadListener}
 * for the optional spawn-safety platform that prevents players from falling
 * into the void on first join.
 */
public class VoidChunkGenerator extends ChunkGenerator {

    @Override
    public boolean shouldGenerateNoise() {
        return false;
    }

    @Override
    public boolean shouldGenerateSurface() {
        return false;
    }

    @Override
    public boolean shouldGenerateBedrock() {
        return false;
    }

    @Override
    public boolean shouldGenerateCaves() {
        return false;
    }

    @Override
    public boolean shouldGenerateDecorations() {
        return false;
    }

    @Override
    public boolean shouldGenerateMobs() {
        return false;
    }

    @Override
    public boolean shouldGenerateStructures() {
        return false;
    }
}
