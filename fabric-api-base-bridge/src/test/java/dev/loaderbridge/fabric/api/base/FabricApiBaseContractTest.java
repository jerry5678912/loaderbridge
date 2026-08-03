package dev.loaderbridge.fabric.api.base;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class FabricApiBaseContractTest {
    @Test
    void arrayBackedEventUsesEmptySingleAndCombinedInvokers() {
        Runnable empty = () -> {};
        Event<Runnable> event = EventFactory.createArrayBacked(Runnable.class, empty,
                listeners -> () -> {
                    for (Runnable listener : listeners) listener.run();
                });
        List<String> calls = new ArrayList<>();
        assertThat(event.invoker()).isSameAs(empty);
        Runnable first = () -> calls.add("first");
        event.register(first);
        assertThat(event.invoker()).isSameAs(first);
        event.register(() -> calls.add("second"));
        event.invoker().run();
        assertThat(calls).containsExactly("first", "second");
    }

    @Test
    void phasesHonorDeclaredOrderingRegardlessOfRegistrationOrder() {
        ResourceLocation early = ResourceLocation.fromNamespaceAndPath("test", "early");
        ResourceLocation late = ResourceLocation.fromNamespaceAndPath("test", "late");
        Event<Runnable> event = EventFactory.createWithPhases(Runnable.class,
                listeners -> () -> {
                    for (Runnable listener : listeners) listener.run();
                }, early, Event.DEFAULT_PHASE, late);
        List<String> calls = new ArrayList<>();
        event.register(late, () -> calls.add("late"));
        event.register(early, () -> calls.add("early"));
        event.register(() -> calls.add("default"));
        event.invoker().run();
        assertThat(calls).containsExactly("early", "default", "late");
    }

    @Test
    void phaseValidationMatchesFabricContract() {
        ResourceLocation phase = ResourceLocation.fromNamespaceAndPath("test", "phase");
        assertThatThrownBy(() -> EventFactory.createWithPhases(
                Runnable.class, listeners -> () -> {}, phase))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DEFAULT_PHASE");
        assertThatThrownBy(() -> EventFactory.createWithPhases(
                Runnable.class, listeners -> () -> {}, Event.DEFAULT_PHASE, Event.DEFAULT_PHASE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate");
    }

    @Test
    void triStateMatchesDefaultAndConcreteSemantics() {
        assertThat(TriState.of((Boolean) null)).isEqualTo(TriState.DEFAULT);
        assertThat(TriState.TRUE.getBoxed()).isTrue();
        assertThat(TriState.FALSE.orElse(true)).isFalse();
        assertThat(TriState.DEFAULT.orElseGet(() -> true)).isTrue();
        assertThat(TriState.TRUE.map(value -> value ? "yes" : "no")).contains("yes");
        assertThat(TriState.DEFAULT.map(value -> "unused")).isEmpty();
        assertThatThrownBy(() -> TriState.DEFAULT.orElseThrow(() -> new IllegalStateException("unset")))
                .isInstanceOf(IllegalStateException.class).hasMessage("unset");
    }
}
