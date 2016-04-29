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

Support of modularity is a highly desirable feature in any non-trivial project. An alternative approach that focuses on this feature only is [JBoss Modules](https://docs.jboss.org/author/display/MODULES/Home). However, it seems that this approach hasn't caught on outside its initial usage in JBoss Application Server 7. An upcoming alternative to watch is [JSR 376](https://jcp.org/en/jsr/detail?id=376)[^376fr], which is part of project [Jigsaw](http://openjdk.java.net/projects/jigsaw/).

[^376fr]: A [posting](http://mail.openjdk.java.net/pipermail/jpms-spec-experts/2015-December/000196.html) on the expert group's mailing list schedules a final release for March 2017. There have, however, been attempts before to introduce modularization into the "Java core". Searching through the web, you find a lot of doubts about that schedule (and the final feature scope of the JSR).

## Dynamic modules

The OSGi specification defines a life cycle for bundles. This includes that bundles can be installed into and removed from a running framework at any time. While this feature is certainly nice to play with, it causes developers a lot of trouble, especially the latter possibility to remove a bundle.  

Handling added modules is easy. It is common in an application that modules are started one after the other. Often, you are in control of the startup, because you have to invoke some kind of initialization method before being able to use the module. The required startup code can also be hidden in the module's implementation using a static object that triggers the execution of arbitrarily complex code as part of its initialization. The classloader logic ensures that any required classes are initialized before the triggering module. And after the initial startup, you can extend your program further by setting up additional class loaders and use them to load more classes.

The use case for removing classes from a running system is less common. It's not easy to do this because it requires that there are no more instances of the class that you want to remove. Enforcing this is effectively impossible. Our simple bundle is an example of the complexity of the problem. We cannot simply establish a reference to the log service at startup and then use it for the rest of the program. Rather, we have to provide a significant amount of code[^less-complex] to handle the case that the log service goes away, thus invalidating the reference. Of course, we could simple keep the reference despite the notification about the removal of the log service, which would prevent the log service object and its class definition from being garbage collected. But this attempt to adhere to the service doesn't guarantee that it continues to work. Chances are high the we get some `RuntimeException` if we call a method after the module has been removed. So we have to take all the trouble to either work around the unavailability of the other module or "invalidate" (stop) our own module if another required module becomes unavailable.

[^less-complex]: Let me emphasize again that there are means to greatly simplify this for the programmer (see below). But we should have a clear idea about what functionality they actually provide.

There are two reasons why you would want to remove a module from a running system. The first is resource consumption. When OSGi started in 1999, hardware wasn't as cheap as it is today. I found a [post](http://aqute.biz/pipermail/osgi_aqute.biz/2005-September/000004.html) by Peter Kriens[^PK] from 2005 where he wanted to evaluate OSGi on a device with 32-64MB RAM and 2-8MB Flash, USB and Ethernet costing less than $100. Compare these requirements with what you get nowadays for $30 ([Raspberry Pi B+](https://www.raspberrypi.org/blog/price-cut-raspberry-pi-model-b-now-only-25/), 900MHz quad-core, 1GB RAM for $25; flash depending on the microSD, e.g. 8GB for $5). But of course, depending on the requirements, resources may still be too limited to put everything at work at once. The cheap availability of the resources, however, usually leads to solving the problem at a different level. Take Android as an example. The apps aren't installed as modules into a single virtual machine. Rather, you have the old fashioned pattern of an operating systems that runs the apps as independent process.

[^PK]: One of the drivers behind OSGi.

The other reason why you'd want to remove a module is the desire to replace it with an improved (often bug-fixed) version without shutting down the complete system. Obviously, this doesn't apply to the common desktop application or a server application with a small group of users. It may be a point for an embedded system that is part of your infrastructure. And it may be a point for 24x7 enterprise systems -- though those usually have a fail-over concept that e.g. allows you to provide the new version on a secondary system and switch over. Consider the (additional) testing that has to be performed for bringing a new module into a running productive system. Keep in mind that "No matter how clean the framework, administrators will always prefer a freshly booted machine." ([Caucho Blog](http://blog.caucho.com/2009/06/15/why-osgi-is-cool-but-not-for-most-enterprise-apps/)) And, course, consider that being able to replace a module does not mean that you have uninterrupted service. Other modules may depend on the module being replaced and may go down until the substitute becomes available, just like our simple bundle will, when you replace the log service[^uwa].

[^uwa]: If there is a dependency between the modules on the service level only,  you can alternatively add the new version in parallel before removing the old version. Properly programmed consumers (such as our simple bundle) will switch to the new version one on-the-fly by updating the reference.

Dynamic modules are something that most scenarios don't need. Nevertheless, when using OSGi you buy into them and and this can cause of lot of additional effort if you don't make them easy to use (or even better: transparent) for the developers, using one of the techniques shown in the next parts of this introduction.

## Services

*To be continued*

---

