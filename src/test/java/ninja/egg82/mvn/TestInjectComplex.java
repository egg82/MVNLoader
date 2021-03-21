package ninja.egg82.mvn;

import ninja.egg82.mvn.classloaders.IsolatedInjectableClassLoader;
import org.apache.maven.model.building.ModelBuildingException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

class TestInjectComplex {
    @Test
    void test() throws URISyntaxException, IOException, ModelBuildingException {
        JarInjector injector = new JarInjector(new File(getCurrentDir(), "cache"));

        // Caffeine
        injector.addBuilder(new JarBuilder("com.github.ben-manes.caffeine", "caffeine", "2.9.0")
                                    .setRepositoryProxy("https://repo.maven.apache.org/maven2", "https://nexus.egg82.me/repository/maven-central/"));
        injector.addRelocation(new RelocationBuilder("com{}github{}benmanes{}caffeine", "me.egg82.antivpn.external").build("{}", "."));

        // Zstd
        try {
            Class.forName("com.github.luben.zstd.Zstd");
        } catch (ClassNotFoundException ignored) {
            injector.addBuilder(new JarBuilder("com.github.luben", "zstd-jni", "1.4.9-1")
                                        .setRepositoryProxy("https://repo.maven.apache.org/maven2", "https://nexus.egg82.me/repository/maven-central/"));
        }

        // IPAddress
        injector.addBuilder(new JarBuilder("com.github.seancfoley", "ipaddress", "5.3.3")
                                    .setRepositoryProxy("https://repo.maven.apache.org/maven2", "https://nexus.egg82.me/repository/maven-central/"));
        injector.addRelocation(new RelocationBuilder("inet{}ipaddr", "me.egg82.antivpn.external").build("{}", "."));

        // H2
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException ignored) {
            injector.addBuilder(new JarBuilder("com.h2database", "h2", "1.4.200")
                                        .setRepositoryProxy("https://repo.maven.apache.org/maven2", "https://nexus.egg82.me/repository/maven-central/"));
        }

        // MySQL
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ignored) {
            injector.addBuilder(new JarBuilder("mysql", "mysql-connector-java", "8.0.23")
                                        .setRepositoryProxy("https://repo.maven.apache.org/maven2", "https://nexus.egg82.me/repository/maven-central/"));
        }

        // RabbitMQ
        injector.addBuilder(new JarBuilder(replace("com{}rabbitmq"), "amqp-client", "5.11.0")
                                    .setRepositoryProxy("https://repo.maven.apache.org/maven2", "https://nexus.egg82.me/repository/maven-central/"));
        injector.addRelocation(new RelocationBuilder("com{}rabbitmq", "me.egg82.antivpn.external").build("{}", "."));

        // Ebean
        injector.addBuilder(new JarBuilder(replace("io{}ebean"), "ebean-core", "12.7.2")
                                    .setRepositoryProxy("https://repo.maven.apache.org/maven2", "https://nexus.egg82.me/repository/maven-central/"));
        injector.addRelocation(new RelocationBuilder("io{}ebean", "me.egg82.antivpn.external").build("{}", "."));
        injector.addRelocation(new RelocationBuilder("io{}ebeaninternal", "me.egg82.antivpn.external").build("{}", "."));
        injector.addRelocation(new RelocationBuilder("io{}ebeanservice", "me.egg82.antivpn.external").build("{}", "."));

        // Javassist
        injector.addBuilder(new JarBuilder("org.javassist", getJavassistPackage(), "3.27.0-GA")
                                    .setRepositoryProxy("https://repo.maven.apache.org/maven2", "https://nexus.egg82.me/repository/maven-central/"));
        injector.addRelocation(new RelocationBuilder(getJavassistPackage(), "me.egg82.antivpn.external").build());

        // PostgreSQL
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException ignored) {
            injector.addBuilder(new JarBuilder("org.postgresql", "postgresql", "42.2.19")
                                        .setRepositoryProxy("https://repo.maven.apache.org/maven2", "https://nexus.egg82.me/repository/maven-central/"));
        }

        // SQLite
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ignored) {
            injector.addBuilder(new JarBuilder("org.xerial", "sqlite-jdbc", "3.34.0")
                                        .setRepositoryProxy("https://repo.maven.apache.org/maven2", "https://nexus.egg82.me/repository/maven-central/"));
        }

        // Jedis
        injector.addBuilder(new JarBuilder("redis.clients", "jedis", "3.5.1")
                                    .setRepositoryProxy("https://repo.maven.apache.org/maven2", "https://nexus.egg82.me/repository/maven-central/"));
        injector.addRelocation(new RelocationBuilder("redis{}clients{}jedis", "me.egg82.antivpn.external").build("{}", "."));

        // Inject
        injector.inject(new IsolatedInjectableClassLoader(getClass().getClassLoader()), 1);
    }

    // Prevent Maven from relocating these
    @NotNull
    private String replace(@NotNull String pkg) { return pkg.replace("{}", "."); }

    private @NotNull String getJavassistPackage() { return new String(new byte[] { 'j', 'a', 'v', 'a', 's', 's', 'i', 's', 't' }); }

    @NotNull
    private File getCurrentDir() throws URISyntaxException {
        return new File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile();
    }
}
