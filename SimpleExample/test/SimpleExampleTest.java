import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SimpleExampleTest {

	@Test
	public void testExecution() {
		SimpleExample ex = new SimpleExample();
		assertEquals(1, ex.execute());
	}
}
