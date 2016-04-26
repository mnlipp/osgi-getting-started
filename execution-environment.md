---
layout: default
title: The container
date: 2016-03-31 12:00:00
---

# The Container

In the simplest case of component based development, the components are assembled using some static configuration. More advanced concepts introduce a container as execution environment and allow you to dynamically add or remove components at runtime.

In OSGi terms, the container is provided by the OSGi framework and the components are provided by OSGi (compliant) bundles[^cm]. So in order to get started with OSGi, we need an OSGi framework. There are several implementations of the framework available. Let's use [Apache Felix](http://felix.apache.org/) to get started, because I found that to be very intuitive and easy to use.

I won't repeat things here that have already been well described by others. So simply go to the [Apache Felix Framework Usage Documentation](http://felix.apache.org/documentation/subprojects/apache-felix-framework/apache-felix-framework-usage-documentation.html) and follow it up to and including the section "Starting the framework". Make sure to only download the [Felix Framework Distribution](http://felix.apache.org/downloads.cgi#framework) and none of the subprojects (yet).

After starting the framework, type "`felix:lb`" (short for "list bundles") and you'll get something like this:

```
START LEVEL 1
   ID|State      |Level|Name
    0|Active     |    0|System Bundle (5.4.0)|5.4.0
    1|Active     |    1|Apache Felix Bundle Repository (2.0.6)|2.0.6
    2|Active     |    1|Apache Felix Gogo Command (0.16.0)|0.16.0
    3|Active     |    1|Apache Felix Gogo Runtime (0.16.2)|0.16.2
    4|Active     |    1|Apache Felix Gogo Shell (0.10.0)|0.10.0
```

An "empty" OSGi runtime environment doesn't seem to be really empty. It already contains some bundles. As we can guess from the "Level" it's maybe not really necessary to have the "Apache Felix ..." bundles, but most likely we need the "System bundle".

Let's see if we can find out a bit more about that bundle. Type "`felix:headers 1`" and you get:

```
Apache Felix Bundle Repository (1)
----------------------------------
Bnd-LastModified = 1442917174385
Build-Jdk = 1.7.0_51
Built-By = David
Bundle-Activator = org.apache.felix.bundlerepository.impl.Activator
Bundle-Description = Bundle repository service.
Bundle-DocURL = http://felix.apache.org/site/apache-felix-osgi-bundle-repository.html
Bundle-License = http://www.apache.org/licenses/LICENSE-2.0.txt
Bundle-ManifestVersion = 2
Bundle-Name = Apache Felix Bundle Repository
Bundle-Source = http://felix.apache.org/site/downloads.cgi
Bundle-SymbolicName = org.apache.felix.bundlerepository
Bundle-Url = http://felix.apache.org/site/downloads.cgi
Bundle-Vendor = The Apache Software Foundation
Bundle-Version = 2.0.6
Created-By = Apache Maven Bundle Plugin
DynamicImport-Package = org.apache.felix.shell
Export-Package = org.osgi.service.repository;version="1.0";uses:="org.osgi.resource",org.apache.felix.bundlerepository;version="2.1";uses:="org.osgi.framework"
Export-Service = org.apache.felix.bundlerepository.RepositoryAdmin,org.osgi.service.obr.RepositoryAdmin
Import-Package = org.osgi.service.repository;resolution:=mandatory;version="[1.0,2)",org.osgi.service.log;resolution:=optional;version="[1.3,2)",org.osgi.service.obr;resolution:=optional;version="[1.0,2)",javax.xml.stream;resolution:=optional,org.apache.felix.bundlerepository;version="[2.1,3)",org.apache.felix.shell;version="[1.0,2)";resolution:=optional,org.osgi.framework;version="[1.7,2)",org.osgi.framework.wiring;version="[1.1,2)",org.osgi.resource;version="[1.0,2)",org.osgi.service.cm;version="[1.5,2)";resolution:=optional,org.osgi.service.url;version="[1.0,2)",org.osgi.util.tracker;version="[1.5,2)"
Manifest-Version = 1.0
Tool = Bnd-2.1.0.20130426-122213
```

Looks like there is a lot of information associated with a bundle and maybe that's why people consider bundles to be difficult to build. But most of that information is optional. In part 2, we'll see that it is really easy to build a (simple) bundle.

---

[^cm]: An OSGi bundle is actually only a module. It *can* be a component if it fulfills some additional constraints. However, as OSGi also provides support for components, OSGi is often presented as a component framework rather than a module framework. The tons of introductions that focus on a runtime environment such as Felix emphasize that angle. I stumbled upon OSGi while searching for a component framework myself, and if you are still reading this, chances are high that you you are also thinking about components, so let's stick to that view for the first parts of this introduction.
