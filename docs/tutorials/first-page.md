# Render your first page

You will compose a page in Java and print its HTML. You need a checkout of SimplyPages,
JDK 25, and Maven available as `mvn` for the separate example project.

## Install the library

From the SimplyPages repository root:

```bash
java -version
./mvnw -pl simplypages install
```

The Java version should be 25. Maven should finish with `BUILD SUCCESS` and install version
`1.1.1` in your local Maven repository.

## Create an example project

Create an empty directory outside the SimplyPages checkout. Add this `pom.xml`:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>example</groupId>
  <artifactId>first-page</artifactId>
  <version>1.0-SNAPSHOT</version>
  <properties>
    <maven.compiler.release>25</maven.compiler.release>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  </properties>
  <dependencies>
    <dependency>
      <groupId>io.mindspice</groupId>
      <artifactId>simplypages</artifactId>
      <version>1.1.1</version>
    </dependency>
  </dependencies>
  <build><plugins><plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.14.1</version>
  </plugin></plugins></build>
</project>
```

Create `src/main/java/example/FirstPage.java`:

```java
package example;

import io.mindspice.simplypages.components.Header;
import io.mindspice.simplypages.components.Paragraph;
import io.mindspice.simplypages.layout.Page;

public final class FirstPage {
    public static void main(String[] args) {
        Page page = Page.builder()
            .addComponents(Header.H1("Inventory"))
            .addComponents(new Paragraph("Three items are ready."))
            .build();
        System.out.println(page.render());
    }
}
```

## Run and inspect

From your example directory:

```bash
mvn compile dependency:build-classpath -Dmdep.outputFile=target/classpath.txt
java -cp "target/classes:$(cat target/classpath.txt)" example.FirstPage
```

The output contains `<h1>Inventory</h1>` and `<p>Three items are ready.</p>` inside the
page's layout containers. `Page` produces page content; it does not add a document head or
load styles by itself.

Change the paragraph to `"Ready <soon> & available"` and run the commands again.
The output contains escaped angle brackets and an escaped ampersand. The text stays text.

Continue with [request data and slots](dynamic-page.md), using this same example project.
To add a complete browser document and assets, use the
[serving guide](../how-to/serve-pages-and-fragments.md).

[All tutorials](README.md) · [Documentation home](../README.md)
