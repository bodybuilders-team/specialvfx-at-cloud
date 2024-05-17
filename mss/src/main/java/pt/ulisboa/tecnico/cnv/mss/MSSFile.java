package pt.ulisboa.tecnico.cnv.mss;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

/**
 * File implementation of the MetricStorageSystem interface.
 */
public class MSSFile implements MetricStorageSystem {

    private Map<Long, Request> requests = new HashMap<>();
    private final static String OUTPUT_FILE = "requests.json";
    private final Object lock = new Object();


    @Override
    public void save(Request request) {
        requests.put(request.getId(), request);

        try (Writer writer = new FileWriter(OUTPUT_FILE)) {
            new Gson().toJson(requests, writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Request getRequestById(Request request) {
        synchronized (lock) {

            try (FileReader reader = new FileReader(OUTPUT_FILE)) {
                requests = new Gson().fromJson(reader, new TypeToken<>() {
                }.getType());

                return requests.get(request.getId());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
