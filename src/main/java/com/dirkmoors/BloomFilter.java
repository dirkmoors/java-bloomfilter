package com.dirkmoors;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BloomFilter {	
	private static final Logger logger = LoggerFactory.getLogger(BloomFilter.class.getName());
	
	public static final String VERSION = "1.0";
	
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
	
	public String toJSON(boolean compressed) throws JSONException, IOException{
		JSONObject result = new JSONObject();		
		result.put("v", BloomFilter.VERSION);
		result.put("n", getIdealNumberOfElements());
		result.put("p", getErrorRate());
		result.put("zlib", compressed);
		result.put("data", getB64Data(compressed));
		return result.toString();
	}
	
	private String getB64Data(boolean zlibCompressed) throws IOException{		
		byte[] bytes = longArrayToByteArray(data);
		if(zlibCompressed){
			bytes = zlibCompress(bytes);
		}
		byte[] b64bytes = Base64.encodeBase64(bytes);	
		return new String(b64bytes);
	}
	
	private void setB64Data(String b64data, boolean zlibCompressed) throws IOException, DataFormatException{
		byte[] bytes = Base64.decodeBase64(b64data);
		if(zlibCompressed){
			bytes = zlibDecompress(bytes);
		}		
		long[] data	= byteArrayToLongArray(bytes);
		this.data = data;
	}
	
	public static BloomFilter fromJSON(String jsonString) throws IOException, DataFormatException{
		JSONObject data = new JSONObject(jsonString);
		String version = data.optString("v", null);		
		long idealNumElementsN = data.optInt("n", -1);
		double errorRateP = data.optDouble("p", -1);
		boolean compressed = data.optBoolean("zlib");
		String b64data = data.optString("data", null);
		
		if(version == null || idealNumElementsN == -1 || errorRateP == -1 || b64data == null){
			throw new IllegalArgumentException("Invalid BloomFilter JSON structure");
		}
		
		if(!version.equals(BloomFilter.VERSION)){
			throw new IllegalArgumentException("Incompatible BloomFilter version");
		}
		
		BloomFilter newBloomFilter = new BloomFilter(idealNumElementsN, errorRateP);
		newBloomFilter.setB64Data(b64data, compressed);
		
		return newBloomFilter;
	}
	
	private static byte[] longArrayToByteArray(long[] data){
		byte[] result = new byte[data.length * 8];		
		byte[] temp;
		for(int i = 0; i < data.length; i++){
			temp = ByteBuffer.allocate(8).putLong(data[i]).array();
			System.arraycopy(temp, 0, result, i*8, 8);
		}		
		return result;
	}
	
	private static long[] byteArrayToLongArray(byte[] data){
		long[] result = new long[data.length / 8];		
		byte[] slice = new byte[8];
		int index = 0;
		for(int i = 0; i < data.length; i+=8){
			System.arraycopy(data, i, slice, 0, 8);
			long nextLong = ByteBuffer.wrap(slice).getLong();
			result[index] = nextLong;
			index++;
		}		
		return result;
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
	
	private static byte[] zlibCompress(byte[] data) throws IOException {
		Deflater deflater = new Deflater();
		deflater.setInput(data);

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(
				data.length);

		deflater.finish();
		byte[] buffer = new byte[1024];
		while (!deflater.finished()) {
			int count = deflater.deflate(buffer); // returns the generated
													// code... index
			outputStream.write(buffer, 0, count);
		}
		outputStream.close();
		byte[] output = outputStream.toByteArray();

		deflater.end();

		logger.debug("Original: " + data.length / 1024 + " Kb");
		logger.debug("Compressed: " + output.length / 1024 + " Kb");
		return output;
	}

	private static byte[] zlibDecompress(byte[] data) throws IOException,
			DataFormatException {
		Inflater inflater = new Inflater();
		inflater.setInput(data);

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(
				data.length);
		byte[] buffer = new byte[1024];
		while (!inflater.finished()) {
			int count = inflater.inflate(buffer);
			outputStream.write(buffer, 0, count);
		}
		outputStream.close();
		byte[] output = outputStream.toByteArray();

		inflater.end();

		logger.debug("Original: " + data.length / 1024 + " Kb");
		logger.debug("Compressed: " + output.length / 1024 + " Kb");
		return output;
	}
	
	public static enum Result{
		MAYBE,
		NO
	}
}
