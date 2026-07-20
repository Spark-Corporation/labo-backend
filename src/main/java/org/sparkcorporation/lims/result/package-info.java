@NullMarked
@ApplicationModule(
        displayName = "Result",
        allowedDependencies = {
            "shared::domain",
            "shared::id",
            "patient::api",
            "catalog::api",
            "order::api",
            "iam::api",
            "labsettings::api",
            "pdf::api",
            "export::api"
        })
package org.sparkcorporation.lims.result;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
