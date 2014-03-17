java-bloomfilter
================

A java bloomfilter implementation with JSON (de)serialisation and (zlib) compression

You can find a compatible PYTHON implementation here: https://github.com/dirkmoors/python-bloomfilter

Example:
```
BloomFilter bf1 = new BloomFilter(1000000, 0.001);
bf1.add("Alabama");
bf1.add("Illinois");
bf1.add("Nevada");
bf1.add("RhodeIsland");

String serializedBloomFilter = bf1.toJSON();

#You can transmit the serializedBloomFilter easily accross the network

BloomFilter bf2 = BloomFilter.fromJSON(serializedBloomFilter);

for(String state: new String[]{"Alabama", "Illinois", "Nevada", "RhodeIsland"}){
    if(bf2.contains(state) == BloomFilter.Result.MAYBE){
        System.out.println("Yay!!");
    }
}

for(String state: new String[]{"Vermont", "Louisiana", "Mississippi", "Texas"}){
    if(bf2.contains(state) == BloomFilter.Result.NO){
        System.out.println("Yay!!");
    }
}

```
