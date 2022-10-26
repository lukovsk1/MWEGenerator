package calculator;

public class DividedByZeroException extends RuntimeException {

    public DividedByZeroException() {
        super("Divided by 0");
    }
}
