# react-native-spatialconnect

## Configuration & Installation

**iOS:**
* Make sure that you have [Carthage](https://github.com/Carthage/Carthage) installed on your system.
* Run `npm install react-native-spatialconnect --save`
* Open iOS project located in `./ios` folder.
* Move `RNSpatialConnect.xcodeproj` located in `.node_modules/react-native-spatialconnect/ios`
  using drag & drop to `Libraries` folder in your project.
* In general settings of a target add `libRNSpatialConnect.a` to Linked Frameworks and Libraries.
* In `Build Settings`/`Search Paths`/`Framework search paths` add path: `$(SRCROOT)/../node_modules/react-native-spatialconnect/ios/Carthage/Build/iOS`.
* In `Build Settings`/`Build Options`/`Always Embed Swift Standard Libraries` set to `Yes`.
* In `Build Phases` click on top left button and add `New Run Script Phase`.
  * Shell command: `/usr/local/bin/carthage copy-frameworks`
  * Input Files:
    * `$(SRCROOT)/../node_modules/react-native-spatialconnect/ios/Carthage/Build/iOS/SpatialConnect.framework`
    * `$(SRCROOT)/../node_modules/react-native-spatialconnect/ios/Carthage/Build/iOS/ReactiveCocoa.framework`
    * `$(SRCROOT)/../node_modules/react-native-spatialconnect/ios/Carthage/Build/iOS/wkb_ios.framework`
    * `$(SRCROOT)/../node_modules/react-native-spatialconnect/ios/Carthage/Build/iOS/JWT.framework`
    * `$(SRCROOT)/../node_modules/react-native-spatialconnect/ios/Carthage/Build/iOS/libgpkgios.framework`
    * `$(SRCROOT)/../node_modules/react-native-spatialconnect/ios/Carthage/Build/iOS/MQTTFramework.framework`
    * `$(SRCROOT)/../node_modules/react-native-spatialconnect/ios/Carthage/Build/iOS/proj4.framework`
    * `$(SRCROOT)/../node_modules/react-native-spatialconnect/ios/Carthage/Build/iOS/ZipZap.framework`
    * `$(SRCROOT)/../node_modules/react-native-spatialconnect/ios/Carthage/Build/iOS/CocoaLumberjack.framework`
* Minimal supported version of iOS is 8.0

**Android:**