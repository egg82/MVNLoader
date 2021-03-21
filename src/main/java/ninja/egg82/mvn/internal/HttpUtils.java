package ninja.egg82.mvn.internal;

import com.nixxcode.jvmbrotli.common.BrotliLoader;
import com.nixxcode.jvmbrotli.dec.BrotliInputStream;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.model.Repository;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.DeflaterInputStream;
import java.util.zip.GZIPInputStream;

public class HttpUtils {
    private static final boolean HAS_BROTLI;

    static {
        boolean hasBrotli = true;
        try {
            Class.forName("com.nixxcode.jvmbrotli.common.BrotliLoader");
        } catch (ClassNotFoundException ignored) {
            hasBrotli = false;
        }
        if (hasBrotli) {
            hasBrotli = BrotliLoader.isBrotliAvailable();
        }
        HAS_BROTLI = hasBrotli;
    }

    private static final @NotNull Pattern RE_COMMA = Pattern.compile("\\s*,\\s*");

    private HttpUtils() { }

    @NotNull
    public static File getJarCacheFile(@NotNull File cacheDir, @NotNull String groupId, @NotNull String artifactId, @NotNull String version) throws IOException {
        File retVal = new File(
                new File(new File(new File(cacheDir, groupId.replace('.', File.separatorChar)), artifactId), version),
                artifactId + "-" + version + ".jar"
        );
        ensureCacheFileStability(retVal);
        return retVal;
    }

    @NotNull
    public static File getJarCacheFile(
            @NotNull File cacheDir,
            @NotNull String groupId,
            @NotNull String artifactId,
            @NotNull String version,
            @NotNull String realVersion
    ) throws IOException {
        File retVal = new File(
                new File(new File(new File(cacheDir, groupId.replace('.', File.separatorChar)), artifactId), version),
                artifactId + "-" + realVersion + ".jar"
        );
        ensureCacheFileStability(retVal);
        return retVal;
    }

    @NotNull
    public static File getRelocatedJarCacheFile(@NotNull File cacheDir, @NotNull String groupId, @NotNull String artifactId, @NotNull String version) throws IOException {
        File retVal = new File(
                new File(new File(new File(cacheDir, groupId.replace('.', File.separatorChar)), artifactId), version),
                artifactId + "-" + version + "-relocated.jar"
        );
        ensureCacheFileStability(retVal);
        return retVal;
    }

    @NotNull
    public static File getRelocatedJarCacheFile(
            @NotNull File cacheDir,
            @NotNull String groupId,
            @NotNull String artifactId,
            @NotNull String version,
            @NotNull String realVersion
    ) throws IOException {
        File retVal = new File(
                new File(new File(new File(cacheDir, groupId.replace('.', File.separatorChar)), artifactId), version),
                artifactId + "-" + realVersion + "-relocated.jar"
        );
        ensureCacheFileStability(retVal);
        return retVal;
    }

    @NotNull
    public static File getPomCacheFile(@NotNull File cacheDir, @NotNull String groupId, @NotNull String artifactId, @NotNull String version) throws IOException {
        File retVal = new File(
                new File(new File(new File(cacheDir, groupId.replace('.', File.separatorChar)), artifactId), version),
                artifactId + "-" + version + ".pom"
        );
        ensureCacheFileStability(retVal);
        return retVal;
    }

    @NotNull
    public static File getPomCacheFile(
            @NotNull File cacheDir,
            @NotNull String groupId,
            @NotNull String artifactId,
            @NotNull String version,
            @NotNull String realVersion
    ) throws IOException {
        File retVal = new File(
                new File(new File(new File(cacheDir, groupId.replace('.', File.separatorChar)), artifactId), version),
                artifactId + "-" + realVersion + ".pom"
        );
        ensureCacheFileStability(retVal);
        return retVal;
    }

    private static void ensureCacheFileStability(@NotNull File cacheFile) throws IOException {
        if (cacheFile.exists() && cacheFile.isFile()) {
            return;
        }

        File parent = cacheFile.getParentFile();
        if (parent.exists() && !parent.isDirectory()) {
            Files.delete(parent.toPath());
        }
        if (!parent.exists() && !parent.mkdirs()) {
            throw new IOException("Could not create directory structure for " + parent.getAbsolutePath());
        }

        if (cacheFile.exists()) {
            try (Stream<Path> walker = Files.walk(cacheFile.toPath())) {
                walker
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
            if (cacheFile.exists()) {
                throw new IOException("Could not create directory structure for " + cacheFile.getAbsolutePath());
            }
        }
    }

    @NotNull
    public static String simplify(@NotNull String url) throws IOException {
        URL retVal = new URL(url);
        String path = retVal.getPath();
        if (path.length() > 0 && (path.charAt(path.length() - 1) == '/' || path.charAt(path.length() - 1) == '\\')) {
            path = path.substring(0, path.length() - 1);
        }
        return retVal.getHost() != null ? retVal.getHost() + path : path;
    }

    @NotNull
    public static List<@NotNull Repository> proxy(
            @NotNull List<@NotNull Repository> repositories,
            @NotNull Map<@NotNull String, @Nullable String> proxies
    ) throws IOException {
        List<@NotNull Repository> retVal = new ArrayList<>();
        Set<@NotNull String> repositoryUrls = new HashSet<>();
        for (Repository repository : repositories) {
            String simplified = simplify(repository.getUrl());
            String proxy = proxies.get(simplified);
            if (proxy != null) {
                if (!repositoryUrls.add(simplify(proxy))) {
                    continue;
                }
                Repository proxyRepository = repository.clone();
                if (proxyRepository.getId() != null) {
                    proxyRepository.setId(proxyRepository.getId() + "-proxy");
                }
                if (proxyRepository.getName() != null) {
                    proxyRepository.setName("Proxy for " + proxyRepository.getName());
                }
                proxyRepository.setUrl(proxy);
                retVal.add(proxyRepository);
            }
            if (!repositoryUrls.add(simplified)) {
                continue;
            }
            retVal.add(repository);
        }
        return retVal;
    }

    @NotNull
    public static Repository proxy(@NotNull Repository repository, @NotNull Map<@NotNull String, @Nullable String> proxies) throws IOException {
        String proxy = proxies.get(simplify(repository.getUrl()));
        if (proxy != null) {
            Repository proxyRepository = repository.clone();
            if (proxyRepository.getId() != null) {
                proxyRepository.setId(proxyRepository.getId() + "-proxy");
            }
            if (proxyRepository.getName() != null) {
                proxyRepository.setName("Proxy for " + proxyRepository.getName());
            }
            proxyRepository.setUrl(proxy);
            return proxyRepository;
        }
        return repository;
    }

    @NotNull
    public static File tryDownloadJar(
            @NotNull File outFile,
            @NotNull String repositoryUrl,
            @NotNull String groupId,
            @NotNull String artifactId,
            @NotNull String version
    ) throws IOException {
        if (repositoryUrl.length() > 0 && repositoryUrl.charAt(repositoryUrl.length() - 1) != '/') {
            repositoryUrl += '/';
        }
        repositoryUrl += groupId.replace('.', '/') + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version + ".jar";

        return tryDownloadFile(outFile, repositoryUrl);
    }

    @NotNull
    public static File tryDownloadJar(
            @NotNull File outFile,
            @NotNull String repositoryUrl,
            @NotNull String groupId,
            @NotNull String artifactId,
            @NotNull String version,
            @NotNull String realVersion
    ) throws IOException {
        if (repositoryUrl.length() > 0 && repositoryUrl.charAt(repositoryUrl.length() - 1) != '/') {
            repositoryUrl += '/';
        }
        repositoryUrl += groupId.replace('.', '/') + "/" + artifactId + "/" + version + "/" + artifactId + "-" + realVersion + ".jar";

        return tryDownloadFile(outFile, repositoryUrl);
    }

    @NotNull
    public static File tryDownloadPom(
            @NotNull File outFile,
            @NotNull String repositoryUrl,
            @NotNull String groupId,
            @NotNull String artifactId,
            @NotNull String version
    ) throws IOException {
        if (repositoryUrl.length() > 0 && repositoryUrl.charAt(repositoryUrl.length() - 1) != '/') {
            repositoryUrl += '/';
        }
        repositoryUrl += groupId.replace('.', '/') + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version + ".pom";

        return tryDownloadFile(outFile, repositoryUrl);
    }

    @NotNull
    public static File tryDownloadPom(
            @NotNull File outFile,
            @NotNull String repositoryUrl,
            @NotNull String groupId,
            @NotNull String artifactId,
            @NotNull String version,
            @NotNull String realVersion
    ) throws IOException {
        if (repositoryUrl.length() > 0 && repositoryUrl.charAt(repositoryUrl.length() - 1) != '/') {
            repositoryUrl += '/';
        }
        repositoryUrl += groupId.replace('.', '/') + "/" + artifactId + "/" + version + "/" + artifactId + "-" + realVersion + ".pom";

        return tryDownloadFile(outFile, repositoryUrl);
    }

    @NotNull
    public static File tryDownloadFile(@NotNull File outFile, @NotNull String fileUrl) throws IOException {
        URL url = new URL(fileUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setInstanceFollowRedirects(true);
        conn.setConnectTimeout(3500);
        conn.setReadTimeout(5000);
        conn.setDoInput(true);
        conn.setDoOutput(false);

        conn.setRequestProperty("Accept-Language", "en-US,en;q=0.8");
        conn.setRequestProperty("Accept-Encoding", HAS_BROTLI ? "br,gzip,deflate;q=0.8" : "gzip,deflate;q=0.8");
        conn.setRequestProperty("User-Agent", "egg82/MVNLoader");

        throwOnStandardErrors(conn);

        try (
                InputStream in = decompress(conn.getInputStream(), conn.getHeaderField("Content-Encoding"));
                FileOutputStream out = new FileOutputStream(outFile)
        ) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
        }

        return outFile;
    }

    @NotNull
    public static Metadata tryGetMetadata(
            @NotNull String repositoryUrl,
            @NotNull String groupId,
            @NotNull String artifactId,
            @NotNull String version
    ) throws IOException {
        if (repositoryUrl.length() > 0 && repositoryUrl.charAt(repositoryUrl.length() - 1) != '/') {
            repositoryUrl += '/';
        }
        repositoryUrl += groupId.replace('.', '/') + "/" + artifactId + "/" + version + "/maven-metadata.xml";

        URL url = new URL(repositoryUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setInstanceFollowRedirects(true);
        conn.setConnectTimeout(3500);
        conn.setReadTimeout(5000);
        conn.setDoInput(true);
        conn.setDoOutput(false);

        conn.setRequestProperty("Accept-Language", "en-US,en;q=0.8");
        conn.setRequestProperty("Accept-Encoding", HAS_BROTLI ? "br,gzip,deflate;q=0.8" : "gzip,deflate;q=0.8");
        conn.setRequestProperty("User-Agent", "egg82/MVNLoader");

        throwOnStandardErrors(conn);

        try (InputStream in = decompress(conn.getInputStream(), conn.getHeaderField("Content-Encoding"))) {
            return new MetadataXpp3Reader().read(in);
        } catch (XmlPullParserException ex) {
            throw new IOException("Could not parse maven-metadata XML file " + repositoryUrl, ex);
        }
    }

    @NotNull
    public static String getRealVersion(@NotNull String repositoryUrl, @Nullable String proxy, @NotNull String groupId, @NotNull String artifactId, @NotNull String version, @Nullable Logger logger) throws IOException {
        if (version.toLowerCase(Locale.ROOT).endsWith("-snapshot")) {
            Metadata metadata = null;
            if (proxy != null) {
                try {
                    metadata = tryGetMetadata(proxy, groupId, artifactId, version);
                } catch (IOException ex) {
                    if (logger != null) {
                        logger.warn("Could not download metadata XML from proxy URL " + proxy, ex);
                    }
                }
            }
            if (metadata == null) {
                metadata = tryGetMetadata(repositoryUrl, groupId, artifactId, version);
            }

            return version.substring(0, version.lastIndexOf('-')) + "-" + metadata.getVersioning()
                    .getSnapshot()
                    .getTimestamp() + "-" + metadata.getVersioning().getSnapshot().getBuildNumber();
        }
        return version;
    }

    @NotNull
    private static InputStream decompress(@NotNull InputStream in, @Nullable String contentEncoding) throws IOException {
        contentEncoding = contentEncoding != null ? contentEncoding.trim() : "";
        if (contentEncoding.trim().isEmpty()) {
            return in;
        }
        String[] encoding = RE_COMMA.split(contentEncoding);
        InputStream retVal = in;
        for (String e : encoding) {
            switch (e) {
                case "br":
                    if (!HAS_BROTLI) {
                        throw new IOException("Brotli compression given but Brotli is not available.");
                    }
                    retVal = new BrotliInputStream(retVal);
                    break;
                case "gzip":
                    retVal = new GZIPInputStream(retVal);
                    break;
                case "deflate":
                    retVal = new DeflaterInputStream(retVal);
                    break;
                default:
                    break;
            }
        }
        return retVal;
    }

    private static void throwOnStandardErrors(@NotNull HttpURLConnection connection) throws IOException {
        int status = connection.getResponseCode();

        if (status >= 200 && status < 300) {
            if (status == HttpURLConnection.HTTP_RESET) {
                throw new IOException("Could not get connection (HTTP status " + status + " - reset connection) " + connection.getURL());
            }
        } else if (status >= 300 && status < 400) {
            if (status != HttpURLConnection.HTTP_MOVED_TEMP && status != HttpURLConnection.HTTP_MOVED_PERM && status != HttpURLConnection.HTTP_SEE_OTHER) {
                throw new IOException("Could not get connection (HTTP status " + status + " - reset connection) " + connection.getURL());
            }
        } else if (status >= 400 && status < 500) {
            if (status == HttpURLConnection.HTTP_UNAUTHORIZED || status == HttpURLConnection.HTTP_FORBIDDEN) {
                throw new IOException("Could not get connection (HTTP status " + status + " - access denied) " + connection.getURL());
            }
            if (status == 429) { // Too many queries
                throw new IOException("Could not get connection (HTTP status " + status + " - too many queries, temporary issue) " + connection.getURL());
            }
            if (status == 404) { // Not found
                throw new IOException("Could not get connection (HTTP status " + status + " - not found) " + connection.getURL());
            }
            throw new IOException("Could not get connection (HTTP status " + status + ") " + connection.getURL());
        } else if (status >= 500 && status < 600) { // Server errors (usually temporary)
            throw new IOException("Could not get connection (HTTP status " + status + " - remote server issue) " + connection.getURL());
        } else {
            throw new IOException("Could not get connection (HTTP status " + status + ") " + connection.getURL());
        }
    }
}
