package utility;

public class StatsException extends RuntimeException {

    public StatsException(String msg) {
        super(msg);
    }

    public StatsException(String msg, Throwable throwable) {
        super(msg, throwable);
    }
}
