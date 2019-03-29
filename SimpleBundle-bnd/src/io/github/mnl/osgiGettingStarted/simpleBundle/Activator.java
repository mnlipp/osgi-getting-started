package io.github.mnl.osgiGettingStarted.simpleBundle;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	private HelloWorld helloWorld;
	
	@Override
	public void start(BundleContext context) throws Exception {
		System.out.println("Hello World started.");
		helloWorld = new HelloWorld();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		helloWorld = null;
		System.out.println("Hello World stopped.");
	}

}
