/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.push.protocol;

import org.push.util.Utils;

/**
 * A pool to store lots of <code>MemorySegment</code> objects
 * 
 * @author Lei Wang
 */

public class SegmentPool extends AbstractPool<MemorySegment> {
    
	// The size of memory within the MemorySegment
    private int nSize;

	public SegmentPool(int nSize) {
        Utils.unsignedIntArgCheck(nSize, "nSize");
        this.nSize = nSize;
    }

    @Override
    protected void deleteImpl(MemorySegment seg) {
    	// Nothing will be done.
    }

    @Override
    protected MemorySegment createImpl() {
        return new MemorySegment(nSize);
    }

    @Override
    protected void recycleObject(MemorySegment p) {
        // Nothing will be done.
    }
    
}
