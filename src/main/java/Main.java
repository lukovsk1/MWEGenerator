import generator.ASTMWEGenerator;
import generator.CodeLineMWEGenerator;

public class Main {

    public static void main(String[] args) {
        //new CodeLineMWEGenerator().runGenerator(true);
        new ASTMWEGenerator().runGenerator(false);
    }
}
