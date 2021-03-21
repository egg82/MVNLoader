package ninja.egg82.mvn.classloaders;

import org.jetbrains.annotations.NotNull;

import java.net.URL;

public class IsolatedInjectableClassLoader extends InjectableClassLoader {
    static {
        ClassLoader.registerAsParallelCapable();
    }

    public IsolatedInjectableClassLoader(@NotNull ClassLoader parent) {
        super(new URL[0], parent);
    }

    @Override
    public void addJar(@NotNull URL url) {
        addURL(url);
    }
}
