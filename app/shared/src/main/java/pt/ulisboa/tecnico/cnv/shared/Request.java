package pt.ulisboa.tecnico.cnv.shared;

import lombok.Getter;
import lombok.Setter;

/**
 * A request in the system, containing an ID, a completion status, and some metrics.
 */
@Getter
@Setter
public abstract class Request {
    public final long id;
    protected boolean completed = false;
    protected long bblCount;
    protected long instructionCount;
    protected long opTime;

    protected Request(long id) {
        this.id = id;
    }
}
