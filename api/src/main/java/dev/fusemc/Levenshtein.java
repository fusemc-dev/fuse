package dev.fusemc;

import com.manchickas.optionated.Option;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Objects;

public abstract class Levenshtein {

    private Levenshtein() {
        throw new UnsupportedOperationException();
    }

    public static @NotNull Option<Identifier> findClosestCandidate(@NotNull Registry<?> registry,
                                                                   @NotNull Identifier query) {
        Objects.requireNonNull(registry);
        Objects.requireNonNull(query);
        return registry.keySet()
                .stream()
                .filter(id -> {
                    var nd = Levenshtein.distance(id.getNamespace(), query.getNamespace());
                    var pd = Levenshtein.distance(id.getPath(), query.getPath());
                    if (nd < 2)
                        return pd < 3;
                    return pd < 2;
                })
                .sorted(Comparator.comparingInt(a -> Levenshtein.distance(a.toString(), query.toString())))
                .limit(1)
                .findFirst()
                .map(Option::some)
                .orElseGet(Option::none);
    }

    public static int distance(@NotNull String a, @NotNull String b) {
        Objects.requireNonNull(a);
        Objects.requireNonNull(b);
        var xs = new int[b.length() + 1];
        var ys = new int[b.length() + 1];

        for (var i = 0; i <= b.length(); i++)
            xs[i] = i;
        for (var i = 0; i < a.length(); i++) {
            ys[0] = i + 1;
            for (var j = 0; j < b.length(); j++) {
                var deletion = xs[j + 1] + 1;
                var insertion = ys[j] + 1;
                var substitution = a.charAt(i) == b.charAt(j)
                        ? xs[j]
                        : xs[j] + 1;
                ys[j + 1] = Math.min(insertion, Math.min(deletion, substitution));
            }
            System.arraycopy(ys, 0, xs, 0, xs.length);
        }
        return xs[b.length()];
    }
}
