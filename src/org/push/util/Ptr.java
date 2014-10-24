package org.push.util;

/**
 * This class simulates the C++ pointer, it contains the object it point to.
 * And you could get/set the real object.
 * 
 * @author Lei Wang
 */

public final class Ptr<T> {

    private T value;

    public Ptr(T value) {
        this.value = value;
    }
    
    /**
     * Get the value the pointer points to, same as the operator "*"
     * 
     * @return the value which the pointer points to
     */
    public T get() {
        return this.value;
    }
    
    /**
     * Set the value the pointer points to, same as "*ptr=xxx".
     * @param value  the new value to set
     */
    public void set(T value) {
        this.value = value;
    }
    
    /**
     * Check if the pointer points to NULL.
     * 
     * @return true if not points to NULL
     */
    public boolean notNull() {
        return this.value == null;
    }
}
