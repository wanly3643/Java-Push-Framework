package org.push.protocol;

import org.push.util.Utils;

/**
 * This class represents a piece of memory space.
 * Usually the object is reused by some object pool
 * @see {@link SegmentPool}
 * 
 * @author Lei Wang
 */

public class MemorySegment {

    private byte[] data;
	private int size;
    
    public MemorySegment(int size) {
        Utils.unsignedIntArgCheck(size, "size");
        
        data = new byte[size];
        this.size = size;
    }
    
    public byte[] getData() { return this.data; }
    
    public int getSize() { return this.size; }
}
