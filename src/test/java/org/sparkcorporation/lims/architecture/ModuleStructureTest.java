package org.sparkcorporation.lims.architecture;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

import org.sparkcorporation.lims.LimsApplication;

class ModuleStructureTest {

    @Test
    void modules_satisfy_their_declared_boundaries() {
        ApplicationModules modules = ApplicationModules.of(LimsApplication.class);
        modules.verify();
    }

    @Test
    void modules_render_documentation_canvas() {
        ApplicationModules modules = ApplicationModules.of(LimsApplication.class);
        new Documenter(modules).writeModuleCanvases();
    }
}
