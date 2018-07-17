# react-native-spatialconnect

react-native-spatialconnect is Javascript library used to integrate SpatialConnect with your React Native applications. 

## Prerequisites 
For iOS, you need to have [Carthage](https://github.com/Carthage/Carthage) and [Xcode](https://developer.apple.com/xcode/) installed on your system. 

## Configuration & Installation

From the root directory of your React Native app, you can install by running:

```
npm install react-native-spatialconnect --save
```

> Note: this may take a few minutes to download and compile all the dependencies.

**iOS:**
* Open your React Native iOS project in Xcode.
* Drag `RNSpatialConnect.xcodeproj` located in `.node_modules/react-native-spatialconnect/ios`
  to the `Libraries` folder of your project in Xcode.
* In the `General` settings tab of your app under `Linked Frameworks and Libraries`, add `libRNSpatialConnect.a`.
* In `Build Settings`/`Search Paths`/`Framework search paths` add path: `$(SRCROOT)/../node_modules/react-native-spatialconnect/ios/Carthage/Build/iOS`.
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

**Android:**
* Modify `settings.gradle` located in `./android` folder.
  * Add the following:
    * `include ':react-native-spatialconnect'`
    * `project(':react-native-spatialconnect').projectDir = new File(rootProject.projectDir,'../node_modules/react-native-spatialconnect/android')`
* Modify `build.gradle` located in `./android/app` folder.
  * Add the following under the dependencies:
    * `compile project(':react-native-spatialconnect')`

## Usage

This native modules relies on a config files to define forms and data stores locally or from a remote server.  The config file are defined in the native folders of your project.  For iOS `<your-project-name>/ios/config.scfg`, for android `<your-project-name>/android/app/src/main/res/raw/config.scfg`

Example of a .scfg file that point to a remote server for authentication and defintion of forms and stores.
```
{
  "remote": {
    "auth": "server",
    "http_protocol": "http",
    "http_host": "efc-dev.boundlessgeo.com",
    "http_port": 8085,
    "mqtt_protocol": "tcp",
    "mqtt_host": "efc-dev.boundlessgeo.com",
    "mqtt_port": 1883
  }
}
``` 

Example of a .scfg file that defines its datastores (geopackage and geojson) and a form without using a remote server.
```
{
  "stores":[
    {
      "store_type": "geojson",
      "version": "1",
      "uri": "https://s3.amazonaws.com/test.spacon/bars.geojson",
      "id":"3dc5afc9-393b-444c-8581-582e2c2d98a3",
      "name":"bars",
      "default_layers": [ ],
      "style": [
        {
            "id":"default",
            "paint":
                {
                    "fill-color":"#34fb71",
                    "fill-opacity":0.3,
                    "line-color":"#4357fb",
                    "line-opacity":1,
                    "icon-color":"#3371fb"
                }
        }]
    },
    {
      "id": "c96c0155-31b3-434a-8171-beb36fb24512",
      "store_type": "gpkg",
      "version": "1",
      "uri": "https://s3.amazonaws.com/test.spacon/haiti4mobile.gpkg",
      "name": "Haiti",
      "default_layers": [ ]
    }
  ],
  "forms":[
    {
      "id":2,
      "version":0,
      "form_key":"baseball_team",
      "form_label":"Baseball Team",
      "fields":[
        {
          "id":13,
          "type":"string",
          "field_label":"Favorite?",
          "field_key":"team","position":0
        },
        {
          "id":14,
          "type":"string",
          "field_label":"Why?",
          "field_key":"why",
          "position":1
        }
      ]
    }
  ]
}
```

**Import spatialconnect for usage in your application**

```
import * as sc from 'react-native-spatialconnect';
```

**Initialize spatialconnect library**
```
sc.startAllServices();
```

**Grab the form(s) from the spatialconnect**

```
sc.forms$()..subscribe(
  (formsArray) => {
    setState({forms:formsArray});
  }
);
```

**Display list of defined datastores**
```
sc.stores$().subscribe(
  (storesArray) => {
    setState({stores:storesArray});
  }
);
```

**Create a feature for the form store**
```
const gj = {
  geometry: {
    type: 'Point',
    coordinates: [
      position.coords.longitude,
      position.coords.latitude,
    ],
  },
  properties: {
    team: 'foo', why: 'bar'
  },
};
const f = sc.geometry('FORM_STORE', formInfo.form_key, gj);
sc.createFeature$(f).first().subscribe(this.formSubmitted.bind(this));
```

**Query for features**
```
const bbox = [-180, -90, 180, 90];
const limit = 50
const filter = sc.filter().geoBBOXContains(bbox).limit(limit);
sc.spatialQuery$(filter)
  .bufferWithTime(1000)
  .take(5)
  .subscribe((data) => {
    //do something with the data
  });
```

**Update feature**
```
let modifiedFeature = {
  geometry: {
    type: 'Point',
    coordinates: [
      position.coords.longitude,
      position.coords.latitude,
    ],
  },
  properties: {
    team: 'foo', why: 'bar'
  },
};
sc.updateFeature$(modifiedFeature);
```

**Delete feature**
```
sc.deleteFeature(feature.id);
```

**Enable location tracking**
```
if (Platform.OS === 'android' && Platform.Version >= 23) {
  try {
    const granted = PermissionsAndroid.requestPermission(
      PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION, {
        title: 'GPS permission',
        message: 'EFC needs access to your GPS',
      },
    );
    if (granted) {
      sc.enableGPS();
    }
  } catch (err) {
    console.warn(err);
  }
} else {
  sc.enableGPS();
}
```

**Get last known location**
```
sc.lastKnownLocation().subscribe(
  (loc) => { console.log(loc); }
);
```



