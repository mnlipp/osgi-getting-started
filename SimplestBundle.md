---
layout: default
title: Simplest Bundle
---

# Simplest Bundle

I don't know if what we're going to build here is really the "simplest" possible bundle. But it is definitely very simple.

Create a new Java project (yes, just Java, nothing fancy required here) or use the [existing sample project](https://github.com/mnlipp/osgi-getting-started/tree/master/SimplestBundle) and import it in Eclipse. Then create a class that represents the component. Here's the class from the sample project:

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
Bundle-RequiredExecutionEnvironment: JavaSE-1.7
```

That's where the magic is. The "`Bundle-...`" entries (that you usually don't find in a `MANIFEST.MF`) make the jar file with that manifest an OSGi bundle. `Bundle-ManifestVersion` and `Bundle-SymbolicName` are mandatory entries. `Bundle-RequiredExecutionEnvironment` is actually deprecated, but must still be supported by framework implementations and is easier to use than the newer alternative. It's not really needed for our example, but you should add it if you rely on specific Java features to be available.

Now pack the `class` file and `MANIFEST.MF` into a jar file. If you're using Eclipse, you can simply double click on the `jar-export.jardesc` that I have provided with the project. The content of the resulting jar file looks like this:

```
META-INF/MANIFEST.MF
io/github/mnl/osgiGettingStarted/simplestBundle/HelloWorld.class
```

The jar file can now be deployed in the OSGi framework. Go to the console of your running Felix and type "`felix:install file:<path-to-jar>`" (details can be found in the [Felix Documentation](http://felix.apache.org/documentation/subprojects/apache-felix-framework/apache-felix-framework-usage-documentation.html#installing-bundles)). Enter `felix:lb`. You'll get:

```
START LEVEL 1
   ID|State      |Level|Name
    0|Active     |    0|System Bundle (5.4.0)|5.4.0
    1|Active     |    1|Apache Felix Bundle Repository (2.0.6)|2.0.6
    2|Active     |    1|Apache Felix Gogo Command (0.16.0)|0.16.0
    3|Active     |    1|Apache Felix Gogo Runtime (0.16.2)|0.16.2
    4|Active     |    1|Apache Felix Gogo Shell (0.10.0)|0.10.0
    5|Resolved   |    1|HelloWorld (1.0.0)|1.0.0
```

So here it is, your first OSGi bundle. Its entry looks a bit different from the others because the state is "Resolved" instead of "Active". This indicates that it has been installed without problems, but it hasn't been started yet. Let's do this now. Issue the command "`felix:start 5`". When you list the bundles again, the state has changed:

```
    5|Active     |    1|HelloWorld (1.0.0)|1.0.0
```

Despite having an active bundle, nothing happens. This is not really surprising. First, we haven't provided any code aside from the empty class definition. Second, the class hasn't been referenced in the manifest (actually, the class is completely irrelevant for building this bundle, but introducing a bundle without a component base would have been a bit irritating). Obviously, we need something similar to the method "`static void main(...)`" in an application's startup class. We need a method that is invoked to get our component started. This issue will be covered in the next part.

Meanwhile &mdash; as it doesn't do anything useful anyway &mdash; you can remove the simplest bundle again. Type "`felix:uninstall 5`". When you re-list the bundles, you'll find that it is gone.