package pt.ulisboa.tecnico.cnv.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionUtils {

    public static String getStackTrace(Throwable t) {
        final var sw = new StringWriter();
        final var pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        return sw.getBuffer().toString();
    }
}
