# Kotlin  Plugin 

Improves pitest's support for Kotlin

This is a work in progress looking for someone to take over and progress it.

**UPDATE** - There has been little progress on an open source kotlin plugin, but GroupCDG are now looking for beta testers for a closed source version. If you're interested in helping kotlin support happen, please contact pitest.demo@groupcdg.com.

## Usage

The plugin requires pitest 1.4.1 or later. 

To activate the plugin it must be placed on the classpath of the pitest tool (**not** on the classpath of the project being mutated).

e.g for maven

```xml
    <plugins>
      <plugin>
        <groupId>org.pitest</groupId>
        <artifactId>pitest-maven</artifactId>
        <version>1.2.5</version>
        <dependencies>
          <dependency>
            <groupId>org.pitest</groupId>
            <artifactId>pitest-kotlin-plugin</artifactId>
            <version>0.1-SNAPSHOT</version>
          </dependency>
        </dependencies>

        <configuration>
blah
        </configuration>
      </plugin>
   </pluginsugin>
```

or for gradle

```
buildscript {
   repositories {
       mavenCentral()
   }
   configurations.maybeCreate("pitest")
   dependencies {
       classpath 'info.solidsoft.gradle.pitest:gradle-pitest-plugin:1.2.4'
       pitest 'org.pitest:pitest-kotlin-plugin:0.1-SNAPSHOT'
   }
}

apply plugin: "info.solidsoft.pitest"

pitest {
    pitestVersion = "1.3.2"
    targetClasses = ['our.base.package.*']  // by default "${project.group}.*"
}
```
See [gradle-pitest-plugin documentation](http://gradle-pitest-plugin.solidsoft.info/) for more configuration options.

## About

When the plugin is enabled pitest will avoid creating junk mutations in code that uses

* de structuring code
* null casts
* safe casts
* elvis operator


