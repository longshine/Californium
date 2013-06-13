package ch.inf.vs.californium.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import ch.inf.vs.californium.coap.BlockOption;


public class BlockOptionTest {

	@Test
	public void testGetValue() {
		assertArrayEquals(toBytes(0, false,   0), b(0x0));
		assertArrayEquals(toBytes(0, false,   1), b(0x10));
		assertArrayEquals(toBytes(0, false,  15), b(0xf0));
		assertArrayEquals(toBytes(0, false,  16), b(0x00, 0x01));
		assertArrayEquals(toBytes(0, false,  79), b(0xf0, 0x04));
		assertArrayEquals(toBytes(0, false, 113), b(0x10, 0x07));
		assertArrayEquals(toBytes(0, false, 26387), b(0x30, 0x71, 0x06));
		assertArrayEquals(toBytes(0, false, 1048575), b(0xf0, 0xff, 0xff));
		assertArrayEquals(toBytes(7, false, 1048575), b(0xf7, 0xff, 0xff));
		assertArrayEquals(toBytes(7,  true, 1048575), b(0xff, 0xff, 0xff));
	}
	
	@Test
	public void testCombined() {
		testCombined(0, false,   0);
		testCombined(0, false,   1);
		testCombined(0, false,  15);
		testCombined(0, false,  16);
		testCombined(0, false,  79);
		testCombined(0, false, 113);
		testCombined(0, false, 26387);
		testCombined(0, false, 1048575);
		testCombined(7, false, 1048575);
		testCombined(7,  true, 1048575);
	}
	
	private void testCombined(int szx, boolean m, int num) {
		BlockOption block = new BlockOption(szx, m, num);
		BlockOption copy = new BlockOption(block.getValue());
		assertEquals(block.getSzx(), copy.getSzx());
		assertEquals(block.isM(), copy.isM());
		assertEquals(block.getNum(), copy.getNum());
	}
	
	private byte[] toBytes(int szx, boolean m, int num) {
//		System.out.println(Hex.encodeHex(new BlockOption(szx,m,num).getValue()));;
		return new BlockOption(szx, m, num).getValue();
	}
	
	private byte[] b(int... a) {
		byte[] ret = new byte[a.length];
		for (int i=0;i<a.length;i++)
			ret[i] = (byte) a[i];
		return ret;
	}
	
}