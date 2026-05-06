package org.sparkcorporation.lims.catalog.infra;

import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.catalog.api.RecipientApi;
import org.sparkcorporation.lims.catalog.api.RecipientCode;
import org.sparkcorporation.lims.catalog.api.RecipientId;
import org.sparkcorporation.lims.catalog.api.RecipientSummary;
import org.sparkcorporation.lims.catalog.domain.Recipient;
import org.sparkcorporation.lims.catalog.domain.RecipientNotFoundException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultRecipientApi implements RecipientApi {

    private final RecipientRepository repo;

    @Override
    public Optional<RecipientSummary> findById(RecipientId id) {
        return repo.findById(id.value()).map(DefaultRecipientApi::toSummary);
    }

    @Override
    public Optional<RecipientSummary> findByCode(RecipientCode code) {
        return repo.findByCode(code.value()).map(DefaultRecipientApi::toSummary);
    }

    @Override
    public RecipientSummary requireById(RecipientId id) {
        return repo.findById(id.value())
                .map(DefaultRecipientApi::toSummary)
                .orElseThrow(() -> new RecipientNotFoundException(id));
    }

    @Override
    public boolean existsById(RecipientId id) {
        return repo.existsById(id.value());
    }

    @Override
    public List<RecipientSummary> findAllActive() {
        return repo.findAllActiveOrderByCode().stream()
                .map(DefaultRecipientApi::toSummary)
                .toList();
    }

    private static RecipientSummary toSummary(Recipient r) {
        var matrix =
                r.defaultMatrix() == null
                        ? null
                        : new RecipientSummary.MatrixSnapshot(
                                r.defaultMatrix().id(),
                                r.defaultMatrix().code(),
                                r.defaultMatrix().name());
        return new RecipientSummary(
                r.id(),
                r.code(),
                r.name(),
                r.description(),
                r.color(),
                r.volumeMl(),
                matrix,
                r.isActive());
    }
}
