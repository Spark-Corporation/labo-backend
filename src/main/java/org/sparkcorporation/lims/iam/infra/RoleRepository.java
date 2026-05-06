package org.sparkcorporation.lims.iam.infra;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import org.sparkcorporation.lims.iam.domain.RoleEntity;

public interface RoleRepository extends JpaRepository<RoleEntity, String> {

    Optional<RoleEntity> findByName(String name);

    List<RoleEntity> findByNameIn(Collection<String> names);
}
