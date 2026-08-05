package dev.loaderbridge.fabric.api.tag.v1.mixin;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dev.loaderbridge.fabric.api.tag.v1.FabricTagJson;
import java.io.Reader;
import net.minecraft.tags.TagLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TagLoader.class)
public abstract class TagLoaderMixin {
    @Redirect(method = "load", at = @At(value = "INVOKE",
            target = "Lcom/google/gson/JsonParser;parseReader(Ljava/io/Reader;)Lcom/google/gson/JsonElement;"))
    private JsonElement loaderbridge$translateFabricRemove(Reader reader) {
        return FabricTagJson.translateRemove(JsonParser.parseReader(reader));
    }
}
