package rs.ac.bg.etf.pp1.error_reporting;


import org.apache.log4j.Logger;
import rs.ac.bg.etf.pp1.ast.SyntaxNode;

public class Reporter {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    public static final String ANSI_BLACK_BACKGROUND = "\u001B[40m";
    public static final String ANSI_RED_BACKGROUND = "\u001B[41m";
    public static final String ANSI_GREEN_BACKGROUND = "\u001B[42m";
    public static final String ANSI_YELLOW_BACKGROUND = "\u001B[43m";
    public static final String ANSI_BLUE_BACKGROUND = "\u001B[44m";
    public static final String ANSI_PURPLE_BACKGROUND = "\u001B[45m";
    public static final String ANSI_CYAN_BACKGROUND = "\u001B[46m";
    public static final String ANSI_WHITE_BACKGROUND = "\u001B[47m";

    public static Reporter reporter = null;

    public static void init() {
        reporter = new Reporter();
    }



    boolean errorDetected = false;
    Logger log = Logger.getLogger(getClass());



    public boolean isErrorDetected() {
        return errorDetected;
    }


    public void submit(String message, Boolean isError, SyntaxNode info) {
        errorDetected |= isError;

        StringBuilder msg = new StringBuilder(isError ? ANSI_RED : "");
        if (info != null)
            msg.append("line ").append(String.format("%3d", info.getLine())).append(" - ");

        msg.append(message).append(ANSI_RESET);

        if (isError) log.error(msg);
        else log.info(msg);

    }

    public void submit(Status status, SyntaxNode info) {
        submit(status.toString(), status.isError(), info);
    }

    public void error(String message, SyntaxNode info) {
        submit(message, true, info);
    }

    public void info(String message, SyntaxNode info) {
        submit(message, false, info);
    }





}
