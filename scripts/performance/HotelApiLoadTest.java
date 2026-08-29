import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class HotelApiLoadTest {
    public static void main(String[] args) throws Exception {
        String baseUrl = args.length > 0 ? args[0] : "http://127.0.0.1:8080";
        String path = args.length > 1 ? args[1] : "/api/room/list?page=1&size=10";
        int requests = args.length > 2 ? Integer.parseInt(args[2]) : 500;
        int concurrency = args.length > 3 ? Integer.parseInt(args[3]) : 20;
        long p95LimitMs = args.length > 4 ? Long.parseLong(args[4]) : 1000;
        String token = System.getenv("HOTEL_AUTH_TOKEN");

        try (ExecutorService executor = Executors.newFixedThreadPool(concurrency)) {
            // Keep the client's transport executor separate from load-generator workers;
            // sharing one bounded pool can starve HTTP response processing.
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(3)).build();
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(5)).GET();
            if (token != null && !token.isBlank()) requestBuilder.header("Authorization", "Bearer " + token);
            HttpRequest request = requestBuilder.build();

            for (int i = 0; i < Math.min(20, requests); i++) {
                client.send(request, HttpResponse.BodyHandlers.discarding());
            }

            List<Long> latencies = Collections.synchronizedList(new ArrayList<>());
            AtomicInteger successes = new AtomicInteger();
            AtomicInteger failures = new AtomicInteger();
            long started = System.nanoTime();
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (int i = 0; i < requests; i++) {
                futures.add(CompletableFuture.runAsync(() -> {
                    long requestStarted = System.nanoTime();
                    try {
                        int status = client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
                        if (status >= 200 && status < 300) successes.incrementAndGet(); else failures.incrementAndGet();
                    } catch (Exception exception) {
                        failures.incrementAndGet();
                    } finally {
                        latencies.add(Duration.ofNanos(System.nanoTime() - requestStarted).toMillis());
                    }
                }, executor));
            }
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
            double seconds = (System.nanoTime() - started) / 1_000_000_000.0;
            Collections.sort(latencies);
            long p50 = percentile(latencies, 0.50);
            long p95 = percentile(latencies, 0.95);
            long p99 = percentile(latencies, 0.99);
            System.out.printf("endpoint=%s requests=%d concurrency=%d%n", path, requests, concurrency);
            System.out.printf("success=%d failure=%d successRate=%.2f%%%n", successes.get(), failures.get(),
                    successes.get() * 100.0 / requests);
            System.out.printf("throughput=%.2f req/s p50=%dms p95=%dms p99=%dms max=%dms%n",
                    requests / seconds, p50, p95, p99, latencies.get(latencies.size() - 1));
            if (failures.get() > 0 || p95 > p95LimitMs) System.exit(1);
        }
    }

    private static long percentile(List<Long> values, double percentile) {
        int index = Math.min(values.size() - 1, (int) Math.ceil(values.size() * percentile) - 1);
        return values.get(Math.max(index, 0));
    }
}
