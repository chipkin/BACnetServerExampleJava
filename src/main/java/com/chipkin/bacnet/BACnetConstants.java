/*
 * CAS BACnet Stack Java Adapter
 * 
 * Copyright and License Notice
 * Chipkin Automation Systems Inc (CAS)
 * BACnet Stack as Source Code Product
 * 
 * BACnet Constants
 */

package com.chipkin.bacnet;

/**
 * BACnet constants and enumerations
 */
public class BACnetConstants {
    
    // Network Types
    public static final byte NETWORK_TYPE_IP = 0;
    public static final byte NETWORK_TYPE_MSTP = 1;
    
    
    // Object Types
    public static final short OBJECT_TYPE_ANALOG_INPUT = 0;
    public static final short OBJECT_TYPE_ANALOG_OUTPUT = 1;
    public static final short OBJECT_TYPE_ANALOG_VALUE = 2;
    public static final short OBJECT_TYPE_BINARY_INPUT = 3;
    public static final short OBJECT_TYPE_BINARY_OUTPUT = 4;
    public static final short OBJECT_TYPE_BINARY_VALUE = 5;
    public static final short OBJECT_TYPE_DEVICE = 8;
    
    // Property Identifiers
    public static final int PROPERTY_IDENTIFIER_PRESENT_VALUE = 85;
    public static final int PROPERTY_IDENTIFIER_OBJECT_NAME = 77;
    public static final int PROPERTY_IDENTIFIER_OBJECT_TYPE = 79;
    public static final int PROPERTY_IDENTIFIER_OBJECT_IDENTIFIER = 75;
    public static final int PROPERTY_IDENTIFIER_UNITS = 117;

    public static final int SERVICE_READ_PROPERTY_MULTIPLE = 14;
    
    // Encoding Types
    public static final byte ENCODING_UTF8 = 0;
    
    private BACnetConstants() {
        // Utility class - prevent instantiation
    }
}