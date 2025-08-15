/*
 * CAS BACnet Stack Java Adapter
 * 
 * Copyright and License Notice
 * Chipkin Automation Systems Inc (CAS)
 * BACnet Stack as Source Code Product
 * 
 * BACnet Exception
 */

package com.chipkin.bacnet;

/**
 * Exception thrown by BACnet operations
 */
public class BACnetException extends Exception {
    
    public BACnetException(String message) {
        super(message);
    }
    
    public BACnetException(String message, Throwable cause) {
        super(message, cause);
    }
}