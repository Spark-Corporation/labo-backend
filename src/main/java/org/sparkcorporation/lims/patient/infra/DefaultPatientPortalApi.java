package org.sparkcorporation.lims.patient.infra;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.patient.api.PatientId;
import org.sparkcorporation.lims.patient.api.PatientPortalApi;

@Service
@RequiredArgsConstructor
public class DefaultPatientPortalApi implements PatientPortalApi {

    private final PatientRepository repo;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public Optional<PatientId> authenticate(String portalLogin, String rawPassword) {
        return repo.findByPortalLogin(portalLogin)
                .filter(
                        p ->
                                p.portalPasswordHash() != null
                                        && passwordEncoder.matches(
                                                rawPassword, p.portalPasswordHash()))
                .map(p -> p.id()); // Patient.id() already returns PatientId
    }
}
