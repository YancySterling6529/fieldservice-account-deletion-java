package fieldservice;

public final class InfraiException extends Exception {
    private final int status;
    private final String code;

    public InfraiException(String code, String message, int status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public int status() { return status; }
    public String code() { return code; }
}
