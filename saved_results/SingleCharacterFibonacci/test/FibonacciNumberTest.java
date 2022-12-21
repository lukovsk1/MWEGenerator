import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FibonacciNumberTest {

    @Test
    public void testFibonacci() {
        assertThrows(ArithmeticException.class, () -> FibonacciNumber.getFibonacciNumber(-1));
        assertEquals(0, FibonacciNumber.getFibonacciNumber(0));
        assertEquals(1, FibonacciNumber.getFibonacciNumber(2));
        assertEquals(1, FibonacciNumber.getFibonacciNumber(2));
        assertEquals(2, FibonacciNumber.getFibonacciNumber(3));
        assertEquals(3, FibonacciNumber.getFibonacciNumber(4));
        assertEquals(5, FibonacciNumber.getFibonacciNumber(5));
        assertEquals(8, FibonacciNumber.getFibonacciNumber(6));
    }
}