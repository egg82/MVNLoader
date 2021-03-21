package ninja.egg82.mvn;

import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.net.URLClassLoader;

// https://github.com/lucko/LuckPerms/blob/21f5c2484744317d6998ca1739ad32f2f0c28418/common/loader-utils/src/main/java/me/lucko/luckperms/common/loader/JarInJarClassLoader.java
public class InjectableClassLoader extends URLClassLoader {
    static {
        ClassLoader.registerAsParallelCapable();
    }

    public InjectableClassLoader(@NotNull ClassLoader parent) {
        super(new URL[0], parent);
    }

    public void addJar(@NotNull URL url) {
        addURL(url);
    }
}
