package pt.ulisboa.tecnico.cnv.mss.imageprocessor;

import pt.ulisboa.tecnico.cnv.mss.DynamoDbClientManager;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class EnhanceImageRequestMetricDynamoDbRepositoryImpl implements ImageProcessorRequestMetricRepository {
    public static final String TABLE_NAME = "enhance_image_requests";
    private static final long READ_CAPACITY_UNITS = 10L;
    private static final long WRITE_CAPACITY_UNITS = 10L;

    private final DynamoDbTable<ImageProcessorRequestMetric> requestTable;

    public EnhanceImageRequestMetricDynamoDbRepositoryImpl(DynamoDbClientManager dynamoDbClientManager) {
        DynamoDbClient dynamoDbClient = dynamoDbClientManager.getDynamoDbClient();
        DynamoDbEnhancedClient enhancedClient = dynamoDbClientManager.getEnhancedClient();

        requestTable = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(ImageProcessorRequestMetric.class));

        try {
            requestTable.createTable(r ->
                    r.provisionedThroughput(
                            ProvisionedThroughput.builder()
                                    .readCapacityUnits(READ_CAPACITY_UNITS)
                                    .writeCapacityUnits(WRITE_CAPACITY_UNITS)
                                    .build()
                    )
            );

            dynamoDbClient.waiter().waitUntilTableExists(r -> r.tableName(TABLE_NAME));
        } catch (ResourceInUseException ignored) {

        }
    }

    @Override
    public void save(ImageProcessorRequestMetric request) {
        requestTable.putItem(request);
    }

    @Override
    public ImageProcessorRequestMetric getRequestById(String id) {
        return requestTable.getItem(Key.builder().partitionValue(id).build());
    }

    @Override
    public List<ImageProcessorRequestMetric> getAllRequests() {
        return requestTable.scan().items().stream().toList();
    }

    @Override
    public List<ImageProcessorRequestMetric> getAllDistinctRequests() {

        Comparator<ImageProcessorRequestMetric> comparator = Comparator
                .comparing(ImageProcessorRequestMetric::getInstructionCount)
                .thenComparing(ImageProcessorRequestMetric::getBblCount)
                .thenComparing(ImageProcessorRequestMetric::getNumPixels);

        Set<ImageProcessorRequestMetric> uniqueRequests = new TreeSet<>(comparator);

        final var requests = requestTable.scan().items().stream().toList();

        uniqueRequests.addAll(requests);

        return uniqueRequests.stream().toList();
    }

}