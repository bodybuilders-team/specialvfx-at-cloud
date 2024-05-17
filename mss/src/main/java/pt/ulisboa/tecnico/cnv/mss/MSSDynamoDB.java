package pt.ulisboa.tecnico.cnv.mss;

import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.net.URI;

public class MSSDynamoDB implements MetricStorageSystem {
    public static final String AWS_REGION = "us-west-2";
    public static final String TABLE_NAME = "requests";
    public static final String TABLE_KEY = "request_id";

    private static final DynamoDbClient dynamoDbClient;
    private static final DynamoDbEnhancedClient enhancedClient;
    private static final DynamoDbTable<Request> requestTable;
    private static final boolean LOCAL = false;

    private static final String LOCAL_ENDPOINT = "http://localhost:8010";

    static {
        // Initialize the DynamoDB client and enhanced client
        final var builder = DynamoDbClient.builder()
                .region(Region.of(AWS_REGION))
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create());

        if (LOCAL)
            builder.endpointOverride(URI.create(LOCAL_ENDPOINT));

        dynamoDbClient = builder.build();

        enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();

        requestTable = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(Request.class));

        // Create the table if it doesn't exist
        CreateTableRequest createTableRequest = CreateTableRequest.builder()
                .tableName(TABLE_NAME)
                .keySchema(KeySchemaElement.builder().attributeName(TABLE_KEY).keyType(KeyType.HASH).build())
                .attributeDefinitions(AttributeDefinition.builder().attributeName(TABLE_KEY).attributeType(ScalarAttributeType.S).build())
                .provisionedThroughput(ProvisionedThroughput.builder().readCapacityUnits(1L).writeCapacityUnits(1L).build())
                .build();

        try {
            dynamoDbClient.createTable(createTableRequest);
            dynamoDbClient.waiter().waitUntilTableExists(r -> r.tableName(TABLE_NAME));
        } catch (ResourceInUseException e) {
            // Table already exists
        }
    }

    @Override
    public void save(Request request) {
        requestTable.putItem(PutItemEnhancedRequest.builder(Request.class).item(request).build());
    }

    @Override
    public Request getRequestById(Request request) {
        return requestTable.getItem(GetItemEnhancedRequest.builder().key(k -> k.partitionValue(request.getId())).build());
    }
}