package cn.gtemc.craftengine.dependency;

import cn.gtemc.craftengine.CraftEngineBlocks;
import cn.gtemc.craftengine.classpath.ClassPathAppender;
import cn.gtemc.craftengine.dependency.classloader.IsolatedClassLoader;
import cn.gtemc.craftengine.dependency.relocation.Relocation;
import cn.gtemc.craftengine.dependency.relocation.RelocationHandler;
import cn.gtemc.craftengine.util.FileUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.stream.Stream;

public final class DependencyManagerImpl implements DependencyManager {
    private final Map<Dependency, Path> loaded = Collections.synchronizedMap(new HashMap<>());
    private final Map<Set<Dependency>, IsolatedClassLoader> loaders = new HashMap<>();
    private final Path cacheDirectory;
    private final DependencyRegistry registry;
    private final ClassPathAppender classPathAppender;
    private final RelocationHandler relocationHandler;
    private final CraftEngineBlocks plugin;

    public DependencyManagerImpl(CraftEngineBlocks plugin) {
        this.plugin = plugin;
        this.cacheDirectory = setupCacheDirectory(plugin);
        this.registry = new DependencyRegistry();
        this.classPathAppender = plugin.classPathAppender();
        this.relocationHandler = new RelocationHandler(this);
    }

    private static Path setupCacheDirectory(CraftEngineBlocks plugin) {
        Path cacheDirectory = plugin.getDataFolder().toPath().toAbsolutePath().resolve("libs");
        try {
            if (Files.exists(cacheDirectory) && (Files.isDirectory(cacheDirectory) || Files.isSymbolicLink(cacheDirectory))) {
                cleanDirectoryJars(cacheDirectory);
                return cacheDirectory;
            }

            try {
                Files.createDirectories(cacheDirectory);
            } catch (FileAlreadyExistsException ignored) {
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to create libs directory", e);
        }

        return cacheDirectory;
    }

    private static void cleanDirectoryJars(Path directory) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path file : stream) {
                if (Files.isRegularFile(file) && file.getFileName().toString().endsWith(".jar")) {
                    Files.delete(file);
                }
            }
        }
    }

    @Override
    public void loadDependencies(Set<Dependency> dependencies) {
        CountDownLatch latch = new CountDownLatch(dependencies.size());

        for (Dependency dependency : dependencies) {
            if (this.loaded.containsKey(dependency)) {
                latch.countDown();
                continue;
            }
            this.plugin.scheduler().async().execute(() -> {
                try {
                    loadDependency(dependency);
                } catch (Throwable e) {
                    this.plugin.getLogger().log(Level.WARNING,"Unable to load dependency " + dependency.id(), e);
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void loadDependency(Dependency dependency) throws Exception {
        if (this.loaded.containsKey(dependency)) {
            return;
        }

        Path file = remapDependency(dependency, downloadDependency(dependency));

        this.loaded.put(dependency, file);

        if (this.classPathAppender != null && this.registry.shouldAutoLoad(dependency)) {
            this.classPathAppender.addJarToClasspath(file);
        }
    }

    private Path downloadDependency(Dependency dependency) throws DependencyDownloadException {
        String fileName = dependency.fileName(null);
        Path file = this.cacheDirectory.resolve(dependency.toLocalPath()).resolve(fileName);

        if (Files.exists(file)) {
            return file;
        }

        Path versionFolder = file.getParent().getParent();
        if (Files.exists(versionFolder) && Files.isDirectory(versionFolder)) {
            String version = dependency.getVersion();
            try (Stream<Path> dirStream = Files.list(versionFolder)) {
                dirStream.filter(Files::isDirectory)
                        .filter(it -> !it.getFileName().toString().equals(version))
                        .forEach(dir -> {
                            try {
                                FileUtils.deleteDirectory(dir);
                                plugin.getLogger().info("Cleaned up outdated dependency " + dir);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
            } catch (IOException e) {
                throw new RuntimeException("Failed to clean " + versionFolder, e);
            }
        }

        DependencyDownloadException lastError = null;
        List<DependencyRepository> repository = DependencyRepository.getByID("maven");
        if (!repository.isEmpty()) {
            int i = 0;
            while (i < repository.size()) {
                try {
                    plugin.getLogger().info("Downloading dependency " + repository.get(i).url() + dependency.mavenPath());
                    repository.get(i).download(dependency, file);
                    plugin.getLogger().info("Successfully downloaded " + fileName);
                    return file;
                } catch (DependencyDownloadException e) {
                    lastError = e;
                    i++;
                }
            }
        }
        throw Objects.requireNonNull(lastError);
    }

    private Path remapDependency(Dependency dependency, Path normalFile) throws Exception {
        List<Relocation> rules = new ArrayList<>(dependency.relocations());
        if (rules.isEmpty()) {
            return normalFile;
        }

        Path remappedFile = this.cacheDirectory.resolve(dependency.toLocalPath()).resolve(dependency.fileName(DependencyRegistry.isGsonRelocated() ? "remapped-legacy" : "remapped"));

        if (Files.exists(remappedFile) && dependency.verify(remappedFile)) {
            return remappedFile;
        }

        this.plugin.getLogger().info("Remapping " + dependency.fileName(null));
        this.relocationHandler.remap(normalFile, remappedFile, rules);
        this.plugin.getLogger().info("Successfully remapped " + dependency.fileName(null));
        return remappedFile;
    }

    @Override
    public ClassLoader obtainClassLoaderWith(Set<Dependency> dependencies) {
        Set<Dependency> set = new HashSet<>(dependencies);

        for (Dependency dependency : dependencies) {
            if (!this.loaded.containsKey(dependency)) {
                throw new IllegalStateException("Dependency " + dependency.id() + " is not loaded.");
            }
        }

        synchronized (this.loaders) {
            IsolatedClassLoader classLoader = this.loaders.get(set);
            if (classLoader != null) {
                return classLoader;
            }

            URL[] urls = set.stream()
                    .map(this.loaded::get)
                    .map(file -> {
                        try {
                            return file.toUri().toURL();
                        } catch (MalformedURLException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .toArray(URL[]::new);

            classLoader = new IsolatedClassLoader(urls);
            this.loaders.put(set, classLoader);
            return classLoader;
        }
    }

    @Override
    public void close() {
        IOException firstEx = null;

        for (IsolatedClassLoader loader : this.loaders.values()) {
            try {
                loader.close();
            } catch (IOException ex) {
                if (firstEx == null) {
                    firstEx = ex;
                } else {
                    firstEx.addSuppressed(ex);
                }
            }
        }

        if (firstEx != null) {
            this.plugin.getLogger().log(Level.SEVERE, firstEx.getMessage(), firstEx);
        }

    }
}
