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
     * Get a request by its ID.
     *
     * @param id the ID of the request
     * @return the request with the given ID
     */
    ImageProcessorRequestMetric getRequestById(String id);

    List<ImageProcessorRequestMetric> getAllRequests();

    List<ImageProcessorRequestMetric> getAllDistinctRequests();
}
