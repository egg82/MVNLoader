package ninja.egg82.mvn;

import me.lucko.jarrelocator.Relocation;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Objects;
import java.util.regex.Pattern;

public class RelocationBuilder implements Serializable {
    private final @NotNull String pattern;
    private final @NotNull String relocatedPrefix;

    public RelocationBuilder(@NotNull String pattern, @NotNull String relocatedPrefix) {
        this.pattern = pattern;

        if (relocatedPrefix.charAt(relocatedPrefix.length() - 1) != '.') {
            relocatedPrefix = relocatedPrefix + ".";
        }
        this.relocatedPrefix = relocatedPrefix;
    }

    @NotNull
    public Relocation build(char find, char replace) {
        String replaced = pattern.replace(find, replace);
        return new Relocation(replaced, relocatedPrefix + replaced);
    }

    @NotNull
    public Relocation build(@NotNull String find, @NotNull String replace) {
        String replaced = pattern.replace(find, replace);
        return new Relocation(replaced, relocatedPrefix + replaced);
    }

    @NotNull
    public Relocation build(@NotNull Pattern find, @NotNull String replace) {
        String replaced = find.matcher(pattern).replaceAll(replace);
        return new Relocation(replaced, relocatedPrefix + replaced);
    }

    @NotNull
    public Relocation build() {
        return new Relocation(pattern, relocatedPrefix + pattern);
    }

    @NotNull
    public String getPattern() { return pattern; }

    @NotNull
    public String getRelocatedPrefix() { return relocatedPrefix; }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RelocationBuilder that = (RelocationBuilder) o;
        return pattern.equals(that.pattern) && relocatedPrefix.equals(that.relocatedPrefix);
    }

    @Override
    public int hashCode() { return Objects.hash(pattern, relocatedPrefix); }

    @Override
    public String toString() {
        return "RelocationBuilder{" +
                "pattern='" + pattern + '\'' +
                ", relocatedPrefix='" + relocatedPrefix + '\'' +
                '}';
    }
}
