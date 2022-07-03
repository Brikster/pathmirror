package ru.brikster.pathmirror;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.AgentBuilder.Default;
import net.bytebuddy.agent.builder.AgentBuilder.Listener.StreamWriting;
import net.bytebuddy.agent.builder.AgentBuilder.Listener.WithErrorsOnly;
import net.bytebuddy.agent.builder.AgentBuilder.LocationStrategy;
import net.bytebuddy.agent.builder.AgentBuilder.LocationStrategy.Compound;
import net.bytebuddy.agent.builder.AgentBuilder.LocationStrategy.Simple;
import net.bytebuddy.agent.builder.AgentBuilder.RedefinitionStrategy;
import net.bytebuddy.agent.builder.AgentBuilder.TypeStrategy;
import net.bytebuddy.asm.MemberSubstitution;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.ClassFileLocator.ForClassLoader;
import net.bytebuddy.matcher.ElementMatcher.Junction;
import net.bytebuddy.matcher.ElementMatchers;
import org.bukkit.plugin.java.JavaPluginLoader;
import ru.brikster.pathmirror.bytebuddy.LazyClassFileLocator;
import ru.brikster.pathmirror.config.PathMirrorConfig;
import ru.brikster.pathmirror.config.PathMirrorConfig.PathMirror;
import ru.brikster.pathmirror.factory.PathMirrorFileFactory;
import ru.brikster.pathmirror.util.ConfigUtil;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class PathMirrorAgent {

    public static List<ClassLoader> pluginClassLoaders = new ArrayList<>();

    private static PathMirrorConfig config;

    public static void premain(String args, Instrumentation instrumentation) throws Throwable {
        ByteBuddyAgent.install();
        System.out.println("[PathMirror] PathMirror initialized.");
        loadConfig();
        installTransformers();
    }

    public static PathMirrorConfig getConfig() {
        return config;
    }

    private static void loadConfig() throws IOException {
        ConfigUtil.saveDefaultConfig();

        Path directoryPath = Path.of("pathmirror");
        if (Files.notExists(directoryPath)) {
            Files.createDirectories(directoryPath);
        }

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        PathMirrorAgent.config = mapper.readValue(new FileReader("pathmirror.yml"), PathMirrorConfig.class);
    }

    private static void installTransformers() throws NoSuchMethodException {
        Method stringMethod = PathMirrorFileFactory.class.getMethod("createFile", String.class);
        Method stringStringMethod = PathMirrorFileFactory.class.getMethod("createFile", String.class, String.class);
        Method fileStringMethod = PathMirrorFileFactory.class.getMethod("createFile", File.class, String.class);
        Method forNameMethod = PathMirrorFileFactory.class.getMethod("forName", String.class, boolean.class, ClassLoader.class);

        new Default()
                .with(new Compound(
                        new Simple(ForClassLoader.ofPlatformLoader()),
                        new Simple(ForClassLoader.ofBootLoader()),
                        new Simple(ForClassLoader.ofSystemLoader()),
                        new Simple(LazyClassFileLocator.of(() ->
                                List.of(JavaPluginLoader.class.getClassLoader())))))
                .type(named("org.bukkit.plugin.java.PluginClassLoader"))
                .transform((builder, typeDescription, classLoader, module) -> builder.visit(
                        MemberSubstitution.relaxed()
                                .method(ElementMatchers.isDeclaredBy(ElementMatchers.named("java.lang.Class"))
                                        .and(named("forName")))
                                .replaceWith(forNameMethod)
                                .on(ElementMatchers.isConstructor())))
                .with(RedefinitionStrategy.RETRANSFORMATION)
                .with(new WithErrorsOnly(StreamWriting.toSystemError()))
                .installOnByteBuddyAgent();

        Junction<NamedElement> typeMatcher = null;
        for (Junction<NamedElement> matcher : config.getMirrors()
                .stream()
                .map(PathMirror::getPackage)
                .map(ElementMatchers::nameStartsWith)
                .toList()) {
            if (typeMatcher == null) {
                typeMatcher = matcher;
            } else {
                typeMatcher = typeMatcher.or(matcher);
            }
        }

        if (typeMatcher == null) {
            return;
        }

        new AgentBuilder.Default()
                .with(new LocationStrategy.Compound(
                        new LocationStrategy.Simple(ClassFileLocator.ForClassLoader.ofPlatformLoader()),
                        new LocationStrategy.Simple(ClassFileLocator.ForClassLoader.ofBootLoader()),
                        new LocationStrategy.Simple(ClassFileLocator.ForClassLoader.ofSystemLoader()),
                        new Simple(LazyClassFileLocator.of(() -> pluginClassLoaders))
                ))
                .type(typeMatcher)
                .transform((builder, typeDescription, classLoader, module) -> builder.visit(MemberSubstitution
                        .relaxed()
                        .constructor(isDeclaredBy(File.class)
                                .and(takesArguments(String.class)))
                        .replaceWith(stringMethod)
                        .on(any())))
                .transform((builder, typeDescription, classLoader, module) -> builder.visit(MemberSubstitution
                        .relaxed()
                        .constructor(isDeclaredBy(File.class)
                                .and(takesArguments(File.class, String.class)))
                        .replaceWith(fileStringMethod)
                        .on(any())))
                .transform((builder, typeDescription, classLoader, module) -> builder.visit(MemberSubstitution
                        .relaxed()
                        .constructor(isDeclaredBy(File.class)
                                .and(takesArguments(String.class, String.class)))
                        .replaceWith(stringStringMethod)
                        .on(any())))
                .with(TypeStrategy.Default.REDEFINE)
                .installOnByteBuddyAgent();

    }

}
