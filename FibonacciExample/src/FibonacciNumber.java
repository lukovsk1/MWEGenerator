public final class FibonacciNumber {

    private FibonacciNumber() {
    }

    public static int getFibonacciNumber(int n) {
        if(n < 0) {
            throw new ArithmeticException("negative fibonacci numbers don't exist");
        }
        if(n == 0) {
            return 0;
        }
        if(n == 1) {
            return 1;
        }

        return getFibonacciNumber(n - 2 ) + getFibonacciNumber( n );
    }

}
