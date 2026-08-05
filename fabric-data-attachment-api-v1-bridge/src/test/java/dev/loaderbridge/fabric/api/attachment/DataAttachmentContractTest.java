package dev.loaderbridge.fabric.api.attachment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mojang.serialization.Codec;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.impl.attachment.AttachmentRegistryImpl;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class DataAttachmentContractTest {
    @Test
    void providerPinsTheInitialPublicSurfaceAndTargetMixin() throws Exception {
        var descriptor = new FabricDataAttachmentBridgeProvider().descriptor();
        assertThat(descriptor.contractVersion())
                .isEqualTo("fabric-data-attachment-api-v1:1.4.7");
        assertThat(descriptor.implementationVersion())
                .isEqualTo("1.4.7+5b36e0f719-loaderbridge.1");
        assertThat(descriptor.providedClasses()).contains(
                "net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry",
                "net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate",
                "net.fabricmc.fabric.api.attachment.v1.AttachmentTarget",
                "net.fabricmc.fabric.api.attachment.v1.AttachmentType");
        try (var stream = getClass().getResourceAsStream(
                "/loaderbridge.fabric-data-attachment-api-v1.mixins.json")) {
            assertThat(stream).isNotNull();
            assertThat(new String(stream.readAllBytes(), StandardCharsets.UTF_8))
                    .contains("AttachmentTargetsMixin");
        }
    }

    @Test
    void registryBuilderPreservesThePinnedContract() {
        AtomicInteger defaults = new AtomicInteger();
        AttachmentType<Integer> type = AttachmentRegistry.create(
                ResourceLocation.fromNamespaceAndPath("loaderbridge", "counter"),
                builder -> builder.initializer(defaults::incrementAndGet)
                        .persistent(Codec.INT).copyOnDeath());

        assertThat(type.identifier().toString()).isEqualTo("loaderbridge:counter");
        assertThat(type.initializer()).isNotNull();
        assertThat(type.isPersistent()).isTrue();
        assertThat(type.isSynced()).isFalse();
        assertThat(type.copyOnDeath()).isTrue();
        assertThat(AttachmentRegistryImpl.get(type.identifier())).isSameAs(type);
    }

    @Test
    void targetConvenienceMethodsMatchFabricSemantics() {
        AttachmentType<Integer> type = AttachmentRegistry.createDefaulted(
                ResourceLocation.fromNamespaceAndPath("loaderbridge", "target"), () -> 4);
        TestTarget target = new TestTarget();

        assertThat(target.hasAttached(type)).isFalse();
        assertThat(target.getAttachedOrElse(type, 2)).isEqualTo(2);
        assertThat(target.getAttachedOrCreate(type)).isEqualTo(4);
        assertThat(target.modifyAttached(type, value -> value + 3)).isEqualTo(4);
        assertThat(target.getAttachedOrThrow(type)).isEqualTo(7);
        assertThat(target.removeAttached(type)).isEqualTo(7);
        assertThatThrownBy(() -> target.getAttachedOrThrow(type))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("No value was attached");
    }

    @Test
    void builtInSyncPredicatesUseTargetIdentity() {
        TestTarget target = new TestTarget();
        assertThat(AttachmentSyncPredicate.all().test(target, null)).isTrue();
        assertThat(AttachmentSyncPredicate.targetOnly().test(target, null)).isFalse();
        assertThat(AttachmentSyncPredicate.allButTarget().test(target, null)).isTrue();
    }

    private static final class TestTarget implements AttachmentTarget {
        private final java.util.IdentityHashMap<AttachmentType<?>, Object> values =
                new java.util.IdentityHashMap<>();

        @Override @SuppressWarnings("unchecked")
        public <A> A getAttached(AttachmentType<A> type) { return (A) values.get(type); }

        @Override @SuppressWarnings("unchecked")
        public <A> A setAttached(AttachmentType<A> type, A value) {
            return (A) (value == null ? values.remove(type) : values.put(type, value));
        }

        @Override public boolean hasAttached(AttachmentType<?> type) {
            return values.containsKey(type);
        }
    }
}
