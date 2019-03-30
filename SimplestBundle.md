---
layout: default
title: Simplest Bundle
description: Shows how to create an OSGi compliant bundle using nothing but the JDK.
date: 2019-03-29 12:00:00
commentIssue: 4
---

# Simplest Bundle

I don't know if what we're going to build here is really the "simplest" possible bundle. But it is definitely very simple.

Create a new Java project (yes, just Java, nothing fancy required here) or use the [existing sample project](https://github.com/mnlipp/osgi-getting-started/tree/master/SimplestBundle) and import it in Eclipse. Then create a class that represents the component in our bundle. Here's the class from the sample project:

```java
package io.github.mnl.osgiGettingStarted.simplestBundle;

public class HelloWorld {

}
```

And then there is the `MANIFEST.MF` that goes into the jar file together with the result of the compilation (the class file):

```properties
Manifest-Version: 1.0
Bundle-ManifestVersion: 2
Bundle-Name: HelloWorld
Bundle-SymbolicName: io.github.mnl.osgiGettingStarted.simplestBundle
Bundle-Version: 1.0.0
Bundle-RequiredExecutionEnvironment: JavaSE-1.8
```

That's where the magic is. The "`Bundle-...`" entries (that you usually don't find in a `MANIFEST.MF`) make the jar file with that manifest an OSGi bundle. `Bundle-ManifestVersion` and `Bundle-SymbolicName` are mandatory entries. `Bundle-RequiredExecutionEnvironment` is actually deprecated, but must still be supported by framework implementations and is easier to use than the newer alternative. It's not really needed for our example, but you should add it if you rely on specific Java features to be available.

Now pack the `class` file and `MANIFEST.MF` into a jar file. If you're using Eclipse, you can simply double click on the `jar-export.jardesc` that I have provided with the project. You can also execute a `gradle build`. The content of the resulting jar file looks like this[^jarWhere]:

[^jarWhere]: If you use `jar-export.jardesc`, the jar can be found in the root directory
    (the parent directory of the directory `SimplestBundle`). Should you use gradle, you'll
    find the jar in the sub-directory `build/libs` of `SimplestBundle`, as is usual with 
    gradle.

```
META-INF/MANIFEST.MF
io/github/mnl/osgiGettingStarted/simplestBundle/HelloWorld.class
```

The jar file can now be deployed in the OSGi framework. Go to the console of your running Felix and type "`felix:install file:<path-to-jar>`" (details can be found in the [Felix Documentation](https://felix.apache.org/documentation/subprojects/apache-felix-framework/apache-felix-framework-usage-documentation.html#installing-bundles)). Enter `felix:lb`. You'll get:

```
START LEVEL 1
   ID|State      |Level|Name
    0|Active     |    0|System Bundle (6.0.2)|6.0.2
    1|Active     |    1|jansi (1.17.1)|1.17.1
    2|Active     |    1|JLine Bundle (3.7.0)|3.7.0
    3|Active     |    1|Apache Felix Bundle Repository (2.0.10)|2.0.10
    4|Active     |    1|Apache Felix Gogo Command (1.0.2)|1.0.2
    5|Active     |    1|Apache Felix Gogo JLine Shell (1.1.0)|1.1.0
    6|Active     |    1|Apache Felix Gogo Runtime (1.1.0)|1.1.0
    7|Installed  |    1|HelloWorld (1.0.0)|1.0.0
```

So here it is, your first OSGi bundle. Its display in the list looks a bit different from the others because the state is "Installed" instead of "Active". This indicates that it has been installed without problems, but it hasn't been started yet. Let's do this now. Issue the command "`felix:start 7`". When you list the bundles again, the state has changed:

```
    7|Active     |    1|HelloWorld (1.0.0)|1.0.0
```

Despite having an active bundle, nothing happens. This is not really surprising. First, we haven't provided any code aside from the empty class definition. Second, the class hasn't been referenced in the manifest. Actually, the class is completely irrelevant for building this bundle. But as I have started this with a "component framework view" on OSGi, introducing a bundle without a component base would have been a bit irritating. Obviously, if we want code to be executed, we need something similar to the method "`static void main(...)`" in an application's startup class. This issue will be covered in the next part.

Meanwhile &mdash; as it doesn't do anything useful anyway &mdash; you can remove the simplest bundle again. Type "`felix:uninstall 7`". When you re-list the bundles, you'll find that it is gone.


