package ninja.egg82.mvn;

import me.lucko.jarrelocator.JarRelocator;
import me.lucko.jarrelocator.Relocation;
import ninja.egg82.mvn.internal.DependencyWrapper;
import ninja.egg82.mvn.internal.HttpUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Repository;
import org.apache.maven.model.building.ModelBuildingException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class JarInjector {
    private static final Method ADD_URL_METHOD;

    static {
        try {
            ADD_URL_METHOD = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            ADD_URL_METHOD.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final @NotNull Logger logger;

    private final long maxDownloadTime;
    private final @NotNull TimeUnit maxDownloadTimeUnit;

    private final @NotNull File cacheDir;
    private final @NotNull List<@NotNull JarBuilder> builders = new ArrayList<>();
    private final @NotNull List<@NotNull Relocation> relocations = new ArrayList<>();

    public JarInjector(@NotNull File cacheDir) {
        this(cacheDir, 20L, TimeUnit.MINUTES);
    }

    public JarInjector(@NotNull File cacheDir, long maxDownloadTime, @NotNull TimeUnit maxDownloadTimeUnit) {
        this(cacheDir, maxDownloadTime, maxDownloadTimeUnit, null);
    }

    public JarInjector(@NotNull File cacheDir, @Nullable Logger logger) {
        this(cacheDir, 20L, TimeUnit.MINUTES, logger);
    }

    public JarInjector(@NotNull File cacheDir, long maxDownloadTime, @NotNull TimeUnit maxDownloadTimeUnit, @Nullable Logger logger) {
        this.cacheDir = cacheDir;
        this.maxDownloadTime = maxDownloadTime;
        this.maxDownloadTimeUnit = maxDownloadTimeUnit;
        this.logger = logger != null ? logger : LoggerFactory.getLogger(getClass());
    }

    @NotNull
    public JarInjector addBuilder(@NotNull JarBuilder builder) {
        this.builders.add(builder);
        return this;
    }

    @NotNull
    public JarInjector addBuilders(@NotNull Collection<@NotNull JarBuilder> builders) {
        this.builders.addAll(builders);
        return this;
    }

    @NotNull
    public JarInjector addBuilders(@NotNull JarBuilder @NotNull ... builders) {
        this.builders.addAll(Arrays.asList(builders));
        return this;
    }

    @NotNull
    public JarInjector addRelocation(@NotNull Relocation relocation) {
        this.relocations.add(relocation);
        return this;
    }

    @NotNull
    public JarInjector addRelocations(@NotNull Collection<@NotNull Relocation> relocations) {
        this.relocations.addAll(relocations);
        return this;
    }

    @NotNull
    public JarInjector addRelocations(@NotNull Relocation @NotNull ... relocations) {
        this.relocations.addAll(Arrays.asList(relocations));
        return this;
    }

    public void inject(@NotNull URLClassLoader classLoader) throws IOException, ModelBuildingException {
        inject(classLoader, Math.max(2, Runtime.getRuntime().availableProcessors() / 2));
    }

    public void inject(@NotNull URLClassLoader classLoader, int threads) throws IOException, ModelBuildingException {
        Map<@NotNull String, @Nullable DependencyWrapper> dependencies = new HashMap<>();
        List<@NotNull Model> models = new ArrayList<>();
        for (JarBuilder builder : builders) {
            Model model = buildChain(builder, cacheDir, dependencies, true);
            if (model != null) {
                models.add(model);
            }
        }
        dependencies.values().removeIf(v -> v == null || v.isCompiled());

        if (threads <= 1) {
            for (DependencyWrapper wrapper : dependencies.values()) {
                if (wrapper != null) {
                    inject(classLoader, wrapper);
                }
            }
            for (Model model : models) {
                inject(classLoader, model);
            }
            return;
        }

        AtomicInteger errors = new AtomicInteger(0);
        CountDownLatch downloadLatch = new CountDownLatch(dependencies.size() + models.size());
        ExecutorService pool = Executors.newWorkStealingPool(threads);
        for (DependencyWrapper wrapper : dependencies.values()) {
            if (wrapper != null) {
                pool.execute(() -> {
                    try {
                        inject(classLoader, wrapper);
                    } catch (IOException ex) {
                        errors.incrementAndGet();
                        logger.error("Could not inject artifact " + wrapper.getDependency().getGroupId() + ":" + wrapper.getDependency()
                                .getArtifactId() + ":" + wrapper.getDependency().getVersion(), ex);
                    }
                    downloadLatch.countDown();
                });
            } else {
                downloadLatch.countDown();
            }
        }
        for (Model model : models) {
            pool.execute(() -> {
                try {
                    inject(classLoader, model);
                } catch (IOException ex) {
                    errors.incrementAndGet();
                    logger.error("Could not inject artifact " + model.getGroupId() + ":" + model.getArtifactId() + ":" + model.getVersion(), ex);
                }
                downloadLatch.countDown();
            });
        }

        try {
            if (!downloadLatch.await(maxDownloadTime, maxDownloadTimeUnit)) {
                logger.error("Could not download required artifacts.");
            }
            pool.shutdownNow();
            if (errors.get() > 0) {
                logger.error("Some artifacts reported errors while downloading.");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    @NotNull
    private File inject(@NotNull URLClassLoader classLoader, @NotNull Model model) throws IOException {
        File retVal = downloadOrThrow(model.getRepositories(), model.getGroupId(), model.getArtifactId(), model.getVersion());
        try {
            ADD_URL_METHOD.invoke(classLoader, retVal.toURI().toURL());
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            throw new IOException("Could not inject artifact " + retVal.getAbsolutePath(), ex);
        }
        return retVal;
    }

    @NotNull
    private File inject(@NotNull URLClassLoader classLoader, @NotNull DependencyWrapper wrapper) throws IOException {
        File retVal = downloadOrThrow(
                wrapper.getRepositories(),
                wrapper.getDependency().getGroupId(),
                wrapper.getDependency().getArtifactId(),
                wrapper.getDependency().getVersion()
        );
        try {
            ADD_URL_METHOD.invoke(classLoader, retVal.toURI().toURL());
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            throw new IOException("Could not inject artifact " + retVal.getAbsolutePath(), ex);
        }
        return retVal;
    }

    @NotNull
    private File downloadOrThrow(
            @NotNull Collection<@NotNull Repository> repositories,
            @NotNull String groupId,
            @NotNull String artifactId,
            @NotNull String version
    ) throws IOException {
        File outFile;
        for (Repository repository : repositories) {
            try {
                String realVersion = HttpUtils.getRealVersion(repository.getUrl(), null, groupId, artifactId, version, null);
                if (!realVersion.equals(version)) {
                    outFile = HttpUtils.getJarCacheFile(cacheDir, groupId, artifactId, version, realVersion);
                } else {
                    outFile = HttpUtils.getJarCacheFile(cacheDir, groupId, artifactId, version);
                }
                if (outFile.exists()) {
                    return relocate(outFile, groupId, artifactId, version, realVersion);
                }

                if (!realVersion.equals(version)) {
                    return relocate(
                            HttpUtils.tryDownloadJar(outFile, repository.getUrl(), groupId, artifactId, version, realVersion),
                            groupId,
                            artifactId,
                            version,
                            realVersion
                    );
                } else {
                    return relocate(HttpUtils.tryDownloadJar(outFile, repository.getUrl(), groupId, artifactId, version), groupId, artifactId, version, realVersion);
                }
            } catch (IOException ignored) {
            }
        }
        throw new IOException("Artifact was not found in any provided repository.");
    }

    @NotNull
    private File relocate(
            @NotNull File inFile,
            @NotNull String groupId,
            @NotNull String artifactId,
            @NotNull String version,
            @NotNull String realVersion
    ) throws IOException {
        if (relocations.isEmpty()) {
            return inFile;
        }

        File outFile;
        if (!realVersion.equals(version)) {
            outFile = HttpUtils.getRelocatedJarCacheFile(cacheDir, groupId, artifactId, version, realVersion);
        } else {
            outFile = HttpUtils.getRelocatedJarCacheFile(cacheDir, groupId, artifactId, version);
        }

        JarRelocator relocator = new JarRelocator(inFile, outFile, relocations);
        relocator.run();
        return outFile;
    }

    @Nullable
    private Model buildChain(
            @NotNull JarBuilder builder,
            @NotNull File cacheDir,
            @NotNull Map<@NotNull String, @Nullable DependencyWrapper> dependencies,
            boolean recurse
    ) throws IOException, ModelBuildingException {
        Model model = builder.build(cacheDir);
        if (model == null) {
            return null;
        }

        // Add dependencies breadth-first so we behave like Maven
        List<@NotNull JarBuilder> currentBuilders = new ArrayList<>();

        boolean hasShade = hasShadePlugin(model);

        for (Dependency dependency : model.getDependencies()) {
            if (
                    dependency.getScope().equalsIgnoreCase("provided")
                            || dependency.getScope().equalsIgnoreCase("runtime")
                            || dependency.getScope().equalsIgnoreCase("system")
                            || (!hasShade && dependency.getScope().equalsIgnoreCase("compile"))
            ) {
                if (!dependencies.containsKey(dependency.getGroupId() + ":" + dependency.getArtifactId())) {
                    dependencies.put(
                            dependency.getGroupId() + ":" + dependency.getArtifactId() + ":" + dependency.getVersion(),
                            new DependencyWrapper(model, dependency)
                    );
                    currentBuilders.add(builder.clone(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion()));
                }
            } else {
                dependencies.compute(dependency.getGroupId() + ":" + dependency.getArtifactId(), (k, v) -> {
                    if (v == null) {
                        v = new DependencyWrapper(model, dependency);
                    }
                    v.setCompiled(true);
                    return v;
                });
            }
        }

        // Again, breadth-first. Only add deps, don't recurse until everything under this pom has been accounted for
        if (recurse) {
            for (JarBuilder b : currentBuilders) {
                buildChain(b, cacheDir, dependencies, false);
            }
            for (JarBuilder b : currentBuilders) {
                buildChain(b, cacheDir, dependencies, true);
            }
        }

        return model;
    }

    private boolean hasShadePlugin(@NotNull Model model) {
        for (Plugin plugin : model.getBuild().getPlugins()) {
            if ("org.apache.maven.plugins".equals(plugin.getGroupId()) && "maven-shade-plugin".equals(plugin.getArtifactId())) {
                return true;
            }
        }
        return false;
    }
}
