@NullMarked
@ApplicationModule(
        displayName = "Cashbox",
        allowedDependencies = {
            "shared::domain",
            "shared::id",
            "shared::money",
            "billing::api",
            "order::api",
            "patient::api",
            "iam::api"
        })
package org.sparkcorporation.lims.cashbox;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
