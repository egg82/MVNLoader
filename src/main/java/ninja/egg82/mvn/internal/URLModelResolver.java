package ninja.egg82.mvn.internal;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Repository;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.model.resolution.InvalidRepositoryException;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class URLModelResolver implements ModelResolver {
    private final @NotNull ReadWriteLock repositoryLock = new ReentrantReadWriteLock();
    private final @NotNull List<@NotNull Repository> repositories = new ArrayList<>();

    private final @NotNull File cacheDir;
    private final @NotNull String repositoryUrl;
    private final @NotNull Map<@NotNull String, @Nullable String> proxies = new HashMap<>();

    public URLModelResolver(@NotNull File cacheDir, @NotNull String repositoryUrl) {
        this(cacheDir, repositoryUrl, null);
    }

    public URLModelResolver(@NotNull File cacheDir, @NotNull String repositoryUrl, @Nullable Map<@NotNull String, @Nullable String> proxies) {
        this.cacheDir = cacheDir;
        this.repositoryUrl = repositoryUrl;
        if (proxies != null) {
            this.proxies.putAll(proxies);
        }
    }

    @Override
    @NotNull
    public ModelSource resolveModel(@NotNull String groupId, @NotNull String artifactId, @NotNull String version) throws UnresolvableModelException {
        String proxy;
        try {
            proxy = proxies.get(HttpUtils.simplify(repositoryUrl));
        } catch (IOException ex) {
            throw new UnresolvableModelException(ex, groupId, artifactId, version);
        }

        String realVersion;
        try {
            realVersion = HttpUtils.getRealVersion(repositoryUrl, proxy, groupId, artifactId, version, null);
        } catch (IOException ex) {
            throw new UnresolvableModelException(ex, groupId, artifactId, version);
        }

        File outFile;
        try {
            if (!realVersion.equals(version)) {
                outFile = HttpUtils.getPomCacheFile(cacheDir, groupId, artifactId, version, realVersion);
            } else {
                outFile = HttpUtils.getPomCacheFile(cacheDir, groupId, artifactId, version);
            }
        } catch (IOException ex) {
            throw new UnresolvableModelException(ex, groupId, artifactId, version);
        }
        if (outFile.exists()) {
            return new URLModelSource(outFile.getAbsolutePath(), outFile);
        }

        if (proxy != null) {
            try {
                if (!realVersion.equals(version)) {
                    return new URLModelSource(repositoryUrl, HttpUtils.tryDownloadPom(outFile, proxy, groupId, artifactId, version, realVersion));
                } else {
                    return new URLModelSource(repositoryUrl, HttpUtils.tryDownloadPom(outFile, proxy, groupId, artifactId, realVersion));
                }
            } catch (IOException ignored) {
            }
        }
        try {
            if (!realVersion.equals(version)) {
                return new URLModelSource(repositoryUrl, HttpUtils.tryDownloadPom(outFile, repositoryUrl, groupId, artifactId, version, realVersion));
            } else {
                return new URLModelSource(repositoryUrl, HttpUtils.tryDownloadPom(outFile, repositoryUrl, groupId, artifactId, realVersion));
            }
        } catch (IOException ignored) {
        }

        for (Repository repository : repositories) {
            try {
                if (!realVersion.equals(version)) {
                    return new URLModelSource(repository.getUrl(), HttpUtils.tryDownloadPom(outFile, repository.getUrl(), groupId, artifactId, version, realVersion));
                } else {
                    return new URLModelSource(repository.getUrl(), HttpUtils.tryDownloadPom(outFile, repository.getUrl(), groupId, artifactId, realVersion));
                }
            } catch (IOException ignored) {
            }
        }
        throw new UnresolvableModelException("Artifact was not found in any provided repository", groupId, artifactId, version);
    }

    @Override
    @NotNull
    public ModelSource resolveModel(@NotNull Parent parent) throws UnresolvableModelException {
        try {
            parent.setRelativePath(HttpUtils.getPomCacheFile(cacheDir, parent.getGroupId(), parent.getArtifactId(), parent.getVersion()).getPath());
        } catch (IOException ex) {
            throw new UnresolvableModelException(ex, parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
        }
        return resolveModel(parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
    }

    @Override
    @NotNull
    public ModelSource resolveModel(@NotNull Dependency dependency) throws UnresolvableModelException {
        return resolveModel(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
    }

    @Override
    public void addRepository(@NotNull Repository repository) throws InvalidRepositoryException {
        addRepository(repository, false);
    }

    @Override
    public void addRepository(@NotNull Repository repository, boolean replace) throws InvalidRepositoryException {
        Repository proxyRepo;
        try {
            proxyRepo = HttpUtils.proxy(repository, proxies);
        } catch (IOException ex) {
            throw new InvalidRepositoryException("Repository does not have a valid URL: " + repository.getUrl(), repository, ex);
        }
        if (repository.equals(proxyRepo)) {
            proxyRepo = null;
        }

        if (!replace) {
            repositoryLock.writeLock().lock();
            try {
                for (Repository r : repositories) {
                    if (Objects.equals(repository.getId(), r.getId())) {
                        return;
                    }
                }
                if (proxyRepo != null && !repositories.contains(proxyRepo)) {
                    repositories.add(proxyRepo);
                }
                repositories.add(repository);
            } finally {
                repositoryLock.writeLock().unlock();
            }
            return;
        }

        repositoryLock.writeLock().lock();
        try {
            int index = -1;
            for (int i = 0; i < repositories.size(); i++) {
                if (Objects.equals(repository.getId(), repositories.get(i).getId())) {
                    index = i;
                    break;
                }
            }
            if (index == -1) {
                if (proxyRepo != null && !repositories.contains(proxyRepo)) {
                    repositories.add(proxyRepo);
                }
                repositories.add(repository);
            } else {
                repositories.set(index, repository);
                if (proxyRepo != null && !repositories.contains(proxyRepo)) {
                    repositories.add(index, proxyRepo);
                }
            }
        } finally {
            repositoryLock.writeLock().unlock();
        }
    }

    @Override
    @NotNull
    public ModelResolver newCopy() {
        URLModelResolver retVal = new URLModelResolver(cacheDir, repositoryUrl, proxies);
        repositoryLock.readLock().lock();
        try {
            for (Repository repository : repositories) {
                retVal.repositories.add(repository.clone());
            }
        } finally {
            repositoryLock.readLock().unlock();
        }
        return retVal;
    }

    private static class URLModelSource implements ModelSource {
        private @NotNull String url;
        private @NotNull File file;

        public URLModelSource(@NotNull String url, @NotNull File file) {
            this.url = url;
            this.file = file;
        }

        @Override
        @NotNull
        public InputStream getInputStream() throws IOException { return new FileInputStream(file); }

        @Override
        @NotNull
        public String getLocation() { return url; }
    }
}
