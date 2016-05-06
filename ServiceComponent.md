---
layout: default
title: Service Components
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

To get this working, we have to add the DM to the sets of "build" and "run" bundles. During runtime, the DM has additional required dependencies on the OSGi "Configuration Admin" and the "Metatype" services. So we have to add those as well. You can do this using the GUI (see below), of course. What you end up with are entries in the `bnd.bnd` similar to this:

```properties
-buildpath: \
        osgi.cmpn;version=4.3.1,\
        osgi.core;version=4.3.1,\
        org.apache.felix.dependencymanager
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

Regrettably, the Felix DM is in none of the Bndtools "standard repositories". An obvious fix would be to add the Apache Felix Repository (http://felix.apache.org/obr/releases.xml) in the same way as we added the "de.mnl.osgi" repository [before](UsingAService.html#add-repo). But -- at the time of this writing -- the Felix respository doesn't contain the latest version yet. So you have to [download](http://www-us.apache.org/dist//felix/org.apache.felix.dependencymanager-r8-bin.zip) the latest version, unpack the `org.apache.felix.dependencymanager-x.y.z.jar` and add it to the local repository as described in Bndtools [FAQ](http://bndtools.org/faq.html). If you have checked out the [companion project](https://github.com/mnlipp/osgi-getting-started) for this tutorial, it's already there, of course.

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

@Component
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

Note that we don't need an activator any more. The annotations specify the same information as the API calls in the previous example and should be self-explanatory. In order to make them known to the java compiler, you have to add `org.apache.felix.dependencymanager.annotation-x.y.z.jar` to the build path (as you did before with `org.apache.felix.dependencymanager`, see above). The annotations are used to create a file `META-INF/dependencymanager/io.github.mnl.osgiGettingStarted.simpleBundle.HelloWorld` in the bundle during build. Using the code above, the file looks like this:

```json
{"impl":"io.github.mnl.osgiGettingStarted.simpleBundle.HelloWorld",
 "stop":"stop",
 "provides":["java.lang.Runnable"],
 "start":"start",
 "type":"Component"}
{"service":"org.osgi.service.log.LogService",
 "autoConfig":"logService",
 "type":"ServiceDependency",
 "required":"true"}
```

There are two ways to add the additional build step for creating this file to the `bnd` packaging tool. If you want to use the dependency manager only in a single project, you can add a line

```
-plugin: org.apache.felix.dm.annotation.plugin.bnd.AnnotationPlugin;\
path:="${workspace}/cnf/localrepo/org.apache.felix.dependencymanager.annotation-4.1.0.jar";\
log=debug
```

to the project's `bnd.bnd`. If you want to enable support for the whole (bnd-)workspace, add the line to the `build.bnd` in the `cnf` project.

If you're impatient and run the compiled bundle now, nothing's going to happen. As there is no activator any more, another mechanism must notice if a bundle with files `META-INF/dependencymanager/*` in it is started, read the information from the files and act accordingly. Such a mechanism is provided by the dependency manager as bundle `org.apache.felix.dependencymanager.runtime-x.y.z.jar`. Add it to the run bundles and start it again.


*To be continued*
