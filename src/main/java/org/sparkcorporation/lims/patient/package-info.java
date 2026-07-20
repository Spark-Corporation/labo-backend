@NullMarked
@ApplicationModule(
        displayName = "Patient",
        allowedDependencies = {"shared::domain", "shared::id", "catalog::api"})
package org.sparkcorporation.lims.patient;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
