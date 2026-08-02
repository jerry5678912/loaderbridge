package dev.loaderbridge.repository.modrinth;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

final class HttpModrinthTransport implements ModrinthTransport {
    private static final String USER_AGENT = "LoaderBridge/0.1.0";
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    @Override
    public byte[] read(URI uri, long maximumBytes) throws IOException, InterruptedException {
        HttpResponse<InputStream> response = send(uri);
        try (InputStream input = response.body()) {
            byte[] bytes = input.readNBytes(Math.toIntExact(maximumBytes + 1));
            if (bytes.length > maximumBytes) {
                throw new IOException("Modrinth response exceeds safety limit");
            }
            return bytes;
        }
    }

    @Override
    public void download(URI uri, Path destination, long maximumBytes) throws IOException, InterruptedException {
        HttpResponse<InputStream> response = send(uri);
        try (InputStream input = response.body(); var output = Files.newOutputStream(destination)) {
            byte[] buffer = new byte[8192];
            long total = 0;
            int count;
            while ((count = input.read(buffer)) != -1) {
                total += count;
                if (total > maximumBytes) {
                    throw new IOException("Modrinth download exceeds declared size");
                }
                output.write(buffer, 0, count);
            }
        }
    }

    private HttpResponse<InputStream> send(URI uri) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofMinutes(5))
                .header("User-Agent", USER_AGENT).GET().build();
        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            response.body().close();
            throw new IOException("Modrinth returned HTTP " + response.statusCode());
        }
        return response;
    }
}
