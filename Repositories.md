---
layout: default
title: Repositories
description: Describes how the OSGi tools make use of repositories and why this is not really compatible with the "Maven way".
date: 2022-01-09 12:00:00
commentIssue: 15
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
header from the part ["Accessing a Service"](./AccessingAService.html#version-range):

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
An [article](https://blog.osgi.org/2015/12/using-requirements-and-capabilities.html)
in the OSGI Alliance Block gives a short introduction to the general
"Requirements and Capabilities" model.

This powerful model allows you to easily choose a component providing
required capabilities from a repository if&mdash;and that's the important point 
in this context&mdash;you can query the repository for such a component, 
using the "Require-Capability" expression as search expression. This is
the main functionality of the 
[OSGi repository interface](https://osgi.org/javadoc/r6/cmpn/index.html?org/osgi/service/repository/Repository.html).
Pity enough, this kind of query is something that Maven Repositories in general 
don't support.

## OSGi "native" repositories

In its simplest form, the persistent data of a "native" OSGi Bundle Repository (OBR)
consists of a directory with bundles and an index file that describes the capabilities
(and requirements) of the bundles in the repository. The information in the index
file is extracted from the manifests of the bundles in the repository.

The `cnf` directory of our sample bnd workspace contains three such local 
repositories: `local`, `release` and `templates`. They are made known to bnd
in `cnf/build.bnd` by these statements:

```properties
-plugin.1.Local: \
	aQute.bnd.deployer.repository.LocalIndexedRepo; \
		name = Local; \
		pretty = true; \
		local = ${build}/local

-plugin.2.Templates: \
	aQute.bnd.deployer.repository.LocalIndexedRepo; \
		name = Templates; \
		pretty = true; \
		local = ${build}/templates

-plugin.3.Release: \
	aQute.bnd.deployer.repository.LocalIndexedRepo; \
		name = Release; \
		pretty = true; \
		local = ${build}/release
```

The `local` and `release` repositories are empty, but if you have a look at the file
`cnf/templates/index.xml`, you get an idea how the information about capabilities and
requirements is represented. The plugin type 
`aQute.bnd.deployer.repository.LocalIndexedRepo` provides an editable repository in the
local file system. "Editable" means that you can deploy bundles to it and the index 
is regenerated automatically by bndtools. Details about the configuration properties
can be found in the 
[bndtools documentation](https://bndtools.org/repositories.html#local-indexed-repository).

Another implementation of an OSGi repository that uses an index file is the
`aQute.bnd.deployer.repository.osgi.OSGiRepository`. Bndtools makes no attempt to modify the 
content of such a repository. The index file is specified by a URL, i.e. the data 
can be kept on a remote server. Again, the configuration options can be found in the 
[bndtools documentation](https://bndtools.org/repositories.html#fixed-index-repositories).
Until 2020 you could make use of such a repository to access the Apache Felix bundles:

```properties
-plugin.5.Felix: \
	aQute.bnd.deployer.repository.osgi.OSGiRepository; \
		name=Felix; \
		cache=${workspace}/cnf/cache; \
		locations=https://felix.apache.org/obr/releases.xml
```

In an ideal world (from OSGi's point of view) all freely available bundles would be
maintained in a "native" repository. Not necessarily using the simple persistence,
because this could result in a very large index file. Rather, a server would provide
some remotely accessible implementation of the `Repository` API. In reality, however,
there have never been more than a few "vendor" maintained[^badly] repositories 
(such as the "Felix" repository) and some special purpose repositories maintained by
individual projects (such as the 
["Bndtools Hub"](https://github.com/bndtools/bundle-hub)).

[^badly]: Sometimes badly maintained, as you can see 
	[here](https://github.com/mnlipp/osgi-getting-started/issues/1)
	and [here](https://www.mail-archive.com/users@felix.apache.org/msg18533.html).

## OSGi views on Maven repositories

Today's Java development is Maven Central centric[^sorry]. No matter whether
a project uses Maven or some other tool like Gradle for project management,
(almost) everybody makes Java Open Source Software artifacts available on
Maven Central and downloads required libraries from there using their Maven 
coordinates. As mentioned before, the deficiency from an OSGi's point of view 
is that Maven repositories do not provide the requirements and capabilities 
based search facility[^osgi-support]. 

[^sorry]: Sorry, couldn't resist.

[^osgi-support]: At least not out-of-the-box. I found that Sonatype's Nexus
	repository manager[^relation] can provide a 
	[Virtual OSGi Bundle Repository](https://help.sonatype.com/display/NXRM2/OSGi+Bundle+Repositories#OSGiBundleRepositories-VirtualOSGiBundleRepositories)
	for a Maven repository. However, this does not seem to be enabled on their
	[most popular installation](https://oss.sonatype.org/#welcome). And it doesn't
	look as if it is planned to support it in 
	[version 3](https://help.sonatype.com/repomanager3/product-information/repository-manager-feature-matrix)
	any more.


[^relation]: I have no relations with this company except for an account
	on oss.sonatype.org. It just happens that I found the feature description
	when searching through the web. If you're aware of other 
	repository products with OSGi support, tell me and I'll add it here.

A nice solution would be to have an OSGi search facility maintained by e.g.
the OSGi Alliance that uses the major public repositories as backing repositories[^JPM].
Since no such facility exists, bnd workspaces have to fall back to downloading
a subset of a Maven repository's artifacts and creating an OSGi Bundle Repository 
(index) from them. Various bnd plugins exist for that purpose.

[^JPM]: The "JPM" project (Java Package Manager) developed by 
	[Peter Kriens](https://github.com/pkriens) when he was *not* working
	for the OSGi Alliance included an attempt to create a "Meta Repository"[^broader]
	with the possibility to query bundles by packages.
	You will still find references to it when searching for sample 
	OSGi project configurations in the Web, but the JPM Web site was "officially" 
	[declared down](https://groups.google.com/forum/#!topic/bndtools-users/_epXQiMDiX4)
	on March 1st, 2017. Support for JPM will be removed from
	bnd in the [next release](https://github.com/bndtools/bnd/wiki/Changes-in-3.5.0).

[^broader]: Actually, the project had a somewhat broader scope. The idea
	was to provide a `jpm` command that would work similar to the 
	[`npm`](https://www.npmjs.com/). From the (now gone) Web Site:
	
	> jpm4j is a package manager for java. Languages like Ruby with its 
	> Gems, Node.js with npm, and Perl's CPAN, provide a package manager 
	> that makes it easy to install libraries and applications regardless 
	> of platform. jpm4j unleashes the power of Java with a "write once, 
	> deploy anywhere" model. Just publish the binaries on the Internet, 
	> notify jpm4j, and then deploy from anywhere to any platform with jpm4j.
	>
	> An open index provides already organized access to almost binaries 
	> (and growing) organized in programs. An index that is searchable, 
	> editable, rankable, and extendable (in real time) by you. An index 
	> that can be used during development from a growing number of build 
	> technologies and IDEs (including of course Eclipse and Maven).
	
	(Retrieved from 
	[WayBackMachine](https://web.archive.org/web/20170105235749/http://www.jpm4j.org:80/#!/))

### Maven Bnd Repository Plugin

The `aQute.bnd.repository.maven.provider.MavenBndRepository` is probably the easiest
to setup. Here is a sample configuration (details can be found in the 
[bnd documentation](https://bnd.bndtools.org/plugins/maven.html)):

```properties
-plugin.CentralMvn: \
	aQute.bnd.deployer.repository.wrapper.Plugin; \
		location = "${build}/cache/wrapper"; \
		reindex = true, \
	aQute.bnd.repository.maven.provider.MavenBndRepository; \
		name="Central (Maven)"; \
		snapshotUrl=https://oss.sonatype.org/content/repositories/snapshots/; \
		releaseUrl=https://repo.maven.apache.org/maven2/; \
		index=${.}/central.mvn
```

The plugin uses the file configured with the
`index` property[^idx-nc] which specifies the Maven artifacts to be downloaded
and to be included in the repository (view) provided by the plugin. 

[^idx-nc]: Not to be confused with an OBR index file.

The Maven Bnd Repository plugin uses
a simple list of Maven coordinates to specify the artifacts to be included in
the configured repository. If you have the Repositories view open in Eclipse
(usually when you are in the bndtools perspective), you can even drag and drop
the URL of a POM file (e.g. found by searching Maven Central) into a
Maven Bnd Repository and the Maven coordinates will be added to the
index file. 

This sounds like a nice way to get the artifacts that your project depends on,
until you notice that the plugins don't support transitive dependencies. You
get exactly what you have specified, and if an artifact depends on some other
artifact, you have to explicitly specify this as well -- a very arduous task.

### Bnd POM Repository Plugin

This is where the `aQute.bnd.repository.maven.pom.provider.BndPomRepository` plugin
comes in (details can, again, be found in the 
[bnd documentation](https://bnd.bndtools.org/plugins/pomrepo.html)). This plugin
is kind of a strange beast, because it allows you to configure an initial
set of artifacts to retrieve in very different ways, including the possiblity
to use Maven Central's search facility. No matter how you obtain the initial
set of artifacts, the plugin's default behavior is to also include the transitive
dependencies, i.e. the dependencies specified in the POM files of the artifacts.

As an example, we can get all Apache Felix artifacts and the artifacts that they
depend on with a Bnd POM Repository configured like this:

```properties
-plugin.Felix: \
    aQute.bnd.repository.maven.pom.provider.BndPomRepository; \
        name=Felix; \
    	snapshotUrls=https://oss.sonatype.org/content/repositories/snapshots/; \
        releaseUrls=https://repo1.maven.org/maven2; \
        query='q=g:%22org.apache.felix%22&rows=10000'
```

Here's the query for [Equinox](https://www.eclipse.org/equinox/). Note that they 
have changed the group id in later versions, so in order to get everything, we
have to query using the artifact id.

```properties
-plugin.Equinox = \
    aQute.bnd.repository.maven.pom.provider.BndPomRepository; \
        name=Equinox; \
    	snapshotUrls=https://oss.sonatype.org/content/repositories/snapshots/; \
        releaseUrls=https://central.maven.org/maven2; \
        query='q=a:%22org.eclipse.osgi%22&rows=10000'
```

I have personally experienced two problems with this plugin. The first is 
that the search works only with Maven Central, which means
that it won't include snapshots of artifacts.

The second problem (I found out about that at the beginning of 2022) 
is that maven central doesn't support a `rows` parameter 
greater 200 any more (and the plugin doesn't support pagination). So for
the example queries above it has become useless.

### Indexed Maven Repository Plugin

My Indexed Maven Repository Plugin maintains an index of one or more 
maven repositories using a configuration that is aligned with maven 
group ids. It makes use of the maven dependency information both for
adding artifacts to the index and for filtering artifacts. For more
details, have a look at the plugin's 
[documentation](https://mnlipp.github.io/de.mnl.osgi/IndexedMavenRepository.html)

### Nexus Search Repository Plugin

In a previous attempt to index maven artifacts, I had started a 
[Nexus Search Plugin](https://github.com/mnlipp/de.mnl.osgi/tree/master/de.mnl.osgi.bnd.repository) plugin
that&mdash;as its name suggests&mdash;uses the Nexus search API to find both
the released artifacts and the snapshot artifacts on a Nexus server
(especially those on the [Open Source](https://oss.sonatype.org/#welcome) repository).

I'm not perfectly sure yet, but with the Indexed Maven Repository Plugin available,
I consider to deprecate this plugin.

### Aether Plugin

The bndtools documentation also mentions an 
["Aether (Maven) Repositories" plugin](https://bndtools.org/repositories.html#aether-maven-repositories) that I haven't tested. The documentation seems incomplete
and what I could find in addition was 
[not encouraging](https://groups.google.com/forum/#!topic/bndtools-users/yefAUFz_1eg).
Besides, the underlying Eclipse Aether project has been archived and isn't easily
accessible any more.

### JPM Repository Plugin (deprecated)

When you create a new workspace with bndtools 3.5.0, it configures
an `aQute.bnd.jpm.Repository` plugin for accessing Maven Central:

```properties
# Configure Repositories
-plugin.1.Central: \
	aQute.bnd.deployer.repository.wrapper.Plugin; \
		location = "${build}/cache/wrapper"; \
		reindex = true, \
	aQute.bnd.jpm.Repository; \
		includeStaged = true; \
		name = Central; \
		location = ~/.bnd/shacache; \
		index = ${build}/central.json
```

This initial setup is a bit strange, considering the fact that JPM support 
will be removed from bndtools in the next release. Details about the 
configuration of the plugin could be found in OSGi's 
[enroute Maven tutorial](https://web.archive.org/web/20160418231617/https://enroute.osgi.org/tutorial_maven/310-central.html).

## Conclusion

Once you have found a configuration that suits your needs, it isn't too hard
to include content from the popular Maven repositories in your bnd/bndtools
projects. A different issue is how you make the bundles from your own project
available to others. This is closely related to your project's build and CI/CD
chain. I'll cover this topic in another part.


---

