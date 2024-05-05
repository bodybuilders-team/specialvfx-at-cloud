package pt.ulisboa.tecnico.cnv.webserver;

import com.sun.net.httpserver.HttpServer;
import pt.ulisboa.tecnico.cnv.imageproc.BlurImageHandler;
import pt.ulisboa.tecnico.cnv.imageproc.EnhanceImageHandler;
import pt.ulisboa.tecnico.cnv.raytracer.RaytracerHandler;

import java.net.InetSocketAddress;

/**
 * The SpecialVFX@Cloud web server that listens for HTTP requests and delegates them to the appropriate handler.
 */
public class WebServer {

    private static final int PORT = 8000;

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.createContext("/", new RootHandler());
        server.createContext("/raytracer", new RaytracerHandler());
        server.createContext("/blurimage", new BlurImageHandler());
        server.createContext("/enhanceimage", new EnhanceImageHandler());
        server.start();
        System.out.println("Server started on http://localhost:" + PORT + "/");
    }
}
