package ninja.egg82.mvn;

import ninja.egg82.mvn.internal.HttpUtils;
import ninja.egg82.mvn.internal.URLModelResolver;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class JarBuilder {
    private final @NotNull Logger logger;

    private final @NotNull String groupId;
    private final @NotNull String artifactId;
    private final @NotNull String version;
    private final @NotNull String repositoryUrl;

    private final @NotNull Map<@NotNull String, @Nullable String> proxies = new HashMap<>();

    private static final ModelBuilder MODEL_BUILDER = new DefaultModelBuilderFactory().newInstance();

    public JarBuilder(@NotNull String group, @NotNull String artifact, @NotNull String version, @NotNull String repositoryUrl) {
        this(group, artifact, version, repositoryUrl, null);
    }

    public JarBuilder(@NotNull String groupId, @NotNull String artifactId, @NotNull String version, @NotNull String repositoryUrl, @Nullable Logger logger) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.repositoryUrl = repositoryUrl;

        this.logger = logger != null ? logger : LoggerFactory.getLogger(getClass());
    }

    @NotNull
    public JarBuilder setRepositoryProxy(@NotNull String repositoryUrl, @NotNull String proxyUrl) throws IOException {
        proxies.put(HttpUtils.simplify(repositoryUrl), proxyUrl);
        return this;
    }

    public @NotNull JarBuilder clone(@NotNull String groupId, @NotNull String artifactId, @NotNull String version) {
        JarBuilder retVal = new JarBuilder(groupId, artifactId, version, repositoryUrl, logger);
        retVal.proxies.putAll(proxies);
        return retVal;
    }

    @NotNull
    public Model build(@NotNull File cacheDir) throws IOException, ModelBuildingException {
        ModelBuildingRequest request = new DefaultModelBuildingRequest();
        request.setProcessPlugins(true);
        request.setPomFile(downloadPom(cacheDir));
        request.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
        request.setSystemProperties(System.getProperties());
        request.setModelResolver(new URLModelResolver(cacheDir, repositoryUrl, proxies));

        Model retVal = MODEL_BUILDER.build(request).getEffectiveModel();
        retVal.setRepositories(HttpUtils.proxy(retVal.getRepositories(), proxies));
        retVal.setPluginRepositories(HttpUtils.proxy(retVal.getPluginRepositories(), proxies));
        return retVal;
    }

    @NotNull
    private File downloadPom(@NotNull File cacheDir) throws IOException {
        String proxy = proxies.get(HttpUtils.simplify(repositoryUrl));

        String realVersion = HttpUtils.getRealVersion(repositoryUrl, proxy, groupId, artifactId, version, logger);
        File outFile;
        if (!realVersion.equals(version)) {
            outFile = HttpUtils.getPomCacheFile(cacheDir, groupId, artifactId, version, realVersion);
        } else {
            outFile = HttpUtils.getPomCacheFile(cacheDir, groupId, artifactId, version);
        }
        if (outFile.exists()) {
            return outFile;
        }

        if (proxy != null) {
            try {
                if (!realVersion.equals(version)) {
                    return HttpUtils.tryDownloadPom(outFile, proxy, groupId, artifactId, version, realVersion);
                } else {
                    return HttpUtils.tryDownloadPom(outFile, proxy, groupId, artifactId, realVersion);
                }
            } catch (IOException ex) {
                logger.warn("Could not download artifact POM from proxy URL " + proxy, ex);
            }
        }
        if (!realVersion.equals(version)) {
            return HttpUtils.tryDownloadPom(outFile, repositoryUrl, groupId, artifactId, version, realVersion);
        } else {
            return HttpUtils.tryDownloadPom(outFile, repositoryUrl, groupId, artifactId, realVersion);
        }
    }
}