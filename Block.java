public class Block {
	String address;
	String tag;
	int count;
	boolean valid;
	boolean dirty;

	public Block(String adr, String t, int c) {
		address = adr;
		tag = t;
		count = c;
		valid = true;
		dirty = true;
	}

	public void inc() { count++; }
	public void setCount(int c) { count = c; }
	public void toggleDirty() { dirty = !dirty; }
	String blockString() {
		return address + " " + (dirty ? "D" : " ") + " " + count;
	}
}
