package dev.loaderbridge.fixture.mixin;

import dev.loaderbridge.fixture.StandardMixinTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(StandardMixinTarget.class)
abstract class StandardInjectionFamiliesMixin {
    @Shadow
    private int secret;

    @Inject(method = "injected", at = @At("HEAD"), cancellable = true)
    private void loaderbridge$inject(CallbackInfoReturnable<String> callback) {
        callback.setReturnValue("injected");
    }

    @ModifyArg(method = "modifyArg", at = @At(value = "INVOKE",
            target = "Ldev/loaderbridge/fixture/StandardMixinTarget;twice(I)I"), index = 0)
    private int loaderbridge$modifyArg(int value) {
        return value + 1;
    }

    @ModifyArgs(method = "modifyArgs", at = @At(value = "INVOKE",
            target = "Ldev/loaderbridge/fixture/StandardMixinTarget;combine(II)I"))
    private void loaderbridge$modifyArgs(Args arguments) {
        arguments.set(0, 4);
        arguments.set(1, 5);
    }

    @ModifyVariable(method = "modifyVariable", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int loaderbridge$modifyVariable(int value) {
        return value + 10;
    }

    @ModifyConstant(method = "modifyConstant", constant = @Constant(intValue = 5))
    private int loaderbridge$modifyConstant(int value) {
        return 9;
    }

    @Redirect(method = "redirect", at = @At(value = "INVOKE",
            target = "Ldev/loaderbridge/fixture/StandardMixinTarget;originalRedirect()I"))
    private int loaderbridge$redirect(StandardMixinTarget target) {
        return 7;
    }

    /** @author LoaderBridge fixture @reason Runtime proof of standard overwrite application. */
    @Overwrite
    public String overwrite() {
        return secret == 4 ? "overwritten" : "invalid-shadow";
    }
}
