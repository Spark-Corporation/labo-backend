@NullMarked
@ApplicationModule(
        displayName = "Identity and Access",
        allowedDependencies = {"shared::domain", "shared::id", "catalog::api"})
package org.sparkcorporation.lims.iam;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
