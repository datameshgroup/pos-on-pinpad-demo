# TerminalPOSDemo
This app demonstrates the fusion-sdk library for terminals.

Please reference the [Fusion SDK API](https://datameshgroup.github.io/fusion/#introduction) for structuring messages

### Overview

***

This app demonstrates how to send the following requests via intents to the Satellite app using the fusion-sdk library.
* Payment
* Transaction Status
* Refund
* Preauthorisation
* Completion
  
#### Other features of the demo

* Printing
* Barcode scanning


### Getting Started

***

##### This demo app is built and run using:
* Android Studio Chipmunk | 2021.2.1 Patch 2
* Ingenico (tested on DX8000)
* Pax (tested on PAX 920)

### Building the TerminalPOSDemo demo app
***

Clone the TerminalPOSDemo demo app
* `git clone https://github.com/datameshgroup/pos-on-pinpad-demo.git`

Choose Active Build Variant depending on the device of choice (PAX or Ingenico)

##### Configuration
In this demo app, the the POIID and SALEID is set up inside the `res/values/string.xml` file. The fields must be updated with the correct values before running the app.

### Dependencies

***

This project uses the following dependencies:

- **[Java Fusion SDK](https://github.com/datameshgroup/fusionsatellite-sdk-java):** contains all the models necessary to create request and response messages to the Fusion websocket server
  _This library is included in the project as a jar file under libs folder, and is implemented on the build.gradle_



### Minimum Required JDK

***

- Java 1.8

> **Note:** Other versions may work as well, but have not been tested.
