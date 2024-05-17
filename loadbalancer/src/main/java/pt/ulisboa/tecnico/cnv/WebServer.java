package pt.ulisboa.tecnico.cnv;

import com.sun.net.httpserver.HttpServer;
import pt.ulisboa.tecnico.cnv.mss.MSSDynamoDB;
import pt.ulisboa.tecnico.cnv.mss.MetricStorageSystem;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.logging.Logger;

public class WebServer {
    private static final int PORT = 8001;
    private static final Logger LOGGER = Logger.getLogger(WebServer.class.getName());

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());

        MetricStorageSystem metricStorageSystem = new MSSDynamoDB();

        server.createContext("/", new LoadBalancerHandler(metricStorageSystem));

        server.start();
        System.out.println("Server started on http://localhost:" + PORT + "/");
    }
}