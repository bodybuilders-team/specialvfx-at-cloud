package pt.ulisboa.tecnico.cnv.mss;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;


@DynamoDbBean
public abstract class RequestMetric {
    private String id;
    private long bblCount;
    private long instructionCount;

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public long getBblCount() {
        return bblCount;
    }

    public void setBblCount(final long bblCount) {
        this.bblCount = bblCount;
    }

    public long getInstructionCount() {
        return instructionCount;
    }

    public void setInstructionCount(final long instructionCount) {
        this.instructionCount = instructionCount;
    }
}
