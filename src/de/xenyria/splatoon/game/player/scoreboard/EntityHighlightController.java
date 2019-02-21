package de.xenyria.splatoon.game.player.scoreboard;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import de.xenyria.core.math.BitUtil;
import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.game.listeners.ProtocolListener;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.player.TeamEntity;
import net.minecraft.server.v1_13_R2.Entity;
import net.minecraft.server.v1_13_R2.PacketPlayOutEntityMetadata;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class EntityHighlightController {

    private SplatoonHumanPlayer player;
    public EntityHighlightController(SplatoonHumanPlayer player) {

        this.player = player;

    }

    private HashMap<ChatColor, Team> highlightTeams = new HashMap<>();
    private ArrayList<TeamEntry> entries = new ArrayList<TeamEntry>();

    public TeamEntry getEntry(int id) {

        for(TeamEntry entry : entries) {

            if(entry.entity.getId() == id) {

                return entry;

            }

        }
        return null;

    }

    public boolean isHighlighted(int id) {

        for(TeamEntry entry : entries) {

            if(entry.entity.getId() == id) {

                return true;

            }

        }
        return false;

    }

    public static class TeamEntry {

        // Spielername oder EntityUUID
        private String name;

        private ChatColor chatColor;

        public Entity entity;

        public int ticks = -1;
        public boolean isTemporary() { return ticks != -1; }

        public TeamEntry(String name, ChatColor chatColor, Entity entity, int ticks) {

            this.name = name;
            this.entity = entity;
            this.chatColor = chatColor;
            this.ticks = ticks;

        }

        public TeamEntry(String name, ChatColor chatColor, Entity entity) {

            this.name = name;
            this.entity = entity;
            this.chatColor = chatColor;

        }

    }

    public void removeEntry(TeamEntry entry) {

        entries.remove(entry);
        Team team = highlightTeams.get(entry.chatColor);
        team.removeEntry(entry.name);
        if(team.getEntries().isEmpty()) {

            team.unregister();
            highlightTeams.remove(entry.chatColor);

        }

        ((CraftPlayer)player.getPlayer()).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityMetadata(
                entry.entity.getId(), entry.entity.getDataWatcher(), entry.entity.onGround
        ));

    }

    public void addEntry(TeamEntry entry) {

        entries.add(entry);

        if(highlightTeams.containsKey(entry.chatColor)) {

            highlightTeams.get(entry.chatColor).addEntry(entry.name);

        } else {

            Scoreboard scoreboard = player.getPlayer().getScoreboard();
            Team team = scoreboard.registerNewTeam("hE-" + entry.chatColor.name());
            team.setColor(entry.chatColor);
            team.addEntry(entry.name);
            highlightTeams.put(entry.chatColor, team);

        }

        /*((CraftPlayer)player.getPlayer()).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityMetadata(
                entry.entity.getId(), entry.entity.getDataWatcher(), entry.entity.onGround
        ));*/
        sendPacket(entry, true);

    }

    public void sendPacket(TeamEntry entry, boolean val) {

        PacketContainer container = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA, new PacketPlayOutEntityMetadata(entry.entity.getId(), entry.entity.getDataWatcher(), entry.entity.onGround));
        WrappedDataWatcher watcher = WrappedDataWatcher.getEntityWatcher(entry.entity.getBukkitEntity()).deepClone();
        WrappedDataWatcher.Serializer serializer = WrappedDataWatcher.Registry.get(Byte.class);

        byte origVal = (byte) watcher.getObject(0);
        watcher.setObject(0, serializer, BitUtil.setBit(origVal, 6, val));
        container.getWatchableCollectionModifier().write(0, watcher.getWatchableObjects());

        try {

            ProtocolLibrary.getProtocolManager().sendServerPacket(player.getPlayer(), container, false);

        } catch (Exception e) {

            e.printStackTrace();

        }

    }

    public void tick() {

        ArrayList<ChatColor> iteratedTeams = new ArrayList<>();
        Iterator<TeamEntry> iterator = entries.iterator();
        while (iterator.hasNext()) {

            TeamEntry entry = iterator.next();
            if(entry.isTemporary()) {

                entry.ticks--;
                if(entry.ticks < 1) {

                    iterator.remove();
                    Team team = highlightTeams.get(entry.chatColor);
                    team.removeEntry(entry.name);

                    Bukkit.getScheduler().runTaskLater(XenyriaSplatoon.getPlugin(), () -> {

                        sendPacket(entry, false);

                    }, 2l);
                    continue;

                }

            }
            if(!iteratedTeams.contains(entry.chatColor)) { iteratedTeams.add(entry.chatColor); }

        }

    }

}
