@NullMarked
@ApplicationModule(
        displayName = "Order",
        allowedDependencies = {
            "shared::domain",
            "shared::id",
            "shared::money",
            "patient::api",
            "catalog::api",
            "iam::api",
            "labsettings::api",
            "pdf::api",
            "notification::api"
        })
package org.sparkcorporation.lims.order;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
