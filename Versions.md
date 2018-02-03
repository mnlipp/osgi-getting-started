---
layout: default
title: Versions
description: A detailed discussion about versioning OSGi bundles (vs. versioning Maven artifacts).
date: 2018-01-02 12:00:00
commentIssue: 16
---

# Versions

> “Success is relative. It is what we make of the mess we have made of things.”
> -- T.S. Eliot

Maybe, for a start have a look at the Wikipedia article about
[Software Versioning](https://en.wikipedia.org/wiki/Software_versioning).
Nice topic, isn't it? Well, luckily we can narrow the scope a bit.

What we have to provide versions for are bundles, packages and (assuming that
we use Maven repositories) Maven artifacts. What we can build upon is that there
seems to be a common consensus that, regarding libraries, "semantic versioning"
is an acceptable approach[^careful]. In our context, we have to consider
two specifications of semantic versioning: the respective 
[OSGi Whitepaper](https://www.osgi.org/wp-content/uploads/SemanticVersioning.pdf)
and a [more generally scoped specification](https://semver.org/)[^indiv], often
referred to as "SemVer". An advantage of the former document is that it
distinguishes between the provider and the consumer of an API. The *big* advantage
of the latter is that it defines how to handle versions during development
(pre-release versions).

[^careful]: Trying to be very careful here.

[^indiv]: This is "only" an individual's 
	([Tom Preston-Werner](http://tom.preston-werner.com/)) project. But an
	influential one. The specification has been picked up by projects such as 
	[NPM](https://docs.npmjs.com/getting-started/semantic-versioning),
	[Racket](https://docs.racket-lang.org/semver/index.html),
	[Ruby Gems](http://guides.rubygems.org/patterns/#semantic-versioning),
	[Angular](http://angularjs.blogspot.de/2016/10/versioning-and-releasing-angular.html)
	(to just name a few of the top search results).

## What to Version

Let's first get a clear idea about the kinds of versions that we have to provide. 
Regarding the API, the contract between components is essentially defined by
the "Export-Package" and "Import-Package". The versions declared in the
"Export-Package" header are the really important versions, because they are
used when wiring importers to exporters, i.e. when trying to find a match
for an "Import-Package" requirement with a given name and version range.

The "Bundle-Version" header is, interesting enough, optional (with a default
value of "0.0.0"). A bundle is an aggregate and therefore, if a bundle version
is given, it "must move as fast as the fastest moving package [it contains]" 
(see the [bnd documentation](http://bnd.bndtools.org/chapters/170-versioning.html)).
However, although the bundle version can be used as a requirement at various
places, the default is always "[0.0.0,&infin;)" (any version). Especially
when providing "ordinary" libraries that include the required headers to 
be faithfully used in an OSGi context, you shouldn't run into problems if 
the bundle version moves differently. Of course, the bundle version is used
by tools to choose among available bundles with the same bundle name, so
it isn't completely insignificant.

Finally, we have the Maven artifact version. Wouldn't it be nice if it matched
the bundle version? It certainly would, and maybe could, if only OSGi
had considered the pre-release versioning a bit more carefully.

## Pre- vs. Post-Qualifiers

Let's head straight at the core of the problem. OSGi defines a version number
with a qualifier ("1.0.0.test") to be higher than the version without the
qualifier ("1.0.0", the "release version"). So the qualifier is in fact a 
post-release qualifier. SemVer considers the release version 
"1.0.0" to be the final version with these major, minor and patch numbers. 
There is nothing "post" this release[^like]. A version with a qualifier, such as
"1.0.0-test"[^notice], is therefore defined to be lower than "1.0.0"[^qorder]. 

[^like]: I admit that I like this approach. IMHO *anything* that causes an artifact
	to be rebuilt after its release (which establishes the release version)
	should result in an increase of the patch number. 

This is important: OSGi: `1.0.0.test > 1.0.0`, SemVer `1.0.0-test < 1.0.0`. And Maven?
Well, Maven effectively has qualifiers that mark a version as pre-release and 
qualifiers that mark it as post-release. The exact rules are decribed in the
[POM reference](https://maven.apache.org/pom.html#Version_Order_Specification)
and are a bit complicated. What it boils down to from a practical point of view is,
that qualifiers that start with a number (such as those used by maven tooling for
snapshot versions) are pre-release versions.

Maven versioning's support for qualifiers that indicate pre-release versions 
is one of the reasons that make working with such pre-releases rather simple.
(The other is, of course, the special keyword "`SNAPSHOT`". When used as the qualifier, 
it simply denotes the latest pre-release of a given release version[^once].)

[^notice]: Notice the different separator being used to separate the qualifier
	from the rest of the version number. Some have brought up the interpretation
	of a "minus", so "1.0.0" is the release "minus" something, which clearly
	indicates that it is "less". Of course, this interpretation becomes dubious
	when you consider the ordering of pre-releases: `1.0.0-2 < 1.0.0-3`. Doesn't
	really comply with mathematics.

[^once]: The OSGi Alliance has once 
	[considered](http://web.archive.org/web/20130618191418/http://www.osgi.org/download/osgi-early-draft-2011-09.pdf)
	to support pre-release versions as well, but
	[discarded the proposal](http://blog.osgi.org/2012/03/)[^maybeBetter]. 

[^maybeBetter]: Maybe for the better, since it would have increased the mess.
	The attempt to define a version system that supports both pre- and
	post-release qualifiers (the use of "`.`" vs. "`-`" came in quite handy)
	made things rather complicated.

[^qorder]: At least there is no difference in the sort order of the qualifiers.
	SemVer defines it to be based on the ASCII values, OSGI refers to
	[`String.compareTo`](https://docs.oracle.com/javase/8/docs/api/java/lang/String.html#compareTo-java.lang.String-).
	Luckily, with respect to the characters allow in a qualifier, this boils down
	to the same result.

The real fun starts when you think about version ranges. How do pre-release
versions fit in? Obviously, a range "[1.0.0,1.1.0)" does not include the
pre-release "1.0.0-test", else the pre-release would have to provide
all the features of the release version[^orNot]. But what about 
"1.1.0-rc1"? It's lower than "1.1.0", so the version range formally
includes it. But the intention of the range definition presumably was to
*exclude* anything "related to" (includes "leading to") version "1.1.0" 
(and beyond) because it was expected to introduce some incompatibility.

[^orNot]: Or maybe not so obvious. The 
	"[Semantic Versioning 2.0.0-rc.2](https://semver.org/spec/v2.0.0-rc.2.html)"
	stated that "Pre-release versions satisfy but have a lower precedence than 
	the associated normal version". It was only in the final 
	[release 2.0.0](https://semver.org/spec/v2.0.0.html) that
	this sentence was changed to the opposite statement "A pre-release version 
	indicates that the version is unstable and might not satisfy the intended 
	compatibility requirements as denoted by its associated normal version".
	The final version was published in June 2013. Keep this in mind when you
	come across old articles that refer to "SemVer".

*To be continued*

---

