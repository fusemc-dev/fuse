package dev.fusemc.pql.path;

import com.manchickas.jet.Jet;
import com.manchickas.jet.template.Template;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
@HostAccess.Implementable
public interface Updater {

    @NotNull Template<Updater> TEMPLATE = Jet.function(Updater.class, "(current: any) => any");

    @HostAccess.Export
    @NotNull Value update(@NotNull Value current);

    @HostAccess.Export
    default Value updateMissing() {
        return this.update(Jet.undefined());
    }
}
