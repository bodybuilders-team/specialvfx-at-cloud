package pt.ulisboa.tecnico.cnv.mss;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.Getter;
import lombok.Setter;

/**
 * A request in the system, containing an ID, a completion status, and some metrics.
 */
@DynamoDBTable(tableName = MSSDynamoDB.TABLE_NAME)
@Getter
@Setter
public abstract class Request {
    @DynamoDBHashKey(attributeName = "request_id")
    public final long id;
    @DynamoDBAttribute(attributeName = "completed")
    protected boolean completed = false;
    @DynamoDBAttribute(attributeName = "bbl_count")
    protected long bblCount;
    @DynamoDBAttribute(attributeName = "instruction_count")
    protected long instructionCount;

    protected Request(long id) {
        this.id = id;
    }
}
