package org.push.protocol;

import org.push.util.CppEnum;
import org.push.util.Debug;
import org.push.util.Utils;

/**
 * The memory space of this buffer is from buffer pool.
 * So it is called "Recyclable"
 * 
 * @author Lei Wang
 */

public class RecyclableBuffer extends Buffer {

	/**
	 * The initialization type for allocating space 
	 * to the buffer
	 * 
	 * @author Lei Wang
	 *
	 */
    public static enum Type implements CppEnum {

        UnAllocated(0),
        Single(1),
        Double(2),
        Multiple(3),
        Socket(4);
        
        private int value;

        private Type(int value) { this.value = value; }

        public int value() { return value; }
    };

	protected Type type;
	protected MemorySegment segment;

	public RecyclableBuffer() {
        this(Type.Single);
    }

	public RecyclableBuffer(Type type) {
        Utils.nullArgCheck(type, "type");
		doAllocate(type);
    }
    
    @Override
    public void release() {
        if (segment != null) {
			BufferPool.getDefaultPool().returnMemorySegment(segment, 
                    type);
			segment = null;
		}
    }
    
    /**
     * Sometimes, it will fail to borrow a segment from the pool
     * so here it requires to allocate a space by itself.
     */
    private void allocateWhenNoSegment() {
    	assign(new byte[1024]);
    }

    /**
     * Allocate memory space for this buffer.
     */
	public final void doAllocate() {
		segment = BufferPool.getDefaultPool().getMemorySegment(type);
		if (segment == null) {
			//assert(0);
			Debug.debug("Failed to borrow segment");
			// Allocate memory space by itself
			allocateWhenNoSegment();
			return;
		}

		assign(segment.getData());
    }

	/**
	 * Allocate memory space for this buffer by the given type.
	 * 
	 * @param type  @see <code>Type</code>
	 */
	public final void doAllocate(Type type) {
        Utils.nullArgCheck(type, "type");
        
		this.type = type;
		this.segment = null;
		if (type != Type.UnAllocated) {
			doAllocate();
		}
    }
}
