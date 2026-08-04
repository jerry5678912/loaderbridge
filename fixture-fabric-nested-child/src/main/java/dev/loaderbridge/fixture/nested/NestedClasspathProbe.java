package dev.loaderbridge.fixture.nested;

/** Non-entrypoint class used to prove Fabric's shared resolved-mod classpath contract. */
public final class NestedClasspathProbe {
    private NestedClasspathProbe() {
    }

    public static String value() {
        return "nested-class-visible";
    }
}
