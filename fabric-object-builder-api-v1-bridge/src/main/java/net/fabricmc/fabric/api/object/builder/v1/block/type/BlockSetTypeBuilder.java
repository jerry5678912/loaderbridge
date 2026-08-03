package net.fabricmc.fabric.api.object.builder.v1.block.type;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.BlockSetType;

/** Binary-compatible builder for Minecraft block-set behavior records. */
public final class BlockSetTypeBuilder {
    private boolean openableByHand = true;
    private boolean openableByWindCharge = true;
    private boolean buttonActivatedByArrows = true;
    private BlockSetType.PressurePlateSensitivity pressurePlateActivationRule =
            BlockSetType.PressurePlateSensitivity.EVERYTHING;
    private SoundType soundGroup = SoundType.STONE;
    private SoundEvent doorCloseSound = SoundEvents.IRON_DOOR_CLOSE;
    private SoundEvent doorOpenSound = SoundEvents.IRON_DOOR_OPEN;
    private SoundEvent trapdoorCloseSound = SoundEvents.IRON_TRAPDOOR_CLOSE;
    private SoundEvent trapdoorOpenSound = SoundEvents.IRON_TRAPDOOR_OPEN;
    private SoundEvent pressurePlateClickOffSound = SoundEvents.STONE_PRESSURE_PLATE_CLICK_OFF;
    private SoundEvent pressurePlateClickOnSound = SoundEvents.STONE_PRESSURE_PLATE_CLICK_ON;
    private SoundEvent buttonClickOffSound = SoundEvents.STONE_BUTTON_CLICK_OFF;
    private SoundEvent buttonClickOnSound = SoundEvents.STONE_BUTTON_CLICK_ON;

    public BlockSetTypeBuilder openableByHand(boolean value) { openableByHand = value; return this; }
    public BlockSetTypeBuilder openableByWindCharge(boolean value) { openableByWindCharge = value; return this; }
    public BlockSetTypeBuilder buttonActivatedByArrows(boolean value) { buttonActivatedByArrows = value; return this; }
    public BlockSetTypeBuilder pressurePlateActivationRule(BlockSetType.PressurePlateSensitivity value) { pressurePlateActivationRule = value; return this; }
    public BlockSetTypeBuilder soundGroup(SoundType value) { soundGroup = value; return this; }
    public BlockSetTypeBuilder doorCloseSound(SoundEvent value) { doorCloseSound = value; return this; }
    public BlockSetTypeBuilder doorOpenSound(SoundEvent value) { doorOpenSound = value; return this; }
    public BlockSetTypeBuilder trapdoorCloseSound(SoundEvent value) { trapdoorCloseSound = value; return this; }
    public BlockSetTypeBuilder trapdoorOpenSound(SoundEvent value) { trapdoorOpenSound = value; return this; }
    public BlockSetTypeBuilder pressurePlateClickOffSound(SoundEvent value) { pressurePlateClickOffSound = value; return this; }
    public BlockSetTypeBuilder pressurePlateClickOnSound(SoundEvent value) { pressurePlateClickOnSound = value; return this; }
    public BlockSetTypeBuilder buttonClickOffSound(SoundEvent value) { buttonClickOffSound = value; return this; }
    public BlockSetTypeBuilder buttonClickOnSound(SoundEvent value) { buttonClickOnSound = value; return this; }

    public static BlockSetTypeBuilder copyOf(BlockSetTypeBuilder source) {
        return new BlockSetTypeBuilder()
                .openableByHand(source.openableByHand)
                .openableByWindCharge(source.openableByWindCharge)
                .buttonActivatedByArrows(source.buttonActivatedByArrows)
                .pressurePlateActivationRule(source.pressurePlateActivationRule)
                .soundGroup(source.soundGroup)
                .doorCloseSound(source.doorCloseSound).doorOpenSound(source.doorOpenSound)
                .trapdoorCloseSound(source.trapdoorCloseSound)
                .trapdoorOpenSound(source.trapdoorOpenSound)
                .pressurePlateClickOffSound(source.pressurePlateClickOffSound)
                .pressurePlateClickOnSound(source.pressurePlateClickOnSound)
                .buttonClickOffSound(source.buttonClickOffSound)
                .buttonClickOnSound(source.buttonClickOnSound);
    }

    public static BlockSetTypeBuilder copyOf(BlockSetType source) {
        return new BlockSetTypeBuilder()
                .openableByHand(source.canOpenByHand())
                .openableByWindCharge(source.canOpenByWindCharge())
                .buttonActivatedByArrows(source.canButtonBeActivatedByArrows())
                .pressurePlateActivationRule(source.pressurePlateSensitivity())
                .soundGroup(source.soundType())
                .doorCloseSound(source.doorClose()).doorOpenSound(source.doorOpen())
                .trapdoorCloseSound(source.trapdoorClose()).trapdoorOpenSound(source.trapdoorOpen())
                .pressurePlateClickOffSound(source.pressurePlateClickOff())
                .pressurePlateClickOnSound(source.pressurePlateClickOn())
                .buttonClickOffSound(source.buttonClickOff()).buttonClickOnSound(source.buttonClickOn());
    }

    public BlockSetType register(ResourceLocation id) { return BlockSetType.register(build(id)); }

    public BlockSetType build(ResourceLocation id) {
        return new BlockSetType(id.toString(), openableByHand, openableByWindCharge,
                buttonActivatedByArrows, pressurePlateActivationRule, soundGroup,
                doorCloseSound, doorOpenSound, trapdoorCloseSound, trapdoorOpenSound,
                pressurePlateClickOffSound, pressurePlateClickOnSound,
                buttonClickOffSound, buttonClickOnSound);
    }
}
