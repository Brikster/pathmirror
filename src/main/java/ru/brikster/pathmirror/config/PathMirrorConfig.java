package ru.brikster.pathmirror.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class PathMirrorConfig {

    private final List<PathMirror> mirrors;

    public PathMirrorConfig() {
        mirrors = new ArrayList<>();
    }

    public List<PathMirror> getMirrors() {
        return mirrors;
    }

    public static class PathMirror {

        @JsonProperty("package")
        private final String pkg;
        private final String from;
        private final String to;

        public PathMirror() {
            pkg = null;
            from = null;
            to = null;
        }

        public String getPackage() {
            return pkg;
        }

        public String getFrom() {
            return from;
        }

        public String getTo() {
            return to;
        }

    }

}
