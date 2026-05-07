package org.sparkcorporation.lims.catalog.infra;

import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.catalog.api.PrescriberApi;
import org.sparkcorporation.lims.catalog.api.PrescriberCode;
import org.sparkcorporation.lims.catalog.api.PrescriberId;
import org.sparkcorporation.lims.catalog.api.PrescriberSummary;
import org.sparkcorporation.lims.catalog.domain.Prescriber;
import org.sparkcorporation.lims.catalog.domain.PrescriberNotFoundException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultPrescriberApi implements PrescriberApi {

    private final PrescriberRepository repo;

    @Override
    public Optional<PrescriberSummary> findById(PrescriberId id) {
        return repo.findById(id.value()).map(DefaultPrescriberApi::toSummary);
    }

    @Override
    public Optional<PrescriberSummary> findByCode(PrescriberCode code) {
        return repo.findByCode(code.value()).map(DefaultPrescriberApi::toSummary);
    }

    @Override
    public PrescriberSummary requireById(PrescriberId id) {
        return repo.findById(id.value())
                .map(DefaultPrescriberApi::toSummary)
                .orElseThrow(() -> new PrescriberNotFoundException(id));
    }

    @Override
    public boolean existsById(PrescriberId id) {
        return repo.existsById(id.value());
    }

    @Override
    public List<PrescriberSummary> findAllActive() {
        return repo.findAllActive().stream().map(DefaultPrescriberApi::toSummary).toList();
    }

    private static PrescriberSummary toSummary(Prescriber p) {
        var snap =
                p.specialty() == null
                        ? null
                        : new PrescriberSummary.SpecialtySnapshot(
                                p.specialty().id(), p.specialty().code(), p.specialty().name());
        return new PrescriberSummary(
                p.id(),
                p.code(),
                p.title(),
                p.name(),
                snap,
                p.phone(),
                p.email(),
                p.licenseNumber(),
                p.institution());
    }
}
