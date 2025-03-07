package es.upm.miw.rest.exceptionshandler;

public class ConflictException extends RuntimeException {
    private static final String DESCRIPTION = "Conflict Exception";

    public ConflictException(String detail) {
        super(DESCRIPTION + ". " + detail);
    }

}
