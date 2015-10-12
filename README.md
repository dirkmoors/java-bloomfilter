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

LICENCE
-------

The MIT License (MIT)

Copyright (c) 2015 Dirk Moors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
