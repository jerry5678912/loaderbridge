package dev.loaderbridge.fabric.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.loader.api.LanguageAdapterException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Mirrors the entrypoint shapes in Fabric Language Kotlin 1.12.3's adapter tests. */
class BridgeKotlinLanguageAdapterTest {
    private final BridgeModContainer provider =
            BridgeModContainer.create("fixture", "1", "Fixture", List.of(), Path.of("."));
    private final BridgeKotlinLanguageAdapter adapter = BridgeKotlinLanguageAdapter.INSTANCE;

    @BeforeEach
    void resetInvocations() {
        INVOCATIONS.set(0);
    }

    @Test
    void adaptsOrdinaryClassEntrypoint() throws Exception {
        adapter.create(provider, ClassShape.class.getName(), Runnable.class).run();

        assertThat(INVOCATIONS).hasValue(1);
    }

    @Test
    void adaptsKotlinObjectClassFunctionAndPropertyJvmShapes() throws Exception {
        assertThat(adapter.create(provider, ObjectShape.class.getName(), Runnable.class))
                .isSameAs(ObjectShape.INSTANCE);
        adapter.create(provider, ObjectShape.class.getName() + "::initializer", Runnable.class).run();
        adapter.create(provider, ObjectShape.class.getName() + "::init", Runnable.class).run();

        assertThat(INVOCATIONS).hasValue(2);
    }

    @Test
    void adaptsKotlinCompanionClassFunctionAndPropertyJvmShapes() throws Exception {
        String companion = CompanionShape.Companion.class.getName();

        assertThat(adapter.create(provider, companion, Runnable.class))
                .isSameAs(CompanionShape.Companion);
        adapter.create(provider, companion + "::initializer", Runnable.class).run();
        adapter.create(provider, companion + "::init", Runnable.class).run();

        assertThat(INVOCATIONS).hasValue(2);
    }

    @Test
    void delegatesTopLevelFunctionJvmShapeToDefaultAdapter() throws Exception {
        adapter.create(provider, TopLevelShape.class.getName() + "::init", Runnable.class).run();

        assertThat(INVOCATIONS).hasValue(1);
    }

    @Test
    void rejectsEmptyMemberReference() {
        assertThatThrownBy(() -> adapter.create(
                provider, ObjectShape.class.getName() + "::", Runnable.class))
                .isInstanceOf(LanguageAdapterException.class)
                .hasMessageContaining("Invalid handle format");
    }

    static final AtomicInteger INVOCATIONS = new AtomicInteger();

    public static final class ClassShape implements Runnable {
        @Override public void run() { INVOCATIONS.incrementAndGet(); }
    }

    public static final class ObjectShape implements Runnable {
        public static final ObjectShape INSTANCE = new ObjectShape();
        private final Runnable initializer = () -> {
            throw new AssertionError("The Kotlin property getter must be used");
        };
        private ObjectShape() {}
        @Override public void run() { INVOCATIONS.incrementAndGet(); }
        public void init() { INVOCATIONS.incrementAndGet(); }
        public Runnable getInitializer() { return INVOCATIONS::incrementAndGet; }
    }

    public static final class CompanionShape {
        public static final Companion Companion = new Companion();

        public static final class Companion implements Runnable {
            private final Runnable initializer = () -> {
                throw new AssertionError("The Kotlin companion property getter must be used");
            };
            private Companion() {}
            @Override public void run() { INVOCATIONS.incrementAndGet(); }
            public void init() { INVOCATIONS.incrementAndGet(); }
            public Runnable getInitializer() { return INVOCATIONS::incrementAndGet; }
        }
    }

    public static final class TopLevelShape {
        private TopLevelShape() {}
        public static void init() { INVOCATIONS.incrementAndGet(); }
    }
}
