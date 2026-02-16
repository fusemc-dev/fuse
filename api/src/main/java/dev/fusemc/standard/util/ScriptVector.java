package dev.fusemc.standard.util;

import com.manchickas.jet.Jet;
import com.manchickas.jet.template.Template;
import org.graalvm.polyglot.HostAccess;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class ScriptVector {

    public static final Template<ScriptVector> TEMPLATE = Jet.union(
            Jet.direct(ScriptVector.class),
            Jet.tuple(
                    Jet.DOUBLE.element(pos -> pos.x),
                    Jet.DOUBLE.element(pos -> pos.y),
                    Jet.DOUBLE.element(pos -> pos.z),
                    ScriptVector::new
            )
    );

    public final @HostAccess.Export double x;
    public final @HostAccess.Export double y;
    public final @HostAccess.Export double z;

    private ScriptVector(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @HostAccess.Export
    public @NotNull ScriptVector up(double distance) {
        return new ScriptVector(this.x, this.y + distance, this.z);
    }

    @HostAccess.Export
    public @NotNull ScriptVector down(double distance) {
        return new ScriptVector(this.x, this.y - distance, this.z);
    }

    @HostAccess.Export
    public @NotNull ScriptVector north(double distance) {
        return new ScriptVector(this.x, this.y, this.z - distance);
    }

    @HostAccess.Export
    public @NotNull ScriptVector south(double distance) {
        return new ScriptVector(this.x, this.y, this.z + distance);
    }

    @HostAccess.Export
    public @NotNull ScriptVector east(double distance) {
        return new ScriptVector(this.x + distance, this.y, this.z);
    }
    
    @HostAccess.Export
    public @NotNull ScriptVector west(double distance) {
        return new ScriptVector(this.x - distance, this.y, this.z);
    }

    @HostAccess.Export
    public double dot(@NotNull ScriptVector other) {
        Objects.requireNonNull(other);
        return this.x * other.x + this.y * other.y + this.z * other.z;
    }

    @HostAccess.Export
    public @NotNull ScriptVector cross(@NotNull ScriptVector other) {
        Objects.requireNonNull(other);
        return new ScriptVector(
                this.y * other.z - this.z * other.y,
                this.z * other.x - this.x * other.z,
                this.x * other.y - this.y * other.x
        );
    }

    @HostAccess.Export
    public @NotNull ScriptVector mul(@NotNull ScriptVector other) {
        Objects.requireNonNull(other);
        return new ScriptVector(this.x * other.x, this.y * other.y, this.z * other.z);
    }

    @HostAccess.Export
    public @NotNull ScriptVector normalize() {
        var len = this.length();
        if (len == 0)
            return new ScriptVector(0, 0, 0);
        return new ScriptVector(this.x / len, this.y / len, this.z / len);
    }

    @HostAccess.Export
    public double squaredDistanceTo(@NotNull ScriptVector other) {
        Objects.requireNonNull(other);
        return Math.pow(this.x - other.x, 2)
                + Math.pow(this.y - other.y, 2)
                + Math.pow(this.z - other.z, 2);
    }

    @HostAccess.Export
    public double distanceTo(@NotNull ScriptVector other) {
        Objects.requireNonNull(other);
        return Math.sqrt(this.squaredDistanceTo(other));
    }

    @HostAccess.Export
    public double angleTo(@NotNull ScriptVector other) {
        Objects.requireNonNull(other);
        var len = this.length() * other.length();
        if (len == 0)
            return Double.NaN;
        return Math.acos(this.dot(other) / len);
    }

    @HostAccess.Export
    public double length() {
        return Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
    }

    public @NotNull String toString() {
        return String.format("(%.2f, %.2f, %.2f)", this.x, this.y, this.z);
    }
}
