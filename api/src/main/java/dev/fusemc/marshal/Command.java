package dev.fusemc.marshal;

import dev.fusemc.marshal.standard.ProxySource;
import dev.fusemc.tau.Template;
import dev.fusemc.tau.proxy.ObjectLike;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface Command {

    @NotNull Template<@NotNull Command> TEMPLATE = Template.functional(Command.class, Template.UNDEFINED);

    void onCommand(@NotNull ProxySource source, @NotNull Value arguments);
}
