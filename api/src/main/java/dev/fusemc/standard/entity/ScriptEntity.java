package dev.fusemc.standard.entity;

import com.manchickas.jet.Jet;
import com.manchickas.jet.exception.TypeException;
import com.manchickas.optionated.Option;
import dev.fusemc.Levenshtein;
import dev.fusemc.ParseException;
import dev.fusemc.ScriptException;
import dev.fusemc.standard.ScriptWrapper;
import dev.fusemc.standard.util.ScriptIdentifier;
import dev.fusemc.syringe.Syringe;
import dev.fusemc.syringe.Vaccine;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScriptEntity<E extends Entity> extends ScriptWrapper<E> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptEntity.class);

    public ScriptEntity(@NonNull E wrapped) {
        super(wrapped);
    }

    @HostAccess.Export
    public @NotNull Value sample(@NotNull Value vaccine) throws TypeException, ParseException, ScriptException {
        var query = ScriptIdentifier.expectVanilla(vaccine);
        var type = Syringe.attemptLookup(query);
        if (type instanceof Option.Some<Vaccine<?, ?>>(var wrapped)) {
            var sample = wrapped.attemptSample(this.wrapped);
            if (sample instanceof Option.Some<Value>(var value))
                return value;
            throw new ScriptException(
                    "Vaccine '%s' is not applicable to entities of type '%s'."
                            .formatted(wrapped, this.wrapped.getType().getDescriptionId())
            );
        }
        var closest = Levenshtein.findClosestCandidate(Syringe.REGISTRY, query);
        if (closest instanceof Option.Some<Identifier>(var wrapped))
            throw new ScriptException(
                    "Attempted to sample an unrecognized vaccine '%s'. Did you mean '%s'?"
                            .formatted(query, wrapped)
            );
        throw new ScriptException(
                "Attempted to sample an unrecognized vaccine '%s'.".formatted(query)
        );
    }

    @HostAccess.Export
    public void inject(@NotNull Value vaccine, @NotNull Value value) throws TypeException, ParseException, ScriptException {
        var query = ScriptIdentifier.expectVanilla(vaccine);
        var type = Syringe.attemptLookup(query);
        if (type instanceof Option.Some<Vaccine<?, ?>>(var wrapped)) {
            if (wrapped.attemptInject(this.wrapped, value))
                return;
            throw new ScriptException(
                    "Vaccine '%s' is not applicable to entities of type '%s'."
                            .formatted(wrapped, this.wrapped.getType().getDescriptionId())
            );
        }
        var closest = Levenshtein.findClosestCandidate(Syringe.REGISTRY, query);
        if (closest instanceof Option.Some<Identifier>(var wrapped))
            throw new ScriptException(
                    "Attempted to inject an unrecognized vaccine '%s'. Did you mean '%s'?"
                            .formatted(query, wrapped)
            );
        throw new ScriptException(
                "Attempted to inject an unrecognized vaccine '%s'.".formatted(query)
        );
    }
}
