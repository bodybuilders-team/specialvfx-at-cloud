package pt.ulisboa.tecnico.cnv.mss.imageprocessor;


import java.util.List;

/**
 * The interface for a metric storage system that can save and retrieve requests.
 */
public interface ImageProcessorRequestMetricRepository {

    /**
     * Save a request to the storage system.
     *
     * @param request the request to save
     */
    void save(ImageProcessorRequestMetric request);

    /**
     * Get all distinct requests stored in the system.
     *
     * @return a list of all distinct requests
     */
    List<ImageProcessorRequestMetric> getAllDistinctRequests();
}
