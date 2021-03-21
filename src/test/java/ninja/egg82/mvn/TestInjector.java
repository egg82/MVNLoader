package ninja.egg82.mvn;

import me.lucko.jarrelocator.Relocation;
import org.apache.maven.model.building.ModelBuildingException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLClassLoader;

class TestInjector {
    @Test
    void injectSimple() throws URISyntaxException, ModelBuildingException, IOException {
        new JarInjector(new File(getCurrentDir(), "cache"))
                .addBuilder(new JarBuilder("io.ebean", "ebean-core", "12.7.2", "https://repo.maven.apache.org/maven2"))
                .inject((URLClassLoader) getClass().getClassLoader(), 1);
    }

    @Test
    void injectSimpleRelocate() throws URISyntaxException, ModelBuildingException, IOException {
        new JarInjector(new File(getCurrentDir(), "cache"))
                .addBuilder(new JarBuilder("io.ebean", "ebean-core", "12.7.2", "https://repo.maven.apache.org/maven2"))
                .addRelocation(new Relocation("io.ebeaninternal", "ninja.egg82.mvn.external.io.ebeaninternal"))
                .addRelocation(new Relocation("io.ebeanservice", "ninja.egg82.mvn.external.io.ebeanservice"))
                .inject((URLClassLoader) getClass().getClassLoader(), 1);
    }

    @Test
    void injectSimpleProxy() throws URISyntaxException, ModelBuildingException, IOException {
        new JarInjector(new File(getCurrentDir(), "cache"))
                .addBuilder(new JarBuilder("io.ebean", "ebean-core", "12.7.2", "https://repo.maven.apache.org/maven2")
                                    .setRepositoryProxy("https://repo.maven.apache.org/maven2", "https://nexus.egg82.me/repository/maven-central/"))
                .inject((URLClassLoader) getClass().getClassLoader(), 1);
    }

    @Test
    void injectSimpleProxyRelocate() throws URISyntaxException, ModelBuildingException, IOException {
        new JarInjector(new File(getCurrentDir(), "cache"))
                .addBuilder(new JarBuilder("io.ebean", "ebean-core", "12.7.2", "https://repo.maven.apache.org/maven2")
                                    .setRepositoryProxy("https://repo.maven.apache.org/maven2", "https://nexus.egg82.me/repository/maven-central/"))
                .addRelocation(new Relocation("io.ebeaninternal", "ninja.egg82.mvn.external.io.ebeaninternal"))
                .addRelocation(new Relocation("io.ebeanservice", "ninja.egg82.mvn.external.io.ebeanservice"))
                .inject((URLClassLoader) getClass().getClassLoader(), 1);
    }

    @Test
    void injectComplex() throws URISyntaxException, ModelBuildingException, IOException {
        new JarInjector(new File(getCurrentDir(), "cache"))
                .addBuilder(new JarBuilder("com.destroystokyo.paper", "paper-api", "1.16.5-R0.1-SNAPSHOT", "https://papermc.io/repo/repository/maven-public/"))
                .inject((URLClassLoader) getClass().getClassLoader(), 1);
    }

    @Test
    void injectComplexRelocate() throws URISyntaxException, ModelBuildingException, IOException {
        new JarInjector(new File(getCurrentDir(), "cache"))
                .addBuilder(new JarBuilder("com.destroystokyo.paper", "paper-api", "1.16.5-R0.1-SNAPSHOT", "https://papermc.io/repo/repository/maven-public/"))
                .addRelocation(new Relocation("com.destroystokyo.paper", "ninja.egg82.mvn.external.com.destroystokyo.paper"))
                .inject((URLClassLoader) getClass().getClassLoader(), 1);
    }

    @Test
    void injectComplexProxy() throws URISyntaxException, ModelBuildingException, IOException {
        new JarInjector(new File(getCurrentDir(), "cache"))
                .addBuilder(new JarBuilder("com.destroystokyo.paper", "paper-api", "1.16.5-R0.1-SNAPSHOT", "https://papermc.io/repo/repository/maven-public/")
                                    .setRepositoryProxy("https://papermc.io/repo/repository/maven-public/", "https://nexus.egg82.me/repository/papermc/")
                                    .setRepositoryProxy("https://repo.maven.apache.org/maven2", "https://nexus.egg82.me/repository/maven-central/"))
                .inject((URLClassLoader) getClass().getClassLoader(), 1);
    }

    @Test
    void injectComplexProxyRelocate() throws URISyntaxException, ModelBuildingException, IOException {
        new JarInjector(new File(getCurrentDir(), "cache"))
                .addBuilder(new JarBuilder("com.destroystokyo.paper", "paper-api", "1.16.5-R0.1-SNAPSHOT", "https://papermc.io/repo/repository/maven-public/")
                                    .setRepositoryProxy("https://papermc.io/repo/repository/maven-public/", "https://nexus.egg82.me/repository/papermc/")
                                    .setRepositoryProxy("https://repo.maven.apache.org/maven2", "https://nexus.egg82.me/repository/maven-central/"))
                .addRelocation(new Relocation("com.destroystokyo.paper", "ninja.egg82.mvn.external.com.destroystokyo.paper"))
                .inject((URLClassLoader) getClass().getClassLoader(), 1);
    }

    @NotNull
    private File getCurrentDir() throws URISyntaxException {
        return new File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile();
    }
}
