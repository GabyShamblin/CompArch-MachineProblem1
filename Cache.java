public class Cache {
	Block[][] blocks;
	int blocksize = 0;
	int numSets = 0;
	int assoc = 0;

	int tagBits = 0;
	int indexBits = 0;
	int offsetBits = 0;
	boolean isFull = false;

	public Cache(int b, int s, int a) {
		blocks = new Block[s][a];
		blocksize = b;
		numSets = s;
		assoc = a;
		indexBits = (int)(Math.log(numSets) / Math.log(2));
		offsetBits = (int)(Math.log(blocksize) / Math.log(2));
		tagBits = 32 - (indexBits + offsetBits);
	}

	// Static = a function that can be accessed without the object
	// Non-static = Only accessed by objects

	// Used to lookup the address
	String getTag(String adr) {
		return Long.toHexString((Long.parseLong(adr, 16) >> (offsetBits + indexBits)));
	}
	// Points to a cache set
	int getIndex(String adr) {
		return (int)((Long.parseLong(adr, 16) >> offsetBits) & (numSets - 1));
	}

	boolean setFull(String adr) {
		int n = getIndex(adr);
		if (n > blocks.length) { return true; }
		isFull = true;
		for (int i = 0; i < blocks[n].length; i++) {
			if (blocks[n][i] == null) { 
				isFull = false;
			}
		}
		return isFull;
	}

	public int[] find(String adr) {
		int n = getIndex(adr);
		String t = getTag(adr);
		if (n <= blocks.length) {
			System.out.print("(tag " + t + ", index " + n + ") ");
		}
		if (n > blocks.length || blocks[n] == null) { return new int[]{-1, -1}; }
		for (int i = 0; i < blocks[n].length; i++) {
			if (blocks[n][i] == null) { continue; }
			else if (blocks[n][i].tag.equals(t)) { return new int[]{n, i}; }
		}
		return new int[]{-1, -1};
	}

	public void insert(String adr, int c) {
		int n = getIndex(adr);
		for (int i = 0; i < blocks[n].length; i++) {
			if (blocks[n][i] == null) { blocks[n][i] = new Block(adr, getTag(adr), c); break; }
		}
	}

	public void replace(String newAdr, String oldAdr) {
		int[] found = find(oldAdr);
		if (found[0] == -1) { return; }
		blocks[found[0]][found[1]] = new Block(newAdr, getTag(newAdr), 0);
	}
}