package com.dirkmoors;

public class BloomFilterProbeGenerators {	
	public static IBloomFilterProbeGenerator getProbeGenerator(String name){
		if(name.equals(MersenneProbeGenerator.NAME)){
			return new MersenneProbeGenerator();
		}
		throw new IllegalArgumentException("Unknown ProbeGenerator: "+name);
	}
}
