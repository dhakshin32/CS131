
//package proj3;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.*;
import java.io.*;
import java.nio.file.*;
import java.lang.Runtime;

public class Pigzj {
	public final static int BLOCK_SIZE = 131072;
	public final static int DICT_SIZE = 32768;
	private final static int GZIP_MAGIC = 0x8b1f;
	private final static int TRAILER_SIZE = 8;

	private static CRC32 crc = new CRC32();
	public static PrintStream outStream = new PrintStream(System.out, true);

	public static int processArgs(String[] args) {
		int p = Runtime.getRuntime().availableProcessors();
		if (args.length > 2) {
			System.err.println("Error: Too many arguments passed");
			Runtime.getRuntime().exit(1);
		} else if (args.length == 2) {
			if (args[0].equals("-p")) {
				int temp = Integer.parseInt(args[1]);
				if (temp < 1) {
					System.err.println("Error: Not a valid process number");
					Runtime.getRuntime().exit(1);
				}
				if (temp <= p) {
					p = temp;
				} else {
					System.err.println("Error: Not enough processors available");
					Runtime.getRuntime().exit(1);
				}
			} else {
				System.err.println("Error: Only supported arg is -p!");
				Runtime.getRuntime().exit(1);
			}
		} else if (args.length == 1) {
			System.err.println("Error: Missing an argument");

		}
		return p;
	}

	// DONT NEED TO CHANGE THIS
	public static void writeHeader() throws IOException {
		// System.err.println("printing header");
		outStream.write(new byte[] { (byte) GZIP_MAGIC, // Magic number (short)
				(byte) (GZIP_MAGIC >> 8), // Magic number (short)
				Deflater.DEFLATED, // Compression method (CM)
				0, // Flags (FLG)
				0, // Modification time MTIME (int)
				0, // Modification time MTIME (int)
				0, // Modification time MTIME (int)
				0, // Modification time MTIME (int)Sfil
				0, // Extra flags (XFLG)
				0 // Operating system (OS)
		});
		if (System.out.checkError()) {
			System.err.println("Error writing to output");
			Runtime.getRuntime().exit(1);
		}
	}

	// DONT NEED TO CHANGE THIS
	public static void writeTrailer(long totalBytes, byte[] buf, int offset) throws IOException {
		writeInt((int) crc.getValue(), buf, offset); // CRC-32 of uncompr. data
		writeInt((int) totalBytes, buf, offset + 4); // Number of uncompr. bytes
		//System.err.println("printing trailer");
		outStream.write(buf);
		if (System.out.checkError()) {
			System.err.println("Error writing to output");
			Runtime.getRuntime().exit(1);
		}
	}

	private static void writeInt(int i, byte[] buf, int offset) throws IOException {
		writeShort(i & 0xffff, buf, offset);
		writeShort((i >> 16) & 0xffff, buf, offset + 2);
	}


	private static void writeShort(int s, byte[] buf, int offset) throws IOException {
		buf[offset] = (byte) (s & 0xff);
		buf[offset + 1] = (byte) ((s >> 8) & 0xff);
	}


	private static void complete(int nBytes) {
		Deflater compressor = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
		//compressor.reset();
		byte[] cmpBlockBuf = new byte[BLOCK_SIZE * 2];
		if (nBytes == -1 && !compressor.finished()) {
			//System.err.println("printing compressor finish");
			compressor.finish();
			while (!compressor.finished()) {
				int leftoverBytes = compressor.deflate(cmpBlockBuf, 0, cmpBlockBuf.length, Deflater.NO_FLUSH);
				if (leftoverBytes > 0) {

					outStream.write(cmpBlockBuf, 0, leftoverBytes);
					if (System.out.checkError()) {
						System.err.println("Error writing to output");
						Runtime.getRuntime().exit(1);
					}

				}
			}
		}
	}

	public static void main(String[] args) throws FileNotFoundException, IOException, InterruptedException {
		int p = processArgs(args);
		ExecutorService executor = Executors.newFixedThreadPool(p);
		writeHeader();
		crc.reset();

		ConcurrentHashMap<Integer, byte[]> dictMap = new ConcurrentHashMap<Integer, byte[]>();
		ConcurrentHashMap<Integer, byte[]> sysOut = new ConcurrentHashMap<Integer, byte[]>();
		ConcurrentHashMap<Integer, Integer> size = new ConcurrentHashMap<Integer, Integer>();
		InputStream inStream = System.in;
		long totalBytesRead = 0;

		// start writer thread
		Writer writer = new Writer(sysOut, size);
		Thread t1 = new Thread(writer);

		byte[] blockBuf = new byte[BLOCK_SIZE];
		int nBytes = inStream.read(blockBuf);
		if (nBytes > 0) {
			t1.start();
		}
		totalBytesRead += nBytes;

		int id = 0;
		while (nBytes > 0) {
			//System.err.println("crcUpdate: " + id);
			crc.update(blockBuf, 0, nBytes);

			executor.execute(new SingleThreadedGZipCompressor(id, blockBuf, nBytes, dictMap, sysOut, size));
			id++;
			//Need to create a new blockBuf every time
			blockBuf = new byte[BLOCK_SIZE];
			nBytes = inStream.read(blockBuf);
			if (nBytes > 0) {
				totalBytesRead += nBytes;
			}
		}

		executor.shutdown();

		synchronized (Pigzj.class) {			
			writer.setcompleted(id);
		}

		t1.join();

		complete(-1);

		/* Finally, write the trailer and then write to STDOUT */
		if (totalBytesRead < 0) {
			totalBytesRead = 0;
		}

		byte[] trailerBuf = new byte[TRAILER_SIZE];
		writeTrailer(totalBytesRead, trailerBuf, 0);
		Runtime.getRuntime().exit(0);

	}
}