import java.io.File;  // Import the File class
import java.io.FileNotFoundException;  // Import this class to handle errors
import java.util.Scanner; // Import the Scanner class to read text files

class sim_cache {
	static Operations ops;

	static void printInput(String[] argv) {
		String replace = "";
		if (Integer.parseInt(argv[5]) == 0) {
			replace = "LRU";
		} else if (Integer.parseInt(argv[5]) == 1) {
			replace = "FIFO";
		} else if (Integer.parseInt(argv[5]) == 2) {
			replace = "optimal";
		} else {
			throw new IllegalArgumentException("Invalid replace (" + argv[5] + ")");
		}
	
		String inclusion = "";
		if (Integer.parseInt(argv[6]) == 0) {
			inclusion = "non-inclusive";
		} else if (Integer.parseInt(argv[6]) == 1) {
			inclusion = "inclusive";
		} else {
			throw new IllegalArgumentException("Invalid inclusion (" + argv[6] + ")");
		}
	
		System.out.println("===== Simulator configuration =====");
		System.out.println("BLOCKSIZE:             " + argv[1]);
		System.out.println("L1_SIZE:               " + argv[2]);
		System.out.println("L1_ASSOC:              " + argv[3]);
		System.out.println("L2_SIZE:               " + argv[4]);
		System.out.println("L2_ASSOC:              " + argv[5]);
		System.out.println("REPLACEMENT POLICY:    " + replace);
		System.out.println("INCLUSION POLICY:      " + inclusion);
		System.out.println("trace_file:            " + argv[7]);
	}
	
	static void readOperations(String line, int count) {
		String adr = line.substring(2, line.length());
		// If length of address is less than 8 bits, add the missing 0s to the beginning
		if (adr.length() < 8) {
			for (int i = 0; i <= (8-adr.length()); i++) {
				adr = '0' + adr;
			}
		}
		System.out.println("----------------------------------------");
		System.out.print((count+1) + " " + line.charAt(0) + " " + adr + " - ");
	
		if (line.charAt(0) == 'r') {
			ops.read(adr);
		}
		else if (line.charAt(0) == 'w') {
			ops.write(adr);
		} else {
			throw new IllegalArgumentException("Invalid instruction");
		}
	}

	public static void main(String[] args) {
		printInput(args);

		// Extract variables from argument
		int blocksize = Integer.parseInt(args[0]);
		int l1Size = Integer.parseInt(args[1]);
		int l1Assoc = Integer.parseInt(args[2]);
		int l2Size = Integer.parseInt(args[3]);
		int l2Assoc = Integer.parseInt(args[4]);
		int replace = Integer.parseInt(args[5]);
		int inclusion = Integer.parseInt(args[6]);
		String traceFile = args[7];

		if (l1Assoc <= 0 || blocksize <= 0) {
			throw new IllegalArgumentException("Invalid arguments");
		}

		int l1SetsNum = (int) l1Size / (l1Assoc * blocksize);

		int l2SetsNum;
		if (l2Assoc <= 0) {
			l2SetsNum = 0;
		} else {
			l2SetsNum = (int) l2Size / (l2Assoc * blocksize);
		}

		ops = new Operations(blocksize, l1SetsNum, l2SetsNum, l1Assoc, l2Assoc, replace, inclusion);

		try {
			File input = new File(traceFile);
			Scanner reader = new Scanner(input);
	
			String line;
			int maxCount = 15;
			int count = 0;
	
			if (replace == 2) {
				// Read inputs line by line (See 5)
				while (reader.hasNextLine()) {
					line = reader.nextLine();
					ops.optimalPreprocess(line.substring(2, line.length()), count);
					count++;
				}
				ops.optimalEdit();
				reader = new Scanner(input);
			}
	
			count = 0;
			// Read inputs line by line (See 5)
			while (reader.hasNextLine() && count < maxCount) {
				line = reader.nextLine();
				readOperations(line, count);
				// ops.outputCaches();
				count++;
			}
	
			ops.outputCaches();
			ops.results();
		} catch (FileNotFoundException e) {
			System.out.println("Error reading file");
			e.printStackTrace();
		}
	}
}
