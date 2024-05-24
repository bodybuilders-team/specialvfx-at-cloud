package pt.ulisboa.tecnico.cnv.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionUtils {

    private ExceptionUtils() {
        // Utility class
    }

    /**
     * Get the stack trace of a throwable as a string
     *
     * @param t the throwable
     * @return the stack trace as a string
     */
    public static String getStackTrace(Throwable t) {
        final var sw = new StringWriter();
        final var pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        return sw.getBuffer().toString();
    }
}
