package dev.loaderbridge.fabric.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.loader.api.LanguageAdapter;
import org.junit.jupiter.api.Test;

class BridgeDefaultLanguageAdapterTest {
    @Test
    void createsClassFieldStaticMethodAndInstanceMethodEntrypoints() throws Exception {
        var provider = BridgeModContainer.create("fixture", "1", "Fixture", List.of(), Path.of("."));
        LanguageAdapter adapter = LanguageAdapter.getDefault();

        adapter.create(provider, FixtureEntrypoint.class.getName(), Runnable.class).run();
        adapter.create(provider, FixtureEntrypoint.class.getName() + "::FIELD", Runnable.class).run();
        adapter.create(provider, FixtureEntrypoint.class.getName() + "::staticRun", Runnable.class).run();
        adapter.create(provider, FixtureEntrypoint.class.getName() + "::instanceRun", Runnable.class).run();

        assertThat(FixtureEntrypoint.INVOCATIONS).hasValue(4);
    }

    public static final class FixtureEntrypoint implements Runnable {
        static final AtomicInteger INVOCATIONS = new AtomicInteger();
        public static final Runnable FIELD = INVOCATIONS::incrementAndGet;
        @Override public void run() { INVOCATIONS.incrementAndGet(); }
        public static void staticRun() { INVOCATIONS.incrementAndGet(); }
        public void instanceRun() { INVOCATIONS.incrementAndGet(); }
    }
}
