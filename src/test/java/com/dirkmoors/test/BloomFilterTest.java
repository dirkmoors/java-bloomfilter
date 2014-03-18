package com.dirkmoors.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.zip.DataFormatException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dirkmoors.util.bloomfilter.BloomFilter;
import com.dirkmoors.util.bloomfilter.BloomFilter.Result;
import com.dirkmoors.util.bloomfilter.probegenerators.MurmurProbeGenerator;

public class BloomFilterTest {
	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(BloomFilterTest.class.getName());
	
	private static final String[] states = (
		"Alabama Alaska Arizona Arkansas California " +
		"Colorado Connecticut Delaware Florida Georgia Hawaii " +
		"Idaho Illinois Indiana Iowa Kansas Kentucky Louisiana " +
		"Maine Maryland Massachusetts Michigan Minnesota " +
		"Mississippi Missouri Montana Nebraska Nevada " +
		"NewHampshire NewJersey NewMexico NewYork NorthCarolina " +
		"NorthDakota Ohio Oklahoma Oregon Pennsylvania RhodeIsland " +
		"SouthCarolina SouthDakota Tennessee Texas Utah Vermont " +
		"Virginia Washington WestVirginia Wisconsin Wyoming"
		).split(" ");
	
	@Before
	public void setUp(){}
	
	@After
	public void tearDown(){}	
		
	@Test
	public void testContents(){		
		BloomFilter bf = new BloomFilter(1000000, 0.001);
		for(String state: states){
			bf.add(state);
		}		
		
		testBloomfilterContents(bf, states);
	}
	
	@Test 
	public void testJSON() throws IOException, DataFormatException{		
		BloomFilter bf = new BloomFilter(1000000, 0.001);
		for(String state: states){
			bf.add(state);
		}	
		
		boolean compressed = false;		
		String bfJSON = bf.toJSON(compressed);
				
		BloomFilter bf2 = BloomFilter.fromJSON(bfJSON);				
		testBloomfilterContents(bf2, states);
	}
	
	@Test 
	public void testCompressedJSON() throws IOException, DataFormatException{		
		BloomFilter bf = new BloomFilter(1000000, 0.001);
		for(String state: states){
			bf.add(state);
		}	
		
		boolean compressed = true;		
		String bfJSON = bf.toJSON(compressed);
				
		BloomFilter bf2 = BloomFilter.fromJSON(bfJSON);				
		testBloomfilterContents(bf2, states);
	}
	
	@Test 
	public void testMurmurProbeGenerator() throws IOException, DataFormatException{		
		BloomFilter bf = new BloomFilter(1000000, 0.001, new MurmurProbeGenerator());
		for(String state: states){
			bf.add(state);
		}	
		
		boolean compressed = true;		
		String bfJSON = bf.toJSON(compressed);
				
		BloomFilter bf2 = BloomFilter.fromJSON(bfJSON);				
		testBloomfilterContents(bf2, states);
	}
	
	@Test
	public void testJsonFromPythonLib() throws IOException, DataFormatException{
		BloomFilter bf = new BloomFilter(100000, 0.001);
		for(String state: states){
			bf.add(state);
		}
		
		String jsonFromPythonLib = readFile("res/test/jsonFromPythonLib.json", 
				Charset.defaultCharset());
		
		BloomFilter bf2 = BloomFilter.fromJSON(jsonFromPythonLib);		
		assertArrayEquals(bf2.getData(), bf.getData());
		testBloomfilterContents(bf2, states);
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
	
	private static String readFile(String path, Charset encoding) 
			  throws IOException 
	{
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return encoding.decode(ByteBuffer.wrap(encoded)).toString().trim();
	}
}
