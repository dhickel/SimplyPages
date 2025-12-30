package io.mindspice.demo.pages;

import io.mindspice.simplypages.components.*;
import io.mindspice.simplypages.components.media.*;
import io.mindspice.simplypages.layout.*;
import io.mindspice.simplypages.modules.*;
import org.springframework.stereotype.Component;

/**
 * Gallery and media components page.
 */
@Component
public class GalleryPage implements DemoPage {

    @Override
    public String render() {
        Page galleryPage = Page.builder()
                .addComponents(Header.H1("Gallery & Media Components"))

                .addRow(row -> row.withChild(new Markdown(
                        """
                        Media components display images, videos, and galleries.

                        ## Gallery Module

                        ```java
                        GalleryModule.create()
                            .withTitle("My Gallery")
                            .withColumns(3)  // 3 images per row
                            .addImage("/img1.jpg", "Alt text", "Caption")
                            .addImage("/img2.jpg", "Alt text", "Caption");
                        ```
                        """)))

                .addRow(row -> {
                    GalleryModule gallery = GalleryModule.create()
                            .withTitle("Sample Photo Gallery")
                            .withColumns(3)
                            .addImage("https://picsum.photos/id/1/400/300", "Landscape", "Beautiful mountain view")
                            .addImage("https://picsum.photos/id/10/400/300", "Forest", "Dense forest path")
                            .addImage("https://picsum.photos/id/20/400/300", "Coast", "Ocean coastline")
                            .addImage("https://picsum.photos/id/30/400/300", "Field", "Open field")
                            .addImage("https://picsum.photos/id/40/400/300", "River", "Flowing river")
                            .addImage("https://picsum.photos/id/50/400/300", "Sky", "Sunset sky");

                    row.withChild(gallery);
                })

                // Video Component
                .addComponents(Header.H2("Video Component"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        **Video** component embeds video players:

                        ```java
                        Video.create()
                            .withSrc("/video.mp4")
                            .withControls(true)
                            .withWidth(640)
                            .withHeight(360);
                        ```
                        """)))
                // Actual Video Component Added Here
                .addRow(row -> {
                    Video video = Video.create("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")
                            .withControls(true)
                            .withWidth(640)
                            .withHeight(360)
                            .withPoster("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/BigBuckBunny.jpg");

                    row.withChild(ContentModule.create()
                            .withTitle("Video Example")
                            .withCustomContent(video));
                })

                // Audio Component
                .addComponents(Header.H2("Audio Component"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        **Audio** component embeds audio players:

                        ```java
                        Audio.create()
                            .withSrc("/audio.mp3")
                            .withControls(true);
                        ```
                        """)))
                // Actual Audio Component Added Here
                .addRow(row -> {
                     Audio audio = Audio.create("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3")
                             .withControls(true);

                     row.withChild(ContentModule.create()
                             .withTitle("Audio Example")
                             .withCustomContent(audio));
                })

                .build();

        return galleryPage.render();
    }
}
