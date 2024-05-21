package pt.ulisboa.tecnico.cnv.mss.imageprocessor;

import com.google.gson.Gson;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ImageProcessorRequestMetricMemoryRepositoryImpl implements ImageProcessorRequestMetricRepository {

    private final Map<String, ImageProcessorRequestMetric> requests = new ConcurrentHashMap<>();

    @Override
    public void save(ImageProcessorRequestMetric request) {
        requests.put(request.getId(), request);

    }

    @Override
    public ImageProcessorRequestMetric getRequestById(String id) {
        return requests.get(id);
    }

    @Override
    public List<ImageProcessorRequestMetric> getAllRequests() {
        return List.copyOf(requests.values());
    }

    @Override
    public List<ImageProcessorRequestMetric> getAllDistinctRequests() {
        return List.of();
    }
}
