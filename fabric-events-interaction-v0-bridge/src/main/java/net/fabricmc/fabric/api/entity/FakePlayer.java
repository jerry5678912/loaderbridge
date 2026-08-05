package net.fabricmc.fabric.api.entity;

import com.google.common.collect.MapMaker;
import com.mojang.authlib.GameProfile;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.UUID;
import net.fabricmc.fabric.impl.event.interaction.FakePlayerNetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.scores.PlayerTeam;
import org.jetbrains.annotations.Nullable;

/** A reusable server player that represents automation rather than a human connection. */
public class FakePlayer extends ServerPlayer {
    public static final UUID DEFAULT_UUID =
            UUID.fromString("41C82C87-7AfB-4024-BA57-13D2C99CAE77");
    private static final GameProfile DEFAULT_PROFILE =
            new GameProfile(DEFAULT_UUID, "[Minecraft]");
    private static final Map<FakePlayerKey, FakePlayer> FAKE_PLAYERS =
            new MapMaker().weakValues().makeMap();

    public static FakePlayer get(ServerLevel world) {
        return get(world, DEFAULT_PROFILE);
    }

    public static FakePlayer get(ServerLevel world, GameProfile profile) {
        Objects.requireNonNull(world, "World may not be null.");
        Objects.requireNonNull(profile, "Game profile may not be null.");
        return FAKE_PLAYERS.computeIfAbsent(
                new FakePlayerKey(world, profile),
                key -> new FakePlayer(key.world(), key.profile()));
    }

    @SuppressWarnings("this-escape")
    protected FakePlayer(ServerLevel world, GameProfile profile) {
        super(world.getServer(), world, profile, ClientInformation.createDefault());
        connection = new FakePlayerNetworkHandler(this);
    }

    @Override public void tick() { }
    @Override public void updateOptions(ClientInformation settings) { }
    @Override public void awardStat(Stat<?> stat, int amount) { }
    @Override public void resetStat(Stat<?> stat) { }
    @Override public boolean isInvulnerableTo(DamageSource damageSource) { return true; }
    @Nullable @Override public PlayerTeam getTeam() { return null; }
    @Override public void startSleeping(BlockPos pos) { }
    @Override public boolean startRiding(Entity entity, boolean force) { return false; }
    @Override public void openTextEdit(SignBlockEntity sign, boolean front) { }
    @Override public OptionalInt openMenu(@Nullable MenuProvider factory) {
        return OptionalInt.empty();
    }
    @Override public void openHorseInventory(AbstractHorse horse, Container inventory) { }

    private record FakePlayerKey(ServerLevel world, GameProfile profile) { }
}
