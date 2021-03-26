package ninja.egg82.mvn;

import ninja.egg82.mvn.internal.HttpUtils;
import ninja.egg82.mvn.internal.URLModelResolver;
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

    public JarBuilder(@NotNull String groupId, @NotNull String artifactId, @NotNull String version) {
        this(groupId, artifactId, version, null, null);
    }

    public JarBuilder(@NotNull String groupId, @NotNull String artifactId, @NotNull String version, @Nullable String repositoryUrl) {
        this(groupId, artifactId, version, repositoryUrl, null);
    }

    public JarBuilder(@NotNull String groupId, @NotNull String artifactId, @NotNull String version, @Nullable Logger logger) {
        this(groupId, artifactId, version, null, logger);
    }

    public JarBuilder(@NotNull String groupId, @NotNull String artifactId, @NotNull String version, @Nullable String repositoryUrl, @Nullable Logger logger) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.repositoryUrl = repositoryUrl != null ? repositoryUrl : "https://repo.maven.apache.org/maven2";

        this.logger = logger != null ? logger : LoggerFactory.getLogger(getClass());
    }

    @NotNull
    public JarBuilder setRepositoryProxy(@NotNull String repositoryUrl, @NotNull String proxyUrl) throws IOException {
        proxies.put(HttpUtils.simplify(repositoryUrl), proxyUrl);
        return this;
    }

    @NotNull
    public JarBuilder clone(@NotNull String groupId, @NotNull String artifactId, @NotNull String version) {
        JarBuilder retVal = new JarBuilder(groupId, artifactId, version, repositoryUrl, logger);
        retVal.proxies.putAll(proxies);
        return retVal;
    }

    @Nullable
    public Model build(@NotNull File cacheDir) throws IOException, ModelBuildingException {
        File pomFile = downloadPom(cacheDir);
        if (pomFile == null) {
            return null;
        }

        ModelBuildingRequest request = new DefaultModelBuildingRequest();
        request.setProcessPlugins(true);
        request.setPomFile(pomFile);
        request.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
        request.setSystemProperties(System.getProperties());
        request.setModelResolver(new URLModelResolver(cacheDir, repositoryUrl, proxies));

        Model retVal = MODEL_BUILDER.build(request).getEffectiveModel();
        retVal.setRepositories(HttpUtils.proxy(retVal.getRepositories(), proxies));
        retVal.setPluginRepositories(HttpUtils.proxy(retVal.getPluginRepositories(), proxies));
        return retVal;
    }

    @Nullable
    private File downloadPom(@NotNull File cacheDir) throws IOException {
        String proxy = proxies.get(HttpUtils.simplify(repositoryUrl));

        String realVersion = HttpUtils.getRealVersion(repositoryUrl, proxy, groupId, artifactId, version, logger);
        File outFile;
        if (version.toLowerCase(Locale.ROOT).endsWith("-snapshot")) {
            outFile = HttpUtils.getPomCacheFile(cacheDir, groupId, artifactId, version, realVersion);
        } else {
            outFile = HttpUtils.getPomCacheFile(cacheDir, groupId, artifactId, realVersion);
        }
        if (outFile.exists()) {
            return outFile;
        }

        if (proxy != null) {
            try {
                if (version.toLowerCase(Locale.ROOT).endsWith("-snapshot")) {
                    return HttpUtils.tryDownloadPom(outFile, proxy, groupId, artifactId, version, realVersion);
                } else {
                    return HttpUtils.tryDownloadPom(outFile, proxy, groupId, artifactId, realVersion);
                }
            } catch (IOException ex) {
                logger.warn("Could not download artifact POM from proxy URL " + proxy, ex);
            }
        }
        try {
            if (version.toLowerCase(Locale.ROOT).endsWith("-snapshot")) {
                return HttpUtils.tryDownloadPom(outFile, repositoryUrl, groupId, artifactId, version, realVersion);
            } else {
                return HttpUtils.tryDownloadPom(outFile, repositoryUrl, groupId, artifactId, realVersion);
            }
        } catch (IOException ex) {
            logger.warn("Could not download artifact POM from repository URL " + repositoryUrl, ex);
        }
        return null;
    }
}
