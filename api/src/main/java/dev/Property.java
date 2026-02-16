package dev;

import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Property implements ProxyExecutable, ProxyObject {

    private final @NotNull String identifier;
    private @Nullable Value value;

    public Property(
            @NotNull String identifier,
            @Nullable Value initial
    ) {
        this.identifier = identifier;
        this.value = initial;
    }

    @Override
    public Object execute(Value... arguments) {
        return switch (arguments.length) {
            case 0 -> Property.this.value;
            case 1 -> (Property.this.value = arguments[0]);
            default -> throw new UnsupportedOperationException();
        };
    }

    @Override
    @HostAccess.Export
    public String toString() {
        return String.format("%s[%s]", this.identifier, this.value);
    }

    @Override
    public Object getMember(String key) {
        return switch (key) {
            case "name" -> this.identifier;
            case "toString" -> (ProxyExecutable) _ -> this.toString();
            default -> throw new UnsupportedOperationException();
        };
    }

    @Override
    public Object getMemberKeys() {
        return new String[] { "name", "toString" };
    }

    @Override
    public boolean hasMember(String key) {
        return key.equals("name") || key.equals("toString");
    }

    @Override
    public void putMember(String key, Value value) {
        throw new UnsupportedOperationException("putMember() not supported.");
    }
}
