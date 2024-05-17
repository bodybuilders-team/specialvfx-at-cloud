package pt.ulisboa.tecnico.cnv;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import pt.ulisboa.tecnico.cnv.mss.MetricStorageSystem;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class LoadBalancerHandler implements HttpHandler {
    final List<String> servers = new LinkedList<>(
            List.of("http://localhost:8000", "http://localhost:8001", "http://localhost:8002")
    );
    final AtomicInteger currentServer = new AtomicInteger();
    private final MetricStorageSystem metricStorageSystem;

    public LoadBalancerHandler(MetricStorageSystem metricStorageSystem) {
        this.metricStorageSystem = metricStorageSystem;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        currentServer.set((currentServer.get() + 1) % servers.size());

        // Send http request to the selected server
        final var url = new java.net.URL(servers.get(currentServer.get()) + exchange.getRequestURI());

        final var connection = (java.net.HttpURLConnection) url.openConnection();
        connection.setRequestMethod(exchange.getRequestMethod());

        for (var header : exchange.getRequestHeaders().entrySet()) {
            for (var value : header.getValue()) {
                connection.setRequestProperty(header.getKey(), value);
            }
        }

        connection.setDoOutput(true);

        // Send the request body
        try (var os = connection.getOutputStream()) {
            os.write(exchange.getRequestBody().readAllBytes());
        }

    }
}
