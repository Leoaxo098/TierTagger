package net.uku3lig.tiertagger;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

@Getter
@AllArgsConstructor
public enum TierList {
    MCTIERS("MCTiers", "https://mctiers.com/api"),
    SUBTIERS("SubTiers", "https://subtiers.net/api"),
    VIET_TIERLIST("Viet tierlist", "https://www.tierslist.net/api");

    private final String name;
    private final String url;

    public boolean usesNameLookup() {
        return this == VIET_TIERLIST;
    }

    public String styledName(boolean current) {
        String s = name;
        if (current) s += " (selected)";
        return s;
    }

    public static Optional<TierList> findByUrl(String url) {
        if (url == null) return Optional.empty();
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);

        final String finalUrl = url;
        return Arrays.stream(values()).filter(list -> list.url.equals(finalUrl)).findFirst();
    }
}
