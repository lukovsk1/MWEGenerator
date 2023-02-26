package utility;

public class SlicingException extends RuntimeException {

    public SlicingException(String msg) {
        super(msg);
    }

    public SlicingException(String msg, Throwable throwable) {
        super(msg, throwable);
    }
}
