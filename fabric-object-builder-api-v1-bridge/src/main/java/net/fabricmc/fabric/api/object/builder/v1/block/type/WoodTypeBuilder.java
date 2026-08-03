package net.fabricmc.fabric.api.object.builder.v1.block.type;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;

/** Binary-compatible builder for Minecraft wood-type behavior records. */
public final class WoodTypeBuilder {
    private SoundType soundGroup = SoundType.WOOD;
    private SoundType hangingSignSoundGroup = SoundType.HANGING_SIGN;
    private SoundEvent fenceGateCloseSound = SoundEvents.FENCE_GATE_CLOSE;
    private SoundEvent fenceGateOpenSound = SoundEvents.FENCE_GATE_OPEN;

    public WoodTypeBuilder soundGroup(SoundType value) { soundGroup = value; return this; }
    public WoodTypeBuilder hangingSignSoundGroup(SoundType value) { hangingSignSoundGroup = value; return this; }
    public WoodTypeBuilder fenceGateCloseSound(SoundEvent value) { fenceGateCloseSound = value; return this; }
    public WoodTypeBuilder fenceGateOpenSound(SoundEvent value) { fenceGateOpenSound = value; return this; }

    public static WoodTypeBuilder copyOf(WoodTypeBuilder source) {
        return new WoodTypeBuilder().soundGroup(source.soundGroup)
                .hangingSignSoundGroup(source.hangingSignSoundGroup)
                .fenceGateCloseSound(source.fenceGateCloseSound)
                .fenceGateOpenSound(source.fenceGateOpenSound);
    }

    public static WoodTypeBuilder copyOf(WoodType source) {
        return new WoodTypeBuilder().soundGroup(source.soundType())
                .hangingSignSoundGroup(source.hangingSignSoundType())
                .fenceGateCloseSound(source.fenceGateClose())
                .fenceGateOpenSound(source.fenceGateOpen());
    }

    public WoodType register(ResourceLocation id, BlockSetType setType) {
        return WoodType.register(build(id, setType));
    }

    public WoodType build(ResourceLocation id, BlockSetType setType) {
        return new WoodType(id.toString(), setType, soundGroup, hangingSignSoundGroup,
                fenceGateCloseSound, fenceGateOpenSound);
    }
}
