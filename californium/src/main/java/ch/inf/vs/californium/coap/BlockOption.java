package ch.inf.vs.californium.coap;


/**
 * BlockOption represents a Block1 or Block2 option in a CoAP message.
 */
public class BlockOption {

	/** The szx. */
	private int szx;
	
	/** The m. */
	private boolean m;
	
	/** The num. */
	private int num;
	
	/**
	 * Instantiates a new block option.
	 */
	public BlockOption() { }
	
	/**
	 * Instantiates a new block option.
	 *
	 * @param szx the szx
	 * @param m the m
	 * @param num the num
	 */
	public BlockOption(int szx, boolean m, int num) {
		this.setSzx(szx);
		this.setM(m);
		this.setNum(num);
	}
	
	// Copy constructor
	/**
	 * Instantiates a new block option with the same values as the specified
	 * block option.
	 * 
	 * @param origin the origin
	 * @throws NullPointerException if the specified block option is null
	 */
	public BlockOption(BlockOption origin) {
		if (origin == null) throw new NullPointerException();
		this.setSzx(origin.getSzx());
		this.setM(origin.isM());
		this.setNum(origin.getNum());
	}
	
	/**
	 * Instantiates a new block option from the specified bytes (1-3 bytes).
	 *
	 * @param value the bytes
	 * @throws NullPointerException if the specified bytes are null
	 * @throws IllegalArgumentException if the specified value's length is 0 or larger than 3
	 */
	public BlockOption(byte[] value) {
		if (value == null)
			throw new NullPointerException();
		if (value.length == 0 || value.length > 3)
			throw new IllegalArgumentException("Block option's length must be between 1 and 3 bytes inclusive");
		
		this.szx = value[0] & 0x7;
		this.m = (value[0] >> 3 & 0x1) == 1;
		this.num = (value[0] & 0xFF) >> 4 ;
		for (int i=1;i<value.length;i++)
			num += (value[i] & 0xff) << (i*8 - 4);
	}
	
	/**
	 * Gets the szx.
	 *
	 * @return the szx
	 */
	public int getSzx() {
		return szx;
	}

	/**
	 * Sets the szx.
	 *
	 * @param szx the new szx
	 */
	public void setSzx(int szx) {
		if (szx < 0 || 7 < szx)
			throw new IllegalArgumentException("Block option's szx must be between 0 and 7 inclusive");
		this.szx = szx;
	}
	
	/**
	 * Gets the size where {@code size == 1 << (4 + szx)}.
	 *
	 * @return the size
	 */
	public int getSize() {
		return 1 << (4 + szx);
	}

	/**
	 * Checks if is m. The value m is true if there are more block that follow
	 * the message with this block option.
	 * 
	 * @return true, if is m
	 */
	public boolean isM() {
		return m;
	}

	/**
	 * Sets the m. The value m is true if there are more block that follow
	 * the message with this block option.
	 *
	 * @param m the new m
	 */
	public void setM(boolean m) {
		this.m = m;
	}

	/**
	 * Gets the num. This is the number of the block message.
	 *
	 * @return the num
	 */
	public int getNum() {
		return num;
	}

	/**
	 * Sets the number of the block message.
	 *
	 * @param num the new num
	 * @throws IllegalArgumentException if num is not a 20-bit value
	 */
	public void setNum(int num) {
		if (num < 0 || (1<<20)-1 < num)
			throw new IllegalArgumentException("Block option's num must be between 0 and "+(1<<20-1)+" inclusive");
		this.num = num;
	}
	
	/**
	 * Gets the encoded block option as 1-3 byte array.
	 * 
	 * The value of the Block Option is a variable-size (0 to 3 byte).
	 * <hr><blockquote><pre>
	 *  0
	 *  0 1 2 3 4 5 6 7
	 * +-+-+-+-+-+-+-+-+
	 * |  NUM  |M| SZX |
	 * +-+-+-+-+-+-+-+-+
	 *  0                   1
	 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5
	 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 * |          NUM          |M| SZX |
	 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *  0                   1                   2
	 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3
	 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 * |                   NUM                 |M| SZX |
	 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 * </pre></blockquote><hr>
	 * 
	 * @return the value
	 */
	public byte[] getValue() {
		int last = szx | (m ? 1<<3 : 0);
		if (num < 1 << 4) {
			return new byte[] {(byte) (last | (num << 4))};
		} else if (num < 1 << 12) {
			return new byte[] {
					(byte) (last | (num << 4)),
					(byte) (num >> 4)
			};
		} else {
			return new byte[] {
					(byte) (last | (num << 4)),
					(byte) (num >> 4),
					(byte) (num >> 12)
			};
		}
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "(szx="+szx+", m="+m+", num="+num+")";
	}
}