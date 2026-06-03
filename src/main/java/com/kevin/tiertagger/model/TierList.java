package com.kevin.tiertagger.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

@Getter
@AllArgsConstructor
public enum TierList {
    MCTIERS("MCTiers", "https://mctiers.com/api", '\uE901'),
    SUBTIERS("SubTiers", "https://subtiers.net/api", '\uE902'),
    VIET_TIERLIST("Viet TierList", "https://www.tierslist.net/api", '\uE903'),
    ;

    private final String name;
    private final String url;
    private final char icon;

    public String styledName(boolean current) {
        String s = icon + " " + name;
        if (current) s += " (selected)";
        return s;
    }

    public boolean usesNameLookup() {
        return this == VIET_TIERLIST;
    }

    public static Optional<TierList> findByUrl(String url) {
        if (url == null) return Optional.empty();
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);

        final String finalUrl = url;
        return Arrays.stream(values()).filter(list -> list.url.equals(finalUrl)).findFirst();
    }
}