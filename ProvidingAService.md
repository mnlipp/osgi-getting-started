---
layout: default
title: Providing a Service
description: A simple example how to provide an OSGi service.
date: 2019-08-28 12:00:00
commentIssue: 18
---

# Providing a Service

Programmatically, providing a service is as simple as registering a class using 
a method from [`BundleContext`](https://osgi.org/javadoc/osgi.core/7.0.0/org/osgi/framework/BundleContext.html#registerService-java.lang.Class-S-java.util.Dictionary-).
The tricky part is bundling the service.

As outlined in a [previous chapter](../CombiningComponents.html#from-modules-to-services),
a service should be defined by a public API and provided by one or more
(alternative) implementations. In order to properly support this concept, the
API must be made available by a bundle that is independent of any bundle that implements
the API, i.&nbsp;e. that provides the service. The API bundle uses an "`Export-Package`"
header to make the API classes available, while a provider bundle uses the "`Import-Package`"
header to access the API classes and registers the implementation that it
provides[^apiInProvider].

[^apiInProvider]: Some developers include the API classes
	in the provider bundle. You find some pros and cons of this practice
	among the Felix [OSGi Frequently Asked Questions](https://felix.apache.org/documentation/tutorials-examples-and-presentations/apache-felix-osgi-faq.html#should-a-service-providerconsumer-bundle-be-packaged-with-its-service-api-packages).
	Up to today, the Felix implementations of the OSGi services in general include the API
	classes from the OSGi API bundle. Nevertheless, several sources suggest that
	having the API classes in the API bundle only is considered best
	practice (e.&nbsp;g. [here, Slide 21](https://de.slideshare.net/mfrancis/os-gi-best-practices-tim-ward)).

It is, of course, perfectly okay to combine several APIs in one 
API bundle. Else, you'd end up with an annoyingly large number of bundles.
Remember the bundle `osgi.cmpn` that we added to the buildpath in chapter
"[Accessing a Service](../AccessingAService.html)"? It simply contains the collection
of all the interfaces from the OSGi specification with the respective version.
This example gives you a hint about the granularity of an API bundle. The interfaces
defined in such a bundle should all "move at the same pace". If you publish
new versions of a bundle with a lot of APIs frequently with only small changes
of a single or some APIs, you'll put many users of the bundle on alert unnecessarily.

Having several APIs in a bundle helps to avoid the "naming trap". With respect to the
OSGi services, we have bundle `osgi.cmpn` with the API classes and e.&nbsp;g.
bundle `org.apache.felix.configadmin` that provides an implementation. Many "single
service" examples that you find on the web use a naming pattern such as "`some.package.api`"
and "`some.package.service`" or "`some.package.provider`". I think the service's API makes
the service and therefore follow those who advocate to use the "base name" for the 
API bundle, i.&nbsp;e. "`some.package`".

## API Bundle

This is the simple service API that I'll use as an example
([here](https://github.com/mnlipp/osgi-getting-started/tree/master/io.github.mnl.osgiGettingStarted.calculator)'s the project):

```java
package io.github.mnl.osgiGettingStarted.calculator;

public interface Calculator {

	double add(double a, double b);
	
}
```

The `bnd.bnd` only has to define the version and the package to be exported: 

```properties
Bundle-Version: 1.0.0
Export-Package: io.github.mnl.osgiGettingStarted.calculator
```

Finally, here's the generated `MANIFEST.MF`:

```properties
Manifest-Version: 1.0
Bnd-LastModified: 1567002053585
Bundle-ManifestVersion: 2
Bundle-Name: io.github.mnl.osgiGettingStarted.calculator
Bundle-SymbolicName: io.github.mnl.osgiGettingStarted.calculator
Bundle-Version: 1.0.0
Created-By: 1.8.0_201 (Oracle Corporation)
Export-Package: io.github.mnl.osgiGettingStarted.calculator;version="1.0.0"
Require-Capability: osgi.ee;filter:="(&(osgi.ee=JavaSE)(version=1.7))"
Tool: Bnd-4.2.0.201903051501
```

## Implementation Bundle

The implementation class is obvious
([here](https://github.com/mnlipp/osgi-getting-started/tree/master/io.github.mnl.osgiGettingStarted.calculator.provider) the complete project):

```java
package io.github.mnl.osgiGettingStarted.calculator.provider;

import io.github.mnl.osgiGettingStarted.calculator.Calculator;

public class CalculatorImpl implements Calculator {

	@Override
	public double add(double a, double b) {
		return a + b;
	}

}
```
The bundle's activator creates an instance of the calculator implementation
and registers it as a service:

```java
package io.github.mnl.osgiGettingStarted.calculator.provider;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import io.github.mnl.osgiGettingStarted.calculator.Calculator;

public class Activator implements BundleActivator {

	private ServiceRegistration<Calculator> publishedService;
	
	@Override
	public void start(BundleContext context) throws Exception {
		publishedService = context.registerService(
				Calculator.class, new CalculatorImpl(), null);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		publishedService.unregister();
	}

}
```

Finally the `bnd.bnd` (including a startup configuration):

```properties
Bundle-Version: 1.0.0
Bundle-Activator: io.github.mnl.osgiGettingStarted.calculator.provider.Activator
Private-Package: io.github.mnl.osgiGettingStarted.calculator.provider

-buildpath: \
	osgi.core;version=4.3.1,\
	io.github.mnl.osgiGettingStarted.calculator;version=latest

-runfw: org.apache.felix.framework;version='[6.0.2,6.1)'
-runee: JavaSE-1.8
-runprogramargs: -console
-runbundles: \
	org.apache.felix.gogo.command,\
	org.apache.felix.gogo.runtime,\
	org.apache.felix.gogo.shell,\
	io.github.mnl.osgiGettingStarted.calculator;version=latest,\
	io.github.mnl.osgiGettingStarted.calculator.provider;version=latest
```

## Running the Service

*To be continued*

---

