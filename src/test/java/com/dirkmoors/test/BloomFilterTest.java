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
import com.dirkmoors.util.bloomfilter.probegenerators.MersenneProbeGenerator;
import com.dirkmoors.util.bloomfilter.probegenerators.MurmurProbeGenerator;

public class BloomFilterTest {	
	private static final Logger logger = LoggerFactory.getLogger(BloomFilterTest.class.getName());
	
	private static final double ERROR_RATE = 0.001;
	
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
	
	private static BloomFilter englishWordsBloomfilter = prepareEnglishWordsBloomFilter(100000, 0.0001); //0.01% error
	private static String[] englishWords = readEnglishWords(5000);
	
	@Before
	public void setUp() throws IOException{}
	
	@After
	public void tearDown(){}	
		
	@Test
	public void testMassiveAmount(){	
		testBloomfilterContents(
			englishWordsBloomfilter, englishWords, ERROR_RATE);
	}
	
	@Test 
	public void testJSON() throws IOException, DataFormatException{		
		BloomFilter bf = new BloomFilter(1000000, ERROR_RATE);
		for(String state: states){
			bf.add(state);
		}	
		
		boolean compressed = false;		
		String bfJSON = bf.toJSON(compressed);
				
		BloomFilter bf2 = BloomFilter.fromJSON(bfJSON);				
		testBloomfilterContents(bf2, states, ERROR_RATE);
	}
	
	@Test 
	public void testMurmurProbeGenerator() throws IOException, DataFormatException{		
		BloomFilter bf = new BloomFilter(100000, ERROR_RATE, new MurmurProbeGenerator());
		for(String state: states){
			bf.add(state);
		}	
		
		boolean compressed = true;		
		String bfJSON = bf.toJSON(compressed);
				
		BloomFilter bf2 = BloomFilter.fromJSON(bfJSON);				
		testBloomfilterContents(bf2, states, ERROR_RATE);
	}
	
	@Test 
	public void testMersenneProbeGenerator() throws IOException, DataFormatException{		
		BloomFilter bf = new BloomFilter(100000, ERROR_RATE, new MersenneProbeGenerator());
		for(String state: states){
			bf.add(state);
		}	
		
		boolean compressed = true;		
		String bfJSON = bf.toJSON(compressed);
				
		BloomFilter bf2 = BloomFilter.fromJSON(bfJSON);				
		testBloomfilterContents(bf2, states, ERROR_RATE);
	}	
	
	@Test
	public void testJsonFromPythonLibMurmur() throws IOException, DataFormatException{
		BloomFilter bf = new BloomFilter(100000, ERROR_RATE);
		for(String state: states){
			bf.add(state);
		}
		
		String jsonFromPythonLib = readFile("res/test/jsonFromPythonLibMurmur.json", 
				Charset.defaultCharset());
		
		BloomFilter bf2 = BloomFilter.fromJSON(jsonFromPythonLib);		
		assertArrayEquals(bf2.getData(), bf.getData());
		testBloomfilterContents(bf2, states, ERROR_RATE);
	}
	
	@Test
	public void testJsonFromPythonLibMersenne() throws IOException, DataFormatException{
		BloomFilter bf = new BloomFilter(100000, ERROR_RATE, new MersenneProbeGenerator());
		for(String state: states){
			bf.add(state);
		}
		
		String jsonFromPythonLib = readFile("res/test/jsonFromPythonLibMersenne.json", 
				Charset.defaultCharset());
		
		BloomFilter bf2 = BloomFilter.fromJSON(jsonFromPythonLib);		
		assertArrayEquals(bf2.getData(), bf.getData());
		testBloomfilterContents(bf2, states, ERROR_RATE);
	}
	
	private void testBloomfilterContents(BloomFilter bf, String[] expectedContents, double maxErrorRate){
		for(String candidate: expectedContents){
			assertTrue(bf.contains(candidate) == Result.MAYBE);
		}
		
		Character[] allChars = allChars(Charset.defaultCharset().name());
		
		int trials = 100000;
		String candidate;
		double count = 0;
		double errors = 0;
		for(int trial = 0; trial < trials; trial++){
			while(true){
				candidate = sample(allChars, 5);
				if(inArray(expectedContents, candidate)){
					//If we accidentally found a real state, try again
					continue;
				}
				count += 1;
				if(bf.contains(candidate) != Result.NO){
					errors++;
				}
				break;
			}
		}
		double errorRate = (errors / count);
		assertTrue(errorRate <= maxErrorRate);
		logger.debug("testBloomfilterContents: errorPct: "+(errorRate*100.0)+" %");
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
	
	private static String[] readEnglishWords(int maxNrWords){
		String path = "res/test/english-words.txt";
		String data = null;
		try {
			data = readFile(path, Charset.defaultCharset());
		} catch (IOException e) {
			e.printStackTrace();
		}
		if(data == null)return null;		
		String[] words = data.split("\n");
		
		if(maxNrWords > words.length){
			maxNrWords = words.length;
		}
		
		String[] result = new String[maxNrWords];
		for(int i = 0; i < maxNrWords; i++){
			result[i] = words[i].trim();
		}
		return result;
	}
	
	private static BloomFilter prepareEnglishWordsBloomFilter(int maxNrWords, double errorRate){
		String[] englishWords = readEnglishWords(maxNrWords);
		BloomFilter bf = new BloomFilter(englishWords.length, errorRate);
		for(String word: englishWords){
			bf.add(word);
		}
		return bf;
	}
}
