package dev.fusemc.marshal;

import com.manchickas.optionated.Option;
import dev.fusemc.standard.ProxyIdentifier;
import org.jetbrains.annotations.NotNull;

public interface Resolver {

    Option<Suggester> resolve(@NotNull ProxyIdentifier identifier);
}
