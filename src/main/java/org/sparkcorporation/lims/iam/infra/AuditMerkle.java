package org.sparkcorporation.lims.iam.infra;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Pure helpers for audit checkpointing: a canonical JSON serialization of the per-subject chain
 * heads and a SHA-256 Merkle root over them. No Spring, no I/O — deterministic and unit-tested.
 *
 * <p>The SAME canonical bytes are used both for the signed checkpoint payload and (logically) for
 * the {@code chain_heads} jsonb column, and are re-derived (never read raw from jsonb) on verify,
 * so a Postgres jsonb reserialization can never break signature verification.
 */
public final class AuditMerkle {

    /** Root for an empty chain-head set (no subjects yet). Documented constant. */
    public static final String EMPTY_ROOT = sha256Hex("LIMS-AUDIT-EMPTY-CHECKPOINT");

    private AuditMerkle() {}

    /** Canonical JSON: keys sorted lexicographically, no insignificant whitespace, UTF-8. */
    public static String canonicalJson(Map<String, String> heads) {
        var sorted = new TreeMap<>(heads);
        var sb = new StringBuilder("{");
        boolean first = true;
        for (var e : sorted.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append('"')
                    .append(escape(e.getKey()))
                    .append("\":\"")
                    .append(escape(e.getValue()))
                    .append('"');
        }
        return sb.append('}').toString();
    }

    /**
     * SHA-256 Merkle root (hex) over sorted (subject,headHash) leaves; empty → {@link #EMPTY_ROOT}.
     */
    public static String root(Map<String, String> heads) {
        if (heads.isEmpty()) {
            return EMPTY_ROOT;
        }
        var sorted = new TreeMap<>(heads);
        List<String> level = new ArrayList<>();
        for (var e : sorted.entrySet()) {
            level.add(sha256Hex(e.getKey() + "|" + e.getValue()));
        }
        while (level.size() > 1) {
            List<String> next = new ArrayList<>();
            for (int i = 0; i < level.size(); i += 2) {
                if (i + 1 < level.size()) {
                    next.add(sha256Hex(level.get(i) + level.get(i + 1)));
                } else {
                    next.add(level.get(i));
                }
            }
            level = next;
        }
        return level.getFirst();
    }

    public static String sha256Hex(String input) {
        try {
            byte[] d =
                    MessageDigest.getInstance("SHA-256")
                            .digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d);
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
