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
     * Get all distinct requests stored in the system.
     *
     * @return a list of all distinct requests
     */
    List<RaytracerRequestMetric> getAllDistinctRequests();
}
