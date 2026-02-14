package com.manchickas.fuse.standard.entity.living;

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import com.manchickas.fuse.ValueOps;
import com.manchickas.jet.Jet;
import com.manchickas.jet.Scope;
import com.manchickas.jet.exception.TypeException;
import com.manchickas.jet.template.Template;
import com.manchickas.optionated.Option;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.level.ServerPlayer;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

public final class ScriptPlayer extends ScriptLivingEntity<ServerPlayer> {

    private static final @NotNull Template<Component> TEXT = new Template<>() {

        @Override
        public @NotNull Option<Component> parse(@NotNull Value value) {
            Objects.requireNonNull(value);
            return ComponentSerialization.CODEC.parse(ValueOps.instance(), value)
                    .mapOrElse(Option::some, __ -> Option.none());
        }

        @Override
        public @NotNull Option<Value> serialize(@NotNull Component value) {
            Objects.requireNonNull(value);
            return ComponentSerialization.CODEC.encodeStart(ValueOps.instance(), value)
                    .mapOrElse(Option::some, __ -> Option.none());
        }

        @Override
        public String describe(Scope<Template<?>> visited) {
            return "Text";
        }
    };
    private static final Interner<ScriptPlayer> INTERNER =
            Interners.newWeakInterner();

    private ScriptPlayer(@NonNull ServerPlayer wrapped) {
        super(wrapped);
    }

    public static ScriptPlayer wrap(@Nullable ServerPlayer player) {
        if (player != null)
            return ScriptPlayer.INTERNER.intern(new ScriptPlayer(player));
        return null;
    }

    @HostAccess.Export
    public void sendMessage(@NotNull Value value) throws TypeException {
        this.wrapped.sendSystemMessage(Jet.expect(TEXT, value));
    }
}
