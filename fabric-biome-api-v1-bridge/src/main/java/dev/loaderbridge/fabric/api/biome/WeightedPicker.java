package dev.loaderbridge.fabric.api.biome;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

final class WeightedPicker<T> {
    private final List<Entry<T>> entries = new ArrayList<>();
    private double totalWeight;

    void add(T value, double weight) {
        totalWeight += weight;
        entries.add(new Entry<>(value, totalWeight));
    }

    int size() {
        return entries.size();
    }

    T pick(SimplexNoise noise, double x, double z) {
        double target = Mth.clamp(Math.abs(noise.getValue(x, 0.0, z)), 0.0, 1.0)
                * totalWeight;
        int low = 0;
        int high = entries.size() - 1;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (target < entries.get(middle).upperBound()) high = middle;
            else low = middle + 1;
        }
        return entries.get(low).value();
    }

    <U> WeightedPicker<U> map(Function<T, U> mapper) {
        WeightedPicker<U> mapped = new WeightedPicker<>();
        double lowerBound = 0.0;
        for (Entry<T> entry : entries) {
            mapped.add(mapper.apply(entry.value()), entry.upperBound() - lowerBound);
            lowerBound = entry.upperBound();
        }
        return mapped;
    }

    private record Entry<T>(T value, double upperBound) { }
}
