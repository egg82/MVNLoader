package ninja.egg82.mvn;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Repository;
import org.apache.maven.model.building.ModelBuildingException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

class TestBuilder {
    @Test
    void buildSimple() throws URISyntaxException, ModelBuildingException, IOException {
        Model model = new JarBuilder("io.ebean", "ebean-core", "12.7.2", "https://repo.maven.apache.org/maven2")
                .build(new File(getCurrentDir(), "cache"));
        System.out.println(model.getGroupId() + ":" + model.getArtifactId() + ":" + model.getVersion());
        for (Repository repo : model.getRepositories()) {
            if (repo.getReleases() != null && repo.getSnapshots() != null) {
                System.out.println(repo.getUrl() + " [" + repo.getId() + "/" + repo.getName() + "] - R:" + repo.getReleases().isEnabled() + ", S:" + repo.getSnapshots()
                        .isEnabled());
            } else {
                System.out.println(repo.getUrl() + " [" + repo.getId() + "/" + repo.getName() + "]");
            }
        }
        for (Dependency dep : model.getDependencies()) {
            System.out.println(dep.getGroupId() + ":" + dep.getArtifactId() + ":" + dep.getVersion() + " [" + dep.getScope() + "]");
        }
    }

    @Test
    void buildSimpleProxy() throws URISyntaxException, ModelBuildingException, IOException {
        Model model = new JarBuilder("io.ebean", "ebean-core", "12.7.2", "https://repo.maven.apache.org/maven2")
                .setRepositoryProxy("https://repo.maven.apache.org/maven2", "https://nexus.egg82.me/repository/maven-central/")
                .build(new File(getCurrentDir(), "cache"));
        System.out.println(model.getGroupId() + ":" + model.getArtifactId() + ":" + model.getVersion());
        for (Repository repo : model.getRepositories()) {
            if (repo.getReleases() != null && repo.getSnapshots() != null) {
                System.out.println(repo.getUrl() + " [" + repo.getId() + "/" + repo.getName() + "] - R:" + repo.getReleases().isEnabled() + ", S:" + repo.getSnapshots()
                        .isEnabled());
            } else {
                System.out.println(repo.getUrl() + " [" + repo.getId() + "/" + repo.getName() + "]");
            }
        }
        for (Dependency dep : model.getDependencies()) {
            System.out.println(dep.getGroupId() + ":" + dep.getArtifactId() + ":" + dep.getVersion() + " [" + dep.getScope() + "]");
        }
    }

    @Test
    void buildComplex() throws URISyntaxException, ModelBuildingException, IOException {
        Model model = new JarBuilder("com.destroystokyo.paper", "paper-api", "1.16.5-R0.1-SNAPSHOT", "https://papermc.io/repo/repository/maven-public/")
                .build(new File(getCurrentDir(), "cache"));
        System.out.println(model.getGroupId() + ":" + model.getArtifactId() + ":" + model.getVersion());
        for (Repository repo : model.getRepositories()) {
            if (repo.getReleases() != null && repo.getSnapshots() != null) {
                System.out.println(repo.getUrl() + " [" + repo.getId() + "/" + repo.getName() + "] - R:" + repo.getReleases().isEnabled() + ", S:" + repo.getSnapshots()
                        .isEnabled());
            } else {
                System.out.println(repo.getUrl() + " [" + repo.getId() + "/" + repo.getName() + "]");
            }
        }
        for (Dependency dep : model.getDependencies()) {
            System.out.println(dep.getGroupId() + ":" + dep.getArtifactId() + ":" + dep.getVersion() + " [" + dep.getScope() + "]");
        }
    }

    @Test
    void buildComplexProxy() throws URISyntaxException, ModelBuildingException, IOException {
        Model model = new JarBuilder("com.destroystokyo.paper", "paper-api", "1.16.5-R0.1-SNAPSHOT", "https://papermc.io/repo/repository/maven-public/")
                .setRepositoryProxy("https://papermc.io/repo/repository/maven-public/", "https://nexus.egg82.me/repository/papermc/")
                .setRepositoryProxy("https://repo.maven.apache.org/maven2", "https://nexus.egg82.me/repository/maven-central/")
                .build(new File(getCurrentDir(), "cache"));
        System.out.println(model.getGroupId() + ":" + model.getArtifactId() + ":" + model.getVersion());
        for (Repository repo : model.getRepositories()) {
            if (repo.getReleases() != null && repo.getSnapshots() != null) {
                System.out.println(repo.getUrl() + " [" + repo.getId() + "/" + repo.getName() + "] - R:" + repo.getReleases().isEnabled() + ", S:" + repo.getSnapshots()
                        .isEnabled());
            } else {
                System.out.println(repo.getUrl() + " [" + repo.getId() + "/" + repo.getName() + "]");
            }
        }
        for (Dependency dep : model.getDependencies()) {
            System.out.println(dep.getGroupId() + ":" + dep.getArtifactId() + ":" + dep.getVersion() + " [" + dep.getScope() + "]");
        }
    }

    @NotNull
    private File getCurrentDir() throws URISyntaxException {
        return new File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile();
    }
}
