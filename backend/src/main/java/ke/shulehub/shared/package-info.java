/**
 * Shared kernel: tenant context, persistence base classes, error handling and small
 * value helpers used by every module. Open so other modules can use its sub-packages.
 */
@ApplicationModule(displayName = "Shared kernel", type = ApplicationModule.Type.OPEN)
package ke.shulehub.shared;

import org.springframework.modulith.ApplicationModule;
