package ke.shulehub;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/** Fails if a module reaches into another module's internals or modules depend on each other in a cycle. */
class ModularityTests {

    private final ApplicationModules modules = ApplicationModules.of(ShuleHubApplication.class);

    @Test
    void modulesRespectTheirBoundaries() {
        modules.verify();
    }
}
