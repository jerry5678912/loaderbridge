package dev.loaderbridge.fixture;

/** Controlled target whose observable results prove each standard Mixin injector family. */
public final class StandardMixinTarget {
    private int secret = 4;

    public String injected() {
        return "original";
    }

    public int modifyArg() {
        return twice(3);
    }

    public int modifyArgs() {
        return combine(2, 3);
    }

    public int modifyVariable(int value) {
        return value;
    }

    public int modifyConstant() {
        return 5;
    }

    public int redirect() {
        return originalRedirect();
    }

    public String overwrite() {
        return "original";
    }

    private static int twice(int value) {
        return value * 2;
    }

    private static int combine(int first, int second) {
        return first * 10 + second;
    }

    private int originalRedirect() {
        return 1;
    }

    private int hidden(int addend) {
        return secret + addend;
    }
}
