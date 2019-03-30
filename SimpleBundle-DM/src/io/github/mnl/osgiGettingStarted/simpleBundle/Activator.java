package io.github.mnl.osgiGettingStarted.simpleBundle;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

public class Activator extends DependencyActivatorBase {
	
	public void init(BundleContext context, DependencyManager manager)
			throws Exception {
		manager.add(
			createComponent() // Create a new service component as instance...
			.setImplementation(HelloWorld.class) // ... of the HelloWorld class.
			.add(             // Add to the service component ...
				createServiceDependency() // ... a dependency on ...
				.setService(LogService.class) // ... the LogService service ...
				.setRequired(true) // ... but don't start the instance
				                   // before the LogService is available.
			)
		);
	}
}
