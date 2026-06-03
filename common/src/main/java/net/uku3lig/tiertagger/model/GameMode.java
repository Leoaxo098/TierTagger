package net.uku3lig.tiertagger.model;

import com.google.gson.JsonObject;
import net.uku3lig.tiertagger.TierList;
import net.uku3lig.tiertagger.TierTagger;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public record GameMode(String id, String title) {
    public static final GameMode NONE = new GameMode("annoying_long_id_that_no_one_will_ever_use_just_to_make_sure", "§cNone§r");

    public static final List<GameMode> VIET_STATIC_GAMEMODES = List.of(
            new GameMode("vanilla", "Vanilla"),
            new GameMode("sword", "Sword"),
            new GameMode("uhc", "UHC"),
            new GameMode("pot", "Pot"),
            new GameMode("nethop", "Nethop"),
            new GameMode("smp", "SMP"),
            new GameMode("axe", "Axe"),
            new GameMode("mace", "Mace"),
            new GameMode("spear", "Spear"),
            new GameMode("trident", "Trident")
    );

    public static CompletableFuture<List<GameMode>> fetchGamemodes(HttpClient client) {
        if (isVietTierlistActive()) {
            return CompletableFuture.completedFuture(VIET_STATIC_GAMEMODES);
        }

        String endpoint = TierTagger.getManager().getConfig().getApiUrl() + "/v2/mode/list";
        final HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint)).GET().build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(r -> {
                    JsonObject obj = TierTagger.GSON.fromJson(r.body(), JsonObject.class);

                    return obj.entrySet().stream().map(e -> {
                        String title = e.getValue().getAsJsonObject().get("title").getAsString();
                        return new GameMode(e.getKey(), title);
                    }).toList();
                });
    }

    private static boolean isVietTierlistActive() {
        return TierList.findByUrl(TierTagger.getManager().getConfig().getApiUrl())
                .map(TierList::usesNameLookup)
                .orElse(false);
    }

    public boolean isNone() {
        return this.id.equals(NONE.id);
    }

    private record IconAndColor(char icon, TextColor color) {}

    private IconAndColor iconAndColor() {
        return switch (this.id) {
            case "axe" -> new IconAndColor('', TextColor.fromLegacyFormat(ChatFormatting.GREEN));
            case "mace" -> new IconAndColor('', TextColor.fromLegacyFormat(ChatFormatting.GRAY));
            case "nethop", "neth_pot" -> new IconAndColor('', TextColor.fromRgb(0x7d4a40));
            case "pot" -> new IconAndColor('', TextColor.fromRgb(0xff0000));
            case "smp" -> new IconAndColor('', TextColor.fromRgb(0xeccb45));
            case "sword" -> new IconAndColor('', TextColor.fromRgb(0xa4fdf0));
            case "uhc" -> new IconAndColor('', TextColor.fromLegacyFormat(ChatFormatting.RED));
            case "vanilla" -> new IconAndColor('', TextColor.fromLegacyFormat(ChatFormatting.LIGHT_PURPLE));
            case "bed" -> new IconAndColor('', TextColor.fromRgb(0xff0000));
            case "bow" -> new IconAndColor('', TextColor.fromRgb(0x663d10));
            case "creeper" -> new IconAndColor('', TextColor.fromLegacyFormat(ChatFormatting.GREEN));
            case "debuff" -> new IconAndColor('', TextColor.fromLegacyFormat(ChatFormatting.DARK_GRAY));
            case "dia_crystal" -> new IconAndColor('', TextColor.fromLegacyFormat(ChatFormatting.AQUA));
            case "dia_smp" -> new IconAndColor('', TextColor.fromRgb(0x8c668b));
            case "elytra" -> new IconAndColor('', TextColor.fromRgb(0x8d8db1));
            case "manhunt" -> new IconAndColor('', TextColor.fromLegacyFormat(ChatFormatting.RED));
            case "minecart" -> new IconAndColor('', TextColor.fromLegacyFormat(ChatFormatting.GRAY));
            case "og_vanilla" -> new IconAndColor('', TextColor.fromLegacyFormat(ChatFormatting.GOLD));
            case "speed" -> new IconAndColor('', TextColor.fromRgb(0x43a9d1));
            case "trident" -> new IconAndColor('', TextColor.fromRgb(0x579b8c));
            case "spear" -> new IconAndColor('•', TextColor.fromLegacyFormat(ChatFormatting.WHITE));
            default -> new IconAndColor('•', TextColor.fromLegacyFormat(ChatFormatting.WHITE));
        };
    }

    public Optional<Character> icon() {
        IconAndColor pair = this.iconAndColor();
        return pair.color().getValue() == 0xFFFFFF ? Optional.empty() : Optional.of(pair.icon());
    }

    public Component asStyled(boolean withDefaultDot) {
        IconAndColor pair = this.iconAndColor();

        if (pair.color().getValue() == 0xFFFFFF && !withDefaultDot) {
            return Component.literal(this.title);
        } else {
            Component name = Component.literal(this.title).withStyle(s -> s.withColor(pair.color()));
            return Component.literal(pair.icon() + " ").append(name);
        }
    }
}
