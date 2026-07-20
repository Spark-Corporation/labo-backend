package org.sparkcorporation.lims.cashbox.architecture;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import org.sparkcorporation.lims.LimsApplication;

class CashboxModuleStructureTest {

    @Test
    void cashboxModuleBoundariesAreValid() {
        var modules = ApplicationModules.of(LimsApplication.class);
        modules.verify();
        // cashbox module must appear in the discovered set.
        // Spring Modulith identifies modules by their package basename (logical
        // identifier), not by displayName — see ApplicationModule#getIdentifier.
        var cashbox =
                modules.getModuleByName("cashbox")
                        .orElseThrow(() -> new AssertionError("Cashbox module not discovered"));
        // Optional sanity: print the allowed-dependencies tree for diagnostics
        // (the verify() call above is the actual assertion).
        System.out.println(cashbox);
    }
}
