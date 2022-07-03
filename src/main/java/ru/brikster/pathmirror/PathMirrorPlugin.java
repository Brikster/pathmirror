package ru.brikster.pathmirror;

import org.bukkit.plugin.java.JavaPlugin;

public class PathMirrorPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        try {
            PathMirrorAgent.premain(null, null);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

}
