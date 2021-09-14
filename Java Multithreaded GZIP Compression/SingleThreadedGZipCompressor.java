
//package proj3;

import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.*;
import java.io.*;
import java.nio.file.*;

class SingleThreadedGZipCompressor implements Runnable {

	public final static int BLOCK_SIZE = 131072;
	public final static int DICT_SIZE = 32768;
	private final static int GZIP_MAGIC = 0x8b1f;
	private final static int TRAILER_SIZE = 8;

	byte[] blockBuf = new byte[BLOCK_SIZE];
	int nBytes;

	ConcurrentHashMap<Integer, byte[]> dictMap;
	ConcurrentHashMap<Integer, byte[]> sysOut;
	ConcurrentHashMap<Integer, Integer> size;

	public static PrintStream outStream = new PrintStream(System.out, true);

	int id;

	public SingleThreadedGZipCompressor(int id, byte[] blockBuf, int nBytes, ConcurrentHashMap<Integer, byte[]> dictMap,
			ConcurrentHashMap<Integer, byte[]> sysOut, ConcurrentHashMap<Integer, Integer> size) {
		// this.fileName = fileName;
		this.id = id;
		// this.outStream = new PrintStream(System.out, true);
		this.blockBuf = blockBuf;
		this.nBytes = nBytes;
		this.dictMap = dictMap;
		this.sysOut = sysOut;
		this.size = size;
	}

	public void compress() throws FileNotFoundException, IOException, InterruptedException {
		// System.err.println("compressing: " + id);
		/* Buffers for input blocks, compressed bocks, and dictionaries */
		byte[] cmpBlockBuf = new byte[BLOCK_SIZE * 2];
		byte[] dictBuf = new byte[DICT_SIZE];

		Deflater compressor = new Deflater(Deflater.DEFAULT_COMPRESSION, true);

		long totalBytesRead = 0;
		boolean hasDict = false;

		compressor.reset();
		

		/*
		 * If we read in enough bytes in this block, store the last part as the
		 * dictionary for the next iteration
		 */
		synchronized (dictMap) {
			if (nBytes >= DICT_SIZE) {
				System.arraycopy(blockBuf, nBytes - DICT_SIZE, dictBuf, 0, DICT_SIZE);
				dictMap.put(id, dictBuf);
				dictMap.notifyAll();
			} else {
				byte[] empty = new byte[0];
				dictMap.put(id, empty);
				dictMap.notifyAll();
			}
		}

		/*
		 * If we saved a dictionary from the last block, prime the deflater with
		 * it
		 */
		if (id > 0) {
			synchronized (dictMap) {
				while (!dictMap.containsKey(id - 1)) {
					dictMap.wait();
				}
				if (dictMap.get(id - 1).length > 0) {
					// System.err.println("dictory" + id);
					compressor.setDictionary(dictMap.get(id - 1));
					dictMap.notifyAll();
				}
			}
		}

		compressor.setInput(blockBuf, 0, nBytes);
		/*
		 * Otherwise, just deflate and then write the compressed block out. Not
		 * using SYNC_FLUSH here leads to some issues, but using it probably
		 * results in less efficient compression. There's probably a better way
		 * to deal with this.
		 */

		int deflatedBytes = compressor.deflate(cmpBlockBuf, 0, cmpBlockBuf.length, Deflater.SYNC_FLUSH);

		if (deflatedBytes > 0) {
			synchronized (sysOut) {
				size.put(id, deflatedBytes);
				sysOut.put(id, cmpBlockBuf);
				// System.err.println("Added: " + id);
				sysOut.notifyAll();
			}
		}


	}

	public void run() {
		try {
			compress();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
