package pt.ulisboa.tecnico.cnv.javassist;

import java.util.UUID;

/**
 * A request in the system, containing an ID, a completion status, and some metrics.
 */
public class Request {
    private String id;
    private boolean completed = false;
    private long bblCount;
    private long instructionCount;

    public Request() {
        id = UUID.randomUUID().toString();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public long getBblCount() {
        return bblCount;
    }

    public void setBblCount(long bblCount) {
        this.bblCount = bblCount;
    }

    public long getInstructionCount() {
        return instructionCount;
    }

    public void setInstructionCount(long instructionCount) {
        this.instructionCount = instructionCount;
    }

}

