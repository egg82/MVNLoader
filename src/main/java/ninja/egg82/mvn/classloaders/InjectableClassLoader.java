package ninja.egg82.mvn.classloaders;

import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;

// https://github.com/lucko/LuckPerms/blob/21f5c2484744317d6998ca1739ad32f2f0c28418/common/loader-utils/src/main/java/me/lucko/luckperms/common/loader/JarInJarClassLoader.java
public abstract class InjectableClassLoader extends URLClassLoader {
    protected InjectableClassLoader(@NotNull URL @NotNull [] urls, @NotNull ClassLoader parent) {
        super(urls, parent);
    }

    protected InjectableClassLoader(@NotNull URL @NotNull [] urls) {
        super(urls);
    }

    protected InjectableClassLoader(@NotNull URL @NotNull [] urls, @NotNull ClassLoader parent, @NotNull URLStreamHandlerFactory factory) {
        super(urls, parent, factory);
    }

    public abstract void addJar(@NotNull URL url);
}
