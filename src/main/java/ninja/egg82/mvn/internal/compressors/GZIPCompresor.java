package ninja.egg82.mvn.internal.compressors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.DeflaterInputStream;
import java.util.zip.GZIPInputStream;

public class GZIPCompresor extends AbstractCompressor {
    @Override
    public @NotNull InputStream decompress(@NotNull InputStream in, @Nullable String contentEncoding) throws IOException {
        contentEncoding = contentEncoding != null ? contentEncoding.trim() : "";
        if (contentEncoding.trim().isEmpty()) {
            return in;
        }
        String[] encoding = RE_COMMA.split(contentEncoding);
        InputStream retVal = in;
        for (String e : encoding) {
            switch (e) {
                case "br":
                    throw new IOException("Brotli compression given but Brotli is not available.");
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
}