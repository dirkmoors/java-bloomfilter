package com.dirkmoors;

import java.math.BigInteger;

public class MersennesProbeGenerator implements IBloomFilterProbeGenerator {
	//http://en.wikipedia.org/wiki/Mersenne_prime
	private static BigInteger[] MERSENNES1;
	private static BigInteger[] MERSENNES2;
	
	public MersennesProbeGenerator() {
		MERSENNES1 = calculateMersennes1();
		MERSENNES2 = calculateMersennes2();
	}

	@Override
	public BigInteger[] getProbes(int numProbesK, int numBitsM, String data) {
		int[] intList = new int[data.length()];		
		for (int i = 0; i < data.length(); i++){
			intList[i] = (int)data.charAt(i);
		}
		
		BigInteger hashValue1 = MersennesProbeGenerator.hash1(intList);
		BigInteger hashValue2 = MersennesProbeGenerator.hash2(intList);	
		
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
		return simpleHash(intList, MERSENNES1[0], MERSENNES1[1], MERSENNES1[2]);
	}
	
	private static BigInteger hash2(int[] intList){
		return simpleHash(intList, MERSENNES2[0], MERSENNES2[1], MERSENNES2[2]);
	}
	
	private static BigInteger[] calculateMersennes(int[] primes){
		BigInteger[] result = new BigInteger[primes.length];
		for(int i = 0; i < primes.length; i++){
			//Math.pow(2, primes[i]) - 1;			
			result[i] = BigInteger.valueOf(2).pow(
				primes[i]).subtract(BigInteger.valueOf(1));
		}
		return result;
	}
	
	private static BigInteger[] calculateMersennes1(){
		int[] primes = new int[]{17, 31, 127};
		return calculateMersennes(primes);
	}
	
	private static BigInteger[] calculateMersennes2(){
		int[] primes = new int[]{19, 67, 257};
		return calculateMersennes(primes);
	}
}
