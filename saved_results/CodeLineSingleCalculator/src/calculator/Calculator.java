package calculator;
public class Calculator {
    public int division(int n, int d) {
        if(d >= 0) {
            return n / d;
        } else {
            throw new DividedByZeroException();
        }
    }
    public int multiplication(int a, int b) {
        return a * b;
    }
    public int subtraction(int a, int b) {
        return a - b;
    }
}
