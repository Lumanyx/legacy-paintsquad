package de.xenyria.splatoon.shootingrange;

import com.mojang.datafixers.schemas.Schema;
import de.xenyria.core.chat.Chat;
import de.xenyria.schematics.internal.XenyriaSchematic;
import de.xenyria.schematics.internal.placeholder.SchematicPlaceholder;
import de.xenyria.schematics.internal.placeholder.StoredPlaceholder;
import de.xenyria.splatoon.SplatoonServer;
import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.arena.ArenaProvider;
import de.xenyria.splatoon.arena.boundary.ArenaBoundaryConfiguration;
import de.xenyria.splatoon.arena.placeholder.ArenaPlaceholder;
import de.xenyria.splatoon.arena.schematic.SchematicProvider;
import de.xenyria.splatoon.game.color.Color;
import de.xenyria.splatoon.game.map.Map;
import de.xenyria.splatoon.game.match.PlaceholderReader;
import de.xenyria.splatoon.game.objects.Dummy;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.game.team.Team;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.craftbukkit.v1_13_R2.generator.CraftChunkData;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;

public class ShootingRangeManager {

    private XenyriaSchematic schematic;
    private World shootingRangeWorld;
    private ArenaBoundaryConfiguration configuration;

    public ShootingRangeManager() throws Exception {

        shootingRangeWorld = Bukkit.createWorld(new WorldCreator("sp_sr").generator(new ChunkGenerator() {
            @Override
            public ChunkData generateChunkData(World world, Random random, int x, int z, BiomeGrid biome) {

                CraftChunkData data = new CraftChunkData(world);
                for(int i = 0; i < 16; i++) {

                    for(int j = 0; j < 16; j++) {

                        biome.setBiome(i, j, Biome.JUNGLE);

                    }

                }
                return data;

            }
        }));
        SplatoonServer.applyGameRules(shootingRangeWorld);
        configuration = ArenaBoundaryConfiguration.fromFile(new File(XenyriaSplatoon.getPlugin().getDataFolder() + File.separator + "arena" + File.separator + "shootingrange.sbounds"));
        schematic = SchematicProvider.loadSchematic("shootingrange.xsc");
        Vector currentOffset = new Vector(0,64,0);
        for(int i = 0; i < Bukkit.getMaxPlayers(); i++) {

            createCluster(currentOffset);
            currentOffset = currentOffset.clone().add(new Vector(300, 0, 0));

        }

    }

    private ArrayList<ShootingRange> shootingRanges = new ArrayList<>();

    public void joinShootingRange(SplatoonHumanPlayer player, boolean fromShop) {

        ArrayList<ShootingRange> freeShootingRanges = new ArrayList<>();
        for(ShootingRange shootingRange : shootingRanges) {

            if(shootingRange.getAllPlayers().isEmpty()) {

                freeShootingRanges.add(shootingRange);

            }

        }

        if(!freeShootingRanges.isEmpty()) {

            if(freeShootingRanges.size() == 1) {

                ShootingRange range = freeShootingRanges.get(0);
                player.leaveMatch();
                range.fromWeaponShop = fromShop;
                player.joinMatch(range);

            } else {

                ShootingRange range = freeShootingRanges.get(new Random().nextInt(freeShootingRanges.size() - 1));
                player.leaveMatch();
                range.fromWeaponShop = fromShop;
                player.joinMatch(range);

            }
            player.getPlayer().sendMessage(Chat.SYSTEM_PREFIX + "Willkommen in der Waffentestumgebung!");
            player.getPlayer().sendMessage(" §8- §eNutze das Item auf dem 6. Slot zum zurücksetzen der Map.");
            player.getPlayer().sendMessage(" §8- §eNutze das Item auf dem 7. Slot zum Wählen einer anderen Waffe.");
            player.getPlayer().sendMessage(" §8- §eNutze das Item auf dem 8. Slot zum zurückkehren zur Lobby.");

        } else {

            player.getPlayer().sendMessage(Chat.SYSTEM_PREFIX + "Es ist derzeit keine Testumgebung frei.");

        }

    }

    public void createCluster(Vector offset) {

        long begin = System.nanoTime();

        Location location1 = new Location(shootingRangeWorld, 0,0,0);
        location1 = location1.add(offset);
        for(StoredPlaceholder placeholder : schematic.getStoredPlaceholders()) {

            if(placeholder.type == SchematicPlaceholder.Splatoon.SPAWN_POINT) {

                location1 = location1.add(placeholder.x + .5, placeholder.y, placeholder.z + .5);
                location1.setYaw(Float.parseFloat(placeholder.getData().get("yaw")));

            }

        }

        ShootingRange range = new ShootingRange(shootingRangeWorld, location1);
        Color primaryColor = Color.getRandomColors(1).get(0);
        ArrayList<ArenaPlaceholder> placeholders = new ArrayList<>();
        placeholders.add(new ArenaPlaceholder() {

            public Material getTriggeringMaterial() { return Material.ORANGE_TERRACOTTA; }
            public Material getReplacement() { return primaryColor.getClay(); }
            public boolean addMetadata() { return false; }
            public Metadata getMetadata() { return null; }

        });
        placeholders.add(new ArenaPlaceholder() {

            public Material getTriggeringMaterial() { return Material.ORANGE_CARPET; }
            public Material getReplacement() { return primaryColor.getCarpet(); }
            public boolean addMetadata() { return false; }
            public Metadata getMetadata() { return null; }

        });

        Team team = new Team(0, primaryColor);
        range.registerTeam(team);

        ArrayList<Map.TeamSpawn> spawns = PlaceholderReader.getSpawns(offset, schematic);
        range.getMap().getSpawns().add(spawns.get(0));

        ArenaProvider.ArenaGenerationTask task = new ArenaProvider.ArenaGenerationTask(shootingRangeWorld, "shootingrange.xsc", offset.clone(), range,
                XenyriaSplatoon.getArenaRegistry().getArenaData(-1), placeholders);
        task.work();
        range.apply(configuration, offset);
        shootingRanges.add(range);

        for(StoredPlaceholder placeholder : schematic.getStoredPlaceholders()) {

            if(placeholder.type == SchematicPlaceholder.Splatoon.DUMMY) {

                float yaw = Float.parseFloat(placeholder.getData().get("yaw"));
                int hp = Integer.parseInt(placeholder.getData().get("health"));

                Location location = new Location(shootingRangeWorld,
                        offset.getX() + (placeholder.x+.5),
                        offset.getY() + (placeholder.y),
                        offset.getZ() + (placeholder.z+.5),
                        yaw, 0);

                Dummy dummy = new Dummy(range, location, hp);
                range.addGameObject(dummy);

            }

        }

        long end = System.nanoTime();
        XenyriaSplatoon.getXenyriaLogger().log("Waffentestumgebung generiert! Dauerte §e" + ((end-begin)/1000000f) + " ms");

    }

}
