
//package proj3;

import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.ConcurrentHashMap;

public class Writer implements Runnable {

	ConcurrentHashMap<Integer, byte[]> sysOut;
	ConcurrentHashMap<Integer, Integer> size;
	public static PrintStream outStream = new PrintStream(System.out, true);

	public Writer(ConcurrentHashMap<Integer, byte[]> sysOut, ConcurrentHashMap<Integer, Integer> size) {
		this.sysOut = sysOut;
		this.size = size;
	}

	int completed = 0;

	public void setcompleted(int x) {
		completed = x;
	}

	@Override
	public void run() {
		int counter = 0;
		synchronized (sysOut) {
			while (true) {
				while (!sysOut.containsKey(counter) && !size.containsKey(counter)) {
					//System.err.println("waiting " + counter);
					try {
						sysOut.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

				//System.err.println("printing " + counter);
				outStream.write(sysOut.get(counter), 0, size.get(counter));
				if (System.out.checkError()) {
					System.err.println("Error writing to output");
					Runtime.getRuntime().exit(1);
				}
				counter++;
				if (completed == counter) {
					//System.err.println("completed break");
					break;
				}

			}

		}

	}

}
