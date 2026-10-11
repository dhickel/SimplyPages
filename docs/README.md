# SimplyPages documentation

SimplyPages builds server-rendered HTML in Java. Start with the task you want to accomplish.
This documentation follows [Diátaxis](https://diataxis.fr/): learning through tutorials,
solving tasks with how-to guides, looking up reference facts, and understanding design through explanation.

| Your need | Start here |
| --- | --- |
| Learn by running a small example | [Tutorials](tutorials/README.md) |
| Serve pages, update fragments, edit content, or publish Markdown | [How-to guides](how-to/README.md) |
| Look up APIs, defaults, assets, and build requirements | [Reference](reference/README.md) |
| Understand rendering, reuse, and security boundaries | [Explanation](explanation/README.md) |
| Browse every existing guide by topic | [Complete index](INDEX.md) |

## Requirements and version

The checkout builds `io.mindspice:simplypages:1.1.0a`. Use **JDK 25** to build and run it.
Install the library locally with `./mvnw -pl simplypages install`; publication to a public Maven
repository is not assumed. The demo emits Java 21 bytecode for compatibility with its Spring
class scanner, but still needs JDK 25 to load the library.

## Read online or locally

These Markdown pages work in repository viewers and in the demo's `/docs` browser.
Run `./mvnw clean install` and then `./mvnw -pl demo spring-boot:run`; open
[local documentation](http://localhost:8080/docs) or
[local Javadocs](http://localhost:8080/javadocs/index.html).
The build packages the Markdown and generated Javadocs into the demo JAR, so deployed
navigation does not require a source checkout.

Existing topic-based pages remain available at their original paths. The four sections above
provide a focused reading path through those pages and the new runnable examples.
