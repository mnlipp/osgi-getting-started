---
layout: default
title: "Interlude: Cleaning up"
date: 2016-04-29 12:00:00
---

# Interlude: Cleaning up

Now that we have some hands-on experience with OSGi and the first curiosity is satisfied, let's lean back and have a closer look at what OSGi is all about. This is more difficult than might be expected, because OSGi addresses several, mostly independent topics.

## Strong Modularity for Java

So far, this topic has only shown up when we used the `BundleActivator` for the first time in our own bundle. In order to get access to the interface in the runtime environment, we had to [add](SimpleBundle.html#need-for-import) the header "`Import-Package: org.osgi.framework`" to the `MANIFEST.MF`.

Modularity is about "separating the functionality of a program into independent, interchangeable modules" ([Wikipedia](https://en.wikipedia.org/wiki/Modular_programming)). The big challenge when doing a modular design is to find the right modules. The big challenge when *implementing* a modular design is to prevent developers from using some other module's classes that are not part of the module's public API. There is only a limited provision for this in Java. You can hide classes in a package by declaring them as "protected"[^novis]. This mechanism supports very simple modularity at the package level: a public class serves as API and protected classes in the same package implement the required functionality. The approach falls apart when your module becomes too complex to be implemented in a single package. If you use different packages for structuring your implementation, you have to change the visibility of the classes in sub-packages to public, else you won't be able to use them within your module[^jsr294].

[^wiki-mod]: 

[^novis]: Or declaring no visibility at all, as "protected" is the default visibility. 
 
[^jsr294]: A "fine grained" access control to the content of Java packages using "normal" Java syntax was proposed in the (now withdrawn) [JSR 294](http://jcp.org/en/jsr/detail?id=294). A lot of information that went along with it seems to have vanished from the web together with the withdrawal. I found some explanations in a [blog](https://community.oracle.com/docs/DOC-983193) and in an [article](http://www.javabeat.net/new-features-in-java-7-0-part-1/) about new features in Java 7 (written too early, obviously). The cause for the withdrawal is the advent of the "successor" JSR 376 (see below). And [politics](https://mmilinkov.wordpress.com/2006/10/20/java-component-war/). Java and modules seems to be a mined area.
 
One means to prevent programmers from using classes that are intended for a module's internal use only is to do regular dependency analysis using source code analysis tools[^byteCode]. Of course, this requires that the information about the modules' APIs (the permitted use of interfaces and classes of a module) is maintained properly and that the compliance with the rules is enforced organizationally[^fallorg].

[^byteCode]: When using Java, those are usually source code/byte code analysis tools, i.e. they can analyze dependencies even if only the byte code is available.

[^fallorg]: This usually falls apart when a developer claims that the only way to meet the deadline is to let him use a particular class, "no matter what the rules say". 

OSGi takes a different approach. Everything that a module comprises of is assembled in a jar (the "bundle"), and the Java package that provides the public API is explicitly declared in that jar's manifest. We didn't make use of this feature in our sample bundle (being only a consumer of a service, there was nothing to export), but an `Export-Package` header could be seen as one of the [headers of the Felix System Bundle](execution-environment.html#package-export-example). The OSGi framework controls the availability of the classes contained in the bundle at runtime (as specified by the `Export-Package` header) using special class loaders.

Making use of another module is, of course, also part of a module's interface. Such a dependency on a module that provides a specific package should be explicitly declared as well. This is enforced by OSGi, and it is the reason why we had to add the `Import-Package` header to our bundle's manifest. 

A nasty property of modules is that they tend to evolve over time. The changes can be bug-fixes, additional features offered by a package or more fundamental changes of the API. In order to be able to track the changes, it is necessary to assign a version to every published package and to clearly state the version (or version range) that a modules is prepared to accept for imported packages. 

In the OSGi context, version numbers follow the convention of [semantic versioning](https://www.osgi.org/wp-content/uploads/SemanticVersioning.pdf). The basic idea is that changes in the third part of the version number indicate bug fixes, i.e. the API is unchanged. An increment of the second part of the version number indicates a backward compatible change of the API (backward compatible from the point of view of the user of the API). A change of the first part indicates some major change of the API. Usually, modules programmed against version `1.a.b` will not be able to use an implementation with version `2.y.z`. This is the reason why Bndtools generated a version range `[1.6,2)` when we [added the import](AccessingAService.html#version-range) of the framework.

Support of modularity is a highly desirable feature in any non-trivial project. An alternative approach that focuses on this feature only is [JBoss Modules](https://docs.jboss.org/author/display/MODULES/Home). However, it seems that this approach hasn't caught on outside its initial usage in JBoss Application Server 7. An upcoming alternative to watch is [JSR 376:](https://jcp.org/en/jsr/detail?id=376) "Java Platform Module System"[^376fr], which is part of project [Jigsaw](http://openjdk.java.net/projects/jigsaw/).

[^376fr]: A [posting](http://mail.openjdk.java.net/pipermail/jpms-spec-experts/2015-December/000196.html) on the expert group's mailing list schedules a final release for March 2017. I found a nice "first impressions" [article](https://www.infoq.com/articles/Latest-Project-Jigsaw-Usage-Tutorial) by Nicolai Parlog  (and [another one](http://branchandbound.net/blog/java/2015/10/osgi-to-jigsaw/) by Sander Mak) about the JPMS (as they also refer to aspects of dynamics and services, I recommend to finish reading this part before reading those articles).


## Dynamic modules

The OSGi specification defines a life cycle for bundles. This includes that bundles can be installed into and removed from a running framework at any time. While this feature is certainly nice to play with, it causes developers a lot of trouble, especially the latter possibility to remove a bundle.  

Handling added modules is easy. It is common in an application that modules are started one after the other. Often, you are in control of the startup, because you have to invoke some kind of initialization method before being able to use the module. The required startup code can also be hidden in the module's implementation using a static object that triggers the execution of arbitrarily complex code as part of its initialization. The classloader logic ensures that any required classes are initialized before the triggering module. And after the initial startup, you can extend your program further by setting up additional class loaders and use them to load more classes.

The use case for removing classes from a running system is less common. It's not easy to do this because it requires that there are no more instances of the class that you want to remove. Enforcing this is effectively impossible. Our simple bundle is an example of the complexity of the problem[^mixms]. We cannot simply establish a reference to the log service at startup and then use it for the rest of the program. Rather, we have to provide a significant amount of code[^less-complex] to handle the case that the log service goes away, thus invalidating the reference. Of course, we could simple keep the reference despite the notification about the removal of the log service, which would prevent the log service object and its class definition from being garbage collected. But this attempt to adhere to the service doesn't guarantee that it continues to work. Chances are high the we get some `RuntimeException` if we call a method after the module has been removed. So we have to take all the trouble to either work around the unavailability of the other module or "invalidate" (stop) our own module if another required module becomes unavailable.

[^mixms]: I admit that I am mixing up module dependencies and service dependencies here. Take it as an illustration of the underlying problem. I don't want to complicate things by introducing a "pure module" example.

[^less-complex]: Let me emphasize again that there are means to greatly simplify this for the programmer (see below). But we should have a clear idea about what kind of functionality fully dynamic modules actually provide and at what cost.

There are two reasons why you would want to remove a module from a running system. The first is resource consumption. When OSGi started in 1999, hardware wasn't as cheap as it is today. I found a [post](http://aqute.biz/pipermail/osgi_aqute.biz/2005-September/000004.html) by Peter Kriens[^PK] from 2005 where he wanted to evaluate OSGi on a device with 32-64MB RAM and 2-8MB Flash, USB and Ethernet costing less than $100. Compare these requirements with what you get nowadays for $30 ([Raspberry Pi B+](https://www.raspberrypi.org/blog/price-cut-raspberry-pi-model-b-now-only-25/), 900MHz quad-core, 1GB RAM for $25; flash depending on the microSD, e.g. 8GB for $5). But of course, depending on the requirements, resources may still be too limited to put everything at work at once. Today's cheap availability of the resources, however, oftem leads to solving the problem on a different level. Take Android as an example. The apps aren't installed as modules into a single virtual machine. Rather, you have the "old fashioned pattern" of an operating systems that runs the apps as independent process.

[^PK]: One of the drivers behind OSGi.

The other reason why you'd want to remove a module is the desire to replace it with an improved (often bug-fixed) version without shutting down the complete system. Obviously, this doesn't apply to the common desktop application or a server application with a small group of users. It may be a point for an embedded system that is part of your infrastructure. And it may be a point for 24x7 enterprise systems -- though those usually have a fail-over concept that e.g. allows you to provide the new version on a secondary system and switch over. Consider the (additional) testing that has to be performed for bringing a new module into a running productive system. Keep in mind that "No matter how clean the framework, administrators will always prefer a freshly booted machine." ([Caucho Blog](http://blog.caucho.com/2009/06/15/why-osgi-is-cool-but-not-for-most-enterprise-apps/)) And, of course, consider that being able to replace a module does not mean that you have an uninterrupted service. Other modules may depend on the module being replaced and may go down until the substitute becomes available, just like our simple bundle will, when you replace the log service[^uwa].

[^uwa]: If there is a dependency between the modules on the service level only (and you have put the interface in a bundle of its own),  you can alternatively add the new version in parallel before removing the old version. Properly programmed consumers (such as our simple bundle) will switch to the new version on-the-fly by updating the reference. However, modules that take possession of a unique resource (e.g. a TCP server port) cannot coexist in two versions, so there are limits to this approach.

Dynamic modules are something that most scenarios don't need. Nevertheless, when using OSGi you buy into them and and this can cause of lot of additional effort if you don't make them easy to use (or even better: transparent) for the developers, using one of the techniques shown in the next parts of this introduction.

## Services

An important aspect of a modularized design is [loose coupling](https://en.wikipedia.org/wiki/Loose_coupling) between the components. The functionalities made available (called "services" by OSGi) are defined by Java interfaces. The challenge is to provide a mechanism that allows the user of a service to find a provider for the service, i.e. an instance of a class that implements the interface. JavaSE provides such a mechanism (since JavaSE 6) in the form of the [ServiceLoader](https://docs.oracle.com/javase/7/docs/api/index.html?java/util/ServiceLoader.html). The service loader implements a rather static approach. It searches all jars in the classpath for files "`META-INF/services/<interface name>`". The file with a given interface name contains the name of an implementation class for that interface.

The OSGi core framework defines a different, more dynamic approach as its "Service Layer". This  layer works almost independent of the module related features described above. It depends on bundles only in so far as a relationship between services and the bundles that provide them is maintained[^sl-mu]. This relationship is used by the framework to e.g. automatically remove all services provided by a bundle if this bundle is stopped.

[^sl-mu]: Which makes it impossible to use the service layer in your code without providing the code in bundles. My impression is that some people looking for a service layer stumble upon the OSGi platform, and then complain about its complexity when they notice that they have to build modularized applications in order to use the OSGi service layer.

At the core of the service layer is the service registry. It can be accessed using methods from the [BundleContext](https://osgi.org/javadoc/r6/core/index.html?org/osgi/framework/BundleContext.html). Bundles that provide services register them with the registry, usually in the activator's start method. Bundles that need a service look it up using `getServiceObjects`, which returns all registered services of the requested kind. The service objects are instances of classes that implement the required interface. After the initial lookup, there is no overhead in using an object returned by the service registry.

Changes of the service registry can be tracked by registering service listeners. This allows components to react to the appearance or disappearance of a service. (This is what we did in our simple bundle with the log service.) The service registry and its operations should be considered implementation primitives. They are powerful enough to build anything from them, but you'll want something more sophisticated for every day's work. Or, to [quote](http://blog.osgi.org/2010/03/services.html) Peter Kriens: "The original model of handling your service dependency manually is and was broken from the start". The service tracker was introduced as a minor amendment in release 2 (October 2001)[^shame]. But as we have seen, it fails to really simplify the implementation of dependent services.

[^shame]: I think it's unfortunate that an explicit reference to the very basic character of the service layer hasn't found its way into the specification of the service layer, not even into the latest release 6.

The only way to use the OSGi service layer efficiently is to wrap it in some "higher level" API. Actually, the best way to manage services is to externalize the task of registering and unregistering them by providing a component that takes care of the registration process based on information supplied by the bundles and also uses this information for dependency resolutions. There are several approaches to this (see next part). The good thing is that you don't have to make a final choice at once, as most solutions are based on the OSGi service layer and can therefore interoperate. The bad thing is that your application is rarely ever based on OSGi alone. Usually you'll have some OSGi based stack -- which implies an even steeper learning curve.

*To be continued*

---

