package org.push.protocol;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.push.protocol.RecyclableBuffer.Type;
import org.push.util.Releasable;
import org.push.util.Utils;

/**
 * A pool to store lots of <code>MemorySegment</code>. Here uses singleton 
 * pattern so you could use {@link #getDefaultPool} to get the instance.
 * 
 * By default the pool is empty. So before use it, method {@link #create}
 * must be called to create the <code>MemorySegment</code> by type.
 * 
 * @author Lei Wang
 */

public class BufferPool implements Releasable {
    
    private static final BufferPool defaultPool = new BufferPool();
	
    private ConcurrentMap<Type, SegmentPool> segmentsBySize;
    
    public static BufferPool getDefaultPool() { return defaultPool; }
    
	private BufferPool() {
        segmentsBySize = new ConcurrentHashMap<Type, SegmentPool>();
    }

	/**
	 * Create some <code>MemorySegment</code> for the given {@link Type}. 
	 * If the type already exists, false will be returned.
	 * @param type   @see {@link Type}
	 * @param nCount how many <code>MemorySegment</code> to create
	 * @param nSize  the size of the buffer created
	 * @return true if the buffers are created, otherwise false
	 */
	public boolean create(Type type, int nCount, int nSize) {
		Utils.nullArgCheck(type, "type");
        Utils.unsignedIntArgCheck(nCount, "nCount");
        Utils.unsignedIntArgCheck(nSize, "nSize");
        
        // The type already exists, return false
        if (segmentsBySize.containsKey(type)) {
        	return false;
        }

		SegmentPool segPool = new SegmentPool(nSize);

		if (!segPool.initialize(nCount, nCount)) {
			return false;
        }

		segmentsBySize.put(type, segPool);

		return true;
    }

	/**
	 * Get a <code>MemorySegment</code> of the given {@link Type}
	 * If the type does not exist or no <code>MemorySegment</code>
	 * is available, null will be returned.
	 * @param type @see {@link Type}
	 * @return  A <code>MemorySegment</code> or null
	 */
	public MemorySegment getMemorySegment(Type type) {
        SegmentPool segPool = segmentsBySize.get(type);
        if (segPool == null) {
            throw new NullPointerException("Type '" + type + "' not found");
        } else {
            return segPool.borrowObject();
        }
    }

	/**
	 * Return <code>MemorySegment</code> to the pool.
	 * 
	 * @param segment  The <code>MemorySegment</code> object
	 * @param type     Type of the segment
	 */
	public void returnMemorySegment(MemorySegment segment, Type type) {
        SegmentPool segPool = segmentsBySize.get(type);
        if (segPool == null) {
            throw new NullPointerException("Type '" + type + "' not found");
        } else {
            segPool.returnObject(segment);
        }
    }

    public void release() {
        this.segmentsBySize.clear();
    }
}
