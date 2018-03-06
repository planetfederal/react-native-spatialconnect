# react-native-spatialconnect

react-native-spatialconnect is Javascript library used to integrate [SpatialConnect](https://github.com/boundlessgeo/spatialconnect) with your React Native applications. 

## Prerequisites 
For iOS, you need to have [Carthage](https://github.com/Carthage/Carthage) and [Xcode](https://developer.apple.com/xcode/) installed on your system. 

## Configuration & Installation

From the root directory of your React Native app, you can install by running:

```
npm install react-native-spatialconnect --save
```

> Note: this may take a few minutes to download and compile all the dependencies.

### iOS:
* Open your React Native iOS project in Xcode.
* Drag `RNSpatialConnect.xcodeproj` located in `<your-project>/node_modules/react-native-spatialconnect/ios`
  to the `Libraries` folder of your project in Xcode.
* In the `General` settings tab of your app under `Linked Frameworks and Libraries`, add `libRNSpatialConnect.a`.
* In `Build Settings`/`Search Paths`/`Framework Search Paths` add path: `$(SRCROOT)/../node_modules/react-native-spatialconnect/ios/Carthage/Build/iOS` for Any Architecture | Any SDK for Debug and Release
* In `Build Settings`/`Build Options`/`Always Embed Swift Standard Libraries` set to `Yes`.
* In `Build Phases` click on top left plus (+) button and add `New Run Script Phase`.
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

### Android:
* Modify `settings.gradle` located in `<your-project>/android` folder.
  * Add the following:
    * `include ':react-native-spatialconnect'`
    * `project(':react-native-spatialconnect').projectDir = new File(rootProject.projectDir,'../node_modules/react-native-spatialconnect/android')`
* Modify `build.gradle` located in `<your-project>/android/app` folder.
  * Add the following under the dependencies:
    * `compile project(':react-native-spatialconnect')`
