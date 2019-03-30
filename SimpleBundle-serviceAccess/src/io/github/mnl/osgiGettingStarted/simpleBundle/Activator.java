package io.github.mnl.osgiGettingStarted.simpleBundle;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

public class Activator implements BundleActivator {

	private HelloWorld helloWorld;
	
	@Override
	public void start(BundleContext context) throws Exception {
        LogService logService = context.getService(
                context.getServiceReference(LogService.class));
        System.out.println("Hello World started.");
		helloWorld = new HelloWorld();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		helloWorld = null;
		System.out.println("Hello World stopped.");
	}

}
