/*
 * SmartReload
 * Copyright (C) 2013 bogeymanEST
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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
