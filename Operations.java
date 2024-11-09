
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
		if (!optimalStore.containsKey(adr)) {
			result = Integer.MAX_VALUE;
		} else {
			if (optimalStore.get(adr).size() == 0) {
				result = Integer.MAX_VALUE;
			} else {
				result = optimalStore.get(adr).get(0);
			}
		}
		return result;
	}

	public int optimalRead(String adr) {
		int result = -1;
		if (!optimalStore.containsKey(adr)) {
			result = Integer.MAX_VALUE;
		} else {
			if (optimalStore.get(adr).size() == 0) {
				result = Integer.MAX_VALUE;
			} else {
				result = optimalStore.get(adr).get(0);
			}
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

		if (replace == 2) {
			cache1.allocate(adr, optimalPop(adr), false);
		}
		if (hit) {
			return;
		} else {
			if (replace != 2) {
				cache1.allocate(adr, false);
			}
		}

		// Write victim block to lower level
		if (cache1.victim != "") {
			if (cache1.lower != null) {
				cache1.lower.write(cache1.victim);
			}
			cache1.victim = "";
		}

		if (cache1.lower != null) { 
			hit = cache1.lower.read(adr);
			if (!hit) {
				cache1.lower.allocate(adr, false);
			}
			if (inclusion == 1 && cache1.lower.victim != "") {
				cache1.invalidate(cache1.lower.victim);
				cache1.lower.victim = "";
			}
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

		// Write victim block to lower level
		if (cache1.victim != "") {
			if (cache1.lower != null) {
				cache1.lower.write(cache1.victim);
			}
			cache1.victim = "";
		}

		if (cache1.lower != null) { 
			hit = cache1.lower.read(adr);
			if (!hit) {
				cache1.lower.allocate(adr, false);
			}
			if (inclusion == 1 && cache1.lower.victim != "") {
				cache1.invalidate(cache1.lower.victim);
				cache1.lower.victim = "";
			}
		} 
	}


	// Do preprocessing for the optimal path
	public void optimalPreprocess(String adr, int count) {
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
			System.out.printf("Set     %-5s   ", (String.valueOf(i) + ":"));
			for (int j = 0; j < cache1.blocks[i].length; j++) {
				if (cache1.blocks[i][j] != null) {
					String[] st = cache1.blocks[i][j].blockString();
					// if (j == cache1.blocks[i].length-1) {
					// 	System.out.printf("%6s %s %5s", st[0], st[1], st[2]);
					// } else {
					// 	System.out.printf("%6s %s %5s, ", st[0], st[1], st[2]);
					// }
					System.out.printf("%6s %s  ", st[0], st[1]);
				}
			}
			System.out.println();
		}

		if (cache2.blocks.length == 0) { return; }
		System.out.println("===== L2 contents =====");
		for (int i = 0; i < cache2.blocks.length; i++) {
			System.out.printf("Set     %-5s  ", (String.valueOf(i) + ":"));
			for (int j = 0; j < cache2.blocks[i].length; j++) {
				if (cache2.blocks[i][j] != null) {
					String[] st = cache2.blocks[i][j].blockString();
					// if (j == cache2.blocks[i].length-1) {
					// 	System.out.printf("%6s %s %5s", st[0], st[1], st[2]);
					// } else {
					// 	System.out.printf("%6s %s %5s,  ", st[0], st[1], st[2]);
					// }
					System.out.printf("%6s %s  ", st[0], st[1]);
				}
			}
			System.out.println();
		}
	}

	public void results() {
		float l1MissRate = (float)(cache1.readMiss + cache1.writeMiss) / (float)(cache1.reads + cache1.writes);
		float l2MissRate = (cache2.reads == 0) ? 0 : ((float)cache2.readMiss / (float)cache2.reads);
		int totalTraffic = cache2.numSets == 0 ? 
			cache1.readMiss + cache1.writeMiss + cache1.writebacks :
			cache2.readMiss + cache2.writeMiss + cache2.writebacks + cache1.mainTraffic;

		// System.out.println("%d %d %d %d %f %d %d %d %d %d %f %d %d\n",
		// 	cache1.reads, cache1.readMiss, cache1.writes, cache1.writeMiss, l1MissRate, l1Writebacks,
		// 	cache2.reads, cache2.readMiss, l2Writes, l2WriteMiss, l2MissRate, l2Writebacks,
		// 	totalTraffic);
		System.out.println("===== Simulation results (raw) =====");
		System.out.println("a. number of L1 reads:        " + cache1.reads);
		System.out.println("b. number of L1 read misses:  " + cache1.readMiss);
		System.out.println("c. number of L1 writes:       " + cache1.writes);
		System.out.println("d. number of L1 write misses: " + cache1.writeMiss);
		System.out.printf ("e. L1 miss rate:              %.6f\n", l1MissRate);
		System.out.println("f. number of L1 writebacks:   " + cache1.writebacks);

		System.out.println("g. number of L2 reads:        " + cache2.reads);
		System.out.println("h. number of L2 read misses:  " + cache2.readMiss);
		System.out.println("i. number of L2 writes:       " + cache2.writes);
		System.out.println("j. number of L2 write misses: " + cache2.writeMiss);
		if (l2MissRate == 0) {
			System.out.println("k. L2 miss rate:              0");
		} else {
			System.out.printf ("k. L2 miss rate:              %.6f\n", l2MissRate);
		}
		System.out.println("l. number of L2 writebacks:   " + cache2.writebacks);

		System.out.println("m. total memory traffic:      " + totalTraffic);
	}
}