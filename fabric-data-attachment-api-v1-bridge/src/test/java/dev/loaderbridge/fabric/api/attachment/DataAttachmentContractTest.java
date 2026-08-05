package dev.loaderbridge.fabric.api.attachment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mojang.serialization.Codec;
import io.netty.buffer.Unpooled;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.impl.attachment.AttachmentRegistryImpl;
import net.fabricmc.fabric.impl.attachment.AttachmentSerialization;
import net.fabricmc.fabric.impl.attachment.AttachmentTargetImpl;
import net.fabricmc.fabric.impl.attachment.sync.AttachmentChange;
import net.fabricmc.fabric.impl.attachment.sync.AttachmentTargetInfo;
import net.fabricmc.fabric.impl.attachment.sync.s2c.AttachmentSyncPayloadS2C;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import org.junit.jupiter.api.Test;

class DataAttachmentContractTest {
    @Test
    void providerPinsTheInitialPublicSurfaceAndTargetMixin() throws Exception {
        assertThat(FabricDataAttachmentBridgeMod.class.getAnnotation(Mod.class).value())
                .isEqualTo("loaderbridge_fabric_data_attachment_api_v1");
        var descriptor = new FabricDataAttachmentBridgeProvider().descriptor();
        assertThat(descriptor.contractVersion())
                .isEqualTo("fabric-data-attachment-api-v1:1.4.7");
        assertThat(descriptor.implementationVersion())
                .isEqualTo("1.4.7+5b36e0f719-loaderbridge.8");
        assertThat(descriptor.providedClasses()).contains(
                "net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry",
                "net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate",
                "net.fabricmc.fabric.api.attachment.v1.AttachmentTarget",
                "net.fabricmc.fabric.api.attachment.v1.AttachmentType");
        try (var stream = getClass().getResourceAsStream(
                "/loaderbridge.fabric-data-attachment-api-v1.mixins.json")) {
            assertThat(stream).isNotNull();
            assertThat(new String(stream.readAllBytes(), StandardCharsets.UTF_8))
                    .contains("AttachmentTargetsMixin", "ConnectionMixin",
                            "ServerEntityPairingMixin");
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

    @Test
    void persistenceRoundTripsOnlyCodecBackedValues() {
        AttachmentType<Integer> persistent = AttachmentRegistry.createPersistent(
                ResourceLocation.fromNamespaceAndPath("loaderbridge", "persistent"), Codec.INT);
        AttachmentType<Integer> transientType = AttachmentRegistry.create(
                ResourceLocation.fromNamespaceAndPath("loaderbridge", "transient"));
        java.util.IdentityHashMap<AttachmentType<?>, Object> values =
                new java.util.IdentityHashMap<>();
        values.put(persistent, 19);
        values.put(transientType, 23);
        CompoundTag root = new CompoundTag();

        AttachmentSerialization.write(root, NbtOps.INSTANCE, values);
        var restored = AttachmentSerialization.read(root, NbtOps.INSTANCE);

        assertThat(root.contains(AttachmentTarget.NBT_ATTACHMENT_KEY)).isTrue();
        assertThat(restored).containsOnlyKeys(persistent);
        assertThat(restored.get(persistent)).isEqualTo(19);
        assertThat(AttachmentSerialization.hasPersistent(values)).isTrue();
    }

    @Test
    void deathTransferCopiesOnlyOptedInAttachmentTypes() {
        AttachmentType<Integer> ordinary = AttachmentRegistry.create(
                ResourceLocation.fromNamespaceAndPath("loaderbridge", "ordinary"));
        AttachmentType<Integer> retained = AttachmentRegistry.create(
                ResourceLocation.fromNamespaceAndPath("loaderbridge", "retained"),
                AttachmentRegistry.Builder::copyOnDeath);
        TestTarget original = new TestTarget();
        TestTarget replacement = new TestTarget();
        original.setAttached(ordinary, 1);
        original.setAttached(retained, 2);

        AttachmentTargetImpl.transfer(original, replacement, true);

        assertThat(replacement.getAttached(ordinary)).isNull();
        assertThat(replacement.getAttached(retained)).isEqualTo(2);
    }

    @Test
    void synchronizedAttachmentPayloadRoundTripsTargetTypeAndValue() {
        AttachmentType<Integer> type = AttachmentRegistry.create(
                ResourceLocation.fromNamespaceAndPath("loaderbridge", "sync_round_trip"),
                builder -> builder.syncWith(ByteBufCodecs.VAR_INT,
                        AttachmentSyncPredicate.all()));
        var payload = new AttachmentSyncPayloadS2C(java.util.List.of(
                new AttachmentChange(AttachmentTargetInfo.LevelTarget.INSTANCE, type, 73)));
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);

        AttachmentSyncPayloadS2C.CODEC.encode(buffer, payload);
        AttachmentSyncPayloadS2C restored = AttachmentSyncPayloadS2C.CODEC.decode(buffer);

        assertThat(restored.attachments()).hasSize(1);
        assertThat(restored.attachments().getFirst().targetInfo())
                .isEqualTo(AttachmentTargetInfo.LevelTarget.INSTANCE);
        assertThat(restored.attachments().getFirst().type()).isSameAs(type);
        assertThat(restored.attachments().getFirst().value()).isEqualTo(73);
    }

    private static final class TestTarget implements AttachmentTargetImpl {
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

        @Override public java.util.IdentityHashMap<AttachmentType<?>, Object>
                fabric_getAttachments() { return values; }

        @Override public void fabric_setAttachments(
                java.util.IdentityHashMap<AttachmentType<?>, Object> attachments) {
            values.clear();
            if (attachments != null) values.putAll(attachments);
        }
    }
}
