package net.uku3lig.tiertagger.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.uku3lig.tiertagger.TierCache;
import net.uku3lig.tiertagger.TierTagger;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public record PlayerInfo(
        String uuid,
        String name,
        Map<String, Ranking> rankings,
        String region,
        int points,
        int overall,
        List<String> badges,
        @SerializedName("combat_master") boolean combatMaster
) {
    public record Ranking(
            int tier,
            int pos,
            @SerializedName("peak_tier") Integer peakTier,
            @SerializedName("peak_pos") Integer peakPos,
            long attained,
            boolean retired
    ) {
        public int comparableTier() {
            return tier * 2 + pos;
        }

        public int comparablePeak() {
            if (peakTier == null || peakPos == null) {
                return comparableTier();
            }
            return peakTier * 2 + peakPos;
        }

        public NamedRanking asNamed(GameMode mode) {
            return new NamedRanking(mode, this);
        }
    }

    public record NamedRanking(GameMode mode, Ranking ranking) {}

    public static CompletableFuture<Map<String, Ranking>> getRankings(HttpClient client, UUID uuid) {
        String endpoint = TierTagger.getManager().getConfig().getApiUrl() + "/v2/tiers/" + uuid.toString().replace("-", "");
        final HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint)).GET().build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(r -> {
                    JsonObject obj = TierTagger.GSON.fromJson(r.body(), JsonObject.class);
                    Map<String, Ranking> rankings = new HashMap<>();

                    for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                        JsonObject ranking = entry.getValue().getAsJsonObject();
                        int tier = ranking.get("tier").getAsInt();
                        int pos = ranking.get("pos").getAsInt();
                        Integer peakTier = ranking.has("peakTier") ? ranking.get("peakTier").getAsInt() : null;
                        Integer peakPos = ranking.has("peakPos") ? ranking.get("peakPos").getAsInt() : null;
                        long attained = ranking.has("attained") ? ranking.get("attained").getAsLong() : 0L;
                        boolean retired = ranking.has("retired") && ranking.get("retired").getAsBoolean();
                        rankings.put(entry.getKey(), new Ranking(tier, pos, peakTier, peakPos, attained, retired));
                    }
                    return rankings;
                });
    }

    public static CompletableFuture<Map<String, Ranking>> getRankingsByName(HttpClient client, String name) {
        return net.uku3lig.tiertagger.tierlist.VietTierListApi.search(client, name)
                .thenApply(net.uku3lig.tiertagger.tierlist.VietTierListConverter::toRankings);
    }

    public static CompletableFuture<PlayerInfo> search(HttpClient client, String query) {
        String endpoint = TierTagger.getManager().getConfig().getApiUrl() + "/v2/search/" + URLEncoder.encode(query, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint)).GET().build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(r -> {
                    JsonObject obj = TierTagger.GSON.fromJson(r.body(), JsonObject.class);
                    String uuid = obj.get("uuid").getAsString();
                    String name = obj.get("name").getAsString();
                    Map<String, Ranking> rankings = new HashMap<>();

                    JsonObject tiers = obj.get("tiers").getAsJsonObject();
                    for (Map.Entry<String, JsonElement> entry : tiers.entrySet()) {
                        JsonObject ranking = entry.getValue().getAsJsonObject();
                        int tier = ranking.get("tier").getAsInt();
                        int pos = ranking.get("pos").getAsInt();
                        Integer peakTier = ranking.has("peakTier") ? ranking.get("peakTier").getAsInt() : null;
                        Integer peakPos = ranking.has("peakPos") ? ranking.get("peakPos").getAsInt() : null;
                        long attained = ranking.has("attained") ? ranking.get("attained").getAsLong() : 0L;
                        boolean retired = ranking.has("retired") && ranking.get("retired").getAsBoolean();
                        rankings.put(entry.getKey(), new Ranking(tier, pos, peakTier, peakPos, attained, retired));
                    }
                    return new PlayerInfo(uuid, name, rankings, null, 0, 0, List.of(), false);
                });
    }

    public static Optional<NamedRanking> getHighestRanking(Map<String, Ranking> rankings) {
        if (rankings.isEmpty()) return Optional.empty();

        return rankings.entrySet().stream()
                .filter(e -> !e.getValue().retired())
                .max(Comparator.comparingInt(e -> e.getValue().comparableTier()))
                .map(e -> e.getValue().asNamed(TierCache.findModeOrUgly(e.getKey())));
    }

    public int getRegionColor() {
        return switch (region == null ? "" : region) {
            case "NA" -> 0xff6a6e;
            case "EU" -> 0x6aff6e;
            case "SA" -> 0xff9900;
            case "AU" -> 0xf6b26b;
            case "ME" -> 0xffd966;
            case "AS" -> 0xc27ba0;
            case "AF" -> 0x674ea7;
            default -> 0xFFFFFF;
        };
    }

    public PointInfo getPointInfo() {
        return PointInfo.fromPoints(points, combatMaster);
    }

    public List<NamedRanking> getSortedTiers() {
        List<NamedRanking> tiers = new ArrayList<>(this.rankings.entrySet().stream()
                .map(e -> e.getValue().asNamed(TierCache.findModeOrUgly(e.getKey())))
                .toList());

        tiers.sort(Comparator.comparing((NamedRanking a) -> a.ranking().retired(), Boolean::compare)
                .thenComparingInt(a -> a.ranking().tier())
                .thenComparingInt(a -> a.ranking().pos()));

        return tiers;
    }

    @Getter
    @AllArgsConstructor
    public enum PointInfo {
        COMBAT_GRANDMASTER("Combat Grandmaster", 0xE6C622, 0xFDE047),
        COMBAT_MASTER("Combat Master", 0xFBB03B, 0xFFD13A),
        COMBAT_ACE("Combat Ace", 0xCD285C, 0xD65474),
        COMBAT_SPECIALIST("Combat Specialist", 0xAD78D8, 0xC7A3E8),
        COMBAT_CADET("Combat Cadet", 0x9291D9, 0xADACE2),
        COMBAT_NOVICE("Combat Novice", 0x9291D9, 0xFFFFFF),
        ROOKIE("Rookie", 0x6C7178, 0x8B979C);

        @Getter
        private final String title;

        public int getColor() { return primaryColor; }
        public int getAccentColor() { return secondaryColor; }

        private final int primaryColor;
        private final int secondaryColor;

        public static PointInfo fromPoints(int points, boolean combatMaster) {
            if (combatMaster) return COMBAT_GRANDMASTER;
            if (points >= 60) return COMBAT_GRANDMASTER;
            if (points >= 45) return COMBAT_MASTER;
            if (points >= 30) return COMBAT_ACE;
            if (points >= 20) return COMBAT_SPECIALIST;
            if (points >= 10) return COMBAT_CADET;
            if (points >= 6) return COMBAT_NOVICE;
            return ROOKIE;
        }
    }
}
