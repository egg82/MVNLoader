package ninja.egg82.mvn;

import org.apache.maven.model.building.ModelBuildingException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

class TestInjector {
    @Test
    void injectSimple() throws URISyntaxException, ModelBuildingException, IOException {
        new JarInjector(new File(getCurrentDir(), "cache"))
                .addBuilder(new JarBuilder("io.ebean", "ebean-core", "12.7.2", "https://repo.maven.apache.org/maven2"))
                .inject(new InjectableClassLoader(getClass().getClassLoader()), 1);
    }

    @Test
    void injectSimpleRelocate() throws URISyntaxException, ModelBuildingException, IOException {
        new JarInjector(new File(getCurrentDir(), "cache"))
                .addBuilder(new JarBuilder("io.ebean", "ebean-core", "12.7.2", "https://repo.maven.apache.org/maven2"))
                .addRelocation(new RelocationBuilder("io.ebeaninternal", "ninja.egg82.mvn.external").build())
                .addRelocation(new RelocationBuilder("io.ebeanservice", "ninja.egg82.mvn.external").build())
                .inject(new InjectableClassLoader(getClass().getClassLoader()), 1);
    }

    @Test
    void injectSimpleProxy() throws URISyntaxException, ModelBuildingException, IOException {
        new JarInjector(new File(getCurrentDir(), "cache"))
                .addBuilder(new JarBuilder("io.ebean", "ebean-core", "12.7.2", "https://repo.maven.apache.org/maven2")
                                    .setRepositoryProxy("https://repo.maven.apache.org/maven2", "https://nexus.egg82.me/repository/maven-central/"))
                .inject(new InjectableClassLoader(getClass().getClassLoader()), 1);
    }

    @Test
    void injectSimpleProxyRelocate() throws URISyntaxException, ModelBuildingException, IOException {
        new JarInjector(new File(getCurrentDir(), "cache"))
                .addBuilder(new JarBuilder("io.ebean", "ebean-core", "12.7.2", "https://repo.maven.apache.org/maven2")
                                    .setRepositoryProxy("https://repo.maven.apache.org/maven2", "https://nexus.egg82.me/repository/maven-central/"))
                .addRelocation(new RelocationBuilder("io.ebeaninternal", "ninja.egg82.mvn.external").build())
                .addRelocation(new RelocationBuilder("io.ebeanservice", "ninja.egg82.mvn.external").build())
                .inject(new InjectableClassLoader(getClass().getClassLoader()), 1);
    }

    @Test
    void injectComplex() throws URISyntaxException, ModelBuildingException, IOException {
        new JarInjector(new File(getCurrentDir(), "cache"))
                .addBuilder(new JarBuilder("com.destroystokyo.paper", "paper-api", "1.16.5-R0.1-SNAPSHOT", "https://papermc.io/repo/repository/maven-public/"))
                .inject(new InjectableClassLoader(getClass().getClassLoader()), 1);
    }

    @Test
    void injectComplexRelocate() throws URISyntaxException, ModelBuildingException, IOException {
        new JarInjector(new File(getCurrentDir(), "cache"))
                .addBuilder(new JarBuilder("com.destroystokyo.paper", "paper-api", "1.16.5-R0.1-SNAPSHOT", "https://papermc.io/repo/repository/maven-public/"))
                .addRelocation(new RelocationBuilder("com.destroystokyo.paper", "ninja.egg82.mvn.external").build())
                .inject(new InjectableClassLoader(getClass().getClassLoader()), 1);
    }

    @Test
    void injectComplexProxy() throws URISyntaxException, ModelBuildingException, IOException {
        new JarInjector(new File(getCurrentDir(), "cache"))
                .addBuilder(new JarBuilder("com.destroystokyo.paper", "paper-api", "1.16.5-R0.1-SNAPSHOT", "https://papermc.io/repo/repository/maven-public/")
                                    .setRepositoryProxy("https://papermc.io/repo/repository/maven-public/", "https://nexus.egg82.me/repository/papermc/")
                                    .setRepositoryProxy("https://repo.maven.apache.org/maven2", "https://nexus.egg82.me/repository/maven-central/"))
                .inject(new InjectableClassLoader(getClass().getClassLoader()), 1);
    }

    @Test
    void injectComplexProxyRelocate() throws URISyntaxException, ModelBuildingException, IOException {
        new JarInjector(new File(getCurrentDir(), "cache"))
                .addBuilder(new JarBuilder("com.destroystokyo.paper", "paper-api", "1.16.5-R0.1-SNAPSHOT", "https://papermc.io/repo/repository/maven-public/")
                                    .setRepositoryProxy("https://papermc.io/repo/repository/maven-public/", "https://nexus.egg82.me/repository/papermc/")
                                    .setRepositoryProxy("https://repo.maven.apache.org/maven2", "https://nexus.egg82.me/repository/maven-central/"))
                .addRelocation(new RelocationBuilder("com.destroystokyo.paper", "ninja.egg82.mvn.external").build())
                .inject(new InjectableClassLoader(getClass().getClassLoader()), 1);
    }

    @NotNull
    private File getCurrentDir() throws URISyntaxException {
        return new File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile();
    }
}
