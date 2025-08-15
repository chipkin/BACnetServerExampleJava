# CAS BACnet Stack Java Example

A basic BACnet IP server example written with Java using the [CAS BACnet Stack](https://store.chipkin.com/services/stacks/bacnet-stack)

- Device: 389001 (Device Rainbow)
  - Aanalog_input: 0 (AnalogInput Bronze)

## Architecture

This adapter follows the recommended separation pattern:

- **CASBACnetStackAdapter.java** - Pure interface wrapper around the native library.
- **[SimpleServerExample.java](/src/main/java/com/chipkin/bacnet/SimpleServerExample.java)** - The application's business logic implementation showing how to use the interface wrapper.

## Quick Start for Customers

### 1. Copy Interface Files to Your Project

Copy these files to your project for the interface wrapper:

```txt
CASBACnetStackAdapter.java
ICASBACnetStackLibrary.java
BACnetConstants.java  
BACnetException.java
```

### 2. Implement Your Business Logic

Reference `SimpleServerExample.java` for patterns on how to:

- Initialize devices and add objects
- Implement callback handlers for property requests
- Handle message sending/receiving
- Store and manage property values

### 3. Basic Usage Pattern

```java
// Create the interface wrapper
CASBACnetStackAdapter adapter = new CASBACnetStackAdapter();

// Get direct access to the library
ICASBACnetStackLibrary library = adapter.getLibrary();

// Use the library functions directly
library.BACnetStack_AddDevice(12345);
library.BACnetStack_AddObject(12345, BACnetConstants.OBJECT_TYPE_ANALOG_INPUT, 1);

// Register your own callbacks
library.BACnetStack_RegisterCallbackGetPropertyReal(yourCallback);

// Main processing loop
while (true) {
    library.BACnetStack_Tick();
    Thread.sleep(100);
}
```

## Build Instructions

### Using Maven

```bash
mvn clean compile
mvn mvn exec:java"
```

## Requirements

- **Java 8 or higher**
- **CAS BACnet Stack native library** (DLL/SO file)
- **Maven** build system

## Native Library Loading

The interface wrapper automatically detects your platform and loads the appropriate library:

- **Windows x64**: `CASBACnetStack_x64_Release.dll`
- **Windows x86**: `CASBACnetStack_x86_Release.dll`  
- **Linux x64**: `libCASBACnetStack_x64_Release.so`
- **Linux ARM**: `libCASBACnetStack_arm7_Release.so`
- **Linux x86**: `libCASBACnetStack_x86_Release.so`

Make sure the library is in your system PATH or `java.library.path`.

## Testing

1) Chipkin BACnet Explorer

Windows or Linux desktop application for discovering and polling BACnet servers.

Download for free [Chipkin BACnet Explorer](https://casbacnetexplorer.chipkin.com/)

2) Test from another computer:

```bash
# Send Who-Is to discover the Java BACnet server
echo "810400080120FFFF00FF" | xxd -r -p | nc -u <server-ip> 47808
```

## Related Examples

For more advanced implementations, see these successful adapter examples:

- [Python BACnet Server Example](https://github.com/chipkin/BACnetServerExamplePython)
- [C# BACnet Server Example](https://github.com/chipkin/BACnetServerExampleCSharp)  
- [Go BACnet Server Example](https://github.com/chipkin/BACnetServerExampleGolang)
- [TypeScript BACnet Server Example](https://github.com/chipkin/BACnetServerExampleTypeScript)
- [Rust BACnet Server Example](https://github.com/chipkin/BACnetServerExampleRUST)
