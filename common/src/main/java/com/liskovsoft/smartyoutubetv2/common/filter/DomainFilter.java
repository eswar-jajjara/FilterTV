package com.liskovsoft.smartyoutubetv2.common.filter;

import java.net.IDN;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Immutable, bounded domain matcher. Supports only ||domain^ and @@||domain^. */
public final class DomainFilter {
    private final Set<String> blocked = new HashSet<>();
    private final Set<String> allowed = new HashSet<>();
    public DomainFilter(String rules) {
        if (rules.length() > 65536) throw new IllegalArgumentException("Rules exceed 64 KiB");
        int lineNumber = 0;
        for (String raw : rules.split("\r?\n")) {
            lineNumber++;
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("!")) continue;
            boolean allow = line.startsWith("@@");
            String rule = allow ? line.substring(2) : line;
            if (!rule.startsWith("||") || !rule.endsWith("^"))
                throw new IllegalArgumentException("Line " + lineNumber + ": use ||domain^ or @@||domain^");
            String host = normalize(rule.substring(2, rule.length() - 1));
            if (host.isEmpty() || host.length() > 253 || host.contains("..") || !host.contains("."))
                throw new IllegalArgumentException("Line " + lineNumber + ": invalid domain");
            (allow ? allowed : blocked).add(host);
        }
    }
    private static String normalize(String host) {
        String value = host.endsWith(".") ? host.substring(0, host.length()-1) : host;
        return IDN.toASCII(value, IDN.USE_STD3_ASCII_RULES).toLowerCase(Locale.ROOT);
    }
    public String matchedRule(String host) {
        String value;
        try { value = normalize(host); } catch (IllegalArgumentException e) { return null; }
        String match = null;
        while (!value.isEmpty()) {
            if (allowed.contains(value)) return null;
            if (match == null && blocked.contains(value)) match = "||" + value + "^";
            int dot = value.indexOf('.');
            if (dot < 0) break;
            value = value.substring(dot + 1);
        }
        return match;
    }
    public int size() { return blocked.size() + allowed.size(); }
}
