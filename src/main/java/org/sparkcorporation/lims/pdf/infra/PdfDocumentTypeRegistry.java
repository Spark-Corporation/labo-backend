package org.sparkcorporation.lims.pdf.infra;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import org.sparkcorporation.lims.pdf.api.PdfDocumentType;

@Component
@RequiredArgsConstructor
public class PdfDocumentTypeRegistry {

    private final List<PdfDocumentType<?>> documentTypes;
    private final Map<String, PdfDocumentType<?>> byId = new HashMap<>();

    @PostConstruct
    public void validateAfterConstruction() {
        byId.clear();
        for (var dt : documentTypes) {
            var existing = byId.putIfAbsent(dt.id(), dt);
            if (existing != null) {
                throw new IllegalStateException(
                        "duplicate PdfDocumentType id '"
                                + dt.id()
                                + "' — implementations: "
                                + existing.getClass().getName()
                                + " and "
                                + dt.getClass().getName());
            }
        }
    }

    public PdfDocumentType<?> byId(String id) {
        var dt = byId.get(id);
        if (dt == null) {
            throw new IllegalStateException("no PdfDocumentType registered with id '" + id + "'");
        }
        return dt;
    }

    public Set<String> allIds() {
        return byId.keySet().stream().collect(Collectors.toUnmodifiableSet());
    }
}
