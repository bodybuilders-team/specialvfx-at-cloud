package pt.ulisboa.tecnico.cnv.mss.raytracer;


import java.util.List;

/**
 * The interface for a metric storage system that can save and retrieve requests.
 */
public interface RaytracerRequestMetricRepository {

    /**
     * Save a request to the storage system.
     *
     * @param request the request to save
     */
    void save(RaytracerRequestMetric request);

    /**
     * Get a request by its ID.
     *
     * @param id the ID of the request
     * @return the request with the given ID
     */
    RaytracerRequestMetric getRequestById(String id);

    /**
     * Get all requests stored in the system.
     *
     * @return a list of all requests
     */
    List<RaytracerRequestMetric> getAllRequests();

    /**
     * Get all distinct requests stored in the system.
     *
     * @return a list of all distinct requests
     */
    List<RaytracerRequestMetric> getAllDistinctRequests();
}
