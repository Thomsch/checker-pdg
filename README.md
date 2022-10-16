# Dataflow Checker

A common problem when programming is TODO.
This results in a run-time exception.

The Dataflow Checker guarantees, at compile time, that your code will
not suffer that run-time exception.


## How to run the checker

First, publish the checker to your local Maven repository by running
`./gradlew publishToMavenLocal` in this repository.

Then, if you use Gradle, add the following to the `build.gradle` file in
the project you wish to type-check (using Maven is similar):

```
repositories {
    mavenLocal()
    mavenCentral()
}
dependencies {
    annotationProcessor 'org.checkerframework:templatefora-checker:0.1-SNAPSHOT'
}
```

Now, when you build your project, the Dataflow Checker will also run,
informing you of any potential errors related to TODO.


## How to specify your code

At compile time, the DataflowChecker estimates what values the program
may compute at run time.  It issues a warning if the program may TODO.
It works via a technique called pluggable typechecking.

You need to specify the contracts of methods and fields in your code --
that is, their requirements and their guarantees.  The DataflowChecker
ensures that your code is consistent with the contracts, and that the
contracts guarantee that TODO.

You specify your code by writing *qualifiers* such as `@TemplateforaBottom`
on types, to indicate more precisely what values the type represents.
Here are the type qualifiers that are supported by the DataflowChecker:

`@TemplateforaUnknown`:
The value might or might not be TODO. It is not safe to use for TODO.
This is the default type, so programmers usually do not need to write it.

`@TemplateforaBottom`:
The value is definitely TODO. It is safe to use for TODO.


## How to build the checker

Run these commands from the top-level directory.

`./gradlew build`: build the checker

`./gradlew publishToMavenLocal`: publish the checker to your local Maven repository.
This is useful for testing before you publish it elsewhere, such as to Maven Central.


## More information

The Dataflow Checker is built upon the Checker Framework.  Please see
the [Checker Framework Manual](https://checkerframework.org/manual/) for
more information about using pluggable type-checkers, including this one.
