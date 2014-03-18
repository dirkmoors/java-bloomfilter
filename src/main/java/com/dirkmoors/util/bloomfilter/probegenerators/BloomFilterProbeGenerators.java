package com.dirkmoors.util.bloomfilter.probegenerators;


public class BloomFilterProbeGenerators {	
	public static IBloomFilterProbeGenerator getProbeGenerator(String name){
		if(name.equals(MersenneProbeGenerator.NAME)){
			return new MersenneProbeGenerator();
		}
		else if(name.equals(MurmurProbeGenerator.NAME)){
			return new MurmurProbeGenerator();
		}
		throw new IllegalArgumentException("Unknown ProbeGenerator: "+name);
	}
}
