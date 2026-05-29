package org.sparkcorporation.lims.notification.architecture;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import org.sparkcorporation.lims.LimsApplication;

class NotificationModuleStructureTest {

    @Test
    void notificationModuleHonoursAllowedDependencies() {
        var modules = ApplicationModules.of(LimsApplication.class);
        modules.verify();
        modules.getModuleByName("notification")
                .orElseThrow(() -> new AssertionError("Notification module not discovered"));
    }
}
