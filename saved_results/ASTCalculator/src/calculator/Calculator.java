package calculator;

class Calculator {

    int division(int n, int d) {
        if(d >= 0) {
            return n / d;
        } else {
            throw new DividedByZeroException();
        }
    }

    int multiplication(int a, int b) {
        return a * b;
    }

    

    }
