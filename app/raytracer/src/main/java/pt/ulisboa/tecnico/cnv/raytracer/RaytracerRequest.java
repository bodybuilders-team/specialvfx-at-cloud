package pt.ulisboa.tecnico.cnv.raytracer;

import lombok.Getter;
import pt.ulisboa.tecnico.cnv.shared.Request;

@Getter
class RaytracerRequest extends Request {
    private final byte[] input;
    private final byte[] texmap;
    private final int scols;
    private final int srows;
    private final int wcols;
    private final int wrows;
    private final int coff;
    private final int roff;

    protected RaytracerRequest(long id, byte[] input, byte[] texmap, int scols, int srows, int wcols, int wrows, int coff, int roff) {
        super(id);
        this.input = input;
        this.texmap = texmap;
        this.scols = scols;
        this.srows = srows;
        this.wcols = wcols;
        this.wrows = wrows;
        this.coff = coff;
        this.roff = roff;
    }

    @Override
    public String toString() {
        return "RaytracerRequest{" +
                "id=" + id +
                ", scols=" + scols +
                ", srows=" + srows +
                ", wcols=" + wcols +
                ", wrows=" + wrows +
                ", coff=" + coff +
                ", roff=" + roff +
                ", bblCount=" + bblCount +
                ", instructionCount=" + instructionCount +
                ", completed=" + completed +
                '}';
    }
}
