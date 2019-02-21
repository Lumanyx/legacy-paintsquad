package de.xenyria.splatoon.arena.builder;

import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.arena.ArenaProvider;
import net.minecraft.server.v1_13_R2.ChunkCoordIntPair;
import net.minecraft.server.v1_13_R2.ChunkGenerator;
import net.minecraft.server.v1_13_R2.PaperAsyncChunkProvider;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

public class ArenaBuilder {

    private LinkedBlockingQueue<ArenaProvider.ArenaGenerationTask> tasks = new LinkedBlockingQueue<>();
    private static CopyOnWriteArrayList<ChunkCoordIntPair> forcedLoadedChunks = new CopyOnWriteArrayList<>();

    public static boolean keepLoaded(ChunkCoordIntPair chunkCoordIntPair) {

        return forcedLoadedChunks.contains(chunkCoordIntPair);

    }
    public static Chunk forceChunkLoaded(ChunkCoordIntPair pair) {

        forcedLoadedChunks.add(pair);
        World world = XenyriaSplatoon.getArenaProvider().getArenaWorld();
        net.minecraft.server.v1_13_R2.World world1 = ((CraftWorld)world).getHandle();

        PaperAsyncChunkProvider provider = (PaperAsyncChunkProvider) world1.getChunkProvider();

        net.minecraft.server.v1_13_R2.Chunk chunk = provider.getChunkAt(pair.x, pair.z, true, true);
        Chunk chunk1 = chunk.bukkitChunk;

        chunk1.setForceLoaded(true);
        return chunk1;

    }
    public static void unloadChunk(ChunkCoordIntPair pair) {

        forcedLoadedChunks.remove(pair);
        XenyriaSplatoon.getArenaProvider().getArenaWorld().unloadChunk(pair.x, pair.z, false);
        World world = XenyriaSplatoon.getArenaProvider().getArenaWorld();
        Chunk chunk = world.getChunkAt(pair.x, pair.z);
        chunk.setForceLoaded(false);
        chunk.unload(false);

    }

    public void queueTask(ArenaProvider.ArenaGenerationTask task) {

        tasks.add(task);

    }

    public ArenaBuilder() {

        XenyriaSplatoon.getXenyriaLogger().log("ArenaBuilder bereit!");
        worker.setName("ArenaBuilder Worker");
        worker.start();

    }

    private Thread worker = new Thread(() -> {

        while (true) {

            try { Thread.sleep(1); } catch (Exception e) { e.printStackTrace(); }
            if(!tasks.isEmpty()) {

                ArenaProvider.ArenaGenerationTask task = tasks.peek();
                task.work();
                if (task.isDone()) {

                    XenyriaSplatoon.getXenyriaLogger().log("ArenaBuilder hat eine Arena eingefügt. Dauerte " + task.elapsedTime() + " ms");
                    tasks.poll();

                }

            }

        }

    });

}
