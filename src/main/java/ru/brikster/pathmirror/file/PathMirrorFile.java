package ru.brikster.pathmirror.file;

import java.io.File;

public class PathMirrorFile extends File {

    public PathMirrorFile(String pathname) {
        super(pathname);
    }

    public PathMirrorFile(String parent, String child) {
        super(parent, child);
    }

    public PathMirrorFile(File parent, String child) {
        super(parent, child);
    }

}
