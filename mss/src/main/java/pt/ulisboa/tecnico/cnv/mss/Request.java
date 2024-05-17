package pt.ulisboa.tecnico.cnv.mss;

import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.util.UUID;

/**
 * A request in the system, containing an ID, a completion status, and some metrics.
 */
@DynamoDbBean
@Data
public abstract class Request {
    public String id;
    public long idKey;

    public boolean completed = false;
    public long bblCount;
    public long instructionCount;

    public Request() {
        this.id = UUID.randomUUID().toString();
    }

    protected Request(long idKey) {
        this.idKey = idKey;
    }

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }


}

