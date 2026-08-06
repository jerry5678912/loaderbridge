package dev.loaderbridge.fabric.api.dimension;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.codecs.BaseMapCodec;
import java.util.Map;

/** Decodes valid map entries and reports malformed entries without failing the whole map. */
public record FailSoftMapCodec<K, V>(Codec<K> keyCodec, Codec<V> elementCodec)
        implements BaseMapCodec<K, V>, Codec<Map<K, V>> {
    @Override public <T> DataResult<Pair<Map<K, V>, T>> decode(
            DynamicOps<T> operations, T input) {
        return operations.getMap(input).setLifecycle(Lifecycle.stable())
                .flatMap(map -> decode(operations, map))
                .map(result -> Pair.of(result, input));
    }

    @Override public <T> DataResult<T> encode(
            Map<K, V> input, DynamicOps<T> operations, T prefix) {
        return encode(input, operations, operations.mapBuilder()).build(prefix);
    }

    @Override public <T> DataResult<Map<K, V>> decode(
            DynamicOps<T> operations, MapLike<T> input) {
        ImmutableMap.Builder<K, V> result = ImmutableMap.builder();
        input.entries().forEach(entry -> {
            try {
                var key = keyCodec.parse(operations, entry.getFirst()).result();
                var value = elementCodec.parse(operations, entry.getSecond()).result();
                if (key.isPresent() && value.isPresent()) result.put(key.get(), value.get());
            } catch (RuntimeException ignored) {
                // One hostile or obsolete entry must not invalidate the remaining dimensions.
            }
        });
        return DataResult.success(result.build());
    }

    @Override public String toString() {
        return "LoaderBridgeFailSoftMapCodec[" + keyCodec + " -> " + elementCodec + ']';
    }
}
