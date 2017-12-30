---
layout: default
title: Bndtools
date: 2016-03-21 12:00:00
---

# Bndtools

[Bndtools](http://bndtools.org/) is an Eclipse plugin that integrates the (command line) tool [bnd](http://bnd.bndtools.org/) in Eclipse and provides "continuous build" for bundles. [Install](http://bndtools.org/installation.html) it now[^bndtools-version].

[^bndtools-version]: This chapter was originally written using bndtools 3.2.0. However, I have
   checked recently that things still work with bndtools 3.5.0 and Eclipse oxygen 
   and made minor changes and updates.

The tool bnd takes a different perspective on defining bundles. From bnd's point of view, `MANIFEST.MF` is the source of information about the bundle at runtime only. While developing the bundle, you need closely related, but sometimes slightly different information and  *additional* information. So, to bnd, `MANIFEST.MF` is an artifact that is generated during build time from information contained in a file called `bnd.bnd`. The eclipse plugin bndtools provides a GUI for editing `bnd.bnd` (again with the possibility to edit the source directly) and components that make the information from `bnd.bnd` available to Eclipse's continuous build. 

There is a [tutorial](http://bndtools.org/tutorial.html) for Bndtools, which I found (at the time of this writing) to be rather confusing. It addresses developers with some OSGi experience rather than users who want to get an (Eclipse based) environment for writing their first bundle. So let's simply once more focus on our Simple Bundle and port it to a Bndtools project.

In order to be able to work with Bndtools without problems, you need a so called configuration project. Use the wizard to create a "Bndtools OSGi Workspace" (File/New/Other/Bndtools). Choose "bndtools/workspace" as workspace template. You'll see a project `cnf` having been created in your workspace. Ignore it for the time being. Use the wizard again and create a new "Bndtools OSGi Project". Choose the Bndtools/Empty template and use `SimpleBundle-bnd` as project name.

Have a look at the `generated` folder in `SimpleBundle-bnd`. Double-click on `SimpleBundle-bnd.jar` and then -- in the "Jar File Viewer" that appears -- double-click on `MANIFEST.MF`. Looks a bit familiar but much more verbose than what we have written so far[^sb]:

[^sb]: Probably this is really the *simplest bundle* that you can have.  

![Jar File Viewer](images/JarFileView.png){: width="700px" }

Copy our source package into the `src` folder of the new project. Open `Activator.java` and have a look at the error. Looks familiar. Regrettably, there's no quick fix this time. Open `bnd.bnd` and select the "Build" tab. In the "Build Path sub-window use "+" to add `osgi.core`. Save the file, click on "Rebuild project" (under "Build Operations") and see the error disappear. If you switch to the "JAR File Viewer" again, you can see that the jar is still empty (apart from the `MANIFEST.MF`, of course). Bndtools doesn't include Java sources just because they are there. Rather, you have to add them to the "Private Packages" in `bnd.bnd`, tab "Contents". Do this, save, and you can see the project's classes in the jar file now.

On tab "Contents", you can also add the bundle's activator. You can use content assist to enter the class name into the field (it's the only proposal). Save again, and you can see the `Bundle-Activator` header having been added to the generated `MANIFEST.MF`. You can also see it on the "Source" tab of `bnd.bnd`. The basic idea about the format of `bnd.bnd` is that entries that are to be copied to `MANIFEST.MF` look just like the headers in `MANIFEST.MF` (well, sometimes they are processed a bit). Entries that control the behavior of the bnd tool start with a dash[^cwp].

[^cwp]: Comparing this with PDE's approach as shown in the previous part, you could say that `bnd.bnd` combines the information maintained by PDE in `MANIFEST.MF` and `build.properties`.

Add version "1.0.3" in the "Content" tab of `bnd.bnd`. Save, and you can immediately install and start the bundle (the jar) in felix as with our previous projects. If you want to have a build time stamp as with the PDE plugin, add `${tstamp}` to the version number ("1.0.3.${tstamp}"). This macro will be replaced with the build time by bnd.

If you want to continue using Bndtools, you should have a look at the [bnd documentation](http://bnd.bndtools.org/) after reading the remaining parts of my introduction (at least up to "Cleaning up" -- there are still some parts of the puzzle missing). I recommend to start with "[Introduction](http://bnd.bndtools.org/chapters/110-introduction.html)", proceed with "[Concepts](http://bnd.bndtools.org/chapters/130-concepts.html)" and read the rest as required when you encounter problems with your projects.

When you create a "Bndtools OSGi workspace", the required files for a gradle built (which is completely independent from Bndtools) are automatically added in the directory that contains your project. It's added there because more often than not an OSGi based project consists of several bundles. From gradle's point of view, all bundles are sub-projects in their respective folders. The generated build configuration uses [bnd's gradle plugins](https://github.com/bndtools/bnd/tree/master/biz.aQute.bnd.gradle). Don't try to develop OSGi bundles with the plugin from the gradle project. As one of the gradle developers remarked in a [discussion](https://discuss.gradle.org/t/the-osgi-plugin-has-several-flaws/2546/25): "The existing OSGi plugin is one of the oldest Gradle plugins. My opinion is that the best thing to do here would be to start again."[^wid] The plugins bundled with bnd actually represent this "fresh start"[^restructure].

[^wid]: I only wish they had put that in the gradle [documentation](https://docs.gradle.org/current/userguide/osgi_plugin.html) of the plugin. Would have saved me half a day.

[^restructure]: A difficulty with this project layout is that you cannot see the files created
    in the same directory as your project in Eclipse. This can be fixed by using the nested
    project layout that Eclipse started to support with version 4.5 (Mars). Create a new
    project of type "General" named e.g. "OSGi-Tests". Delete the projects created so far 
    from your workspace (it should only contain "OSGi-Tests" now) and close Eclipse. Move
    everything in your workspace (except folders "OSGi-Test" and ".metadata") into the
    folder "OSGi-Tests". Make sure not to miss the "hidden" folders such as "`.gradle`" etc.
    Re-open Eclipse again and re-import the projects that are now sub-projects of
    "OSGi-Tests". Choose "Project Presentation: Hierarchical" in the "Project Explorer"
    window to see everything in Eclipse.

To make absolutely sure that the dependencies are clear, let me summarize again. At the center of the build is the bnd command line tool. Its "gradle module" provides the necessary plugins to adapt gradle (via `build.gradle`) in such a way that it takes the project specific build information from `bnd.bnd`, thus making `bnd.bnd` the primary source of information for the gradle build.

Bndtools integrates bnd into Eclipse. It provides plugins for Eclipse that use the information from `bnd.bnd` to provide a class path container and other information for the continuous Eclipse build. The results from the Eclipse build and the gradle build are exactly the same. Using Bndtools therefore does not make your project depend on Eclipse.

---
