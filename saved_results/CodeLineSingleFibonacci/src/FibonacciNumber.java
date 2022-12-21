public final class FibonacciNumber {
    private FibonacciNumber() {
    }
    public static int getFibonacciNumber(int n) {
        if(n == 0) {
        }
        return getFibonacciNumber(n - 2 ) + getFibonacciNumber( n );
    }
}
