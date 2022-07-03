package ru.brikster.pathmirror.factory;

import ru.brikster.pathmirror.PathMirrorAgent;
import ru.brikster.pathmirror.config.PathMirrorConfig.PathMirror;

import java.io.File;

public class PathMirrorFileFactory {

    public static File createFile(String pathname) {
        return mirroredFile(new File(pathname));
    }

    public static File createFile(String parent, String child) {
        return mirroredFile(new File(parent, child));
    }

    public static File createFile(File parent, String child) {
        return mirroredFile(new File(parent, child));
    }

    public static Class<?> forName(String name, boolean initializer, ClassLoader loader) throws ClassNotFoundException {
        PathMirrorAgent.pluginClassLoaders.add(loader);
        return Class.forName(name, initializer, loader);
    }

    private static File mirroredFile(File file) {
        for (PathMirror mirror : PathMirrorAgent.getConfig().getMirrors()) {
            String fixedPath = file.getPath().replace("\\", "/");

            if (fixedPath.equals(mirror.getFrom())) {
                String newFileName = mirror.getTo().replace("{name}", file.getName());
                System.out.println("[PathMirror] Mirrored \"" + fixedPath + "\" to \"pathmirror/" + newFileName + "\"");
                file = new File("pathmirror", newFileName);
            }
        }

        return file;
    }

}
