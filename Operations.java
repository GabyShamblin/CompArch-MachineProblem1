
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

class Operations {
	Cache cache1; // L1
	Cache cache2; // L2
	HashMap<String, ArrayList<Integer>> optimalStore;

	int replace = 0;
	int inclusion = 0;

	// Create arrays for caches
	public Operations(int b, int s1, int s2, int a1, int a2, int rep, int incl) {
		cache2 = new Cache(b, s2, a2, rep, "L2");
		if (s2 > 0) {
			cache1 = new Cache(b, s1, a1, rep, "L1", cache2);
		} else {
			cache1 = new Cache(b, s1, a1, rep, "L1");
		}
		replace = rep;
		inclusion = incl;

		if (rep == 2) {
			optimalStore = new HashMap<String, ArrayList<Integer>>();
		}
	}


	public int optimalPop(String adr) {
		int result = -1;
		if (!optimalStore.containsKey(cache1.getTag(adr))) {
			result = Integer.MAX_VALUE;
		} else {
			result = optimalStore.get(cache1.getTag(adr)).get(0);
			optimalStore.get(cache1.getTag(adr)).remove(0);
		}
		return result;
	}

	public int optimalRead(String adr) {
		int result = -1;
		if (!optimalStore.containsKey(cache1.getTag(adr))) {
			result = Integer.MAX_VALUE;
		} else {
			result = optimalStore.get(cache1.getTag(adr)).get(0);
		}
		return result;
	}

	// Search for what cache the address is saved to
	public void read(String adr) {
		// May need to make hit1 and hit2 and move to bottom
		boolean hit = false;

		// If LRU, touch every block
		if (replace != 2) {
			hit = cache1.read(adr);
		}
		// If FIFO, do nothing
		// If optimal, change count when touched
		else {
			hit = cache1.read(adr, optimalRead(adr));
		}

		if (hit) {
			System.out.println("Found in L1 ");
			return;
		}  else {
			if (replace == 0) {
				cache1.updateCount(adr); 
				cache1.allocate(adr, false);
			} else if (replace == 1) {
				cache1.allocate(adr, false);
			} else {
				cache1.allocate(adr, optimalPop(adr), false);
			}
		}

		// Write victim block to lower level
		if (cache1.victim != "") {
			cache2.write(cache1.victim);
			cache1.victim = "";
		}

		// --- Check L2 ---

		// If LRU, touch every block
		if (replace != 2) {
			hit = cache2.read(adr);
		}
		// If FIFO, do nothing
		// If optimal, change count when touched
		else {
			hit = cache2.read(adr, optimalRead(adr));
		}

		if (hit) {
			System.out.println("Found in L2 ");
			return;
		} else {
			if (replace == 0) {
				cache2.updateCount(adr); 
				cache2.allocate(adr, false);
			} else if (replace == 1) {
				cache2.allocate(adr, false);
			} else {
				cache2.allocate(adr, optimalPop(adr), false);
			}
		}
		// If didn't find before and non-inclusive, allocate to L1
		// else if (inclusion == 0) {
		// 	if (replace == 0) { cache1.updateCount(adr); cache1.allocate(adr, false); }
		// 	else if (replace == 1) { cache1.allocate(adr, false); }
		// 	else { cache1.allocate(adr, optimalPop(adr), false); }
		// }
		// // Otherwise allocate to L2 also
		// else {
		// 	if (replace == 0) { cache2.updateCount(adr); cache2.allocate(adr, false); }
		// 	else if (replace == 1) { cache2.allocate(adr, false); }
		// 	else { cache2.allocate(adr, optimalPop(adr), false); }
		// }

		if (cache2.victim != "") {
			// System.out.println("L2 victim : " + cache2.printAddress(cache2.victim));
			cache2.victim = "";
		}
	}


	// Write address to a cache. If empty, just add. If full, replace one block (See 3.3)
	public void write(String adr) {
		boolean hit = false;

		// If LRU or FIFO, touch every block
		if (replace != 2) {
			hit = cache1.write(adr);
		}
		// If optimal, change count when touched
		else {
			hit = cache1.write(adr, optimalRead(adr));
		}

		if (hit) {
			if (replace == 2) { optimalPop(adr); }
			return;
		}
		System.out.println("Placed in L1 ");

		// Write victim block to lower level
		if (cache1.victim != "") {
			// System.out.println("L1 victim : " + cache1.printAddress(cache1.victim));
			cache2.write(cache1.victim);
			cache1.victim = "";
		}

		if (inclusion == 0) { 
			cache2.updateCount(adr);
			hit = cache2.read(adr);
			if (!hit) {
				cache2.allocate(adr, false);
			}
			return;
		} 

		// --- Check L2 ---

		// If LRU or FIFO, touch every block
		if (replace != 2) {
			hit = cache2.write(adr);
		}
		// If optimal, change count when touched
		else {
			hit = cache2.write(adr, optimalRead(adr));
		}

		if (hit) {
			System.out.println("Placed in L2 ");
			if (inclusion == 1) { cache1.allocate(adr, true); } 
			else if (replace == 2) { optimalPop(adr);	}
		}

		// CHECK IF THIS IS CORRECT --------
		if (!hit) {
			String tempReplace = "";
			// Non-inclusive
			// Note: The evicted block may not be the same in both caches and thats ok
			if (inclusion == 0) {
				cache1.allocate(adr, true);
			} 
			// Inclusive
			// The evicted block must be the same in L1 and L2
			else {
				// Check L2 first. If not there, replace from L2 and L1
				cache2.allocate(adr, true);
				// TODO
				cache1.replace(adr, cache2.blocks[cache2.getIndex(adr)][cache2.find(adr)].tag, true);
			}
			System.out.println("Replace " + tempReplace + " with " + adr);
		}
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
		if (optimalStore.containsKey(adr)) {
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
			System.out.printf("Set     %5s ", (String.valueOf(i) + ":"));
			for (int j = 0; j < cache1.blocks[i].length; j++) {
				if (cache1.blocks[i][j] != null) {
					String[] st = cache1.blocks[i][j].blockString();
					if (j == cache1.blocks[i].length-1) {
						System.out.printf("%6s %s %5s", st[0], st[1], st[2]);
					} else {
						System.out.printf("%6s %s %5s, ", st[0], st[1], st[2]);
					}
				}
			}
			System.out.println();
		}

		if (cache2.blocks.length == 0) { return; }
		System.out.println("===== L2 contents =====");
		for (int i = 0; i < cache2.blocks.length; i++) {
			System.out.print("Set     " + i + ":     ");
			for (int j = 0; j < cache2.blocks[i].length; j++) {
				if (cache2.blocks[i][j] != null) {
					String[] st = cache2.blocks[i][j].blockString();
					if (j == cache2.blocks[i].length-1) {
						System.out.printf("%6s %s %5s", st[0], st[1], st[2]);
					} else {
						System.out.printf("%6s %s %5s,  ", st[0], st[1], st[2]);
					}
				}
			}
			System.out.println();
		}
	}

	public void results() {
		float l1MissRate = (float)(cache1.readMiss + cache1.writeMiss) / (float)(cache1.reads + cache1.writes);
		float l2MissRate = (cache2.reads == 0) ? 0 : ((float)cache2.readMiss / (float)cache2.reads);
		int totalTraffic = cache2.readMiss + cache2.writeMiss + cache2.writebacks + cache1.mainTraffic;

		// System.out.println("%d %d %d %d %f %d %d %d %d %d %f %d %d\n",
		// 	cache1.reads, cache1.readMiss, cache1.writes, cache1.writeMiss, l1MissRate, l1Writebacks,
		// 	cache2.reads, cache2.readMiss, l2Writes, l2WriteMiss, l2MissRate, l2Writebacks,
		// 	totalTraffic);
		System.out.println("===== Simulation results (raw) =====");
		System.out.println("a. number of L1 reads:        " + cache1.reads);
		System.out.println("b. number of L1 read misses:  " + cache1.readMiss);
		System.out.println("c. number of L1 writes:       " + cache1.writes);
		System.out.println("d. number of L1 write misses: " + cache1.writeMiss);
		System.out.printf ("e. L1 miss rate:              %.6f\n" + l1MissRate);
		System.out.println("f. number of L1 writebacks:   " + cache1.writebacks);

		System.out.println("g. number of L2 reads:        " + cache2.reads);
		System.out.println("h. number of L2 read misses:  " + cache2.readMiss);
		System.out.println("i. number of L2 writes:       " + cache2.writes);
		System.out.println("j. number of L2 write misses: " + cache2.writeMiss);
		System.out.printf ("k. L2 miss rate:              %.6f\n" + l2MissRate);
		System.out.println("l. number of L2 writebacks:   " + cache2.writebacks);

		System.out.println("m. total memory traffic:      " + totalTraffic);
	}
}