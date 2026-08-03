package dev.loaderbridge.fabric.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.loader.api.LanguageAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BridgeDefaultLanguageAdapterTest {
    @BeforeEach
    void resetInvocations() {
        FixtureEntrypoint.INVOCATIONS.set(0);
    }

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

    @Test
    void createsConstructorMemberEntrypoints() throws Exception {
        var provider = BridgeModContainer.create("fixture", "1", "Fixture", List.of(), Path.of("."));

        FixtureFactory factory = LanguageAdapter.getDefault().create(provider,
                FixtureEntrypoint.class.getName() + "::<init>", FixtureFactory.class);

        factory.create().run();
        assertThat(FixtureEntrypoint.INVOCATIONS).hasValue(1);
    }

    @FunctionalInterface
    public interface FixtureFactory {
        FixtureEntrypoint create();
    }

    public static final class FixtureEntrypoint implements Runnable {
        static final AtomicInteger INVOCATIONS = new AtomicInteger();
        public static final Runnable FIELD = INVOCATIONS::incrementAndGet;
        @Override public void run() { INVOCATIONS.incrementAndGet(); }
        public static void staticRun() { INVOCATIONS.incrementAndGet(); }
        public void instanceRun() { INVOCATIONS.incrementAndGet(); }
    }
}
