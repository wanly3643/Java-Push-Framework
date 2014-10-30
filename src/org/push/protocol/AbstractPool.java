package org.push.protocol;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.push.util.Releasable;
import org.push.util.Utils;

/**
 * This class provides a skeletal implementation of an object pool.
 * 
 * @author Lei Wang
 */

public abstract class AbstractPool<T> implements Releasable {
    
    // Used as the value of objectsInUse
    private static final Object OBJECT = new Object();
	
    // The objects which are being used
    private ConcurrentMap<T, Object> objectsInUse;
    
    // The objects which are still free in the pool
    private ConcurrentLinkedQueue<T> freeObjects;
    
    /* Maximum objects allowed in the pool */
    private int maxAllowed;
    
    /* How many objects in the pool */
    private AtomicInteger objectCount;

    public AbstractPool() {
        objectsInUse = new ConcurrentHashMap<T, Object>();
        freeObjects = new ConcurrentLinkedQueue<T>();
    }

    public void release() {
        objectsInUse.clear();
        freeObjects.clear();
        maxAllowed = 0;
        objectCount = null;
    }

    /**
     * Initialize the pool to create some objects within the pool.
     * 
     * @param nRequiredObjects  How many objects are created
     * @param maxAllowed        Maximum objects are allowed
     * @return  true if the initialization is finished without error.
     */
    public synchronized boolean initialize(int nRequiredObjects, 
    		int maxAllowed) {
    	// Already initialized
    	if (objectCount != null || maxAllowed != 0) {
    		return false;
    	}

    	Utils.unsignedIntArgCheck(nRequiredObjects, "nRequiredObjects");
    	Utils.unsignedIntArgCheck(maxAllowed, "maxAllowed");
    	
    	if (maxAllowed < nRequiredObjects) {
    		throw new IllegalArgumentException(
    				"'maxAllowed' could not be less than 'nRequiredObjects'");
    	}
    	
    	this.objectCount = new AtomicInteger(nRequiredObjects);
    	this.maxAllowed = maxAllowed;

        try {
            for (int i = 0; i < nRequiredObjects; i ++) {
                freeObjects.add(createImpl());
            }
        } catch (Exception ex) {
        	ex.printStackTrace();
            return false;
        }
        
        return true;
    }

    /**
     * Borrow an object from the pool. If there is no
     * free object, null will be returned.
     * 
     * @return an available object or null
     */
    public T borrowObject() {
		T object = freeObjects.poll();

		if (object == null) {
			if (objectCount.getAndIncrement() < maxAllowed) {
				object = createImpl();
			} else {
				objectCount.decrementAndGet(); //recover
			}
        }
		
		// Add into in use list if there is an available
		if (object != null) {
			objectsInUse.put(object, OBJECT);
		}

		return object;
    }

    /**
     * Return the object to the pool.
     * 
     * @param object The object returned to the pool
     */
    public void returnObject(T object) {
    	if (object == null) {
    		return;
    	}

		Object value = objectsInUse.remove(object);
        
        if (value != null) {
            freeObjects.add(object);
            recycleObject(object);
        }
    }
    
    /**
     * This is a C++ style method, usually use the "delete"
     * to free the memory space by the pointer.
     * In Java, this method may be removed in the later version.
     * 
     * @param object  The object used
     */
	protected abstract void deleteImpl(T object);
	
	/**
	 * Create the object used in the pool
	 * 
	 * @return  the object created
	 */
	protected abstract T createImpl();

	/**
	 * Recycle the object when this object is returned to
	 * the pool.
	 * 
	 * @param object  The object returned to the pool
	 */
	protected abstract void recycleObject(T object);
}
