package testexecutor;

public class TestingException extends RuntimeException {

    public TestingException(String msg) {
        super(msg);
    }

    public TestingException(String msg, Throwable throwable) {
        super(msg, throwable);
    }
}
