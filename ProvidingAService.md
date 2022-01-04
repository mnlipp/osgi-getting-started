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

As outlined in a [previous chapter](./CombiningComponents.html#from-modules-to-services),
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
"[Accessing a Service](./AccessingAService.html)"? It simply contains the collection
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
([here](https://github.com/mnlipp/osgi-getting-started/tree/master/io.github.mnl.osgiGettingStarted.calculator)'s the project).

```java
package io.github.mnl.osgiGettingStarted.calculator;

public interface Calculator {

	double add(double a, double b);
	
}
```

The `bnd.bnd` only has to define the version and the package to be exported.

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

By default, bnd uses the project name as `Bundle-SybolicName` (and `Bundle-Name`). It's
therefore usually a good idea to use the desired bundle symbolic name as project
name[^notDoneInitially].

[^notDoneInitially]: I deliberately didn't follow this best practice in the
	initial examples in order to keep things simple.

## Implementation Bundle

The implementation class is obvious
([here](https://github.com/mnlipp/osgi-getting-started/tree/master/io.github.mnl.osgiGettingStarted.calculator.provider)'s the complete project).

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

An instance of the calculator implementation is registered with the framework
and thus made available as a service by the bundle's activator.

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

Finally here's the `bnd.bnd` (including a startup configuration).

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

Now you could, as has been shown for the predefined log service, write a client
that looks up the service and uses it. Just for fun, let's leverage the power
of the GoGo shell and do it interactively.

The GoGo shell lacks a good documentation. When you read the 
[subproject's web page](https://felix.apache.org/documentation/subprojects/apache-felix-gogo.html) you learn about some built-in commands like `lb`, which by the way now outputs
this:

```
g! lb
START LEVEL 1
   ID|State      |Level|Name
    0|Active     |    0|System Bundle (6.0.3)|6.0.3
    1|Active     |    1|Apache Felix Gogo Command (1.1.0)|1.1.0
    2|Active     |    1|Apache Felix Gogo Runtime (1.1.2)|1.1.2
    3|Active     |    1|Apache Felix Gogo Shell (1.1.2)|1.1.2
    4|Active     |    1|io.github.mnl.osgiGettingStarted.calculator (1.0.0)|1.0.0
    5|Active     |    1|io.github.mnl.osgiGettingStarted.calculator.provider (1.0.0)|1.0.0
```

But the most important information is hidden in a note at the bottom of the page:

> Gogo is based on the OSGi RFC 147, which describes a standard shell for OSGi-based environments. See [RFC 147 Overview](https://felix.apache.org/documentation/subprojects/apache-felix-gogo/rfc-147-overview.html)
> for more information. Unfortunately this RFC was never made a standard.

Only when you read the referenced overview (and maybe the section in the
[specification draft](https://web.archive.org/web/20160606172521/https://osgi.org/download/osgi-4.2-early-draft.pdf)), 
you find that GoGo provides a lot more than just some commands.

The basic idea is that you have a "current" object (or context) and that you can
invoke any public method of that object (or context). The most important member of the
default context is the [`BundleContext`](https://osgi.org/javadoc/osgi.core/7.0.0/index.html?org/osgi/framework/BundleContext.html) of the GoGo bundle. You can therefore
simply type `getBundle` to invoke the `getBundle()` method, which yields:

```
g! getBundle
LastModified         1567026091906
Headers              [Bundle-License=https://www.apache.org/licenses/LICENSE-2.0.txt, Created-By=Apache Maven Bundle Plugin, Manifest-Version=1.0, Bnd-LastModified=1546558580748, Bundle-Name=Apache Felix Gogo Runtime, Build-Jdk=1.8.0_192, Bundle-Description=Apache Felix Gogo Subproject, Bundle-DocURL=https://www.apache.org/, Bundle-Vendor=The Apache Software Foundation, Import-Package=org.osgi.service.event;resolution:=optional;version="[1.3,2)",org.apache.felix.gogo.runtime;version="[1.1,2)",org.apache.felix.gogo.runtime.threadio;version="[1.1,2)",org.apache.felix.service.command;version="[1.0,2)",org.apache.felix.service.threadio;version="[1.0,2)",org.osgi.framework;version="[1.8,2)",org.osgi.util.tracker;version="[1.5,2)", Provide-Capability=org.apache.felix.gogo;org.apache.felix.gogo="runtime.implementation";version:Version="1.0.0",osgi.service;objectClass="org.apache.felix.service.command.CommandProcessor",osgi.service;objectClass="org.apache.felix.service.threadio.ThreadIO", Export-Package=org.apache.felix.gogo.runtime;version="1.1.2";uses:="org.apache.felix.service.command,org.apache.felix.service.threadio,org.osgi.framework",org.apache.felix.gogo.runtime.activator;version="1.1.2";uses:="org.apache.felix.gogo.runtime,org.apache.felix.service.command,org.apache.felix.service.threadio,org.osgi.framework",org.apache.felix.gogo.runtime.threadio;version="1.1.2";uses:="org.apache.felix.service.threadio",org.apache.felix.service.command;version="1.0.0",org.apache.felix.service.command.annotations;version="1.0.0",org.apache.felix.service.threadio;version="1.0.0", Bundle-ManifestVersion=2, Bundle-SymbolicName=org.apache.felix.gogo.runtime, Bundle-Version=1.1.2, Built-By=rotty, Bundle-Activator=org.apache.felix.gogo.runtime.activator.Activator, Require-Capability=org.apache.felix.gogo;filter:="(&(org.apache.felix.gogo=shell.implementation)(version>=1.0.0)(!(version>=2.0.0)))";effective:=active,osgi.ee;filter:="(&(osgi.ee=JavaSE)(version=1.8))", Tool=Bnd-4.1.0.201810241949]
Location             reference:file:/home/mnl/.m2/repository/org/apache/felix/org.apache.felix.gogo.runtime/1.1.2/org.apache.felix.gogo.runtime-1.1.2.jar
State                32
Version              1.1.2
BundleContext        org.apache.felix.framework.BundleContextImpl@3ffc5af1
SymbolicName         org.apache.felix.gogo.runtime
BundleId             2
RegisteredServices   [ThreadIO, CommandProcessor]
ServicesInUse        [Converter, Shell, Procedural, Posix, Basic, Inspect]
Bundle                   2|Active     |    1|org.apache.felix.gogo.runtime (1.1.2)
Revisions            [org.apache.felix.gogo.runtime [2](R 2.0)]
```

As a convenience, `get` is prepended automatically if you specify a method that
isn't found, so simply typing `bundle` results in the same output.

By invoking method `getServiceReference` you can get a service reference to our new service.
Using this, you can get the service itself and invoke it. Finally, being a good
citizen, you should release the service.

```
g! calcRef = serviceReference io.github.mnl.osgiGettingStarted.calculator.Calculator
Properties           [service.id=15, objectClass=[Ljava.lang.String;@1abfe77a, service.scope=singleton, service.bundleid=5]
Attributes           []
Bundle                   5|Active     |    1|io.github.mnl.osgiGettingStarted.calculator.provider (1.0.0)
Namespace            service-reference
Directives           []
UsingBundles         null
Uses                 []
PropertyKeys         [objectClass, service.bundleid, service.id, service.scope]
Resource             null
Resource             null

g! calcService = service $calcRef 
io.github.mnl.osgiGettingStarted.calculator.provider.CalculatorImpl@4e6f0273
g! $calcService add 1 2
3.0
g! ungetService $calcRef
true
```

So, here it is: your first OSGi service, ready to be used.

## More Ways to Provide Services

What we have provided above is a service with so called "singleton scope".
This does not mean that there can be only one instance of the service, it
doesn't even mean that you can register only one instance of your implementation
class as service[^registeringSeveral]. What singleton scope does imply is that 
all consumers of the service get the same object for a particular service 
registration&mdash;the one that you have registered[^wishConform]. In addition
to this straight forward approach OSGi offers two alternative ways to provide 
services.

[^wishConform]: I often think that OSGi specifications would be much easier to
    understand and would have been a much greater success if they had used terms
    just like everybody else does.

[^registeringSeveral]: Registering several instance of the implementation
    class can make sense because services can have properties, i&nbsp;e. they
    are configurable. The values of these properties can be taken into account
	when looking up service instances. Thus it sometimes makes sense to
	register more than one instance of an implementation class, provided that
	the instances have different configurations.

### The Service Factory

Instead of registering an object that implements the service's interface,
you can [register](https://osgi.org/javadoc/osgi.core/7.0.0/org/osgi/framework/BundleContext.html#registerService-java.lang.Class-org.osgi.framework.ServiceFactory-java.util.Dictionary-)
an instance of a [`ServiceFactory`](https://osgi.org/javadoc/osgi.core/7.0.0/index.html?org/osgi/framework/ServiceFactory.html) "as the service". 

If you do this, the framework invokes the service factory's `getService` method every
time a bundle gets the service *for the first time*. The implementation of `getService`
must create a new instance of the requested service for each invocation, using the
information about the requesting bundle (as passed to `getService`) to "customize"
the created service (object). Subsequent invocations of `getService` by the
same bundle result in the service object created by the first invocation
(which is cached and returned by the framework). Because each bundle gets its 
own instance of the service implementation, the service object is said to have 
"bundle scope".

The use case commonly referred to for using a service factory is the log service.
Because each bundle gets its own instance of the log service implementation, it
can be "customized" using the bundle id and when a log method is invoked, the
service implementation can add this bundle id to the logged information.

### The Prototype Service Factory

If you register an object that implements the 
[`PrototypeServiceFactory`](https://osgi.org/javadoc/osgi.core/7.0.0/index.html?org/osgi/framework/PrototypeServiceFactory.html), the framework invokes the prototype
service factory's `getService` method each time the service is obtained by
a call to `getService`. Each user of the service gets its own instance and the
service object is said to have "prototype scope".

This scope was added in OSGi 6. In general, it is used for service objects that
maintain individual state. A commonly referred to use case is a service that
provides an HTTP session context.

---

