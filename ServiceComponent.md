---
layout: default
title: Service Components
description: Introduces service components as a simpler form of services.
date: 2016-05-06 12:00:00
---

# From Services to Service Components

As we have seen in the previous parts, the service registry provides a a very simple and flexible mechanism to establish the relationships between the components of a software system (provided as bundles). The drawback of its simplicity is that it requires a relatively large effort for individual components to manage their service dependencies.

A somewhat different approach to managing services is the use of "service components". The OSGi specification defines a service component as a component that "[...] contains a description that is interpreted at run time to create and dispose objects depending on the availability of other services, the need for such an object, and available configuration data. Such objects can optionally provide a service." Have a look at the [article](http://www-adele.imag.fr/Les.Publications/intConferences/CBSE2003Cer.pdf) cited in the specification, which provides a nice introduction into the topic.

Service components supply the information required for managing the service dependencies. The management functions that evaluate this information are implemented in an independent component (the "dependency manager" or "service component runtime") that is deployed in the OSGi framework in addition to the service components. All dependency managers work according to the same basic pattern. They watch for bundle state changes, either by monitoring the framework or by requiring a bundle to notify the manager about its start explicitly in its bundle activator. Once the bundle has been started, the dependency information is retrieved. When all the required (i.e. non-optional) services are available, some startup-method of the service component is invoked. (And, of course, there is a shutdown-method, which is invoked if one of the required services goes away.)

## Felix Dependency Manager (DM)

One of the oldest and at the same time very recent solutions to dependency management is the [Felix Dependency Manager (DM)](http://felix.apache.org/documentation/subprojects/apache-felix-dependency-manager.html). It has its origins in 2004, but has recently undergone a major [overhaul](http://www.planetmarrs.net/dependency-manager-4/). The DM supports two mechanism for supplying the dependency information: programmatically or by using annotations.

### Using DM programmatically

When using Felix DM programmatically, you have to provide a bundle activator that extends the abstract class [DependencyActivatorBase](http://felix.apache.org/apidocs/dependencymanager/r7/index.html?org/apache/felix/dm/DependencyActivatorBase.html) and implements the `init`-method. This method tells the manager about the service requirements (and about provides services) using the dependency manager's API.

```java
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
```

The adapted version of your component class looks like this:

```java
public class HelloWorld extends Thread {

    private volatile LogService logService;
        
    @Override
    public void run() {
        System.out.println("Hello World!");
        while (!isInterrupted()) {
            try {
                logService.log(LogService.LOG_INFO, "Hello Word sleeping");
                sleep (5000);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
    
}
```

To get this working, we have to add the DM to the sets of "build" and "run" bundles. 
During runtime, the DM has additional required dependencies on the OSGi 
"Configuration Admin" and the "Metatype" services. So we have to add those as 
well. You can do this using the GUI (see below), of course. What you end up 
with are entries in the `bnd.bnd` similar to this:

```properties
-buildpath: \
        osgi.cmpn;version=4.3.1,\
        osgi.core;version=4.3.1,\
        org.apache.felix.dependencymanager;version=4.4.0
-runbundles: \
        org.apache.felix.log,\
        org.apache.felix.gogo.command,\
        org.apache.felix.gogo.runtime,\
        org.apache.felix.gogo.shell,\
        de.mnl.osgi.log.fwd2jul,\
        org.apache.felix.dependencymanager,\
        org.apache.felix.configadmin,\
        org.apache.felix.metatype
```

Regrettably, the Felix DM is in none of the Bndtools "standard repositories".
We can add the Apache Felix Repository (http://felix.apache.org/obr/releases.xml) 
in the same way as we added the "de.mnl.osgi" repository 
[before](UsingAService.html#add-repo). If you have checked out the 
[companion project](https://github.com/mnlipp/osgi-getting-started) for this tutorial, 
it's already there, of course.

```properties
-plugin.6.Felix: \
	aQute.bnd.deployer.repository.FixedIndexedRepo; \
		name=Felix; \
		cache=${workspace}/cnf/cache; \
		locations=http://felix.apache.org/obr/releases.xml
```

Run the modified bundle with the enhanced configuration and -- it works, i.e. we get the "Hello World!" and the log messages. This is nice to see, but where's the magic, I mean, what started our thread? As it happens, the methods invoked by the DM on a service component are 

* `init`,
* `start`,
* `stop` and
* `destroy`.

Except for the first, these methods are all defined by the `Thread` class, which our component inherits from, and this is why our component works. Of course, since the `stop` and `destroy` methods are deprecated (and for good reasons), we should override them with proper implementations for anything beyond this simple example[^overstop].

[^overstop]: Unfortunately, `stop` is final and cannot be overridden. I'll improve the solution in the second part.

The log service has been made available by the DM in the attribute `logService` by [dependency injection](https://en.wikipedia.org/wiki/Dependency_injection). The details can all be found in the DM [documentation](http://felix.apache.org/documentation/subprojects/apache-felix-dependency-manager/reference/components.html).

When it comes to optional services, DM supports a nifty feature known as "[Null Object](https://en.wikipedia.org/wiki/Null_Object_pattern)". Try it out: change the parameter of `setRequired` in the activator's `init`-method to `false` and remove the log service bundle from the set of run bundles. Our component continues to work, despite the fact that we should get a `NullPointerException` when trying to invoke the `log`-method (`logService.log(...)`), since there cannot possibly be a log service available. If an optional dependency is not available, DM injects an instance of class [Proxy](https://docs.oracle.com/javase/7/docs/api/index.html?java/lang/reflect/Proxy.html) that simply handles all method invocations by doing nothing (except for returning a "zero" value matching the method's return type). Of course, it's not guaranteed that this "dummy service implementation" won't cause trouble in the invoking code. But (as in the case of the log service) this feature can sometimes simplify the code because you don't have to check whether an optional service is actually available.

### Using DM with annotations

An easier way to specify service related information is the use of annotations. You can do this in the component class like this:

```java
package io.github.mnl.osgiGettingStarted.simpleBundle;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;
import org.osgi.service.log.LogService;

@Component(provides={})
public class HelloWorld implements Runnable {

    @ServiceDependency(required=true)
    private volatile LogService logService;

    private Thread runner;
    
    @Start
    public void start() {
        runner = new Thread(this);
        runner.start();
    }
    
    @Stop
    public void stop() {
        runner.interrupt();
        try {
            runner.join();
        } catch (InterruptedException e) {
            logService.log(LogService.LOG_WARNING, 
                    "Could not terminate thread properly", e);
        }
    }
    
    @Override
    public void run() {
        System.out.println("Hello World!");
        while (!runner.isInterrupted()) {
            try {
                logService.log(LogService.LOG_INFO, "Hello Word sleeping");
                Thread.sleep (5000);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

}
```

Note that we don't need an activator any more. The annotations specify the same information as the API calls in the previous example and should be self-explanatory[^olddoc], maybe with the exception of the `@Component` annotation's parameter "`provides`". All interfaces directly implemented by a class annotated as `@Component` are by default assumed to be interfaces of services provided by the component. (Using the API, declaring a component as provider of a service would require an extra call.) Of course, we don't want our component to be registered as a provider of the `java.lang.Runnable` "service". Therefore we have to enumerate the provided services (none) explicitly.

[^olddoc]: The "`required`" element included in the `@ServiceDependency` cannot be found in the "[manual](http://felix.apache.org/documentation/subprojects/apache-felix-dependency-manager/reference/dependency-service.html#servicedependency)" pages, which obviously aren't complete. When in doubt, have a look at the [javadoc](http://felix.apache.org/apidocs/dependencymanager.annotations/r7/index.html?org/apache/felix/dm/annotation/api/ServiceDependency.html).

In order to make the annotations known to the java compiler, you have to add `org.apache.felix.dependencymanager.annotation-x.y.z.jar` to the build path (as you did before with `org.apache.felix.dependencymanager`, see above). In an additional build step, the annotations are then used to create a file `META-INF/dependencymanager/io.github.mnl.osgiGettingStarted.simpleBundle.HelloWorld` in the bundle during packaging. Using the code above, the generated file looks like this:

```json
{"impl":"io.github.mnl.osgiGettingStarted.simpleBundle.HelloWorld",
 "stop":"stop",
 "start":"start",
 "type":"Component"}
{"service":"org.osgi.service.log.LogService",
 "autoConfig":"logService",
 "type":"ServiceDependency",
 "required":"true"}
```

There are two ways to add the additional build step for creating this file to 
the `bnd` packaging tool. If you want to use the dependency manager in a 
single project only, you can add the lines

```
-pluginpath:\
	${workspace}/cnf/cache/org.apache.dependencymanager.annotation-4.2.0.jar;\
	url=http://repo1.maven.org/maven2/org/apache/felix/org.apache.felix.dependencymanager.annotation/4.2.0/org.apache.felix.dependencymanager.annotation-4.2.0.jar	
-plugin: org.apache.felix.dm.annotation.plugin.bnd.AnnotationPlugin;log=debug
```

to the project's `bnd.bnd`. If you want to enable support for the whole (bnd-)workspace, 
add the lines to the `build.bnd` in the `cnf` project. The first line tells bnd
about the location of the plugin and the second instructs it to apply the plugin. 

If you're impatient and run the compiled bundle now, nothing's going to happen. 
As there is no activator any more, another mechanism must notice if a bundle 
with files `META-INF/dependencymanager/*` in it is started, read the information 
from the files and act accordingly. Such a mechanism is provided by the dependency 
manager as bundle `org.apache.felix.dependencymanager.runtime-x.y.z.jar`. 
Add it to the run bundles and start it again.


## OSGi Declarative Services

The OSGi alliance has added support for service components as "Declarative Services" in Release 4 of the service specification (2005). As the name suggests, there is no API as in Felix DM. Rather, services are declared using a header in `MANIFEST.MF`, e.g.:

```props
Service-Component: OSGI-INF/io.github.mnl.osgiGettingStarted.simpleBundle.HelloWorld.xml
```

The file name pattern "`OSGI-INF/<fully qualified class name>.xml`" isn't mandatory but recommended. 

If we want our component to be a declared service component, the file should look like this:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<scr:component xmlns:scr="http://www.osgi.org/xmlns/scr/v1.1.0"
  name="io.github.mnl.osgiGettingStarted.simpleBundle.HelloWorld" 
  activate="start" deactivate="stop">
  <implementation class="io.github.mnl.osgiGettingStarted.simpleBundle.HelloWorld"/>
  <reference name="LogService" interface="org.osgi.service.log.LogService" 
    bind="setLogService" unbind="unsetLogService"/>
</scr:component>
```

You would probably not be able to write this file from scratch without further guidance, but when you read it, it's quite self-explanatory. The only thing that might be irritating is the reference to the log service, especially the `bind` and `unbind` attributes. Well, some things have to be approached a bit differently compared to Felix DM. Actually, I didn't write the file shown above. I used the annotations that have been added in release 4.3 of the specification and let `bnd` generate it[^dsbp]. The source code should clarify what happens with the reference to the log service.

[^dsbp]: With declared services, you only have to add `osgi.annotation` to the build path to make `bnd` generate this file. Handling the OSGi annotations is built-in and doesn't require the configuration of a plugin.

```java
package io.github.mnl.osgiGettingStarted.simpleBundle;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.LogService;

@Component(service={})
public class HelloWorld implements Runnable {

    private static LogService logService;

    private Thread runner;
    
    @Reference
    private void setLogService(LogService logService) {
        this.logService = logService;
    }

    private void unsetLogService(LogService logService) {
        this.logService = null;
    }

    @Activate
    public void start(ComponentContext ctx) {
        runner = new Thread(this);
        runner.start();
    }
    
    @Deactivate
    public void stop(ComponentContext ctx) {
        runner.interrupt();
        try {
            runner.join();
        } catch (InterruptedException e) {
            logService.log(LogService.LOG_WARNING, 
                    "Could not terminate thread properly", e);
        }
    }

    @Override
    public void run() {
        System.out.println("Hello World!");
        while (!runner.isInterrupted()) {
            try {
                logService.log(LogService.LOG_INFO, "Hello Word sleeping");
                Thread.sleep (5000);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

}
```

The `@Component` annotation for the class looks similar the one in the Felix DM example. The parameter "`service`" serves the same purpose as the parameter "`provides`" of the DM annotation: it prevents our component from being registered as provider for service `java.lang.Runnable`.

Declarative services doesn't support injection of values into fields. Referenced services are made known to the component by invoking a setter method. Considering our example, that's not too bad because we can make the field with the reference to the log service static again[^apDM]. In order to avoid keeping a reference to a log service implementation even if it disappears (and our component is stopped), we have to provide an "unset" method as well[^ofs]. It doesn't need an annotation. Rather, if there is a "`setXYZ`" method with the `@Reference` annotation, declarative services automatically assume an "`unsetXYZ`" method to implement the corresponding "undo" operation.

[^apDM]: You can make Felix DM invoke a method, too.

[^ofs]: You don't need an "unset" method for ordinary (non-static) attributes because the instance of the service component itself is discarded when a component is deactivated.

As with Felix DM, you need a "Service Component Runtime" (as the OSGi specification calls it) 
to start up the service components. The SCR looks for `Service-Component` headers 
in all deployed bundles and creates service components, registers their 
services and activates the components according to the directives in the XML 
file. Of course, you can use any implementation. But in our environment, 
the easiest way is to add the bundle `org.apache.felix.scr` to the run bundles. 

Since Felix SCR has a dependency on an OSGi service called "Configuration Admin",
we actually have to add two bundles to the configuration, which results
in a `bnd.bnd` that looks like this:

```properties
-buildpath: \
	osgi.cmpn;version=4.3.1,\
	osgi.core;version=4.3.1,\
	osgi.annotation
-runbundles: \
	org.apache.felix.log,\
	org.apache.felix.gogo.command,\
	org.apache.felix.gogo.runtime,\
	org.apache.felix.gogo.shell,\
	de.mnl.osgi.log.fwd2jul,\
	org.apache.felix.configadmin,\
	org.apache.felix.scr
```

*To be continued*

---
