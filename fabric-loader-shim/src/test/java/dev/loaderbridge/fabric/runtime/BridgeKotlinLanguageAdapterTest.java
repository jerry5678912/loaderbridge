package dev.loaderbridge.fabric.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class BridgeKotlinLanguageAdapterTest {
    @Test
    void adaptsKotlinObjectClassPropertyAndMemberMethodJvmShapes() throws Exception {
        var provider = BridgeModContainer.create("fixture", "1", "Fixture", List.of(), Path.of("."));
        var adapter = BridgeKotlinLanguageAdapter.INSTANCE;

        assertThat(adapter.create(provider, ObjectShape.class.getName(), Runnable.class))
                .isSameAs(ObjectShape.INSTANCE);
        adapter.create(provider, ObjectShape.class.getName() + "::callback", Runnable.class).run();
        adapter.create(provider, ObjectShape.class.getName() + "::invoke", Runnable.class).run();

        assertThat(ObjectShape.INVOCATIONS).hasValue(2);
    }

    public static final class ObjectShape implements Runnable {
        static final AtomicInteger INVOCATIONS = new AtomicInteger();
        public static final ObjectShape INSTANCE = new ObjectShape();
        private final Runnable callback = INVOCATIONS::incrementAndGet;
        private ObjectShape() {}
        @Override public void run() { INVOCATIONS.incrementAndGet(); }
        public void invoke() { INVOCATIONS.incrementAndGet(); }
    }
}
