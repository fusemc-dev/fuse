package dev.fusemc.disastrous.event;

import com.manchickas.jet.Jet;
import com.manchickas.jet.template.Template;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;

@HostAccess.Implementable
public interface EventCallback {

    @FunctionalInterface
    @HostAccess.Implementable
    interface Unbound extends EventCallback {

        Template<Unbound> TEMPLATE = Jet.function(Unbound.class, "(...args: any) => any");

        @NotNull Value onEvent(@NotNull Value... args);
    }
}
