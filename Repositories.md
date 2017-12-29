---
layout: default
title: Repositories
date: 2017-12-28 12:00:00
---

# Repositories

If you're used to working with Maven repositories such as 
["Maven Central"](https://search.maven.org/), you may have wondered why
we need those complicated repository configurations for using bnd/bndtools.

The reasons are completely different ideas about what a repository contains.
Maven repositories are comprised of artifacts that can be identified by their
[Maven Coordinates](https://maven.apache.org/pom.html#Maven_Coordinates).
Apart from a very generic type attribute that allows to distinguish
code-jars from jars containing e.g. JavaDoc, nothing is known about the
artifacts. It's up to the user to find out which artifact in a repository
provides whatever is needed and create a dependency on it.

OSGi has a strong component orientation. An important point about components
is that they are interchangeable with other components that provide the
same interface[^contracts]. OSGi projects should therefore not refer to 
required resources by specifying an artifact that they depend on, but 
rather by stating that they depend on a component that implements a 
specific interface or feature.

[^contracts]: In more general terms, we're talking about contracts between
	components.

A special form of specifying a dependency the "OSGI way" is the 
`Import-Package` Header in the `MANIFEST.MF` that we have seen before. Let's re-use the
header from the chapter ["Accessing a Service"](AccessingAService.html):

```properties
Import-Package: org.osgi.framework;version="[1.6,2)"
```

It is internally transformed to the more general form of a requirement:

```properties
Require-Capability: osgi.wiring.package;\
	(&(osgi.wiring.package=org.osgi.framework)\
	  (&(version>=1.6)(!(version>=2.0.0))))
```

Of course, there is also a generalized form of the `Export-Package` header that
describes the features provided by a bundle. An 
[article](http://blog.osgi.org/2015/12/using-requirements-and-capabilities.html)
in the OSGI Alliance Block gives a short introduction to the general
"Requirements and Capabilities" model.

This powerful model allows you to easily choose a component providing
required capabilities from a repository if -- and that's the important point 
in this context -- you can query the repository for such a component, 
using the "Require-Capability" expression as search expression. Pity enough, 
this is something that Maven Repositories in general don't support.

*To be continued*

---

