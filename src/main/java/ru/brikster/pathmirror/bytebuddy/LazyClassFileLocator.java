package ru.brikster.pathmirror.bytebuddy;

import net.bytebuddy.agent.utility.nullability.MaybeNull;
import net.bytebuddy.dynamic.ClassFileLocator;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class LazyClassFileLocator implements ClassFileLocator {

    private final Supplier<List<ClassLoader>> classLoader;

    private LazyClassFileLocator(Supplier<List<ClassLoader>> classLoader) {
        this.classLoader = classLoader;
    }

    public static ClassFileLocator of(@MaybeNull Supplier<List<ClassLoader>> classLoaders) {
        return new LazyClassFileLocator(classLoaders);
    }

    @Override
    public Resolution locate(String name) throws IOException {
        return new ClassFileLocator.Compound(
                classLoader.get()
                        .stream()
                        .map(ForClassLoader::of)
                        .collect(Collectors.toList())
        ).locate(name);
    }

    public void close() {}

}