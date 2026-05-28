@NullMarked
@ApplicationModule(
        displayName = "Delivery",
        allowedDependencies = {
            "shared::domain",
            "shared::id",
            "patient::api",
            "order::api",
            "result::api"
        })
package org.sparkcorporation.lims.delivery;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
