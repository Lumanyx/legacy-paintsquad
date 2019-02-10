package de.xenyria.splatoon.lobby;

import de.xenyria.servercore.spigot.XenyriaSpigotServerCore;
import de.xenyria.servercore.spigot.display.image.ImageManager;
import de.xenyria.servercore.spigot.util.WorldUtil;
import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.craftbukkit.v1_13_R2.generator.CraftChunkData;
import org.bukkit.generator.ChunkGenerator;

import java.io.File;
import java.util.Random;

public class PlazaLobbyManager {

    private static World lobbyWorld;
    public static World getLobbyWorld() { return lobbyWorld; }

    private SplatoonLobby lobby;
    public SplatoonLobby getLobby() { return lobby; }

    public void addPlayerToLobby(SplatoonHumanPlayer player) {

        if(player.getMatch() != null) {

            player.leaveMatch();

        }
        player.joinMatch(lobby);

    }

    public PlazaLobbyManager() {

        try {

            lobbyWorld = Bukkit.createWorld(new WorldCreator("sp_lobby").generator(new ChunkGenerator() {
                @Override
                public ChunkData generateChunkData(World world, Random random, int x, int z, BiomeGrid biome) {
                    return new CraftChunkData(world);
                }
            }));
            ImageManager.loadImage("unavailable", new File(XenyriaSplatoon.getPlugin().getDataFolder() + File.separator + "images" + File.separator + "unavailable.png"), SplatoonLobby.SCREEN_DIMENSIONS);
            ImageManager.loadImage("testfire", new File(XenyriaSplatoon.getPlugin().getDataFolder() + File.separator + "images" + File.separator + "testfire.png"), SplatoonLobby.SCREEN_DIMENSIONS);
            ImageManager.loadImage("testfire_alt", new File(XenyriaSplatoon.getPlugin().getDataFolder() + File.separator + "images" + File.separator + "testfire_alt.png"), SplatoonLobby.SCREEN_DIMENSIONS);

            lobby = new SplatoonLobby(lobbyWorld);
            XenyriaSpigotServerCore.getXenyriaLogger().log("Lobby wurde initialisiert!");

        } catch (Exception e) {

            e.printStackTrace();

        }

    }

}
