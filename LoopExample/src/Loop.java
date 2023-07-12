public class Loop {
    public int loop() {
        boolean notFirstLoop = false;
        while (true) {
            if (notFirstLoop) {
                return 1;
            }
            notFirstLoop = true;
        }
    }
}