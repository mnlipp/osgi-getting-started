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

A special form of specifying a dependency the "OSGi way" is the 
`Import-Package` Header in the `MANIFEST.MF` that we have seen before. Let's re-use the
header from the chapter ["Accessing a Service"](AccessingAService.html#version-range):

```properties
Import-Package: org.osgi.framework;version="[1.6,2)"
```

It is internally transformed to the more general form of a requirement:

```properties
Require-Capability: osgi.wiring.package;\
	filter:="(&(osgi.wiring.package=org.osgi.framework)\
	  (&(version>=1.6)(!(version>=2.0.0))))"
```

Of course, there is also a generalized form of the `Export-Package` header that
describes the features provided by a bundle. The `Require-Capability` header can be used
to express various requirements. The header

```properties
Require-Capability: osgi.ee;filter:="(&(osgi.ee=JavaSE)(version=1.8))"
```

states that the component requires JavaSE 8 as runtime environment. As you can
have a header only once in the `MANIFEST.MF`, all requirements of a bundle are 
combined in one header, using a comma as separator.
An [article](http://blog.osgi.org/2015/12/using-requirements-and-capabilities.html)
in the OSGI Alliance Block gives a short introduction to the general
"Requirements and Capabilities" model.

This powerful model allows you to easily choose a component providing
required capabilities from a repository if -- and that's the important point 
in this context -- you can query the repository for such a component, 
using the "Require-Capability" expression as search expression. This is
the main functionality of the 
[OSGi repository interface](https://osgi.org/javadoc/r6/cmpn/index.html?org/osgi/service/repository/Repository.html).
Pity enough, this kind of query is something that Maven Repositories in general 
don't support[^osgi-support].

[^osgi-support]: At least not out-of-the-box. I found that Sonatype's Nexus
	repository manager[^relation] can provide a 
	[Virtual OSGi Bundle Repository](https://help.sonatype.com/display/NXRM2/OSGi+Bundle+Repositories#OSGiBundleRepositories-VirtualOSGiBundleRepositories)
	for a Maven repository. However, this does not seem to be enabled on their
	[most popular installation](https://oss.sonatype.org/#welcome). 

[^relation]: I have no relations with this company except for an account
	on oss.sonatype.org. It just happens that I found the feature description
	when searching through the web. If you're aware of other 
	repository products with OSGi support, tell me and I'll add it here.

## OSGi "native" repositories

In its simplest form, the persistent data of a "native" OSGi Bundle Repository (OBR)
consists of a directory with bundles and an index file that describes the capabilities
(and requirements) of the bundles in the repository. The information in the index
file is extracted from the manifests of the bundles in the repository.

The `cnf` directory of our sample bnd workspace contains three such local 
repositories: `local`, `release` and `templates`. They are defined in `cnf/build.bnd`
by these statements:

```properties
-plugin.2.Local: \
	aQute.bnd.deployer.repository.LocalIndexedRepo; \
		name = Local; \
		pretty = true; \
		local = ${build}/local

-plugin.3.Templates: \
	aQute.bnd.deployer.repository.LocalIndexedRepo; \
		name = Templates; \
		pretty = true; \
		local = ${build}/templates

-plugin.4.Release: \
	aQute.bnd.deployer.repository.LocalIndexedRepo; \
		name = Release; \
		pretty = true; \
		local = ${build}/release
```

The `local` and `release` repositories are empty, but if you have look at the file
`cnf/templates/index.xml`, you get an idea how the information about capabilities and
requirements is represented.

The implementations of an OBR service that use this kind of persistence usually
accept a URL for the index file, i.e. the data can be kept on a remote server.
We made use of this feature for the definition of the following three repositories:

```properties
-plugin.5.de.mnl.osgi: \
	aQute.bnd.deployer.repository.FixedIndexedRepo; \
		name=de.mnl.osgi; \
		locations=https://raw.githubusercontent.com/mnlipp/de.mnl.osgi/master/cnf/release/index.xml; \
		readonly=true

-plugin.6.Felix: \
	aQute.bnd.deployer.repository.FixedIndexedRepo; \
		name=Felix; \
		cache=${workspace}/cnf/cache; \
		locations=http://felix.apache.org/obr/releases.xml

-plugin.7.bndtoolshub: \
	aQute.bnd.deployer.repository.FixedIndexedRepo; \
		name=Bndtools Hub; \
		locations=https://raw.githubusercontent.com/bndtools/bundle-hub/master/index.xml.gz
```

In an ideal world (from OSGi's point of view) all freely available bundles would be
maintained in a "native" repository. Not necessary using the simple persistence,
because this could result in a very large index file. Rather, a server could provide
a remotely accessible implementation of the `Repository` API. In reality, however,
we have a few "vendor" maintained repositories (such as the "Felix" repository) and
some repositories maintained by individual projects (such as my "de.mnl.osgi"
repository and -- with a broader scope, but no longer maintained, the "Bndtools Hub").

## OSGi views on Maven repositories



*To be continued*

---

