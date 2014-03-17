package com.dirkmoors;

import java.math.BigInteger;

//http://en.wikipedia.org/wiki/Mersenne_prime
//Source: http://stromberg.dnsalias.org/svn/bloom-filter/trunk/bloom_filter_mod.py

public class MersenneProbeGenerator implements IBloomFilterProbeGenerator {
	public static final String NAME = "MERSENNE";
	
	private static final BigInteger[] MERSENNE1 = calculateMersenne1();
	private static final BigInteger[] MERSENNE2 = calculateMersenne2();

	@Override
	public String getName() {
		return MersenneProbeGenerator.NAME;
	}	

	@Override
	public BigInteger[] getProbes(int numProbesK, int numBitsM, String data) {
		int[] intList = new int[data.length()];		
		for (int i = 0; i < data.length(); i++){
			intList[i] = (int)data.charAt(i);
		}
		
		BigInteger hashValue1 = MersenneProbeGenerator.hash1(intList);
		BigInteger hashValue2 = MersenneProbeGenerator.hash2(intList);	
		
		BigInteger[] probes = new BigInteger[numProbesK];
		
		int index = 0;
		for(int probeno = 1; probeno < numProbesK + 1; probeno++){
			BigInteger bitindex = BigInteger.valueOf(probeno);
			bitindex = bitindex.multiply(hashValue2);
			bitindex = bitindex.add(hashValue1);
			
			probes[index] = bitindex.mod(BigInteger.valueOf(numBitsM));
			index++;
		}		
		return probes;
	}
	
	private static BigInteger simpleHash(int[] intList, BigInteger prime1, BigInteger prime2, BigInteger prime3){		
		BigInteger result = BigInteger.valueOf(0);
		for(int i : intList){
			//result += ((result + integer + prime1) * prime2) % prime3
			BigInteger tempValue = BigInteger.valueOf(i).add(result).add(prime1);
			tempValue = tempValue.multiply(prime2);
			tempValue = tempValue.mod(prime3);			
			result = result.add(tempValue);
		}
		return result;
	}
	
	private static BigInteger hash1(int[] intList){
		return simpleHash(intList, MERSENNE1[0], MERSENNE1[1], MERSENNE1[2]);
	}
	
	private static BigInteger hash2(int[] intList){
		return simpleHash(intList, MERSENNE2[0], MERSENNE2[1], MERSENNE2[2]);
	}
	
	private static BigInteger[] calculateMersenne(int[] primes){
		BigInteger[] result = new BigInteger[primes.length];
		for(int i = 0; i < primes.length; i++){
			//Math.pow(2, primes[i]) - 1;			
			result[i] = BigInteger.valueOf(2).pow(
				primes[i]).subtract(BigInteger.valueOf(1));
		}
		return result;
	}
	
	private static BigInteger[] calculateMersenne1(){
		int[] primes = new int[]{17, 31, 127};
		return calculateMersenne(primes);
	}
	
	private static BigInteger[] calculateMersenne2(){
		int[] primes = new int[]{19, 67, 257};
		return calculateMersenne(primes);
	}	
}
