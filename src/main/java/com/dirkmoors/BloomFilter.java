package com.dirkmoors;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.Random;
import java.util.zip.DataFormatException;

import org.apache.commons.codec.binary.Base64;

public class BloomFilter {	
	public static final int VERSION = 1;
	
	private long idealNumElementsN;	
	private double errorRateP;
	
	private int numBitsM;
	private int numProbesK;
	private int numWords;
	private long[] data;	
	
	public BloomFilter(){}
	
	public BloomFilter(long idealNumElementsN, double errorRateP) {
		this(idealNumElementsN, errorRateP, null);
	}
	
	public BloomFilter(long idealNumElementsN, double errorRateP, long[] data) {
		if(idealNumElementsN <= 0){
			throw new IllegalArgumentException("idealNumElementsN must be > 0");
		}
		if(!(0 < errorRateP &&  errorRateP < 1)){
			throw new IllegalArgumentException("errorRateP must be between 0 and 1 exclusive");
		}
		
		this.idealNumElementsN = idealNumElementsN;
		this.errorRateP = errorRateP;
		
		this.numBitsM = BloomFilter.calculateNumBitsM(
			this.idealNumElementsN, this.errorRateP);
		
		this.numProbesK = BloomFilter.calculateNumProbesK(
			this.idealNumElementsN, this.numBitsM);
		
		this.numWords = BloomFilter.calculateNumWords(this.numBitsM);
		
		this.data = data != null ? data : new long[this.numWords];
	}
	
	public long[] getProbes(String data){
		Random r = new Random(data.hashCode());
		long[] probes = new long[this.numProbesK];
		for(int i = 0; i < probes.length; i++){
			probes[i] = (long)(r.nextDouble() * this.numWords);
		}
		return probes;
	}
	
	public long[] getData(){
		return this.data;
	}
	
	public long getIdealNumberOfElements(){
		return this.idealNumElementsN;
	}
	
	public double getErrorRate(){
		return this.errorRateP;
	}
	
	public int getNumberOfProbes(){
		return this.numProbesK;
	}
	
	public int getNumberOfBits(){
		return this.numBitsM;
	}
	
	public void add(String key){
		long[] probes = getProbes(key);
		
		for(long i : probes){
			int bitIndex = (int)Math.floor(i / 8);
			this.data[bitIndex] |= (long)Math.pow(2, (i % 8));
		}
	}
	
	public boolean matchTemplate(BloomFilter bfilter){
		return (
			this.numBitsM == bfilter.numBitsM && 
			this.numProbesK == bfilter.numProbesK);
	}
	
	public void union(BloomFilter bfilter){
		if(!this.matchTemplate(bfilter)){
			throw new IllegalArgumentException("Mismatched bloom filters");
		}
		long[] newData = new long[this.data.length];
		for(int i = 0; i < newData.length; i++){
			newData[i] = this.data[i] | bfilter.data[i];
		}
		this.data = newData;
	}
	
	public void intersection(BloomFilter bfilter){
		if(!this.matchTemplate(bfilter)){
			throw new IllegalArgumentException("Mismatched bloom filters");
		}
		long[] newData = new long[this.data.length];
		for(int i = 0; i < newData.length; i++){
			newData[i] = this.data[i] & bfilter.data[i];
		}
		this.data = newData;
	}
	
	public Result contains(String key){
		long[] probes = this.getProbes(key);
		
		for(long i : probes){
			int bitIndex = (int)Math.floor(i / 8);
			if((this.data[bitIndex] & (long)Math.pow(2, (i % 8))) == 0){			
				return Result.NO;
			}
		}
		return Result.MAYBE;
	}
	
	public String getB64Data(boolean zlibCompressed) throws IOException{
		ByteBuffer buf = ByteBuffer.allocate(this.data.length * 8);
		for(long l : this.data){
			buf.putLong(l);
		}
		byte[] bytes = buf.array();
		if(zlibCompressed){
			bytes = Zlib.compress(bytes);
		}
		byte[] b64bytes = Base64.encodeBase64(bytes);	
		return new String(b64bytes);
	}
	
	public void setB64Data(String b64data, boolean zlibCompressed) throws IOException, DataFormatException{
		byte[] bytes = Base64.decodeBase64(b64data);
		if(zlibCompressed){
			bytes = Zlib.decompress(bytes);
		}
		BitSet dataSet = BitSet.valueOf(bytes);
		long[] data	= dataSet.toLongArray();
		this.data = data;
	}
	
	private static int calculateNumBitsM(long n, double p){
		double numerator = -1 * n * Math.log(p);
		double denominator = Math.pow(Math.log(2), 2);
		double realNumBitsM = numerator / denominator;
		return (int)Math.ceil(realNumBitsM);
	}
	
	private static int calculateNumProbesK(long n, int m){
		double realNumProbesK = (m / n) * Math.log(2);
		return (int)Math.ceil(realNumProbesK);
	}
	
	private static int calculateNumWords(int m){
		return (int)Math.floor((m + 31) / 32);
	}
	
	public static enum Result{
		MAYBE,
		NO
	}
}
