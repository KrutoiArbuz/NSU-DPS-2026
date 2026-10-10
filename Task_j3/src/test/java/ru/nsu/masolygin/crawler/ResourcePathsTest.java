package ru.nsu.masolygin.crawler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import org.junit.jupiter.api.Test;

class ResourcePathsTest {
    @Test
    void testRootAndEquivalentLinks() {
        ResourcePaths paths = new ResourcePaths(URI.create("http://LOCALHOST/ignored?old=1"));
        assertEquals(URI.create("http://localhost/"), paths.root());
        assertEquals(paths.resolve("/b"), paths.resolve("a/../b#fragment"));
        assertEquals(paths.resolve("/b"), paths.resolve("http://LOCALHOST:80/b"));
        assertEquals(URI.create("http://localhost/a%20b?q=%23"), paths.resolve("/a%20b?q=%23#ignored"));
    }

    @Test
    void testRejectsForeignOriginAndInvalidServer() {
        ResourcePaths paths = new ResourcePaths(URI.create("http://localhost:8080"));
        for (String successor : new String[] {"https://localhost:8080/a", "http://example.com/a", "http://localhost:8081/a", "http://user@localhost:8080/a"}) {
            assertThrows(IllegalArgumentException.class, () -> paths.resolve(successor));
        }
        assertThrows(IllegalArgumentException.class, () -> new ResourcePaths(URI.create("file:///tmp/resources")));
    }
}
