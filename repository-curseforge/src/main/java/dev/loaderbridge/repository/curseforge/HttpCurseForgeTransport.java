package dev.loaderbridge.repository.curseforge;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Supplier;

final class HttpCurseForgeTransport implements CurseForgeTransport {
    private static final String USER_AGENT = "LoaderBridge/0.1.0";
    private final HttpClient client;
    private final Supplier<String> apiKey;

    HttpCurseForgeTransport() {
        this(System::getenv, "CURSEFORGE_API_KEY");
    }

    HttpCurseForgeTransport(java.util.function.Function<String, String> environment, String variable) {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20))
                .followRedirects(HttpClient.Redirect.NEVER).build(), () -> environment.apply(variable));
    }

    HttpCurseForgeTransport(HttpClient client, Supplier<String> apiKey) {
        this.client = Objects.requireNonNull(client, "client");
        this.apiKey = Objects.requireNonNull(apiKey, "apiKey");
    }

    @Override
    public byte[] read(URI uri, long maximumBytes) throws IOException, InterruptedException {
        String key = apiKey.get();
        if (key == null || key.isBlank()) {
            throw new IOException("CURSEFORGE_API_KEY is required for CurseForge metadata requests");
        }
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofMinutes(2))
                .header("Accept", "application/json").header("User-Agent", USER_AGENT)
                .header("x-api-key", key.strip()).GET().build();
        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            response.body().close();
            throw new IOException("CurseForge API returned HTTP " + response.statusCode());
        }
        try (InputStream input = response.body()) {
            byte[] bytes = input.readNBytes(Math.toIntExact(maximumBytes + 1));
            if (bytes.length > maximumBytes) {
                throw new IOException("CurseForge response exceeded metadata limit");
            }
            return bytes;
        }
    }

    @Override
    public void download(URI uri, Path destination, long maximumBytes)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofMinutes(5))
                .header("User-Agent", USER_AGENT).GET().build();
        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            response.body().close();
            throw new IOException("CurseForge download returned HTTP " + response.statusCode());
        }
        try (InputStream input = response.body(); var output = java.nio.file.Files.newOutputStream(destination)) {
            byte[] buffer = new byte[8192];
            long total = 0;
            int count;
            while ((count = input.read(buffer)) != -1) {
                total += count;
                if (total > maximumBytes) {
                    throw new IOException("CurseForge download exceeded declared size");
                }
                output.write(buffer, 0, count);
            }
        }
    }
}
