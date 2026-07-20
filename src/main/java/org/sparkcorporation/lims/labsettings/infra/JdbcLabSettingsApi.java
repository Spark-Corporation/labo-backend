package org.sparkcorporation.lims.labsettings.infra;

import java.time.Clock;
import java.time.Instant;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.labsettings.api.LabSettings;
import org.sparkcorporation.lims.labsettings.api.LabSettingsApi;
import org.sparkcorporation.lims.labsettings.api.LabSettingsUpdateRequest;

@Service
@RequiredArgsConstructor
class JdbcLabSettingsApi implements LabSettingsApi {

    private static final String SINGLETON_ID = "LAB_SETTINGS_SINGLETON";

    private final LabSettingsRepository repo;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public LabSettings get() {
        return repo.findById(SINGLETON_ID)
                .map(JdbcLabSettingsApi::toApi)
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "lab_settings singleton row missing — check Liquibase"
                                                + " seed"));
    }

    @Override
    @Transactional
    public LabSettings update(LabSettingsUpdateRequest req) {
        var entity =
                repo.findById(SINGLETON_ID)
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "lab_settings singleton row missing — cannot"
                                                        + " update"));
        entity.applyUpdate(
                req.name(),
                req.address(),
                req.accreditationNumber(),
                req.managerName(),
                Instant.now(clock));
        return toApi(repo.save(entity));
    }

    private static LabSettings toApi(LabSettingsEntity e) {
        return new LabSettings(
                e.getName(),
                e.getAddress(),
                e.getAccreditationNumber(),
                e.getManagerName(),
                e.getUpdatedAt());
    }
}
