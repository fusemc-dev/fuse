package com.manchickas.fuse.lifecycle;

import com.manchickas.jet.Jet;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import org.graalvm.polyglot.Value;

import java.util.stream.Stream;

public final class JSOps implements DynamicOps<Value> {

    private JSOps() {
    }

    @Override
    public Value empty() {
        return Jet.undefined();
    }

    @Override
    public <U> U convertTo(DynamicOps<U> dynamicOps, Value value) {
        return null;
    }

    @Override
    public DataResult<Number> getNumberValue(Value value) {
        return null;
    }

    @Override
    public Value createNumeric(Number number) {
        return null;
    }

    @Override
    public DataResult<String> getStringValue(Value value) {
        return null;
    }

    @Override
    public Value createString(String s) {
        return null;
    }

    @Override
    public DataResult<Value> mergeToList(Value value, Value t1) {
        return null;
    }

    @Override
    public DataResult<Value> mergeToMap(Value value, Value t1, Value t2) {
        return null;
    }

    @Override
    public DataResult<Stream<Pair<Value, Value>>> getMapValues(Value value) {
        return null;
    }

    @Override
    public Value createMap(Stream<Pair<Value, Value>> stream) {
        return null;
    }

    @Override
    public DataResult<Stream<Value>> getStream(Value value) {
        return null;
    }

    @Override
    public Value createList(Stream<Value> stream) {
        return null;
    }

    @Override
    public Value remove(Value value, String s) {
        return null;
    }
}
