package org.push.protocol;

import org.push.util.Releasable;
import org.push.util.Utils;

/**
 * This class holds a line of memory and keeps track of its data.
 * 
 * @author Lei Wang
 */

public class Buffer implements Releasable {

	protected byte[] buf;
	protected int size;
	protected int maxSize;
	protected boolean ownsBuffer;

    public Buffer() {
		buf = null;
		maxSize = 0;
		size = 0;
		ownsBuffer = false;
    }

	public void allocate(int size) {
        Utils.unsignedIntArgCheck(size, "size");

		buf = new byte[size];
		maxSize = size;
		ownsBuffer = true;
    }

	public void assign(byte[] buf) {
        assign(buf, buf.length, 0);
    }

	public void assign(byte[] buf, int size) {
        assign(buf, buf.length, size);
    }

	public void assign(byte[] buf, int maxSize, int size) {
        Utils.unsignedIntArgCheck(size, "size");

		this.buf = buf;
        
        if (buf == null) {
            this.maxSize = 0;
        } else {
            this.maxSize = buf.length;
        }
		this.size = size;
		this.ownsBuffer = false;
    }

	public void assign(Buffer srcBuffer) {
		assign(srcBuffer.getBuffer(), srcBuffer.getCapacity(), srcBuffer.getDataSize());
    }

	public void assign(Buffer srcBuffer, int size) {
		assign(srcBuffer.getBuffer(), srcBuffer.getCapacity(), size);
    }

	public int getRemainingSize() {
		return maxSize - size;
    }

	public boolean append(byte[] buf) {
		return append(buf, buf.length);
	}

	public boolean append(byte[] buf, int size) {
		return append(buf, 0, size);
    }

	public boolean append(byte[] buf, int offset, int size) {
        Utils.unsignedIntArgCheck(size, "size");
        
		if(size == 0) {
			return true;
        }

		if (getRemainingSize() < size) {
			return false;
        }

		System.arraycopy(buf, offset, this.buf, this.size, size);

		this.size = this.size + size;
		return true;
    }

	public boolean append(byte c) {
		return append(new byte[] {c}, 1);
    }

	public boolean append(Buffer srcBuffer) {
		return append(srcBuffer.getBuffer(), srcBuffer.getDataSize());
    }

	public int getDataSize() {
		return size;
    }

	public int getCapacity() {
		return maxSize;
    }

	public byte[] getBuffer() {
        return buf;
    }

	public byte[] getBuffer(int offset) {
        if (offset >= size) {
        	throw new IllegalArgumentException("invalid offset:" + offset);
        }
        
        
        return getBuffer(offset, size - offset);
    }

	public byte[] getBuffer(int offset, int length) {
        Utils.unsignedIntArgCheck(offset, "offset");
        Utils.unsignedIntArgCheck(length, "length");
        
        if (offset >= size) {
        	throw new IllegalArgumentException("invalid offset:" + offset);
        }
        
        byte[] ret = new byte[length];
        
        int copyLenth = Math.min(length, size - offset);
        System.arraycopy(buf, offset, ret, 0, copyLenth);        
        
        return ret;
    }

	public byte[] getPosition() {
        return null;
		//return buf + size;
    }

	public byte getAt(int offset) {
        Utils.unsignedIntArgCheck(offset, "offset");
        
		return buf[offset];
    }

	public void growSize(int growBy) {
        Utils.unsignedIntArgCheck(growBy, "growBy");
        
		size += growBy;
    }

	public void pop(int size) {
        Utils.unsignedIntArgCheck(size, "size");

		if (this.size < size) {
			return;
        }

		this.size = this.size - size;
		System.arraycopy(buf, size, buf, 0, this.size);
    }

	public void clearBytes() {
		size = 0;
    }

	public boolean isEmpty() {
		return size == 0;
    }

	public boolean hasBytes() {
		return size > 0;
    }

	public void setPosition(int size) {
		this.size = size;
    }

	public boolean isFull() {
		return size == maxSize;
    }

    public void release() {
        //
    }
}
