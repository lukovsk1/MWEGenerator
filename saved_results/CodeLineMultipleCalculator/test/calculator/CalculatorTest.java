package calculator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CalculatorTest {

    @Test
    public void testCalculator() {
        Calculator calc = new Calculator();
        int x = calc.multiplication(0, 2);
        assertThrows(DividedByZeroException.class, () -> calc.division(42, x));
    }
}