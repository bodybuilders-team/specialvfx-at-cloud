package pt.ulisboa.tecnico.cnv.imageproc;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BlurImageHandlerTest {

    @org.junit.jupiter.api.Test
    void process() {
        BlurImageHandler handler = new BlurImageHandler();
        final var data = new HashMap<String, String>();

        data.put("fileFormat", "png");
        data.put("body", "iVBORw0KGgoAAAANSUhEUgAAACQAAAASAQMAAAAJ7e5rAAAABlBMVEX///8AAABVwtN+AAAAJUlEQVR4nGNgQAaMB0AkswKIZJEAk/wgIRawrAoS6YJE2jAQBwDODwLkkVY30QAAAABJRU5ErkJggg==");
        final var result = handler.handleRequest(data, null);

        assertEquals("iVBORw0KGgoAAAANSUhEUgAAACQAAAASCAIAAAC8Qet/AAAA3klEQVR4XqXQwRKDIAyEYd//YVuwrXqsw07DsibI0P/gxYRvYMk5J+rZ9miTv8h2c2ktvUrvX5/SgokIQxFmW6mVBIO0bdsQFkXWFIb0VC+exyGGuW9YMcT7enYbT9o6S9drTWI8lgKMpXsseZ4MpIt0g52/Iwx1MFvM3Tf0sRx4brwl1xIM0r7vDoZvP14RCYxIFRMv32Ey/C+GFIkZk9wHhFQxJGchl0G2KNI8FsWLM9g67PHKoHQcx3IO8SYO0rMpHl5JMgySYZB8zFKkJDNXKbpWxSKvX0dyr3X2BU8GLQBossu3AAAAAElFTkSuQmCC",
                result);
    }

}