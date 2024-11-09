public class Cache {
	Block[][] blocks;
	int blocksize = 0;
	int numSets = 0;
	int assoc = 0;
	String name = "";
	Cache lower = null;
	String victim = "";

	int tagBits = 0;
	int indexBits = 0;
	int offsetBits = 0;
	int replace = 0;

	int reads = 0;
	int readMiss = 0;
	int writes = 0;
	int writeMiss = 0;
	int writebacks = 0;
	int mainTraffic = 0;

	boolean debug = true;

	public Cache(int b, int s, int a, int r, String n, Cache c) {
		blocks = new Block[s][a];
		blocksize = b;
		numSets = s;
		assoc = a;
		name = n;
		lower = c;
		replace = r;
		indexBits = (int)(Math.log(numSets) / Math.log(2));
		offsetBits = (int)(Math.log(blocksize) / Math.log(2));
		tagBits = 32 - (indexBits + offsetBits);
	}

	public Cache(int b, int s, int a, int r, String n) {
		blocks = new Block[s][a];
		blocksize = b;
		numSets = s;
		assoc = a;
		name = n;
		replace = r;
		indexBits = (int)(Math.log(numSets) / Math.log(2));
		offsetBits = (int)(Math.log(blocksize) / Math.log(2));
		tagBits = 32 - (indexBits + offsetBits);
	}

	// Simplifies the address for storage
	String getAddress(String adr) {
		return Long.toHexString((Long.parseLong(adr, 16) / 16) * 16);
	}
	String printAddress(String adr) {
		return getAddress(adr) + " (tag " + getTag(adr) + ", index " + getIndex(adr) + ")";
	}
	// Used to lookup the address
	String getTag(String adr) {
		return Long.toHexString((Long.parseLong(adr, 16) >> (offsetBits + indexBits)));
	}
	// Points to a cache set
	int getIndex(String adr) {
		return (int)((Long.parseLong(adr, 16) >> offsetBits) & (numSets - 1));
	}

	// Is the set full
	boolean setFull(String adr) {
		int n = getIndex(adr);
		if (n > blocks.length) { return true; }
		boolean isFull = true;
		for (int i = 0; i < blocks[n].length; i++) {
			if (blocks[n][i] == null) { 
				isFull = false; break;
			}
		}
		return isFull;
	}

	// Find where a block is located
	public int find(String adr) {
		int n = getIndex(adr);
		String t = getTag(adr);
		if (n > blocks.length || blocks[n] == null) { return -1; }
		for (int i = 0; i < blocks[n].length; i++) {
			if (blocks[n][i] == null) { continue; }
			else if (blocks[n][i].tag.equals(t)) { return i; }
		}
		return -1;
	}

	// Insert a block or update a found block
	public void allocate(String adr, boolean dirty) {
		int found = find(adr);
		int n = getIndex(adr);
		if (n > blocks.length) { return; }
		if (debug && dirty) {
			System.out.println(name + " set dirty");
		}
		if (found != -1) {
			updateCount(blocks[n][found].count);
			blocks[n][found].count = 0;
			if (!blocks[n][found].dirty && dirty) {
				blocks[n][found].dirty = dirty;
			}
			return;
		}
		updateCount(Integer.MAX_VALUE);
		// Look for empty block
		for (int i = 0; i < blocks[n].length; i++) {
			if (blocks[n][i] == null) { 
				blocks[n][i] = new Block(adr, getTag(adr), 0, dirty); 
				return; 
			}
		}
		int largest = 0;
		int index = 0;
		// Look for a block to override
		for (int i = 0; i < blocks[n].length; i++) {
			if (!blocks[n][i].valid) { 
				if (debug) { System.out.println(name + " victim : " + printAddress(blocks[n][i].address) + ", clean"); }
				blocks[n][i] = new Block(adr, getTag(adr), 0, dirty); 
				return; 
			}
			// if (blocks[n][i].dirty && v == -1) { 
			// 	v = i;
			// }
			if (blocks[n][i].count > largest) {
				largest = blocks[n][i].count;
				index = i;
			}
		}

		if (blocks[n][index].dirty) {
			victim = blocks[n][index].address;
			if (debug) { System.out.println(name + " victim : " + printAddress(blocks[n][index].address) + ", dirty"); }
			writebacks++;
		} 
		else if (debug) { System.out.println(name + " victim : " + printAddress(blocks[n][index].address) + ", clean"); }

		blocks[n][index] = new Block(adr, getTag(adr), 0, dirty);
	}

	// Insert a block with a preset count (for optimal)
	public void allocate(String adr, int newCount, boolean dirty) {
		if (debug) { System.out.println(name + " update optimal"); }
		int found = find(adr);
		int n = getIndex(adr);
		if (n > blocks.length) { return; }
		if (debug && dirty) {
			System.out.println(name + " set dirty");
		}
		if (found != -1) {
			blocks[n][found].count = newCount;
			blocks[n][found].dirty = dirty;
			return;
		}
		// Look for empty block
		for (int i = 0; i < blocks[n].length; i++) {
			if (blocks[n][i] == null) { 
				blocks[n][i] = new Block(adr, getTag(adr), newCount, dirty); 
				return; 
			}
		}
		int largest = 0;
		int index = 0;
		// Look for a block to override
		for (int i = 0; i < blocks[n].length; i++) {
			if (!blocks[n][i].valid) { 
				if (debug) { System.out.println(name + " victim : " + printAddress(blocks[n][i].address) + ", clean"); }
				blocks[n][i] = new Block(adr, getTag(adr), newCount, dirty); 
				return; 
			}
			if (blocks[n][i].count > largest) {
				largest = blocks[n][i].count;
				index = i;
			}
		}

		if (blocks[n][index].dirty) {
			victim = blocks[n][index].address;
			if (debug) { System.out.println(name + " victim : " + printAddress(blocks[n][index].address) + ", dirty"); }
			writebacks++;
		} 
		else if (debug) { System.out.println(name + " victim : " + printAddress(blocks[n][index].address) + ", clean"); }

		blocks[n][index] = new Block(adr, getTag(adr), newCount, dirty);
	}

	// If cache is inclusive and block is valid, then we can invalidate it in the upper level
	public void invalidate(String adr) {
		int i = find(adr);
		if (i != -1) { 
			int n = getIndex(adr);
			if (debug) { System.out.print(name + " invalidated: " + printAddress(blocks[n][i].address)); }
			if (blocks[n][i].dirty) {
				mainTraffic++;
				blocks[n][i].dirty = false;
				if (debug) {
					System.out.println(", dirty");
					System.out.println(name + " writeback to main memory directly");
				}
			} 
			else if (debug) { System.out.println(", clean"); }
			
			blocks[n][i].valid = false;
		}
	}

	// Replace a block
	public void replace(String adr, String oldTag, boolean dirty) {
		int n = getIndex(adr);
		if (n > blocks.length) { return; }
		for (int i = 0; i < blocks[n].length; i++) {
			if (blocks[n][i] != null && blocks[n][i].tag == oldTag) { 
				blocks[n][i] = new Block(adr, getTag(adr), 0, dirty); 
				return; 
			}
		}
	}

	// Increase the count of every block by 1
	public void updateCount(String adr) {
		if (debug) {
			if (replace == 0) { System.out.println(name + " update LRU"); }
			else if (replace == 1) { System.out.println(name + " update FIFO"); }
		}
		int found = find(adr);
		int n = getIndex(adr);
		int threshold = found != -1 ? blocks[n][found].count : Integer.MAX_VALUE;
		for (int i = 0; i < blocks.length; i++) {
			for (int j = 0; j < blocks[i].length; j++) {
				if (blocks[i][j] != null && blocks[i][j].count < threshold) {
					blocks[i][j].inc();
				}
			}
		}
	}

	// Increase the count of every block by 1
	public void updateCount(int threshold) {
		if (debug) {
			if (replace == 0) { System.out.println(name + " update LRU"); }
			else if (replace == 1) { System.out.println(name + " update FIFO"); }
		}
		// Don't increase any block with a check above that of the current block
		// Stops check from going above current amount of total amount of blocks in the cache
		for (int i = 0; i < blocks.length; i++) {
			for (int j = 0; j < blocks[i].length; j++) {
				if (blocks[i][j] != null && blocks[i][j].count < threshold) {
					blocks[i][j].inc();
				}
			}
		}
	}

	// LRU & FIFO read
	public boolean read(String adr) {
		reads++;
		if (debug) { System.out.println(name + " read : " + printAddress(adr)); }

		int found = find(adr);
		int n = getIndex(adr);
		if (found != -1 && n < numSets) {
			if (debug) { System.out.println(name + " read hit"); }
			if (replace == 0) {
				updateCount(blocks[n][found].count);
				blocks[n][found].setCount(0);
			}
			return true;
		} else if (numSets > 0) { 
			if (debug) { System.out.println(name + " read miss"); }
			// allocate(adr, false);
			readMiss++;
		}
		return false; 
	}

	// Optimal read
	public boolean read(String adr, int opt) {
		reads++;
		if (debug) { System.out.println(name + " read : " + printAddress(adr)); }

		int found = find(adr);
		int n = getIndex(adr);
		if (found != -1) {
			if (debug) { System.out.println(name + " read hit"); }
			blocks[n][found].setCount(opt);
			blocks[n][found].dirty = false;
			return true;
		} else if (numSets > 0) {
			if (debug) { System.out.println(name + " read miss"); }
			allocate(adr, false);
			readMiss++;
		}
		return false; 
	}

	// LRU && FIFO read
	public boolean write(String adr) {
		writes++;
		if (debug) { System.out.println(name + " write : " + printAddress(adr)); }

		int found = find(adr);
		int n = getIndex(adr);
		if (n < numSets) {
			if (debug) {
				if (found != -1) { System.out.println(name + " write hit"); } 
				else { System.out.println(name + " write miss"); }
			}
			allocate(adr, true);;
			if (found != -1) { return true; } 
			else { writeMiss++; return false; }
		}
		return false;
	}

	// Optimal write
	public boolean write(String adr, int opt) {
		writes++;
		if (debug) { System.out.println(name + " write : " + printAddress(adr)); }

		int found = find(adr);
		if (getIndex(adr) < numSets) {
			allocate(adr, opt, true);;
			if (found != -1) { 
				if (debug) { System.out.println(name + " write hit"); }
				return true; 
			} else { 
				if (debug) { System.out.println(name + " write miss"); }
				writeMiss++; 
				return false; 
			}
		}
		return false;
	}
}