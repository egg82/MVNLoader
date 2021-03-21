package ninja.egg82.mvn.internal;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Repository;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.*;

public class DependencyWrapper implements Serializable {
    private final @NotNull Dependency dependency;
    private boolean compiled = false;
    private final @NotNull Set<@NotNull Repository> repositories = new LinkedHashSet<>();

    public DependencyWrapper() {
        this.dependency = new Dependency();
        this.dependency.setGroupId("");
        this.dependency.setArtifactId("");
        this.dependency.setVersion("");
    }

    public DependencyWrapper(@NotNull Model model, @NotNull Dependency dependency) {
        this.repositories.addAll(model.getRepositories());
        this.dependency = dependency;
    }

    @NotNull
    public Set<@NotNull Repository> getRepositories() { return repositories; }

    @NotNull
    public Dependency getDependency() { return dependency; }

    public boolean isCompiled() { return compiled; }

    public void setCompiled(boolean compiled) { this.compiled = compiled; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DependencyWrapper that = (DependencyWrapper) o;
        return dependency.getGroupId().equals(that.dependency.getGroupId())
                && dependency.getArtifactId().equals(that.dependency.getArtifactId())
                && dependency.getVersion().equals(that.dependency.getVersion());
    }

    @Override
    public int hashCode() {
        return Objects.hash(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
    }
}
