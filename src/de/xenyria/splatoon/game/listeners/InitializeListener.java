package de.xenyria.splatoon.game.listeners;

import de.xenyria.servercore.spigot.events.ServerInitializeEvent;
import de.xenyria.servercore.spigot.listener.SpigotListenerUtil;
import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.lobby.PlazaLobbyManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class InitializeListener implements Listener {

    public InitializeListener() {

        SpigotListenerUtil.registerListener(this, XenyriaSplatoon.getPlugin());

    }

    @EventHandler
    public void init(ServerInitializeEvent event) {

        XenyriaSplatoon.initLobbyManager();

    }

}
