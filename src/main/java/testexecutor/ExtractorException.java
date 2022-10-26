package testexecutor;

public class ExtractorException extends RuntimeException {

    public ExtractorException(String msg) {
        super(msg);
    }

    public ExtractorException(String msg, Throwable throwable) {
        super(msg, throwable);
    }
}
