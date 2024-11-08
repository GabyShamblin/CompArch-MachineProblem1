public class Block {
	String address;
	String tag;
	int count;
	boolean valid;
	boolean dirty;

	public Block(String adr, String t, int c, boolean d) {
		address = Long.toHexString((Long.parseLong(adr, 16) / 16) * 16);
		tag = t;
		count = c;
		valid = true;
		dirty = d;
	}

	public void inc() { count++; }
	public void setCount(int c) { count = c; }
	String[] blockString() {
		return new String[]{tag, (dirty ? "D" : " "), String.valueOf(count)};
	}
}
