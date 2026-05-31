package org.sparkcorporation.lims.export.infra;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import org.sparkcorporation.lims.export.api.OfficeDocumentType;

@Component
@RequiredArgsConstructor
public class OfficeDocumentTypeRegistry {

    private final List<OfficeDocumentType<?>> documentTypes;
    private final Map<String, OfficeDocumentType<?>> byId = new HashMap<>();

    @PostConstruct
    public void validateAfterConstruction() {
        byId.clear();
        for (var dt : documentTypes) {
            var existing = byId.putIfAbsent(dt.id(), dt);
            if (existing != null) {
                throw new IllegalStateException(
                        "duplicate OfficeDocumentType id '"
                                + dt.id()
                                + "' — implementations: "
                                + existing.getClass().getName()
                                + " and "
                                + dt.getClass().getName());
            }
        }
    }

    public OfficeDocumentType<?> byId(String id) {
        var dt = byId.get(id);
        if (dt == null) {
            throw new IllegalStateException(
                    "no OfficeDocumentType registered with id '" + id + "'");
        }
        return dt;
    }

    public Set<String> allIds() {
        return byId.keySet().stream().collect(Collectors.toUnmodifiableSet());
    }
}
