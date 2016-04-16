---
layout: default
title: Accessing a Service
---

# Accessing a Service

A major benefit of using a framework is that you can reuse existing components in an architecturally well defined way. In this section we're going to use the OSGi logging service to improve the logging behavior of our simple bundle.

The OSGi framework manages the available services and makes them available via the [BundleContext](https://osgi.org/javadoc/r6/core/org/osgi/framework/BundleContext.html) interface. An implementation of the `BundleContext` is passed to the `start` method of the `BundleActivator`. Maybe you had noticed this parameter already in our activator implementation:

```java
public class Activator implements BundleActivator {
    ...
    @Override
    public void start(BundleContext context) throws Exception {
        System.out.println("Hello World started.");
        helloWorld = new HelloWorld();
    }
    ...
}

```

The obvious choice for accessing a service is the `BundleContext.getService` method. It needs a `ServiceReference` parameter. Luckily, we can find a method `getServiceReference` right below the `getService` in the documentation. `ServiceReference` comes in two flavors. One takes the class as Java type, the other as a string. In both cases the class is presumably the main interface of the service that we want to use. The log service is part of all OSGi specifications, no matter whether they target residential or enterprise environments. [Download](https://www.osgi.org/developer/specifications/) either specification and have a look at the service description.

The full name of the log service interface is `org.osgi.service.log.LogService`. So let's try this code:

```java
...
import org.osgi.service.log.LogService;

public class Activator implements BundleActivator {
    ...
    @Override
    public void start(BundleContext context) throws Exception {
        LogService logService = context.getServiceReference(
                context.getServiceReference(LogService.class));
        ....
    }
    ....
}
```

You should see several error markers. First of all, `LogService` is unknown. That's okay, because up to now, we only needed the OSGi core and had therefore only a jar with the core API in the classpath. Go to the "Build" tab of the `bnd.bnd` editor and add `osgi.residential` to the "Build Path". Save, and Bndtools updates the Eclipse project's library path accordingly.

Going back to the source, one error marker remains. The parameter of type `Class` is not accepted. Reading carefully through the method's [JavaDoc](https://osgi.org/javadoc/r6/core/org/osgi/framework/BundleContext.html#getServiceReference(java.lang.Class)), you will find that the flavor accepting this parameter only exists since version 1.6 of the API. This version was first included in the OSGi platform specification 4.3. So, if we want to use this method, we have to make sure that the environment provides at least this version. Managing application module's versions is one of the big topics of OSGi. If you come from building some simple Java application[^mv], this might surprise you. In an enterprise environment, however, this is definitely an issue. 

You may have noticed already that it is possible to choose versions when adding something to the build path in the Bndtools dialog (first, remove OSGi core again, else it won't be offered):

![Choosing a version](images/Bndtools-version-dialog.png){: width="500px" }

Clicking "Add" and saving results in an extended `buildpath` entry in `bnd.bnd`[^ov] and an extended entry in the generated jar's `MANIFEST.MF`[^rl]:

```properties
Import-Package: org.osgi.framework;version="[1.6,2)"
```

The "version interval" uses the notation known from mathematics: at least version 1.6, higher versions (e.g. 1.6.1, 1.7) are okay, but the version must be less than 2.0[^ug].

Now that we know how to easily run oder debug an OSGi application from Eclipse (using the "Run" or "Debug" buttons on the "Run" tab of the `bnd.bnd` editor), we can check the effect of our statement simply by setting a breakpoint after it. Do this and start the application in debug mode. You should get: 

```
Failed to start bundle SimpleBundle-logging-1.1.0, exception Unable to resolve 
SimpleBundle-logging [1](R 1.0): missing requirement [SimpleBundle-logging [1](R 1.0)] 
osgi.wiring.package; (&(osgi.wiring.package=org.osgi.service.log)(version>=1.3.0)
(!(version>=2.0.0))) Unresolved requirements: [[SimpleBundle-logging [1](R 1.0)] 
osgi.wiring.package; (&(osgi.wiring.package=org.osgi.service.log)(version>=1.3.0)
(!(version>=2.0.0)))]
```

Read the message carefully and try to understand it. You may encounter similar messages later when the cause is less obvious. Note how version constraints are translated to [filter](https://osgi.org/javadoc/r6/core/org/osgi/framework/Filter.html) expressions that can be easily evaluated by the framework. It's a quite intuitive prefix notation known from LDAP search filters.

What the message comes down to is that there is no logging service in the runtime environment. So go back to the "Run" tab of the `bnd.bnd` editor. Add `org.apache.felix.log` and debug again.

![Adding felix log](images/Adding-felix-log.png){: width="400px" }

When the program stops, have a look at the variable `logService`. If you're lucky, it has been assigned a value like `org.apache.felix.log.LogServiceImpl@45afc369`. You have to be lucky, because activation of services happens in no specific order. The fact that our bundle states a dependency on `org.osgi.service.log` in its `MANIFEST.MF` does not imply that the OSGi framework constructs a dependency graph and activates services with dependencies only after the services that they depend on have been activated[^dss]. This would only solve part of the problem anyway, because -- as we well know from playing around with the Felix console -- bundles (and the services that they provide) can be started and stopped at any time[^sr].

The method `getServiceReference` returns a service only if it is registered. If it is needed but not (yet) available, a listener can be added to the `BundleContext`. The listener is then informed about changes of service states. The details are a bit tricky. That's why OSGi includes a [ServiceTracker](https://osgi.org/javadoc/r6/core/index.html?org/osgi/framework/BundleContext.html) class, which simplifies things a bit.

Usually, the availability of a log service is considered optional. If it's not there, your application can still run, you just don't get log entries. In this example, however, I'm going to model a mandatory dependency on the log service. Our component starts only if a log service is available. And it stops, when the log service becomes unavailable. The following code shows how this can be done.

```java
public class Activator implements BundleActivator {

    private ServiceTracker<LogService, LogService> logServiceTracker;
    private LogService logService = null;
    private HelloWorld helloWorld;

    /** Our component isn't started immediately like before. Rather, a service tracker
     * is created and opened. Our component will only be started when the service
     * tracker detects the availability of all required services. */
    @Override
    public void start(BundleContext context) throws Exception {
        if (logServiceTracker == null) {
            // When called for the first time, create a new service tracker 
            // that tracks the availability of a log service.
            logServiceTracker = new ServiceTracker<LogService, LogService>(
                    context, LogService.class, null) {
                /** This method is invoked when a service (of the kind tracked)
                 * is added. In our use case, this means that the log service is 
                 * available now and that our component can be started. */
                @Override
                public LogService addingService(ServiceReference<LogService> reference) {
                    logService = super.addingService(reference);
                    // The required service has become available, so we should 
                    // start our service if it hasn't been started yet.
                    if (helloWorld == null) {
                        System.out.println("Hello World started.");
                        helloWorld = new HelloWorld();
                    }
                    return logService;
                }

                /** This method is invoked when a service is removed. Since we model
                 * a strong relationship between our component and the log service,
                 * our component must be stopped when there's no log service left.
                 * Note that the service tracker remains open (active). When a log
                 * service becomes available again, our component will be restarted. */
                @Override
                public void removedService(ServiceReference<LogService> reference,
                                           LogService service) {
                    super.removedService(reference, service);
                    // After removing this service, another version of the service
                    // may have become the "current version".
                    LogService nowCurrent = getService();
                    if (nowCurrent != null) {
                        logService = nowCurrent;
                        return;
                    }
                    // If no logging service is left, we have to stop our component.
                    if (helloWorld != null) {
                        helloWorld = null;
                        System.out.println("Hello World stopped.");
                    }
                    // Release any left over reference to the log service.
                    logService = null;
                }
            };
        }
        // Now activate (open) the service tracker.
        logServiceTracker.open();
    }

    /** As before, this method stops our component, but in a different way. It stops 
     * (closes) the service tracker (because we don't want our component to be 
     * reactivated only because a log service (re-)appears). As this causes the
     * ServiceTracker to call removedService for the tracked service(s), this will
     * also stop our service. */
    @Override
    public void stop(BundleContext context) throws Exception {
        logServiceTracker.close();
    }
}
```

Run this using a configuration that also includes the Felix console. When the application has started, list the bundles and start and stop the log service and our Hello World component in varying order. Look at the output and understand the start/stop messages from our component's activator. Here's an example:

```
Hello World started.
____________________________
Welcome to Apache Felix Gogo

g! lb
START LEVEL 1
   ID|State      |Level|Name
    0|Active     |    0|System Bundle (5.2.0)
    1|Active     |    1|Apache Felix Log Service (1.0.1)
    2|Active     |    1|Apache Felix Gogo Command (0.14.0)
    3|Active     |    1|Apache Felix Gogo Runtime (0.16.2)
    4|Active     |    1|Apache Felix Gogo Shell (0.10.0)
    5|Active     |    1|SimpleBundle-logging (1.1.0)
g! stop 1
Hello World stopped.
g! 
```

The code handles some conditions that may not be obvious if you come from "ordinary" Java component models[^po]. Using OSGi, there can be more than one service with the same service interface. Though this is not very useful in the case of the logging service, several services with the same interface can, in general, coexists. As an example, you can have two persistence services with different back ends installed and running in parallel[^mti]. A special case -- that also makes sense in the case of the log service -- is having a service twice after installing (and starting) a newer version in a running system[^av].

[^mti]: There's much more to it. But let's leave it a bit vague for now.

[^av]: Quite handy if you have to provide 24x7 availability and want to apply a patch.

 In general,  `addingService` and `serviceRemoved` may be called several times in no specific order. In order to get the "current" service object, you could call `logServiceTracker.getService()` each time you need the service. There's a problem with that, however. Considering its life cycle, a service object is guaranteed to be usable between the invocations of `addingService` and `serviceRemoved` for that service object. But `serviceRemoved` is called *after* the service has been removed from the service tracker. So there is a short time span between the service having been removed from the tracker already and stopping our own service in `serviceRemoved`. When the last (or only) log service object is removed, a call to `logServiceTracker.getService()` returns `null` during that time span. Therefore it is necessary to cache the reference to the log service and update it after each change.  

 
---

[^mv]: Rule of thumb: get the latest versions of all libraries and hope that they provide backward compatibility for parts of the application built with older version, right? Well, often this works surprisingly well... 

[^ov]: There's some Bndtools magic happening here. The entry in `bnd.bnd` reflects exactly
    what you see in the GUI:
    
    ```
    -buildpath: \
	    osgi.residential,\
	    osgi.core;version=4.3
    ```
    
    When you try to build this using gradle, however, it fails with `error: type ServiceTracker does 
    not take parameters`. The reason is that the OSGi libraries provided for version 4.3.0 have been 
    compiled [with a special flag](http://blog.osgi.org/2012/10/43-companion-code-for-java-7.html) 
    that allows generics to be used in pre-Java 5 JVMs. Starting with Java 7, this 
    "trick" doesn't work anymore. Therefore the OSGi alliance has released jars built "in the
    ordinary way" (compatible with Java 7 and beyond) as version 4.3.1. To complicate things a bit,
    there is no 4.3.1 release of the jar with the "residential" subset of services. Only the
    "companion" jar with the full set of services has been re-released.
    
    As the 4.3.1 versions aren't offered in the `bnd.bnd` GUI dialog, you have to change 
    the version "manually" in the source view[^lv]:
    
    ```
    -buildpath: \
	    osgi.cmpn;version=4.3.1,\
	    osgi.core;version=4.3.1
    ```
    
    Using this configuration, the project builds both in Eclipse and "headless" using gradle.

[^lv]: Alternatively, you can choose version 6.0.0 for both jars. So why bother?
    The versions that you choose here result in a requirement for the runtime framework.
    If you choose 6.0.0, you'll have to use a framework that 
    [supports](https://en.wikipedia.org/wiki/OSGi_Specification_Implementations) this version
    of the OSGi specification.

[^rl]: Slightly simplified. If you have already added `osgi.residential` you actually see this:

    ```properties
    Import-Package: org.osgi.framework;version="[1.6,2)",org.osgi.service.log;version="[1.3,2)"
    ```

    Since `MANIFEST.MF` is basically a properties file, keys have to be unique. So if there are several imports, the value of the key `Import-Package` becomes an enumeration of imported packages.


[^ug]: The upper limit is an automatic guess, of course. If there will ever be a version 2.x of the API, chances are high that our code will still run. Usually, however, a change of the major version indicates some incompatible change of the API. So by excluding 2.0 and anything beyond, we're on the safe side. 

[^dss]: But this does sound alluring, doesn't it? Well, that's why OSGi has added such a concept as "Declarative Services". But let's stick to the "mid level API" provided by the `ServiceTracker` utility class for now.

[^sr]: Bundles that provide a service usually register the service when they are started and unregister it when they are stopped.

[^po]: As has been pointed out to me in a [discussion](https://mail.osgi.org/pipermail/osgi-dev/2016-April/005149.html) on the osgi-dev list.

