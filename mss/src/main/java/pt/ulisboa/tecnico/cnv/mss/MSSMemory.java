package pt.ulisboa.tecnico.cnv.mss;

import java.util.HashMap;
import java.util.Map;

/**
 * The in-memory implementation of the MetricStorageSystem interface.
 */
public class MSSMemory implements MetricStorageSystem {

    private final Map<Long, Request> requests = new HashMap<>();

    @Override
    public void save(Request request) {
        requests.put(request.getId(), request);
    }

    @Override
    public Request getRequestById(Request request) {
        return requests.get(request.getId());
    }
}
