package org.sparkcorporation.lims.labsettings.infra;

import org.springframework.data.jpa.repository.JpaRepository;

interface LabSettingsRepository extends JpaRepository<LabSettingsEntity, String> {}
