package com.dirkmoors;

import java.math.BigInteger;

public interface IBloomFilterProbeGenerator {
	public BigInteger[] getProbes(int numProbesK, int numBitsM, String data);
}
