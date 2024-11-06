
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

class Operations {
	Cache cache1; // L1
	Cache cache2; // L2
	HashMap<String, ArrayList<Integer>> optimalStore;

	int replace = 0;
	int inclusion = 0;

	int l1Reads = 0;
	int l1ReadMiss = 0;
	int l1Writes = 0;
	int l1WriteMiss = 0;
	int l1Writebacks = 0;

	int l2Reads = 0;
	int l2ReadMiss = 0;
	int l2Writes = 0;
	int l2WriteMiss = 0;
	int l2Writebacks = 0;

	int totalTraffic = 0;

	// Create arrays for caches
	public Operations(int b, int s1, int s2, int a1, int a2, int rep, int incl) {
		cache1 = new Cache(b, s1, a1);
		cache2 = new Cache(b, s2, a2);
		replace = rep;
		inclusion = incl;
	}


	// Search for what cache the address is saved to
	public void read(String adr) {
		int[] found = cache1.find(adr);
		// Found in L1
		if (found[0] != -1) {
			// If LRU, touch block
			if (replace == 0 && cache1.blocks[found[0]][found[1]].count != 0) {
				// Don't increase any block with a check above that of the current block
				// Stops check from going above current amount of total amount of blocks in the cache
				int threshold = cache1.blocks[found[0]][found[1]].count;
				for (int i = 0; i < cache1.blocks.length; i++) {
					for (int j = 0; j < cache1.blocks[i].length; j++) {
						if (cache1.blocks[i][j] != null && cache1.blocks[i][j].count < threshold) {
							cache1.blocks[i][j].inc();
						}
					}
				}
				cache1.blocks[found[0]][found[1]].setCount(0);
			}
			else if (replace == 2) {
				// Change count when touched
				if (optimalStore.get(cache1.getTag(adr)).size() == 0) {
					cache1.blocks[found[0]][found[1]].setCount(Integer.MAX_VALUE);
				} else {
					cache1.blocks[found[0]][found[1]].setCount(optimalStore.get(cache1.getTag(adr)).get(0));
					optimalStore.get(cache1.getTag(adr)).remove(0);
				}
			}
			System.out.print("Found in L1 ");
			l1Reads++;
		} else {
			l1ReadMiss++;
		}

		found = cache2.find(adr);
		// Found in L2
		if (found[0] != -1) {
			// If LRU, touch block
			if (replace == 0 && cache2.blocks[found[0]][found[1]].count != 0) {
				// Don't increase any block with a check above that of the current block
				// Stops check from going above current amount of total amount of blocks in the cache
				int threshold = cache2.blocks[found[0]][found[1]].count;
				for (int i = 0; i < cache2.blocks.length; i++) {
					for (int j = 0; j < cache2.blocks[i].length; j++) {
						if (cache2.blocks[i][j] != null && cache2.blocks[i][j].count < threshold) {
							cache2.blocks[i][j].inc();
						}
					}
				}
				cache2.blocks[found[0]][found[1]].setCount(0);
			}
			// If Optimal, change count when touched
			else if (replace == 2) {
				if (optimalStore.get(cache2.getTag(adr)).size() == 0) {
					cache2.blocks[found[0]][found[1]].setCount(Integer.MAX_VALUE);
				} else {
					cache2.blocks[found[0]][found[1]].setCount(optimalStore.get(cache2.getTag(adr)).get(0));
					optimalStore.get(cache2.getTag(adr)).remove(0);
				}
			}
			System.out.print("Found in L2 ");
			l2Reads++;
		} else {
			l2ReadMiss++;
		}
		System.out.println();
	}


	// Find the largest count to replace. Works for all replace policies
	public String findNext(Cache cache) {
		int largest = 0;
		String result = "";

		for (int i = 0; i < cache.blocks.length; i++) {
			for (int j = 0; j < cache.blocks[i].length; j++) {
				if (cache.blocks[i][j].count > largest) {
					result = cache.blocks[i][j].address;
					largest = cache.blocks[i][j].count;
				}
			}
		}

		return result;
	}

	// Write address to a cache. If empty, just add. If full, replace one block (See 3.3)
	public void write(String adr) {
		boolean isFound = false;

		int[] found1 = cache1.find(adr);
		System.out.print("Cache1 full? " + cache1.setFull(adr));
		// Found in L1
		if (!cache1.setFull(adr) || found1[0] != -1) {
			// If LRU, touch block
			int threshold = (found1[0] == -1) ? 0 : cache1.blocks[found1[0]][found1[1]].count;
			if (replace != 2) {
				// Touch each block, then add new address
				for (int i = 0; i < cache1.blocks.length; i++) {
					for (int j = 0; j < cache1.blocks[i].length; j++) {
						if (cache1.blocks[i][j] != null && cache1.blocks[i][j].count < threshold) {
							cache1.blocks[i][j].inc();
						}
					}
				}

				if (found1[0] != -1) {
					// If block was found before, make it dirty
					cache1.blocks[found1[0]][found1[1]].setCount(0);
					cache1.blocks[found1[0]][found1[1]].toggleDirty();
				} else {
					cache1.insert(adr, 0);
				}
			} else {
				// Change count when touched
				if (found1[0] != -1) {
					if (optimalStore.get(cache1.getTag(adr)).size() == 0) {
						cache1.blocks[found1[0]][found1[1]].setCount(Integer.MAX_VALUE);
					} else {
						cache1.blocks[found1[0]][found1[1]].setCount(optimalStore.get(cache1.getTag(adr)).get(0));
						optimalStore.get(cache1.getTag(adr)).remove(0);
					}
					cache1.blocks[found1[0]][found1[1]].toggleDirty();
				} else {
					if (optimalStore.get(cache1.getTag(adr)).size() == 0) {
						cache1.insert(adr, Integer.MAX_VALUE);
					} else {
						cache1.insert(adr, optimalStore.get(cache1.getTag(adr)).get(0));
						optimalStore.get(cache1.getTag(adr)).remove(0);
					}
				}
			}
			System.out.print("Placed in L1 ");
			l1Writes++;
			isFound = true;
		} else {
			l1WriteMiss++;
			l1ReadMiss++;
		}

		int[] found2 = cache2.find(adr);
		// Found in L1
		if (!cache2.setFull(adr) || found2[0] != -1) {
			// If LRU, touch block
			int threshold = (found2[0] == -1) ? 0 : cache2.blocks[found2[0]][found2[1]].count;
			if (replace != 2) {
				// Touch each block, then add new address
				for (int i = 0; i < cache2.blocks.length; i++) {
					for (int j = 0; j < cache2.blocks[i].length; j++) {
						if (cache2.blocks[i][j] != null && cache2.blocks[i][j].count < threshold) {
							cache2.blocks[i][j].inc();
						}
					}
				}

				if (found2[0] != -1) {
					// If block was found before, make it dirty
					cache2.blocks[found2[0]][found2[1]].setCount(0);
					cache2.blocks[found2[0]][found2[1]].toggleDirty();
				} else {
					cache2.insert(adr, 0);
				}
			} else {
				// Change count when touched
				if (found2[0] != -1) {
					if (optimalStore.get(cache2.getTag(adr)).size() == 0) {
						cache2.blocks[found2[0]][found2[1]].setCount(Integer.MAX_VALUE);
					} else {
						cache2.blocks[found2[0]][found2[1]].setCount(optimalStore.get(cache2.getTag(adr)).get(0));
						optimalStore.get(cache2.getTag(adr)).remove(0);
					}
					cache2.blocks[found2[0]][found2[1]].toggleDirty();
				} else {
					if (optimalStore.get(cache2.getTag(adr)).size() == 0) {
						cache2.insert(adr, Integer.MAX_VALUE);
					} else {
						cache2.insert(adr, optimalStore.get(cache2.getTag(adr)).get(0));
						optimalStore.get(cache2.getTag(adr)).remove(0);
					}
				}
			}
			System.out.print("Placed in L2 ");
			l2Writes++;
			isFound = true;
		} else {
			l2WriteMiss++;
			l2ReadMiss++;
		}

		// CHECK IF THIS IS CORRECT --------
		if (!isFound) {
			String tempReplace = "";
			// Non-inclusive
			// Note: The evicted block may not be the same in both caches and thats ok
			if (inclusion == 0) {
				if (found1[0] != -1) {
					tempReplace = findNext(cache1);
					cache1.replace(adr, tempReplace);
				}
				if (found2[0] != -1) {
					tempReplace = findNext(cache2);
					cache2.replace(adr, tempReplace);
				}
			} 
			// Inclusive
			// The evicted block must be the same in L1 and L2
			else {
				// Check L2 first. If not there, replace from L2 and L1
				if (cache2.blocks.length > 0 && found2[0] != -1) {
					tempReplace = findNext(cache2);
					cache1.replace(adr, tempReplace);
					cache2.replace(adr, tempReplace);
				}
				else if (found1[0] != -1) {
					tempReplace = findNext(cache1);
					cache1.replace(adr, tempReplace);
				}
			}
			System.out.println("Replace " + tempReplace + " with " + adr);
		}
		System.out.println();
	}


	// Do preprocessing for the optimal path
	public void optimalPreprocess(String adr, int count) {
		// If length of address is less than 8 bits, add the missing 0s to the beginning
		if (adr.length() < 8) {
			for (int i = 0; i <= (8-adr.length()); i++) {
				adr = "0" + adr;
			}
		}

		// Store distance between when it was last seen
		if (optimalStore.get(adr) != null) {
			// Found address in optimal
			optimalStore.get(adr).add(count);
		} else {
			optimalStore.put(adr, new ArrayList<Integer>(List.of(count)));
		}
	}

	// Change numbers to distances
	public void optimalEdit() {
		for (String a : optimalStore.keySet()) {
			for (int i = optimalStore.get(a).size()-1; i > 0; i--) {
				optimalStore.get(a).set(i, optimalStore.get(a).get(i) - optimalStore.get(a).get(i-1));
			}
			optimalStore.get(a).remove(0);
		}
	}


	// Output both caches fully for debugging purposes
	public void outputCaches() {
		System.out.println("===== L1 contents =====");
		for (int i = 0; i < cache1.blocks.length; i++) {
			System.out.print("Set     " + i + ": ");
			for (int j = 0; j < cache1.blocks[i].length; j++) {
				if (cache1.blocks[i][j] != null) {
					System.out.print(cache1.blocks[i][j].blockString() + " ");
				}
			}
			System.out.println();
		}

		System.out.println("===== L2 contents =====");
		for (int i = 0; i < cache2.blocks.length; i++) {
			System.out.print("Set     " + i + ": ");
			for (int j = 0; j < cache2.blocks[i].length; j++) {
				if (cache2.blocks[i][j] != null) {
					System.out.print(cache2.blocks[i][j].blockString() + " ");
				}
			}
			System.out.println();
		}
	}

	public void results() {
		float l1MissRate = (l1ReadMiss + l1WriteMiss) / (l1Reads + l1Writes);
		float l2MissRate = (l2Reads == 0) ? 0 : l2ReadMiss / l2Reads;

		// System.out.println("%d %d %d %d %f %d %d %d %d %d %f %d %d\n",
		// 	l1Reads, l1ReadMiss, l1Writes, l1WriteMiss, l1MissRate, l1Writebacks,
		// 	l2Reads, l2ReadMiss, l2Writes, l2WriteMiss, l2MissRate, l2Writebacks,
		// 	totalTraffic);
		System.out.println("===== Simulation results (raw) =====");
		System.out.println("a. number of L1 reads:        " + l1Reads);
		System.out.println("b. number of L1 read misses: 	" + l1ReadMiss);
		System.out.println("c. number of L1 writes: 		  " + l1Writes);
		System.out.println("d. number of L1 write misses: " + l1WriteMiss);
		System.out.println("e. L1 miss rate: 							" + l1MissRate);
		System.out.println("f. number of L1 writebacks: 	" + l1Writebacks);

		System.out.println("g. number of L2 reads: 				" + l2Reads);
		System.out.println("h. number of L2 read misses: 	" + l2ReadMiss);
		System.out.println("i. number of L2 writes: 			" + l2Writes);
		System.out.println("j. number of L2 write misses: " + l2WriteMiss);
		System.out.println("k. L2 miss rate: 							" + l2MissRate);
		System.out.println("l. number of L2 writebacks: 	" + l2Writebacks);

		System.out.println("m. total memory traffic: 			" + totalTraffic);
	}
}