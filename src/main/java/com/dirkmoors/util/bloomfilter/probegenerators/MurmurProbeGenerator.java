package com.dirkmoors.util.bloomfilter.probegenerators;

import java.math.BigInteger;

public class MurmurProbeGenerator implements IBloomFilterProbeGenerator {
	public static final String NAME = "MURMUR";
	private static final int seed32 = 89478583;

	@Override
	public BigInteger[] getProbes(int numProbesK, int numBitsM, String key) {
		byte[] value = key.getBytes();	
		
		BigInteger[] positions = new BigInteger[numProbesK];

		int hashes = 0;
		int lastHash = 0;
		byte[] data = (byte[]) value.clone();
		while (hashes < numProbesK) {
			// Code taken from:
			// http://dmy999.com/article/50/murmurhash-2-java-port by Derekt
			// Young (Public Domain)
			// as the Hadoop implementation by Andrzej Bialecki is buggy

			for (int i = 0; i < value.length; i++) {
				if (data[i] == 127) {
					data[i] = 0;
					continue;
				} else {
					data[i]++;
					break;
				}
			}

			// 'm' and 'r' are mixing constants generated offline.
			// They're not really 'magic', they just happen to work well.
			int m = 0x5bd1e995;
			int r = 24;

			// Initialize the hash to a 'random' value
			int len = data.length;
			int h = seed32 ^ len;

			int i = 0;
			while (len >= 4) {
				int k = data[i + 0] & 0xFF;
				k |= (data[i + 1] & 0xFF) << 8;
				k |= (data[i + 2] & 0xFF) << 16;
				k |= (data[i + 3] & 0xFF) << 24;

				k *= m;
				int msk = k >>> r;
				k ^= msk;
				k *= m;

				h *= m;
				h ^= k;

				i += 4;
				len -= 4;
			}

			switch (len) {
			case 3:
				h ^= (data[i + 2] & 0xFF) << 16;
			case 2:
				h ^= (data[i + 1] & 0xFF) << 8;
			case 1:
				h ^= (data[i + 0] & 0xFF);
				h *= m;
			}

			h ^= h >>> 13;
			h *= m;
			h ^= h >>> 15;

			lastHash = rejectionSample(h, numBitsM);
			if (lastHash != -1) {
				positions[hashes++] = BigInteger.valueOf(lastHash);
			}
			/*lastHash = Math.abs(h) % numBitsM;
			positions[hashes++] = BigInteger.valueOf(lastHash);*/
		}
		return positions;
	}

	@Override
	public String getName() {		
		return NAME;
	}
	
	/**
	 * Performs rejection sampling on a random 32bit Java int (sampled from
	 * Integer.MIN_VALUE to Integer.MAX_VALUE).
	 * 
	 * @param random
	 *            int
	 * @param numBitsM
	 *            int
	 * @return the number downsampled to interval [0, m]. Or -1 if it has to be
	 *         rejected.
	 */
	private static int rejectionSample(int random, int numBitsM) {
		random = Math.abs(random);
		if (random > (2147483647 - 2147483647 % numBitsM)
				|| random == Integer.MIN_VALUE)
			return -1;
		else
			return random % numBitsM;
	}
}
