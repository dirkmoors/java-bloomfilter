package com.dirkmoors.util.bloomfilter.probegenerators;

import java.math.BigInteger;

public interface IBloomFilterProbeGenerator {
	public BigInteger[] getProbes(int numProbesK, int numBitsM, String data);
	public String getName();
}
