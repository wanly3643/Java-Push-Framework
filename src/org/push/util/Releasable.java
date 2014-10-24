package org.push.util;

/**
 * This interface will be used by the class with C++ destructor
 * It will implement the method release().
 * 
 * When the C++ code use "delete", the method "release" is to call.
 * When it is a local object, the method "release" is to call at the end of
 * the code range
 * 
 * @author Lei Wang
 */

public interface Releasable {

    public void release();
}
