---
layout: default
title: Using a Service
---

# Using a Service

A major benefit of using a framework is that you can reuse existing components in an architecturally well defined way. In this section we're going to use the OSGi logging service to improve the logging behavior of our simple bundle.

The OSGi framework manages the available services and makes them available via the [BundleContext](https://osgi.org/javadoc/r6/core/org/osgi/framework/BundleContext.html) interface. An implementation of the `BundleContext` is passed to the `start` method of the `BundleActivator`. Maybe you had noticed this parameter already in our activator implementation:

```
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

The obvious choice for accessing a service is the `getService` method. It needs a `ServiceReference` parameter. Luckily, we can find a method `getServiceReference` right blow the `getService` in the documentation. `ServiceReference` comes in two flavors. One takes the class as Java type, the other as a string. In both cases the class is presumably the main interface of the service that we want to use. The log service is part of all OSGi specifications, no matter whether they target residential or enterprise environments. [Download](https://www.osgi.org/developer/specifications/) either specification and have a look at the service description.

The full name of the log service interface is `org.osgi.service.log.LogService`. So let's try this code:

```
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

You should see several error markers. First of all, `LogService` is unknown. That's okay, because up to now, we only needed the OSGi core and had therefore only a jar with the core API in the classpath. Go to the "Build" tab of `bnd.bnd` and add `osgi.residential` to the "Build Path". 

Going back to the source, one error marker remains. The parameter of type `Class` is not accepted. Reading carefully through the method's [JavaDoc](https://osgi.org/javadoc/r6/core/org/osgi/framework/BundleContext.html#getServiceReference(java.lang.Class)), you can find that the flavor accepting this parameter only exists since version 1.6 of the API. This version was first included in the OSGi platform specification 4.3. So, if we want to use this method, we have to make sure that the environment provides at least this version. Managing component's versions is one of the big topics of OSGi. If you come from building some simple Java application, this might surprise you. In an enterprise environment, however, this is definitely an issue. 

You may have noticed that it is possible to choose versions when adding something to the build path in the Bndtools dialog (first, remove OSGi core again, else it won't be offered):

![Choosing a version](images/Bndtools-version-dialog.png){: width="500px" }

Clicking "Add" results in an extended entry in `MANIFEST.MF`[^rl]:

```properties
Import-Package: org.osgi.framework;version="[1.6,2)"
```

The "version interval" uses the notation known from mathematics: at least version 1.6, higher versions (1.x) are okay, but the version must be less than 2.0[^ug].



*To be continued*

---

[^rl]: Slightly simplified. If you have already added `osgi.residential` you actually see this:

    ```properties
    Import-Package: org.osgi.framework;version="[1.6,2)",org.osgi.service.log;version="[1.3,2)"
    ```

    Since `MANIFEST.MF` is basically a properties file, keys have to be unique. So if there are several imports, the value of the key `Import-Package` becomes an enumeration of imported packages.


[^ug]: The upper limit is an automatic guess, of course. If there will ever be a version 2.x of the API, chances are high that our code will still run. Usually, however, a change of the major version indicates some incompatible change of the API. So by excluding 2.0 and anything beyond, we're on the safe side. 
