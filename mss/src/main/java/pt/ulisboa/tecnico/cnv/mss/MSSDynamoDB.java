package pt.ulisboa.tecnico.cnv.mss;

import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.internal.waiters.ResponseOrException;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter;

import java.net.URI;

public class MSSDynamoDB implements MetricStorageSystem {
    public static final String AWS_REGION = "eu-west-3";
    public static final String TABLE_NAME = "requests";

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

        try {
            requestTable.createTable(r ->
                    r.provisionedThroughput(
                            ProvisionedThroughput.builder()
                                    .readCapacityUnits(10L)
                                    .writeCapacityUnits(10L)
                                    .build()
                    )
            );

            try (DynamoDbWaiter waiter = DynamoDbWaiter.builder().client(dynamoDbClient).build()) { // DynamoDbWaiter is Autocloseable
                ResponseOrException<DescribeTableResponse> response = waiter
                        .waitUntilTableExists(r -> r.tableName(TABLE_NAME).build())
                        .matched();
                DescribeTableResponse tableDescription = response.response().orElseThrow(
                        () -> new RuntimeException("Customer table was not created."));

                System.out.println("Table " + tableDescription.table() + " has been created.");
            }
        } catch (ResourceInUseException ignored) {
        }
    }

    @Override
    public void save(Request request) {
        requestTable.putItem(request);
    }

    @Override
    public Request getRequestById(Request request) {
        return requestTable.getItem(GetItemEnhancedRequest.builder().key(k -> k.partitionValue(request.getIdKey())).build());
    }
}