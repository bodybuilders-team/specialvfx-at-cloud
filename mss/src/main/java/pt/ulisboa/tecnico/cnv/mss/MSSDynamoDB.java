package pt.ulisboa.tecnico.cnv.mss;

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.util.TableUtils;

import java.util.List;

/**
 * The DynamoDB implementation of the MetricStorageSystem interface.
 */
public final class MSSDynamoDB implements MetricStorageSystem {
    public static final String AWS_REGION = "us-west-2";

    public static final String TABLE_NAME = "requests";
    public static final String TABLE_KEY = "request_id";

    private static final AmazonDynamoDB dynamoDB;
    private static final DynamoDBMapper dynamoDBMapper;

    static {
        // Initialize the DynamoDB client and mapper
        dynamoDB = AmazonDynamoDBClientBuilder.standard()
                .withCredentials(new EnvironmentVariableCredentialsProvider())
                .withRegion(AWS_REGION)
                .build();
        dynamoDBMapper = new DynamoDBMapper(dynamoDB);

        // Create the table if it doesn't exist
        TableUtils.createTableIfNotExists(dynamoDB, new CreateTableRequest()
                .withTableName(TABLE_NAME)
                .withKeySchema(new KeySchemaElement().withAttributeName(TABLE_KEY).withKeyType(KeyType.HASH))
                .withAttributeDefinitions(new AttributeDefinition().withAttributeName(TABLE_KEY).withAttributeType(ScalarAttributeType.S))
                .withProvisionedThroughput(new ProvisionedThroughput().withReadCapacityUnits(1L).withWriteCapacityUnits(1L))
        );

        try {
            TableUtils.waitUntilActive(dynamoDB, TABLE_NAME);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Private constructor to prevent instantiation.
     *
     * @param request the request to save
     */
    @Override
    public void save(Request request) {
        dynamoDBMapper.save(request);
    }

    /**
     * Get a request by its ID.
     *
     * @param request the request to get
     * @return the request with the given ID
     */
    @Override
    public List<Request> getRequestById(Request request) {
        return dynamoDBMapper.query(
                Request.class,
                new DynamoDBQueryExpression<Request>().withHashKeyValues(request)
        );
    }
}
