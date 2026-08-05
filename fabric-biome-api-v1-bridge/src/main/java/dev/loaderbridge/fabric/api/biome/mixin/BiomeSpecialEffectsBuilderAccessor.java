package dev.loaderbridge.fabric.api.biome.mixin;

import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.biome.AmbientAdditionsSettings;
import net.minecraft.world.level.biome.AmbientMoodSettings;
import net.minecraft.world.level.biome.AmbientParticleSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BiomeSpecialEffects.Builder.class)
public interface BiomeSpecialEffectsBuilderAccessor {
    @Accessor("foliageColorOverride")
    void loaderbridge$setFoliageColorOverride(Optional<Integer> value);

    @Accessor("grassColorOverride")
    void loaderbridge$setGrassColorOverride(Optional<Integer> value);

    @Accessor("ambientParticle")
    void loaderbridge$setAmbientParticle(Optional<AmbientParticleSettings> value);

    @Accessor("ambientLoopSoundEvent")
    void loaderbridge$setAmbientLoopSound(Optional<Holder<SoundEvent>> value);

    @Accessor("ambientMoodSettings")
    void loaderbridge$setAmbientMoodSound(Optional<AmbientMoodSettings> value);

    @Accessor("ambientAdditionsSettings")
    void loaderbridge$setAmbientAdditionsSound(Optional<AmbientAdditionsSettings> value);

    @Accessor("backgroundMusic")
    void loaderbridge$setBackgroundMusic(Optional<Music> value);
}
