---
layout: default
title: Shift in Perspective
---

# Shift in Perspective

When presenting their software, OSGi framework providers tend to focus -- not surprisingly -- on the capabilities of the product. Among those is the possibility to deploy components dynamically from a console. We've used this capability throughout this introduction to get our component running in various development stages. This might have left the impression that an OSGi framework is a heavyweight -- or at least light heavyweight -- container.

A more appropriate perspective on the OSGi framework focuses on the aspect that it is a framework: an environment providing some generic functionality to facilitate the development of software (see the [more exhaustive explanation](https://en.wikipedia.org/wiki/Software_framework) on Wikipedia). A characteristic of frameworks (contrary to libraries) is that they implement the startup code. The user-written code is invoked by the framework in a framework dependent way.

The capability to deploy components (bundles) dynamically is part of the OSGi core framework, providing a console isn't. A minimal application using the OSGi framework consists of the framework implementation and at least one bundle. Contrary to some other frameworks, OSGi doesn't provide the ``main`` method. In order to get things started, a minimal application must implement a ``main`` that creates an instance of the framework, installs the bundle and starts the framework (which activates the bundle in turn). You can find a sample main in the OSGi Core specification, Chapter "Life  Cycle Layer", section "Frameworks" ([downloadable](https://www.osgi.org/developer/downloads/release-6/) from the OSGi web site).

To ease this task, Bndtools provides the capability to assemble an application that consists of a framework, your chosen bundle(s), and some application startup code (though not a minimal version). 

*To be continued*

