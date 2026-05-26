@NullMarked
@ApplicationModule(
        displayName = "Billing",
        allowedDependencies = {
            "shared::domain",
            "shared::id",
            "shared::money",
            "patient::api",
            "catalog::api",
            "order::api",
            "result::api",
            "iam::api",
            "labsettings::api",
            "pdf::api"
        })
package org.sparkcorporation.lims.billing;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
