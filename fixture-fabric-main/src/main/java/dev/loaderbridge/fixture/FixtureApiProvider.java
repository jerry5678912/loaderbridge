package dev.loaderbridge.fixture;

public final class FixtureApiProvider implements FixtureApi {
    @Override
    public String value() {
        return "custom-entrypoint";
    }
}
