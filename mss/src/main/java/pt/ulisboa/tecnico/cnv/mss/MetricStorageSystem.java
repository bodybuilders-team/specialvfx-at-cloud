package pt.ulisboa.tecnico.cnv.mss;

/**
 * The interface for a metric storage system that can save and retrieve requests.
 */
public interface MetricStorageSystem {

    /**
     * Save a request to the storage system.
     *
     * @param request the request to save
     */
    void save(Request request);

    /**
     * Get a request by its ID.
     *
     * @param request the request to get
     * @return the request with the given ID
     */
    Request getRequestById(Request request);

}
