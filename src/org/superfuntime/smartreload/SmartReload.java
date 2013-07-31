/*
 * Copyright (c) 2013 Super Fun Time
 * All Rights Reserved
 *
 * This product is protected by copyright and distributed under
 * licenses restricting copying, distribution, and decompilation.
 */

package org.superfuntime.smartreload;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * User: Bogeyman
 * Date: 31.07.13
 * Time: 11:58
 */
public class SmartReload extends JavaPlugin {
    public static volatile boolean reload = false;
    public Thread thread = new Thread() {
        @SuppressWarnings("unchecked")
        @Override
        public void run() {
            outer:
            for (; ; ) {
                WatchKey key;
                try {
                    key = watcher.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == OVERFLOW)
                        continue;
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path name = ev.context();
                    if (!name.toString().endsWith(".jar"))
                        continue;
                    String type = "";
                    if (event.kind() == ENTRY_CREATE)
                        type = "CREATED";
                    else if (event.kind() == ENTRY_DELETE)
                        type = "DELETED";
                    else if (event.kind() == ENTRY_MODIFY)
                        type = "MODIFIED";
                    System.out.println(String.format("[SmartReload] Detected file change: %s(%s). Reloading.",
                                                     name.toString(),
                                                     type));
                    SmartReload.reload = true;
                    break outer;
                }
            }
        }
    };
    WatchService watcher;
    Path pluginsFolder = Paths.get(new File("plugins/").toURI());

    @Override
    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this);
    }

    @Override
    public void onEnable() {
        try {
            watcher = FileSystems.getDefault().newWatchService();
            pluginsFolder.register(watcher, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
            getLogger().info("Watching plugins folder for changes.");
        } catch (IOException e) {
            e.printStackTrace();
        }
        thread.start();
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                if (SmartReload.reload) {
                    Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(), "reload");
                    SmartReload.reload = false;
                }
            }
        }, 0, 20 * 3);
    }
}
