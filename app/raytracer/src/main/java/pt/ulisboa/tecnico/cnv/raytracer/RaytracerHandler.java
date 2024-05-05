package pt.ulisboa.tecnico.cnv.raytracer;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class RaytracerHandler implements HttpHandler, RequestHandler<Map<String, String>, String> {

    private final static ObjectMapper mapper = new ObjectMapper();

    @Override
    public void handle(HttpExchange he) throws IOException {
        // Handling CORS
        he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

        if (he.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            he.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
            he.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type,Authorization");
            he.sendResponseHeaders(204, -1);
            return;
        }

        // Parse request
        URI requestedUri = he.getRequestURI();
        String query = requestedUri.getRawQuery();
        Map<String, String> parameters = queryToMap(query);

        int scols = Integer.parseInt(parameters.get("scols"));
        int srows = Integer.parseInt(parameters.get("srows"));
        int wcols = Integer.parseInt(parameters.get("wcols"));
        int wrows = Integer.parseInt(parameters.get("wrows"));
        int coff = Integer.parseInt(parameters.get("coff"));
        int roff = Integer.parseInt(parameters.get("roff"));
        Main.ANTI_ALIAS = Boolean.parseBoolean(parameters.getOrDefault("aa", "false"));
        Main.MULTI_THREAD = Boolean.parseBoolean(parameters.getOrDefault("multi", "false"));

        InputStream stream = he.getRequestBody();
        Map<String, Object> body = mapper.readValue(stream, new TypeReference<>() {});

        byte[] input = ((String) body.get("scene")).getBytes();
        byte[] texmap = null;
        if (body.containsKey("texmap")) {
            // Convert ArrayList<Integer> to byte[]
            ArrayList<Integer> texmapBytes = (ArrayList<Integer>) body.get("texmap");
            texmap = new byte[texmapBytes.size()];
            for (int i = 0; i < texmapBytes.size(); i++) {
                texmap[i] = texmapBytes.get(i).byteValue();
            }
        }

        byte[] result = handleRequest(input, texmap, scols, srows, wcols, wrows, coff, roff);
        String response = String.format("data:image/bmp;base64,%s", Base64.getEncoder().encodeToString(result));

        he.sendResponseHeaders(200, response.length());
        OutputStream os = he.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    public Map<String, String> queryToMap(String query) {
        if (query == null) {
            return null;
        }
        Map<String, String> result = new HashMap<>();
        for (String param : query.split("&")) {
            String[] entry = param.split("=");
            if (entry.length > 1) {
                result.put(entry[0], entry[1]);
            } else {
                result.put(entry[0], "");
            }
        }
        return result;
    }

    private byte[] handleRequest(byte[] input, byte[] texmap, int scols, int srows, int wcols, int wrows, int coff, int roff) {
        try {
            RayTracer rayTracer = new RayTracer(scols, srows, wcols, wrows, coff, roff);
            rayTracer.readScene(input, texmap);
            BufferedImage image = rayTracer.draw();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "bmp", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage().getBytes();
        }
    }

    @Override
    public String handleRequest(Map<String,String> event, Context context) {
        Main.ANTI_ALIAS = Boolean.parseBoolean(event.getOrDefault("aa", "false"));
        Main.MULTI_THREAD = Boolean.parseBoolean(event.getOrDefault("multi", "false"));
        int scols = Integer.parseInt(event.get("scols"));
        int srows = Integer.parseInt(event.get("srows"));
        int wcols = Integer.parseInt(event.get("wcols"));
        int wrows = Integer.parseInt(event.get("wrows"));
        int coff = Integer.parseInt(event.get("coff"));
        int roff = Integer.parseInt(event.get("roff"));
        Base64.Decoder decoder = Base64.getDecoder();
        byte[] input = decoder.decode(event.get("input"));
        byte[] texmap = event.containsKey("texmap") ? decoder.decode(event.get("texmap")) : null;
        byte[] byteArrayResult = handleRequest(input, texmap, scols, srows, wcols, wrows, coff, roff);
        return Base64.getEncoder().encodeToString(byteArrayResult);
    }
}
