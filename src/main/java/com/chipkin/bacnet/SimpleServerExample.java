/*
 * CAS BACnet Stack Java Adapter
 * 
 * Copyright and License Notice
 * Chipkin Automation Systems Inc (CAS)
 * BACnet Stack as Source Code Product
 * 
 * Simple BACnet Server Example with Business Logic
 */
package com.chipkin.bacnet;

import com.sun.jna.Pointer;
import com.sun.jna.Memory;
import java.util.HashMap;
import java.util.Map;
// import java.util.logging.Logger;
// import java.util.logging.ConsoleHandler;
// import java.util.logging.Level;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.SocketException;
import java.io.IOException;

/**
 * Complete BACnet Server Example demonstrating how to implement business logic
 * using the CAS BACnet Stack Java adapter.
 *
 * This class contains ALL the business logic: - Property storage and management
 * - Callback implementations - Device initialization and object creation - Main
 * processing loop - Real UDP socket communication for BACnet/IP
 *
 * Customers can copy and modify this implementation for their own applications.
 */
public class SimpleServerExample {

    // private static final Logger logger = Logger.getLogger(SimpleServerExample.class.getName());
    // static {
    //     Logger.getLogger("").setLevel(Level.FINE);
    // }
    private static void log(String message) {
        System.out.println(message);
    }

    // ========================================================================
    // Property Storage and Device Management
    // ========================================================================
    // Constants for BACnet/IP communication  
    private static final int BACNET_IP_PORT = 47808;
    private static final int UDP_TIMEOUT_MS = 1; // Non-blocking timeout

    // UDP socket for real BACnet/IP communication
    private DatagramSocket udpSocket;

    private CASBACnetStackAdapter adapter;
    private ICASBACnetStackLibrary library;
    private int deviceInstance;
    private boolean initialized = false;
    private long last_updated = 0;
    private float last_value = 99.1f;

    // Storage for property values (this is the "database" for property requests)
    private final Map<String, Float> realValues = new HashMap<>();
    private final Map<String, String> stringValues = new HashMap<>();
    private final Map<String, Integer> enumValues = new HashMap<>();

    /**
     * Constructor - creates the adapter interface and initializes UDP socket
     */
    public SimpleServerExample() throws BACnetException {
        log("=== SimpleServerExample Constructor ===");
        log("Creating CAS BACnet Stack adapter interface...");

        adapter = new CASBACnetStackAdapter();
        library = adapter.getLibrary();

        log("Setting up UDP socket for BACnet/IP communication...");
        setupUdpSocket();

        log("Adapter interface created successfully");
        log("=== End Constructor ===");
    }

    /**
     * Setup UDP socket for BACnet/IP communication
     */
    private void setupUdpSocket() throws BACnetException {
        log("=== Setup UDP Socket ===");
        try {
            // Create UDP socket and bind to BACnet/IP port
            udpSocket = new DatagramSocket(BACNET_IP_PORT);
            udpSocket.setSoTimeout(UDP_TIMEOUT_MS); // Non-blocking with 1ms timeout

            log("UDP socket created and bound to port: " + BACNET_IP_PORT);
            log("Socket timeout set to: " + UDP_TIMEOUT_MS + "ms (non-blocking)");
            log("Local socket address: " + udpSocket.getLocalAddress() + ":" + udpSocket.getLocalPort());

        } catch (SocketException e) {
            String error = "Failed to create UDP socket on port " + BACNET_IP_PORT + ": " + e.getMessage();
            log(error);
            throw new BACnetException(error);
        }
        log("=== End Setup UDP Socket ===");
    }

    /**
     * Initialize the BACnet device with business logic
     */
    public void initializeDevice(int deviceInstance) throws BACnetException {
        log("=== Initialize Device ===");
        log("Device instance: " + deviceInstance);

        if (initialized) {
            throw new BACnetException("Device already initialized");
        }

        this.deviceInstance = deviceInstance;

        // Add the device to the BACnet stack
        log("Adding device to BACnet stack...");
        if (!library.BACnetStack_AddDevice(deviceInstance)) {
            throw new BACnetException("Failed to add device " + deviceInstance);
        }
        log("Device added successfully");

        // Register all the essential callbacks with business logic
        log("Registering business logic callbacks...");
        registerCallbacks();
        log("Callbacks registered successfully");

        library.BACnetStack_SetServiceEnabled(deviceInstance, BACnetConstants.SERVICE_READ_PROPERTY_MULTIPLE, true);

        initialized = true;
        log("Device initialization complete");
        log("=== End Initialize Device ===");
    }

    /**
     * Update property value in business logic database
     */
    public void updateAnalogInputPresentValue(int objectInstance, float value) {
        log("=== Update Analog Input Present Value ===");
        log("Object instance: " + objectInstance);
        log("New value: " + value);

        String key = makePropertyKey(BACnetConstants.OBJECT_TYPE_ANALOG_INPUT, objectInstance, BACnetConstants.PROPERTY_IDENTIFIER_PRESENT_VALUE);
        Float oldValue = realValues.get(key);
        realValues.put(key, value);

        log("Updated property key: " + key);
        log("Old value: " + oldValue + " -> New value: " + value);
        log("=== End Update Present Value ===");
    }

    /**
     * Cleanup resources - close UDP socket
     */
    public void cleanup() {
        log("=== Cleanup Resources ===");
        if (udpSocket != null && !udpSocket.isClosed()) {
            udpSocket.close();
            log("UDP socket closed");
        }
        log("=== End Cleanup ===");
    }

    // ========================================================================
    // Private Business Logic Helper Methods
    // ========================================================================
    private String makePropertyKey(short objectType, int objectInstance, int propertyIdentifier) {
        return objectType + ":" + objectInstance + ":" + propertyIdentifier;
    }

    /**
     * Register all essential callbacks with business logic implementations
     */
    private void registerCallbacks() {
        log("=== Register Business Logic Callbacks ===");

        // Register GetSystemTime callback
        log("Registering GetSystemTime callback...");
        library.BACnetStack_RegisterCallbackGetSystemTime(new ICASBACnetStackLibrary.GetSystemTimeCallback() {
            @Override
            public long callback() {
                long now = System.currentTimeMillis() / 1000L; // seconds since epoch
                // log("GetSystemTime callback called, returning: " + now);
                return now;
            }
        });
        log("GetSystemTime callback registered");
        // Register SendMessage callback with business logic
        log("Registering SendMessage callback...");
        library.BACnetStack_RegisterCallbackSendMessage(new ICASBACnetStackLibrary.SendMessageCallback() {
            @Override
            public short callback(Pointer message, short messageLength,
                    Pointer connectionString, byte connectionStringLength,
                    byte networkType, boolean broadcast) {
                return handleSendMessage(message, messageLength, connectionString,
                        connectionStringLength, networkType, broadcast);
            }
        });
        log("SendMessage callback registered");

        // Register ReceiveMessage callback with business logic
        log("Registering ReceiveMessage callback...");
        library.BACnetStack_RegisterCallbackReceiveMessage(new ICASBACnetStackLibrary.ReceiveMessageCallback() {
            @Override
            public short callback(Pointer message, short maxMessageLength,
                    Pointer receivedConnectionString, byte maxConnectionStringLength,
                    Pointer receivedConnectionStringLength, Pointer networkType) {
                return handleReceiveMessage(message, maxMessageLength, receivedConnectionString,
                        maxConnectionStringLength, receivedConnectionStringLength, networkType);
            }
        });
        log("ReceiveMessage callback registered");

        // Register GetPropertyReal callback with business logic
        log("Registering GetPropertyReal callback...");
        library.BACnetStack_RegisterCallbackGetPropertyReal(new ICASBACnetStackLibrary.GetPropertyRealCallback() {
            @Override
            public boolean callback(int deviceInstance, short objectType, int objectInstance,
                    int propertyIdentifier, Pointer value,
                    boolean useArrayIndex, int propertyArrayIndex) {
                return handleGetPropertyReal(deviceInstance, objectType, objectInstance,
                        propertyIdentifier, value, useArrayIndex, propertyArrayIndex);
            }
        });
        log("GetPropertyReal callback registered");

        // Register GetPropertyCharacterString callback with business logic
        log("Registering GetPropertyCharacterString callback...");
        library.BACnetStack_RegisterCallbackGetPropertyCharacterString(new ICASBACnetStackLibrary.GetPropertyCharStringCallback() {
            @Override
            public boolean callback(int deviceInstance, short objectType, int objectInstance,
                    int propertyIdentifier, Pointer value,
                    Pointer valueElementCount, int maxElementCount, Pointer encodingType,
                    boolean useArrayIndex, int propertyArrayIndex) {
                return handleGetPropertyCharString(deviceInstance, objectType, objectInstance,
                        propertyIdentifier, value, valueElementCount,
                        maxElementCount, encodingType, useArrayIndex, propertyArrayIndex);
            }
        });
        log("GetPropertyCharacterString callback registered");

        // Register GetPropertyEnumerated callback with business logic
        log("Registering GetPropertyEnumerated callback...");
        library.BACnetStack_RegisterCallbackGetPropertyEnumerated(new ICASBACnetStackLibrary.GetPropertyEnumCallback() {
            @Override
            public boolean callback(int deviceInstance, short objectType, int objectInstance,
                    int propertyIdentifier, Pointer value,
                    boolean useArrayIndex, int propertyArrayIndex) {
                return handleGetPropertyEnum(deviceInstance, objectType, objectInstance,
                        propertyIdentifier, value, useArrayIndex, propertyArrayIndex);
            }
        });
        log("GetPropertyEnumerated callback registered");

        log("All business logic callbacks registered successfully");
        log("=== End Register Callbacks ===");
    }

    // ========================================================================
    // Business Logic Callback Implementations
    // ========================================================================
    /**
     * Business logic for sending BACnet messages over the network
     */
    private short handleSendMessage(Pointer message, short messageLength,
            Pointer connectionString, byte connectionStringLength,
            byte networkType, boolean broadcast) {

        log("=== SendMessage Business Logic ===");
        log("Message length: " + messageLength + ", Connection string length: " + connectionStringLength + ", Network type: " + networkType + ", Broadcast: " + broadcast);

        // Business logic: Extract IP and port from connection string
        String ipAddress = null;
        int port = 0;
        if (connectionString != null && connectionStringLength >= 6) {
            byte[] connBytes = new byte[connectionStringLength];
            connectionString.read(0, connBytes, 0, connectionStringLength);

            // Parse IP address (first 4 bytes) and port (next 2 bytes)  
            int ip1 = connBytes[0] & 0xFF;
            int ip2 = connBytes[1] & 0xFF;
            int ip3 = connBytes[2] & 0xFF;
            int ip4 = connBytes[3] & 0xFF;
            port = ((connBytes[4] & 0xFF) << 8) | (connBytes[5] & 0xFF);

            ipAddress = ip1 + "." + ip2 + "." + ip3 + "." + ip4;

            // Handle broadcast
            if (broadcast) {
                ipAddress = "255.255.255.255";
                log("Broadcasting to: " + ipAddress + ":" + port);
            } else {
                log("Sending to: " + ipAddress + ":" + port);
            }

            // Display raw connection string bytes
            StringBuilder hexConn = new StringBuilder();
            for (int i = 0; i < connBytes.length; i++) {
                hexConn.append(String.format("%02X ", connBytes[i] & 0xFF));
            }
            log("Connection string bytes: " + hexConn.toString());
        }

        // Business logic: Display message hex dump (first 32 bytes)
        byte[] msgBytes = null;
        if (message != null && messageLength > 0) {
            msgBytes = new byte[messageLength];
            message.read(0, msgBytes, 0, messageLength);

            byte[] displayBytes = new byte[Math.min(messageLength, 32)];
            System.arraycopy(msgBytes, 0, displayBytes, 0, displayBytes.length);
            StringBuilder hexDump = new StringBuilder();
            for (int i = 0; i < displayBytes.length; i++) {
                hexDump.append(String.format("%02X ", displayBytes[i] & 0xFF));
                if ((i + 1) % 16 == 0) {
                    hexDump.append("\n                    ");
                }
            }
            log("Message hex dump (first " + displayBytes.length + " bytes):");
            log("                    " + hexDump.toString());

            // Try to identify BACnet message type
            if (displayBytes.length >= 4) {
                int bvlcType = displayBytes[0] & 0xFF;
                int bvlcFunc = displayBytes[1] & 0xFF;
                log("BVLC Type: 0x" + String.format("%02X", bvlcType));
                log("BVLC Function: 0x" + String.format("%02X", bvlcFunc));
            }
        }

        // Business logic: Actually send UDP packet (like Python example)
        if (networkType != BACnetConstants.NETWORK_TYPE_IP) {
            log("Unsupported network type: " + networkType + " (only BACnet/IP supported)");
            log("=== End SendMessage Business Logic ===");
            return 0;
        }

        if (ipAddress != null && msgBytes != null && udpSocket != null) {
            try {
                InetAddress destAddress = InetAddress.getByName(ipAddress);
                DatagramPacket packet = new DatagramPacket(msgBytes, msgBytes.length, destAddress, port);

                log("Sending UDP packet: " + msgBytes.length + " bytes to " + ipAddress + ":" + port);
                udpSocket.send(packet);
                log("UDP packet sent successfully");

                log("=== End SendMessage Business Logic ===");
                return messageLength;

            } catch (IOException e) {
                log("Failed to send UDP packet: " + e.getMessage());
                log("=== End SendMessage Business Logic ===");
                return 0;
            }
        }

        log("Invalid parameters for UDP send");
        log("=== End SendMessage Business Logic ===");
        return 0;
    }

    /**
     * Business logic for receiving BACnet messages from the network
     */
    private short handleReceiveMessage(Pointer message, short maxMessageLength,
            Pointer sourceConnectionString, byte maxConnectionStringLength,
            Pointer sourceConnectionStringLength, Pointer networkType) {

        if (udpSocket == null || message == null || sourceConnectionStringLength == null || networkType == null
                || sourceConnectionString == null) {
            return 0;
        }

        int asUnsignedMaxConnectionStringLength = maxConnectionStringLength & 0xFF;

        if (asUnsignedMaxConnectionStringLength < 6) {
            log("Error: maxConnectionStringLength<6. asUnsignedMaxConnectionStringLength:" + asUnsignedMaxConnectionStringLength);
            return 0;
        }

        // log("=== ReceiveMessage Business Logic ===");
        // log("Max message length: " + maxMessageLength);
        // log("Max connection string length: " + maxConnectionStringLength);
        // Business logic: Actually receive UDP packet (like Python example)
        try {
            byte[] buffer = new byte[maxMessageLength];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            // Receive UDP packet (non-blocking due to timeout)
            udpSocket.receive(packet);
            int receivedLength = packet.getLength();
            if (receivedLength == 0) {
                return 0; // No message, normal
            }
            // Copy received data to message buffer
            if (receivedLength > maxMessageLength) {
                log("Error: receivedLength > maxMessageLength. maxMessageLength: " + maxMessageLength);
                return 0;
            }

            // Extract source address and port
            InetAddress sourceAddr = packet.getAddress();
            int sourcePort = packet.getPort();
            byte[] receivedData = packet.getData();

            if (sourceAddr == null) {
                log("Error: sourceAddr == null");
                return 0;
            }

            log("UDP message received from " + sourceAddr.getHostAddress() + ":" + sourcePort + ", Message length: " + receivedLength + " bytes");

            // Copy the message
            message.write(0, receivedData, 0, receivedLength);

            // Set default return values (following Python example pattern)
            networkType.setByte(0, BACnetConstants.NETWORK_TYPE_IP); // Set to BACnet/IP
            // log("Set network type to BACnet/IP (" + BACnetConstants.NETWORK_TYPE_IP + ")");

            // Set source connection string (IP + port in 6-byte format)
            byte[] ipBytes = sourceAddr.getAddress(); // IPv4 = 4 bytes
            sourceConnectionString.setByte(0, ipBytes[0]); // IP byte 1
            sourceConnectionString.setByte(1, ipBytes[1]); // IP byte 2  
            sourceConnectionString.setByte(2, ipBytes[2]); // IP byte 3
            sourceConnectionString.setByte(3, ipBytes[3]); // IP byte 4
            sourceConnectionString.setByte(4, (byte) (sourcePort / 256)); // Port high byte
            sourceConnectionString.setByte(5, (byte) (sourcePort % 256)); // Port low byte
            // log("Set source address to " + sourceAddr.getHostAddress() + ":" + sourcePort);

            // Print the bytes in sourceConnectionString for debugging
            byte[] connStringBytes = new byte[6];
            sourceConnectionString.read(0, connStringBytes, 0, 6);
            StringBuilder connStringHex = new StringBuilder();
            for (int i = 0; i < connStringBytes.length; i++) {
                connStringHex.append(String.format("%02X ", connStringBytes[i] & 0xFF));
            }
            // log("sourceConnectionString bytes: " + connStringHex.toString());

            // Set as both byte and int for compatibility with C++
            // sourceConnectionStringLength.setByte(0, (byte)6); // IP (4 bytes) + Port (2 bytes)
            sourceConnectionStringLength.setByte(0, (byte) 6); // Also set as int in case C++ expects uint32_t*
            // log("Set connection string length to 6 bytes");

            // Display message hex dump (first 32 bytes)
            if (receivedLength > 0) {
                int displayBytes = Math.min(receivedLength, 32);
                StringBuilder hexDump = new StringBuilder();
                for (int i = 0; i < displayBytes; i++) {
                    hexDump.append(String.format("%02X ", receivedData[i] & 0xFF));
                    if ((i + 1) % 16 == 0) {
                        hexDump.append("\n                    ");
                    }
                }
                log("Received message hex dump (first " + displayBytes + " bytes): " + hexDump.toString());
            }

            // log("=== End ReceiveMessage Business Logic ===");
            return (short) receivedLength;

        } catch (SocketTimeoutException e) {
            // Normal - no message available (non-blocking)
            // log("No UDP message available (timeout)");
            // log("=== End ReceiveMessage Business Logic ===");
            return 0;

        } catch (IOException e) {
            log("UDP receive error: " + e.getMessage());
            log("=== End ReceiveMessage Business Logic ===");
            return 0;
        }
    }

    /**
     * Business logic for retrieving real/float property values
     */
    private boolean handleGetPropertyReal(int deviceInstance, short objectType, int objectInstance,
            int propertyIdentifier, Pointer value,
            boolean useArrayIndex, int propertyArrayIndex) {
        log("GetPropertyReal Business Logic Device instance: " + deviceInstance + ", Object type: " + objectType + ", Object instance: " + objectInstance + ", Property identifier: " + propertyIdentifier + ", Use array index: " + useArrayIndex + ", Property array index: " + propertyArrayIndex);

        // Business logic: Check device instance matches our device
        if (this.deviceInstance != deviceInstance) {
            log("Device instance mismatch (expected " + this.deviceInstance + ", got " + deviceInstance + ")");            
            return false;
        }

        // Business logic: Look up property value in our database
        String key = makePropertyKey(objectType, objectInstance, propertyIdentifier);
        Float floatValue = realValues.get(key);

        if (floatValue != null) {
            // Business logic: Set the value in the output pointer
            value.setFloat(0, floatValue);
            log("Found property value: " + floatValue);
            return true;
        }

        log("Property not found in realValues database");
        return false;
    }

    /**
     * Business logic for retrieving string property values
     */
    private boolean handleGetPropertyCharString(int deviceInstance, short objectType, int objectInstance,
            int propertyIdentifier, Pointer value,
            Pointer valueElementCount, int maxElementCount, Pointer encodingType,
            boolean useArrayIndex, int propertyArrayIndex) {
        log("GetPropertyCharString Business Logic Device instance: " + deviceInstance + ", Object type: " + objectType + " (0=Analog Input, 1=Analog Output, 2=Analog Value, etc.)" + ", Object instance: " + objectInstance + ", Property identifier: " + propertyIdentifier + " (85=Present Value, 77=Object Name, etc.)" + ", Max element count: " + maxElementCount + ", Use array index: " + useArrayIndex + ", Property array index: " + propertyArrayIndex + ", Value element count pointer: " + valueElementCount + ", Encoding type pointer: " + encodingType);

        // Business logic: Check device instance matches our device
        if (this.deviceInstance != deviceInstance) {
            log("Device instance mismatch (expected " + this.deviceInstance + ", got " + deviceInstance + ")");
            return false;
        }

        // Business logic: Look up property value in our database
        String key = makePropertyKey(objectType, objectInstance, propertyIdentifier);
        String stringValue = stringValues.get(key);

        if (stringValue != null) {
            byte[] bytes = stringValue.getBytes();
            log("Found property value: '" + stringValue + "' (" + bytes.length + " bytes)");

            if (bytes.length < maxElementCount) {
                // Business logic: Set string value, length, and encoding
                value.write(0, bytes, 0, bytes.length);
                value.setByte(bytes.length, (byte) 0); // null terminator
                valueElementCount.setInt(0, bytes.length);
                encodingType.setByte(0, BACnetConstants.ENCODING_UTF8);

                log("String property set successfully:");
                log("  - String length: " + bytes.length);
                log("  - Encoding type: " + BACnetConstants.ENCODING_UTF8 + " (UTF-8)");
                log("  - Null terminator added at position " + bytes.length);
                return true;
            } else {
                log("String too long for buffer (" + bytes.length + " >= " + maxElementCount + ")");
                log("Buffer overflow prevented - string not set");
            }
        } 
        return false;
    }

    /**
     * Business logic for retrieving enumerated property values
     */
    private boolean handleGetPropertyEnum(int deviceInstance, short objectType, int objectInstance,
            int propertyIdentifier, Pointer value,
            boolean useArrayIndex, int propertyArrayIndex) {
        log("GetPropertyEnum Business Logic Device instance: " + deviceInstance + ", Object type: " + objectType + " (0=Analog Input, 1=Analog Output, 2=Analog Value, etc.)" + ", Object instance: " + objectInstance + ", Property identifier: " + propertyIdentifier + " (85=Present Value, 77=Object Name, 79=Object Type, etc.)" + ", Use array index: " + useArrayIndex + ", Property array index: " + propertyArrayIndex);

        // Business logic: Check device instance matches our device
        if (this.deviceInstance != deviceInstance) {
            log("Device instance mismatch (expected " + this.deviceInstance + ", got " + deviceInstance + ")");
            return false;
        }

        // Business logic: Look up property value in our database
        String key = makePropertyKey(objectType, objectInstance, propertyIdentifier);
        Integer enumValue = enumValues.get(key);

        if (enumValue != null) {
            // Business logic: Set the enum value in the output pointer
            value.setInt(0, enumValue);
            log("Found property value: " + enumValue);
            log("Successfully set enum value in pointer");
            return true;
        }

        log("Property not found in enumValues database");
        return false;
    }

    public void startup() {

        try {
            // Print version info
            log("Using: " + adapter.getVersionInfo());

            int deviceInstance = 12345;

            // Initialize with device instance 12345
            initializeDevice(deviceInstance);

            // Add the object to the BACnet stack
            log("Adding analog input object to device " + deviceInstance + "...");
            if (!library.BACnetStack_AddObject(deviceInstance, BACnetConstants.OBJECT_TYPE_ANALOG_INPUT, 1)) {
                throw new BACnetException("Failed to add analog input " + 1);
            }
            log("Analog input object added successfully");

            // Store property values in our business logic database
            log("Storing property values in business logic database...");

            String key = makePropertyKey(BACnetConstants.OBJECT_TYPE_ANALOG_INPUT, 1, BACnetConstants.PROPERTY_IDENTIFIER_PRESENT_VALUE);
            realValues.put(key, last_value);

            key = makePropertyKey(BACnetConstants.OBJECT_TYPE_ANALOG_INPUT, 1, BACnetConstants.PROPERTY_IDENTIFIER_OBJECT_NAME);
            stringValues.put(key, "Temperature Sensor");
            log("Stored object name: " + key + " = " + "Temperature Sensor");
            log("Analog input added successfully");

        } catch (Exception e) {
            log("Error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void tick() {
        try {
            library.BACnetStack_Tick();
            Thread.sleep(100);

            long now = System.currentTimeMillis() / 1000L; // seconds since epoch
            if(last_updated + 3 < now ) {
                last_updated = now;
                last_value += 1.1f;
                updateAnalogInputPresentValue(1, last_value);
            }


        } catch (Exception e) {
            log("Error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ========================================================================
    // Main Method - Application Entry Point
    // ========================================================================
    /**
     * Main application demonstrating complete business logic implementation
     */
    public static void main(String[] args) {
        log("=== Starting Simple BACnet Server Example ===");
        SimpleServerExample example = null;
        try {
            example = new SimpleServerExample();

            // Add shutdown hook for cleanup
            final SimpleServerExample finalExample = example;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log("Shutdown hook - cleaning up resources...");
                finalExample.cleanup();
            }));

            example.startup();

            // Main processing loop with business logic
            log("Starting main processing loop with real UDP communication...");
            while (true) {
                example.tick();
            }

        } catch (Exception e) {
            log("Error occurred: " + e.getMessage());
            e.printStackTrace();
            log("=== Application terminated with error ===");
        } finally {
            if (example != null) {
                example.cleanup();
            }
        }
    }
}
