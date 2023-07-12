import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LoopTest {
    @Test
    public void testLoop() {
        Loop loop = new Loop();
        assertEquals(0, loop.loop());
    }
}