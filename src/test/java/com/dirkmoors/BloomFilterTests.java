package com.dirkmoors;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.zip.DataFormatException;

import org.junit.Test;

import com.dirkmoors.BloomFilter;
import com.dirkmoors.BloomFilter.Result;


public class BloomFilterTests {
	@Test 
	public void testSerializedCopy() throws IOException, DataFormatException{
		String[] states = ("Alabama Alaska Arizona Arkansas California " +
				"Colorado Connecticut Delaware Florida Georgia Hawaii " +
				"Idaho Illinois Indiana Iowa Kansas Kentucky Louisiana " +
				"Maine Maryland Massachusetts Michigan Minnesota " +
				"Mississippi Missouri Montana Nebraska Nevada " +
				"NewHampshire NewJersey NewMexico NewYork NorthCarolina" +
				"NorthDakota Ohio Oklahoma Oregon Pennsylvania RhodeIsland " +
				"SouthCarolina SouthDakota Tennessee Texas Utah Vermont " +
				"Virginia Washington WestVirginia Wisconsin Wyoming"
				).split(" ");
		
		BloomFilter bf = new BloomFilter(1000000, 0.001);
		for(String state: states){
			bf.add(state);
		}	
		
		long n = bf.getIdealNumberOfElements();
		double p = bf.getErrorRate();
		String data = bf.getB64Data(true);
		
		BloomFilter bf2 = new BloomFilter(n, p);
		bf2.setB64Data(data, true);
				
		testBloomfilterContents(bf2, states);
	}	
	
	@Test
	public void testContents(){
		String[] states = ("Alabama Alaska Arizona Arkansas California " +
				"Colorado Connecticut Delaware Florida Georgia Hawaii " +
				"Idaho Illinois Indiana Iowa Kansas Kentucky Louisiana " +
				"Maine Maryland Massachusetts Michigan Minnesota " +
				"Mississippi Missouri Montana Nebraska Nevada " +
				"NewHampshire NewJersey NewMexico NewYork NorthCarolina" +
				"NorthDakota Ohio Oklahoma Oregon Pennsylvania RhodeIsland " +
				"SouthCarolina SouthDakota Tennessee Texas Utah Vermont " +
				"Virginia Washington WestVirginia Wisconsin Wyoming"
				).split(" ");
		
		BloomFilter bf = new BloomFilter(1000000, 0.001);
		for(String state: states){
			bf.add(state);
		}		
		
		testBloomfilterContents(bf, states);
	}
	
	private void testBloomfilterContents(BloomFilter bf, String[] expectedContents){
		for(String candidate: expectedContents){
			assertTrue(bf.contains(candidate) == Result.MAYBE);
		}
		
		Character[] allChars = allChars(Charset.defaultCharset().name());
		
		int trials = 100000;
		String candidate;
		for(int trial = 0; trial < trials; trial++){
			while(true){
				candidate = sample(allChars, 5);
				if(inArray(expectedContents, candidate)){
					//If we accidentally found a real state, try again
					continue;
				}
				assertTrue(bf.contains(candidate) == Result.NO);
				break;
			}
		}
	}
	
	private static boolean inArray(String[] dataset, String data){
		for(String d : dataset){
			if(d.equals(data))return true;
		}
		return false;
	}
	
	private static String sample(Character[] allChars, int length){
		Random random = new Random();
		StringBuilder sb = new StringBuilder();		
		int randomIndex;
		while(sb.length() < length){
			randomIndex = random.nextInt(allChars.length);
			sb.append(allChars[randomIndex]);
		}
		return sb.toString();
	}
	
	private static Character[] allChars(String charsetName)
	{
	    CharsetEncoder ce = Charset.forName(charsetName).newEncoder();
	    List<Character> result = new ArrayList<Character>();
	    for(char c = 0; c < Character.MAX_VALUE; c++)
	    {
	        if(ce.canEncode(c) && Character.isLetter(c))
	        {
	            result.add(c);
	        }
	    }
	    return result.toArray(new Character[result.size()]);
	}
}
