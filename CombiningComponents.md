---
layout: default
title: Combining Components
date: 2017-10-02 12:00:00
---

# From Components to Modules and Services

Let's have a look at what we have achieved so far. We can write a bundle with a
component, some startup and shutdown code, and we can deploy it in the OSGi
framework. While this is nice to have, component based development only makes
sense if we can have several components that build on each other and interact.

Having several components is easy, we simply write several bundles. But how
about the interaction? Can classes from one component simply be used by another 
component? Well, they cannot. As I mentioned in a footnote earlier, OSGi is on its
basic layer not a component framework. Rather, its a framework for managing
modules -- which sometimes happen to be components[^several]. 

[^several]: OSGi, with its various specifications, addresses several, 
mostly independent topics. This makes articles about OSGi sometimes hard
to understand. At its core, it really is about managing modules.

## Strong Modularity for Java

Modularity is about "separating the functionality of a program into independent, 
interchangeable modules" 
([Wikipedia](https://en.wikipedia.org/wiki/Modular_programming)). 

So far, the topic of modularity has only shown up when we used the 
`BundleActivator` for the first time in our own bundle. In order to get 
access to the interface in the runtime environment, we had to 
[add](SimpleBundle.html#need-for-import) the header 
"`Import-Package: org.osgi.framework`" to the `MANIFEST.MF`.

When using OSGi, everything that a module comprises of is assembled 
in a jar (the "bundle"), and the Java package that provides the public API is 
explicitly declared as such in that jar's manifest. We didn't make use of this feature 
in our sample bundle (as we don't provide anything, there is nothing to export), 
but an `Export-Package` header could be seen as one of the 
[headers of the Felix System Bundle](execution-environment.html#package-export-example). 

Making use of another module is, of course, also part of a module's interface. 
Such a dependency on a module that provides a specific package should be 
explicitly declared as well. This is enforced by OSGi, and this is the reason 
why we had to add the `Import-Package` header to our bundle's manifest. 


## From Modules to Services

Encapsulating internals of a module and making only the public API available is a
first step to achieve independency and interchangeability. But for fully reaching
these goals, it must be possible to configure the provider of a required
feature (i.e. the implementor of the API) at deployment or even choose among
several providers at runtime.

In Java 6, "a well-known set of interfaces and (usually) abstract classes" 
became termed as a "service" and a specific implementation of a a service
as a "service provider". You find those definitions in the description
of the central support class for this approach, the `ServiceLoader`
([JavaDoc](https://docs.oracle.com/javase/8/docs/api/index.html?java/util/ServiceLoader.html)).

Note that he use of the term service in this context is similar but not identical
to its usage in the context of Service Oriented Architectures 
([SOA](https://en.wikipedia.org/wiki/Service-oriented_architecture)). However, 
SOA focuses on distributed systems while in our context a service is simply 
defined by an API and used by getting an implementation class for that API 
and calling its methods (no networking or other overhead involved).

Due to the strict decoupling of the modules involved, looking up a provider
for a given service is prefered over directly instantiating some class
from another module. In order to meet the special possiblities and requirements
that come from being able to deploy bundles dynamically, OSGi defines a
service layer on top of the modules layer. In the next section I'm going to
use this for a simple logging task.

---

