---
layout: default
title: Tracking a Service
---

# Tracking a Service

First of all, to fully expose all the problems, let's really do some work in the sample service.

```java
public class HelloWorld extends Thread {

    @Override
    public void run() {
        System.out.println("Hello World!");
        while (!isInterrupted()) {
            try {
                // Activator.logService.log(LogService.LOG_INFO, "Hello Word sleeping");
                sleep (5000);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
    
}
```

Though the (intended) usage of the log service is still commented out, it is obvious that eventually the thread can only be run successfully if the log service is available[^ms].

[^ms]: Most examples consider the dependency on the log service an optional dependency and fall 
    back to e.g. printing to `System.err` if it is not available. I consider this dubious at best.
    I expect log messages to appear in the log and I expect them to appear there reliably.

The startup code must be updated to something similar to this:

```java
...
import org.osgi.service.log.LogService;

public class Activator implements BundleActivator {
    ...
    @Override
    public void start(BundleContext context) throws Exception {
        LogService logService = context.getServiceReference(
                context.getServiceReference(LogService.class));
        helloWord = new HelloWorld();
        helloWorld.start();
        ...
    }
    ....
}
```

The problem with this simple approach is that the method `getServiceReference` returns a service only if it is already registered. If it is not (yet) available, a listener can be added to the `BundleContext` to avoid "busy waiting". The listener is informed about changes of service states. So simply put, it should be possible to check whether the log service already exists and, if it is the case, start our service. Else we register a listener and wait for the log service to appear and start our service then. The details, however, are a bit tricky. For example, what happens if the log service appears after we have done the initial check but before we have registered the listener? That's why OSGi includes a [ServiceTracker](https://osgi.org/javadoc/r6/core/index.html?org/osgi/framework/BundleContext.html) class, which is supposed to simplify things a bit.

We want our service thread to start only if a log service is available. And we want it to stop when the log service becomes unavailable. The following code shows how this can be done using the `ServiveTracker` (as before, you can checkout the [project](https://github.com/mnlipp/osgi-getting-started/tree/master/SimpleBundle-logging) with this code).

```java
public class Activator implements BundleActivator {

    private ServiceTracker<LogService, LogService> logServiceTracker;
    static public LogService logService = null;
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
                    LogService result = super.addingService(reference);
                    // If there are several services available, getService() returns the
                    // preferred one. But we cannot use getService() here because the new
                    // service hasn't been added to the tracked services yet (we're in 
                    // the process of adding). We have to decide on our own.
                    if (isPreferred(reference)) {
                        logService = result;
                    }
                    // The required service has become available, so we should 
                    // start our service if it hasn't been started yet.
                    if (helloWorld == null) {
                        System.out.println("Hello World started.");
                        helloWorld = new HelloWorld();
                        helloWorld.start();
                    }
                    return result;
                }

                // When having several services, which one should be used? The ServiceTracker
                // prefers services with higher ranking and, if there is a tie, the service 
                // with the lowest service id.
                private boolean isPreferred(ServiceReference<LogService> candidate) {
                    // Details removed here, see project source.
                    ...
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
                    // may have become the "current version". As this is called after
                    // the service has been removed from the tracker, we can simply
                    // invoke getService() to get the preferred remaining service object
                    // (if here is still one left).
                    LogService nowCurrent = getService();
                    if (nowCurrent != null) {
                        logService = nowCurrent;
                        return;
                    }
                    // If no logging service is left, we have to stop our component.
                    if (helloWorld != null) {
                        helloWorld.interrupt();
                        try {
                            helloWorld.join();
                        } catch (InterruptedException e) {
                        }
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
Hello World!
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
g! start 1
Hello World started.
Hello World!
g! 
```

The required code is quite complex because it has to handle some conditions that aren't obvious if you come from most other Java component models[^po]. The basic approach is to grab the service object in `addingService` and start the `HelloWorld` thread, and then stop this thread again in `serviceRemoved`. This should allow the thread to reliably use the log service, because with respect to its life cycle, a service object is guaranteed to be usable between the invocations of `addingService` and `serviceRemoved` for that service object.

[^po]: As has been pointed out to me in a [discussion](https://mail.osgi.org/pipermail/osgi-dev/2016-April/005149.html) on the osgi-dev list.

As an additional complexity besides the dynamic appearance and disappearance of services, there can also be more than one service with the same service interface at the same time. As an example, it's possible to have two persistence services with different back ends installed and running in parallel[^mti]. A special case -- that makes more sense in the context of the log service -- is having a service twice after installing (and starting) a newer version in a running system[^av].

[^mti]: There's much more to it. But that's for later.

[^av]: Quite handy if you have to provide 24x7 availability and want to apply a patch.

Due to this OSGi feature, we have to take into consideration that `addingService` and `serviceRemoved` may be called several times. In order to get the "current" service object, the `ServiceTracker` provides `getService()` that is supposed to be called each time we need the service. There are two problem with that, however.  

Method `addingService` is called *before* the service has been added to the set of tracked services. As the `HelloWorld` thread is started in `addingService`, a call to `getService()` from that newly started thread may fail because it may occur before `addingService` returns and the service is added to the services known to the `ServiceTracker`. Likewise, `serviceRemoved` is called *after* the service has been removed from the service tracker. So there is a short time span between the service having been removed from the tracker already and stopping `HelloWord` thread in `serviceRemoved`. When the last (or only) log service object is removed, a call to `logServiceTracker.getService()` returns `null` during that time span. 

Therefore, calling `getService()` from the `HelloWorld` thread is not an option. Rather, it is necessary to cache the reference to the log service and update it after each change. Updating the reference in `serviceRemoved` is simple, because the `ServiceTracker` is up to date (the service has been removed already). In `addingService`, however, we have to find out whether the new service will be preferred over an already tracked service *after* `addingService` has finished. It's possible, but makes things even more complicated, of course.



---
