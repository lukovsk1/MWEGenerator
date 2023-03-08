package calculator;

public class DividedByZeroException extends RuntimeException {
    static final long serialVersionUID = 1L;

    public DividedByZeroException() {
        super("Divided by 0");
    }
}
