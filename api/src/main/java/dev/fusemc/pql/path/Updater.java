package dev.fusemc.pql.path;

import dev.fusemc.tau.Tau;
import dev.fusemc.tau.Template;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
@HostAccess.Implementable
public interface Updater {

    @NotNull Template<Updater> TEMPLATE = Template.functional(Updater.class, Template.ANY);

    @HostAccess.Export
    @NotNull Value update(@NotNull Value current);

    @HostAccess.Export
    default Value updateMissing() {
        return this.update(Tau.undefined());
    }
}
