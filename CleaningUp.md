---
layout: default
title: "Interlude: Cleaning up"
date: 2016-04-24 12:00:00
---

# Interlude: Cleaning up

Now that we have some hands-on experience with OSGi and the first curiosity is satisfied, let's lean back and have a closer look at what OSGi is all about. This is more difficult than might be expected, because OSGi addresses several, mostly independent topics.

## Strong Modularity for Java

So far, this topic has only shown up when we used the `BundleActivator` for the first time in our own bundle. In order to get access to the interface in the runtime environment, we had to [add](SimpleBundle.html#need-for-import) the header "`Import-Package: org.osgi.framework`" to the `MANIFEST.MF`.

Modularity is about "separating the functionality of a program into independent, interchangeable modules"[^wiki-mod]. The big challenge when doing a modular design is to find the right modules. The big challenge when *implementing* a modular design is to prevent developers from using some other module's classes that are not part of the module's public API. There is only a limited provision for this in Java. You can hide classes in a package by declaring them as "protected"[^novis]. This mechanism supports very simple modularity at the package level: a public class serves as API and protected classes in the same package implement the required functionality. The approach falls apart when your module becomes too complex to be implemented in a single package. If you use different packages for structuring your implementation, you have to change the visibility of the classes in sub-packages to public, else you won't be able to use them within your module[^jsr294].

[^wiki-mod]: Wikipedia: [Modular programming](https://en.wikipedia.org/wiki/Modular_programming)

[^novis]: Or declaring no visibility at all, as "protected" is the default visibility. 
 
[^jsr294]: A "fine grained" access control to the content of Java packages using "normal" Java syntax was proposed in the (now withdrawn) [JSR 294](http://jcp.org/en/jsr/detail?id=294). A lot of information that went along with it seems to have vanished from the web together with the withdrawal. I found some explanations in a [blog](https://community.oracle.com/docs/DOC-983193) and in an [article](http://www.javabeat.net/new-features-in-java-7-0-part-1/) about new features in Java 7 (written too early, obviously). 
 
One means to prevent programmers from using classes that are intended for a module's internal use only is to do regular dependency analysis using source code analysis tools[^byteCode]. Of course, this requires that the information about the modules' APIs (the permitted use of interfaces and classes of a module) is maintained properly and that the compliance with the rules is enforced organizationally[^fallorg].

[^byteCode]: When using Java, those are usually source code/byte code analysis tools, i.e. they can analyze dependencies even if only the byte code is available.

[^fallorg]: This usually falls apart when a developer claims that the only way to meet the deadline is to let him use a particular class, "no matter what the rules say". 

OSGi takes a different approach. Everything that a module comprises of is assembled in a jar (the "bundle"), and the Java package that provides the public API is explicitly declared in that jar's manifest. We didn't make use of this feature in our sample bundle (being only a consumer of a service, there was nothing to export), but an `Export-Package` header could be seen as one of the [headers of the Felix System Bundle](execution-environment.html#package-export-example). The OSGi framework controls the availability of the classes contained in the bundle at runtime (as specified by the `Export-Package` header) using special class loaders.

Making use of another module is, of course, also part of a module's interface. Such a dependency on a module that provides a specific package should therefore also be explicitly declared. This is enforced by OSGi as well, and it is the reason why we had to add the `Import-Package` header to our bundle's manifest. 

A nasty property of modules is that they tend to evolve over time. The changes can be bug-fixes, additional features offered by a package or more fundamental changes of the API. It is therefore necessary to assign a version to every published package and to clearly state the version (or version range) that a modules is prepared to accept for imported packages. 

In the OSGi context, version numbers follow the convention of [semantic versioning](https://www.osgi.org/wp-content/uploads/SemanticVersioning.pdf). The basic idea is that changes in the third part of the version number indicate bug fixes, i.e. the API is unchanged. An increment of the second part of the version number indicates a backward compatible change of the API (backward compatible from the point of view of the user of the API). A change of the first part indicates some major change of the API. Usually, modules programmed against version `1.a.b` will not be able to use an implementation with version `2.y.z`. This is the reason why Bndtools generated a version range `[1.6,2)` when we [added the import](AccessingAService.html#version-range) of the framework.

Support of modularity is a highly desirable feature in any non-trivial project. An alternative approach that focuses on this feature only is [JBoss Modules](https://docs.jboss.org/author/display/MODULES/Home). However, it seems that this approach hasn't caught on outside its initial usage in JBoss Application Server 7. An upcoming alternative to watch is [JSR 376](https://jcp.org/en/jsr/detail?id=376)[^376fr], which is part of project [Jigsaw](http://openjdk.java.net/projects/jigsaw/).

[^376fr]: A [posting](http://mail.openjdk.java.net/pipermail/jpms-spec-experts/2015-December/000196.html) on the expert group's mailing list schedules a final release for March 2017. There have, however, been attempts before to introduce modularization into the "Java core". Searching through the web, you find a lot of doubts about that schedule (and the final feature scope of the JSR).


*To be continued*



[^LB]: At least if you stumbled upon OSGi while searching for a component framework. As mentioned before, OSGi bundles are actually just modules, they don't have to be (or contain) "active" components. Modules only contain some classes implementing an aspect of functionality (sometimes forming a generally usable library) and usually don't need to be started. Bundles that provide modules must have an `Export-Package` entry in `MANIFEST-MF` in order to make the classes from the module available to other bundles. We'll come back to that later.

[^ds]: Note that an activator class is not the only way to start (and stop) a bundle. The runtime environment can also provide the `OSGi Declarative Services` (in the case of felix you have to add them explicitly, they don't come "out of the box"). The OSGi's Wiki has a short introduction into [Declarative Services](http://wiki.osgi.org/wiki/Declarative_Services).