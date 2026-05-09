@NullMarked
@ApplicationModule(
        displayName = "Order",
        allowedDependencies = {"shared::domain", "shared::id", "patient::api", "catalog::api"})
package org.sparkcorporation.lims.order;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
