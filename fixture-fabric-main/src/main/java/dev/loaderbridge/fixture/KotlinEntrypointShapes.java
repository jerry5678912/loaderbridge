package dev.loaderbridge.fixture;

/** JVM shapes emitted for the Fabric Language Kotlin entrypoint forms. */
public final class KotlinEntrypointShapes {
    private KotlinEntrypointShapes() {}

    private static void mark(String shape) {
        System.out.println("LOADERBRIDGE_FIXTURE_KOTLIN_" + shape + "_READY");
    }

    public static final class ClassEntrypoint implements Runnable {
        @Override public void run() { mark("CLASS"); }
    }

    public static final class ObjectClass implements Runnable {
        public static final ObjectClass INSTANCE = new ObjectClass();
        private ObjectClass() {}
        @Override public void run() { mark("OBJECT_CLASS"); }
    }

    public static final class ObjectFunction {
        public static final ObjectFunction INSTANCE = new ObjectFunction();
        private ObjectFunction() {}
        public void init() { mark("OBJECT_FUNCTION"); }
    }

    public static final class ObjectProperty {
        public static final ObjectProperty INSTANCE = new ObjectProperty();
        private final Runnable initializer = () -> {
            throw new IllegalStateException("Kotlin property backing field was used");
        };
        private ObjectProperty() {}
        public Runnable getInitializer() { return () -> mark("OBJECT_PROPERTY"); }
    }

    public static final class CompanionClass {
        public static final Companion Companion = new Companion();

        public static final class Companion implements Runnable {
            private Companion() {}
            @Override public void run() { mark("COMPANION_CLASS"); }
        }
    }

    public static final class CompanionFunction {
        public static final Companion Companion = new Companion();

        public static final class Companion {
            private Companion() {}
            public void init() { mark("COMPANION_FUNCTION"); }
        }
    }

    public static final class CompanionProperty {
        public static final Companion Companion = new Companion();

        public static final class Companion {
            private final Runnable initializer = () -> {
                throw new IllegalStateException("Kotlin companion backing field was used");
            };
            private Companion() {}
            public Runnable getInitializer() { return () -> mark("COMPANION_PROPERTY"); }
        }
    }

    public static final class TopLevel {
        private TopLevel() {}
        public static void init() { mark("TOP_LEVEL"); }
    }
}
