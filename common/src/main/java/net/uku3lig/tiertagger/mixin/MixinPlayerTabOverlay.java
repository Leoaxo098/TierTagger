package net.uku3lig.tiertagger.mixin;

import net.uku3lig.tiertagger.TierTagger;
import net.uku3lig.tiertagger.config.TierTaggerConfig;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.UUID;

@Mixin(PlayerTabOverlay.class)
public class MixinPlayerTabOverlay {
    @ModifyReturnValue(method = "getNameForDisplay", at = @At("RETURN"))
    @Nullable
    public Component prependTier(Component original, PlayerInfo entry) {
        TierTaggerConfig config = TierTagger.getManager().getConfig();
        if (config.isEnabled() && config.isPlayerList()) {
            UUID uuid = entry.getProfile().id();
            String name = entry.getProfile().name();
            return TierTagger.appendTier(uuid, name, original);
        } else {
            return original;
        }
    }
}
