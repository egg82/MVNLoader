package ninja.egg82.mvn.classloaders;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.util.Enumeration;

public class TransparentInjectableClassLoader extends InjectableClassLoader {
    private static final @NotNull Method ADD_URL_METHOD;
    private static final @NotNull Method FIND_CLASS_METHOD;
    private static final @NotNull Method GET_PERMISSIONS_METHOD;

    static {
        ClassLoader.registerAsParallelCapable();

        try {
            ADD_URL_METHOD = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            ADD_URL_METHOD.setAccessible(true);
            FIND_CLASS_METHOD = URLClassLoader.class.getDeclaredMethod("findClass", String.class);
            FIND_CLASS_METHOD.setAccessible(true);
            GET_PERMISSIONS_METHOD = URLClassLoader.class.getDeclaredMethod("getPermissions", CodeSource.class);
            GET_PERMISSIONS_METHOD.setAccessible(true);
        } catch (NoSuchMethodException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    private final @NotNull Logger logger;

    private final @NotNull URLClassLoader classLoader;

    public TransparentInjectableClassLoader(@NotNull URLClassLoader classLoader) {
        this(classLoader, null);
    }

    public TransparentInjectableClassLoader(@NotNull URLClassLoader classLoader, @Nullable Logger logger) {
        super(new URL[0], classLoader.getParent());
        this.classLoader = classLoader;
        this.logger = logger != null ? logger : LoggerFactory.getLogger(getClass());
    }

    @Override
    public void addJar(@NotNull URL url) { addURL(url); }

    @Override
    @Nullable
    public InputStream getResourceAsStream(String name) { return classLoader.getResourceAsStream(name); }

    @Override
    public void close() throws IOException {
        classLoader.close();
        super.close();
    }

    @Override
    protected void addURL(@NotNull URL url) {
        try {
            ADD_URL_METHOD.invoke(classLoader, url);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            logger.error("Could not inject URL " + url, ex);
        }
    }

    @Override
    @NotNull
    public URL @NotNull [] getURLs() {
        return classLoader.getURLs();
    }

    @Override
    @NotNull
    protected Class<?> findClass(final @NotNull String name) throws ClassNotFoundException {
        try {
            return (Class<?>) FIND_CLASS_METHOD.invoke(classLoader, name);
        } catch (IllegalAccessException | IllegalArgumentException ex) {
            throw new ClassNotFoundException("Could not find class " + name, ex);
        } catch (InvocationTargetException ex) {
            if (ex.getTargetException() instanceof ClassNotFoundException) {
                throw (ClassNotFoundException) ex.getTargetException();
            }
            throw new ClassNotFoundException("Could not find class " + name, ex);
        }
    }

    @Override
    @Nullable
    public URL findResource(final @NotNull String name) {
        return classLoader.findResource(name);
    }

    @Override
    @NotNull
    public Enumeration<@NotNull URL> findResources(final @NotNull String name) throws IOException {
        return classLoader.findResources(name);
    }

    @Override
    @NotNull
    protected PermissionCollection getPermissions(@NotNull CodeSource codesource) {
        try {
            return (PermissionCollection) GET_PERMISSIONS_METHOD.invoke(classLoader, codesource);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            logger.error("Could not get permissions for " + codesource.getLocation(), ex);
        }
        return super.getPermissions(codesource);
    }
}
