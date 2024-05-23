package pt.ulisboa.tecnico.cnv.mss;

import lombok.Getter;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;

@Getter
public class DynamoDbClientManager {
    public static final String AWS_REGION = "eu-west-3";
    private static final String LOCAL_ENDPOINT = "http://localhost:8010";
    private static final boolean LOCAL = false;

    private final DynamoDbClient dynamoDbClient;
    private final DynamoDbEnhancedClient enhancedClient;

    public DynamoDbClientManager() {
        final var builder = DynamoDbClient.builder()
                .region(Region.of(AWS_REGION))
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create());

        if (LOCAL)
            builder.endpointOverride(URI.create(LOCAL_ENDPOINT));

        dynamoDbClient = builder.build();

        enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }

}
