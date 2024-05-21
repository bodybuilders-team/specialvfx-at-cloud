package pt.ulisboa.tecnico.cnv.mss.raytracer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RaytracerRequestMetricMemoryRepositoryImpl implements RaytracerRequestMetricRepository {

    private final Map<String, RaytracerRequestMetric> requests = new ConcurrentHashMap<>();

    @Override
    public void save(RaytracerRequestMetric request) {
        requests.put(request.getId(), request);

    }


    @Override
    public RaytracerRequestMetric getRequestById(String id) {
        return requests.get(id);
    }

    @Override
    public List<RaytracerRequestMetric> getAllRequests() {
        return List.copyOf(requests.values());
    }

    @Override
    public List<RaytracerRequestMetric> getAllDistinctRequests() {
        return List.of();
    }
}
