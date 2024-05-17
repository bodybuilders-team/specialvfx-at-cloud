package pt.ulisboa.tecnico.cnv.mss;

import lombok.Getter;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

/**
 * A request in the system, containing an ID, a completion status, and some metrics.
 */
@Getter
@Setter
@DynamoDbBean
public abstract class Request {
    public final Long id;
    protected boolean completed = false;
    protected long bblCount;
    protected long instructionCount;

    public Request() {
        this.id = null;
    }

    protected Request(long id) {
        this.id = id;
    }

    @DynamoDbPartitionKey
    public Long getId() {
        return id;
    }
}

