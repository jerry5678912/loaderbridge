package dev.loaderbridge.fixture.mixin;

import dev.loaderbridge.fixture.StandardMixinTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(StandardMixinTarget.class)
public interface StandardMixinTargetAccess {
    @Accessor("secret")
    int loaderbridge$getSecret();

    @Invoker("hidden")
    int loaderbridge$invokeHidden(int addend);
}
