package ninja.egg82.mvn.internal.compressors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;

public abstract class AbstractCompressor {
    protected static final @NotNull Pattern RE_COMMA = Pattern.compile("\\s*,\\s*");

    @NotNull
    public abstract InputStream decompress(@NotNull InputStream in, @Nullable String contentEncoding) throws IOException;
}
