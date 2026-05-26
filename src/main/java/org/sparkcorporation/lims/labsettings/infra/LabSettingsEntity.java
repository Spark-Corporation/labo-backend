package org.sparkcorporation.lims.labsettings.infra;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "lab_settings")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class LabSettingsEntity {

    @Id
    @Column(name = "id", length = 26, nullable = false)
    private String id;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Column(name = "address", columnDefinition = "text", nullable = false)
    private String address;

    @Column(name = "accreditation_number", length = 64, nullable = false)
    private String accreditationNumber;

    @Column(name = "manager_name", length = 255, nullable = false)
    private String managerName;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    void applyUpdate(
            String name,
            String address,
            String accreditationNumber,
            String managerName,
            Instant updatedAt) {
        this.name = name;
        this.address = address;
        this.accreditationNumber = accreditationNumber;
        this.managerName = managerName;
        this.updatedAt = updatedAt;
    }
}
