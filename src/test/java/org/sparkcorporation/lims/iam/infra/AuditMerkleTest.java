package org.sparkcorporation.lims.iam.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

class AuditMerkleTest {

    @Test
    void root_is_deterministic_regardless_of_insertion_order() {
        Map<String, String> a = new LinkedHashMap<>();
        a.put("subjB", "h2");
        a.put("subjA", "h1");
        Map<String, String> b = new TreeMap<>();
        b.put("subjA", "h1");
        b.put("subjB", "h2");

        assertThat(AuditMerkle.root(a)).isEqualTo(AuditMerkle.root(b));
        assertThat(AuditMerkle.root(a)).hasSize(64);
    }

    @Test
    void empty_heads_yield_the_documented_sentinel_root() {
        assertThat(AuditMerkle.root(Map.of())).isEqualTo(AuditMerkle.EMPTY_ROOT);
    }

    @Test
    void changing_any_head_changes_the_root() {
        String before = AuditMerkle.root(Map.of("s", "h1"));
        String after = AuditMerkle.root(Map.of("s", "h2"));
        assertThat(before).isNotEqualTo(after);
    }

    @Test
    void canonical_heads_json_sorts_keys_without_whitespace() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("b", "2");
        m.put("a", "1");
        assertThat(AuditMerkle.canonicalJson(m)).isEqualTo("{\"a\":\"1\",\"b\":\"2\"}");
    }
}
