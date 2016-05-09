---
layout: default
title: Simple Bundle
date: 2016-03-14 12:00:00
---

# Simple Bundle

If you want something to happen when the OSGi framework starts your bundle, you have to provide an implementation of the OSGi interface [BundleActivator](https://osgi.org/javadoc/r6/core/index.html?org/osgi/framework/BundleActivator.html). As you might suspect, the `BundleActivator`'s `start` and `stop` methods are called when the bundle is started or stopped by the framework.

Create a new Java project `SimpleBundle`, copy over the package from `SimplestBundle`, rename it to `...simpleBundle` to avoid confusion and add this straightforward activator implementation (or checkout and import the [prepared project](https://github.com/mnlipp/osgi-getting-started/tree/master/SimpleBundle)):

```java
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
```

Our class "`HelloWorld`" is still not doing anything useful. But as it represents our service, the activator creates an instance in the `start` method and discards it again in the `stop` method.

If you simply add this class to your project, you'll get an error indicator, because the interface `BundleActivator` isn't known. As is usual in a Java project, classes that are not part of the JDK must be provided by a jar file that you add to the classpath of your project. Again, let's keep things simple. Download the jar that holds the interface from the [OSGi site](https://www.osgi.org/developer/downloads/), put it in a subdirectory of your project (usually called "`lib`") and add it to your classpath.

The OSGi framework doesn't search through your bundle for a class that implements the `BundleActivator` interface. Rather, it must be made known to the framework by an additional entry in the `MANIFEST.MF`:

```properties
Bundle-Activator: io.github.mnl.osgiGettingStarted.simpleBundle.Activator
```

If you have some experience with using libraries in Java projects, you know that the libraries aren't only required at compile time, they must also be available at runtime. If your program depends on a library, you somehow have to include it in your delivery in order to provide a readily executable application. So when we create the `SimpleBundle.jar` in the next step, should we add the OSGi jar besides our classes and the `MANIFEST.MF`?

Actually, the jar that we have downloaded from OSGi holds only the interface definitions required for compilation, it doesn't provide implementations. So it would only be of limited use in the runtime environment. Besides, as the runtime environment is supposed to load the activator class provided by our bundle and invoke its methods, it is reasonable to assume that any OSGi framework implementation supplies the `BundleInterface` type out-of-the-box.

Therefore, let's simply package what we have so far into a jar file called `SimpleBundle.jar`:

```
META-INF/MANIFEST.MF
io/github/mnl/osgiGettingStarted/simpleBundle/Activator.class
io/github/mnl/osgiGettingStarted/simpleBundle/HelloWorld.class
```

This can be installed in felix without any problem. But when you start the bundle, you get a nasty stack trace (shortened):

```
org.osgi.framework.BundleException: Activator start error in bundle io.github.mnl.osgiGettingStarted.simpleBundle [8].
	at org.apache.felix.framework.Felix.activateBundle(Felix.java:2276)
	at org.apache.felix.framework.Felix.startBundle(Felix.java:2144)
	...
Caused by: java.lang.NoClassDefFoundError: org/osgi/framework/BundleActivator
	at java.lang.ClassLoader.defineClass1(Native Method)
	at java.lang.ClassLoader.defineClass(ClassLoader.java:760)
	...
java.lang.NoClassDefFoundError: org/osgi/framework/BundleActivator
```

Is there no `BundleActivator` in the runtime environment after all? Yes, there is. <a name="need-for-import"></a>It's just that the OSGi runtime doesn't allow a bundle to use everything that is available in the environment freely, not even it is something as obvious as a framework interface. If a bundle wants to use an interface or class from the runtime environment, it must declare this desire with an `Import` statement in the manifest. The complete manifest of our Simple Bundle must hence include such a statement as well:

```properties
Manifest-Version: 1.0
Bundle-ManifestVersion: 2
Bundle-Name: HelloWorld
Bundle-SymbolicName: io.github.mnl.osgiGettingStarted.simpleBundle
Bundle-Version: 1.0.0
Bundle-RequiredExecutionEnvironment: JavaSE-1.7
Bundle-Activator: io.github.mnl.osgiGettingStarted.simpleBundle.Activator
Import-Package: org.osgi.framework
```

Packaging the `SimpleBundle.jar` again with the updated `MANIFEST.MF` gives the expected result:

```
g! felix:install file:<your path>/SimpleBundle.jar
Bundle ID: 10
g! felix:start 10
Hello World started.
g! felix:stop 10
Hello World stopped.
```

