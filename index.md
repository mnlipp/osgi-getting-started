---
layout: default
title: Why?
description: Describes why this introduction was written.
date: 2019-03-29 12:00:00
commentIssue: 2
---

# Why?

You find a lot of tutorials about OSGi on the web. Why add another introduction to OSGi? Well, the usual reason: they didn't really help me. I want to understand things from the ground up, and I want that ground to be as simple as possible.

Most tutorials that I have found are centered around some tool that is supposed to overcome the difficulties of OSGi. But is OSGi really so difficult to master? No it isn't. You just mustn't start with complex tools or patterns. Knowing the JDK and its basic tools is quite sufficient. Of course, you should also have some general knowledge about [modular programming](https://en.wikipedia.org/wiki/Modular_programming) and [component based software](https://en.wikipedia.org/wiki/Component-based_software_engineering), because OSGi is basically about how to build and use modularized and component based applications with a given framework.

The examples used in this introduction are available from the [project site](https://github.com/mnlipp/osgi-getting-started) as an [Eclipse](https://www.eclipse.org/) project. I know that there are other IDEs around, but that's the one I prefer. For the initial parts of this introduction, the IDE is actually irrelevant. If you want to, you can use a text editor and build the examples with `javac` and `jar`. There are also gradle build configurations provided that automate those compilation and packaging steps[^nm].

If you want to comment on this introduction, please use the links at the bottom of the pages.

---

[^nm]: No maven inside! I think maven is the worst thing that has ever happened to Java development. Neil Ford has [described the issue](https://web.archive.org/web/20190427072827/https://nealford.com/memeagora/2013/01/22/why_everyone_eventually_hates_maven.html) very nicely (and far less emotionally than I ever could).