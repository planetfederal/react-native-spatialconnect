/**
 * Copyright 2015-2017 Boundless, http://boundlessgeo.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License
 */
package com.boundlessgeo.spatialconnect.jsbridge;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.Log;

import com.boundlessgeo.spatialconnect.SpatialConnect;
import com.boundlessgeo.spatialconnect.jsbridge.SCJavascriptCommands.SCBridgeStatus;
import com.boundlessgeo.spatialconnect.stores.GeoPackageStore;
import com.boundlessgeo.spatialconnect.stores.SCDataStore;
import com.boundlessgeo.spatialconnect.stores.SCRasterStore;
import com.boundlessgeo.spatialconnect.tiles.GpkgRasterSource;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.uimanager.NativeViewHierarchyManager;
import com.facebook.react.uimanager.UIBlock;
import com.facebook.react.uimanager.UIManagerModule;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.TileOverlay;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rx.Subscriber;

/**
 * This module handles messages sent from Javascript.
 */
public class RNSpatialConnect extends ReactContextBaseJavaModule {

    private static final String LOG_TAG = RNSpatialConnect.class.getSimpleName();
    private SCJavascriptBridgeAPI mBridgeAPI;
    private final ReactContext reactContext;
    private GoogleMap mGoogleMap;
    private HashMap<String, List<TileOverlay>> tileoverlays = new HashMap<>();

    public RNSpatialConnect(ReactApplicationContext reactContext) {
        super(reactContext);
        mBridgeAPI = new SCJavascriptBridgeAPI(reactContext.getApplicationContext());
        this.reactContext = reactContext;
        // todo: get GoogleMap instance from react native
    }

    @Override
    public String getName() {
        return "RNSpatialConnect";
    }

    @ReactMethod
    public void addConfigFilepath(final String path) {
        try {
            Resources resources = getReactApplicationContext().getResources();
            //remove ext from file name
            String fileName = path.substring(0, path.indexOf("."));
            InputStream is = resources.openRawResource(resources.getIdentifier(fileName,
                    "raw", getReactApplicationContext().getPackageName()));
            // write the file to the internal storage location
            FileOutputStream fos = reactContext.openFileOutput("config.scfg", Context.MODE_PRIVATE);
            byte[] data = new byte[is.available()];
            is.read(data);
            fos.write(data);
            is.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(LOG_TAG, "error in addConfigFilepath " + e.getMessage());
        }
    }

    @ReactMethod
    public void bindMapView(final int tag, final Callback successCallback) {
        UIManagerModule uiManager = this.reactContext.getNativeModule(UIManagerModule.class);
        uiManager.addUIBlock(new UIBlock() {
            public void execute(NativeViewHierarchyManager nvhm) {
                MapView mapView = (MapView) nvhm.resolveView(tag);
                //mapView.getMapAsync(this);
                mapView.getMapAsync(new OnMapReadyCallback() {
                    @Override
                    public void onMapReady(final GoogleMap googleMap) {
                        mGoogleMap = googleMap;
                        successCallback.invoke(false, "success");
                    }
                });
            }
        });
    }

    @ReactMethod
    public void addRasterLayers(final ReadableArray storeIdArray) {
        UIManagerModule uiManager = this.reactContext.getNativeModule(UIManagerModule.class);
        uiManager.addUIBlock(new UIBlock() {
            public void execute(NativeViewHierarchyManager nvhm) {
                List<String> storeIds = new ArrayList<>(storeIdArray.size());
                for (int index = 0; index < storeIdArray.size(); index++) {
                    storeIds.add(storeIdArray.getString(index));
                }
                List<SCDataStore> stores = SpatialConnect.getInstance().getDataService().getActiveStores();
                for (SCDataStore store : stores) {
                    List<TileOverlay> tiles = tileoverlays.get(store.getStoreId());
                    if (storeIds.contains(store.getStoreId())) {
                        if (tiles == null) {
                            if (store instanceof GeoPackageStore && ((GeoPackageStore) store).getTileSources().size() > 0) {
                                SCRasterStore rs = new GpkgRasterSource((GeoPackageStore) store);
                                List<TileOverlay> newTiles = new ArrayList<TileOverlay>();
                                for (String tableName : rs.rasterLayers()) {
                                    TileOverlay t = rs.overlayFromLayer(tableName, mGoogleMap);
                                    newTiles.add(t);
                                }
                                tileoverlays.put(store.getStoreId(), newTiles);
                            }
                        }
                    } else {
                        if (tiles != null) {
                            for (TileOverlay tile : tiles) {
                                tile.remove();
                            }
                            tileoverlays.remove(store.getStoreId());
                        }
                    }
                }
            }
        });
    }

    /**
     * Handles a message sent from Javascript.  Expects the message envelope to look like:
     * <code>{"type":<String>,"payload":<JSON Object>}</code>
     *
     * @param message message from javascript
     */
    @ReactMethod
    public void handler(final ReadableMap message) {
        if (message == null) {
            Log.w(LOG_TAG, "data message was null or undefined");
            return;
        }
        Log.d(LOG_TAG, "JS --> sdk " + message.toString());

        String responseId = message.hasKey("responseId") ? message.getString("responseId") : null;
        final String type = !TextUtils.isEmpty(responseId) ? responseId : message.getString("type");

        Subscriber<Object> subscriber = new Subscriber<Object>() {
            @Override
            public void onNext(Object object) {
                WritableMap newAction = Arguments.createMap();
                newAction.putString("type", message.getString("type"));
                writePayloadToMap(newAction, object);

                sendEvent(newAction, SCBridgeStatus.NEXT, type);
            }

            @Override
            public void onError(Throwable e) {
                WritableMap newAction = Arguments.createMap();
                newAction.putString("type", message.getString("type"));
                newAction.putString("payload", e.getLocalizedMessage());

                sendEvent(newAction, SCBridgeStatus.ERROR, type);
            }

            @Override
            public void onCompleted() {
                WritableMap completed = Arguments.createMap();
                completed.putString("type", message.getString("type"));

                sendEvent(completed, SCBridgeStatus.COMPLETED, type);
            }
        };

        mBridgeAPI.parseJSAction(convertMapToHashMap(message), subscriber);
    }

    private void sendEvent(WritableMap newAction, SCBridgeStatus status, String type) {
        if (status == SCBridgeStatus.COMPLETED) {
            type = type + "_completed";
        } else if (status == SCBridgeStatus.ERROR) {
            type = type + "_error";
        }

        Log.v(LOG_TAG, "JS --> sdk: " + type + " :: " + newAction.toString());

        reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(type, newAction);
    }

    private WritableMap writePayloadToMap(WritableMap response, Object value) {
        if (value instanceof Boolean) {
            response.putBoolean("payload", (Boolean) value);
        } else if (value instanceof Integer) {
            response.putInt("payload", (Integer) value);
        } else if (value instanceof String) {
            response.putString("payload", (String) value);
        } else if (value instanceof HashMap) {
            // if we upgrade react, we can just `writeMap` instead of having to convert it
            response.putMap("payload", convertHashMapToMap((Map<String, Object>) value));
        }

        return response;
    }

    private HashMap<String, Object> convertMapToHashMap(ReadableMap readableMap) {
        HashMap<String, Object> json = new HashMap<>();
        ReadableMapKeySetIterator iterator = readableMap.keySetIterator();

        while (iterator.hasNextKey()) {
            String key = iterator.nextKey();
            switch (readableMap.getType(key)) {
                case Null:
                    json.put(key, null);
                    break;
                case Boolean:
                    json.put(key, readableMap.getBoolean(key));
                    break;
                case Number:
                    json.put(key, readableMap.getDouble(key));
                    break;
                case String:
                    json.put(key, readableMap.getString(key));
                    break;
                case Map:
                    json.put(key, convertMapToHashMap(readableMap.getMap(key)));
                    break;
                case Array:
                    json.put(key, convertArrayToArrayList(readableMap.getArray(key)));
                    break;
            }
        }

        return json;
    }

    private ArrayList<Object> convertArrayToArrayList(ReadableArray readableArray) {
        ArrayList<Object> jsonArray = new ArrayList<>();
        for (int i = 0; i < readableArray.size(); i++) {
            switch (readableArray.getType(i)) {
                case Null:
                    break;
                case Boolean:
                    jsonArray.add(readableArray.getBoolean(i));
                    break;
                case Number:
                    jsonArray.add(readableArray.getDouble(i));
                    break;
                case String:
                    jsonArray.add(readableArray.getString(i));
                    break;
                case Map:
                    jsonArray.add(convertMapToHashMap(readableArray.getMap(i)));
                    break;
                case Array:
                    jsonArray.add(convertArrayToArrayList(readableArray.getArray(i)));
                    break;
            }
        }

        return jsonArray;
    }

    private WritableMap convertHashMapToMap(Map<String, Object> hashMap) {
        WritableMap writableMap = Arguments.createMap();

        for (Map.Entry<String, Object> entry : hashMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value == null) {
                writableMap.putNull(key);
            } else if (value instanceof Boolean) {
                writableMap.putBoolean(key, (Boolean) value);
            } else if (value instanceof Double) {
                writableMap.putDouble(key, (Double) value);
            } else if (value instanceof Integer) {
                writableMap.putInt(key, (Integer) value);
            } else if (value instanceof String) {
                writableMap.putString(key, (String) value);
            } else if (value instanceof Map) {
                writableMap.putMap(key, convertHashMapToMap((Map) value));
            } else if (value instanceof List) {
                writableMap.putArray(key, convertArrayToArrayList((List) value));
            }
        }

        return writableMap;
    }

    private WritableArray convertArrayToArrayList(List list) {
        WritableArray writableArray = Arguments.createArray();

        if (list.size() < 1) {
            return writableArray;
        }

        Object firstObject = list.get(0);
        if (firstObject == null) {
            for (int i = 0; i < list.size(); i++) {
                writableArray.pushNull();
            }
        } else if (firstObject instanceof Boolean) {
            for (Object object : list) {
                writableArray.pushBoolean((boolean) object);
            }
        } else if (firstObject instanceof Double) {
            for (Object object : list) {
                writableArray.pushDouble((double) object);
            }
        } else if (firstObject instanceof Integer) {
            for (Object object : list) {
                writableArray.pushInt((int) object);
            }
        } else if (firstObject instanceof String) {
            for (Object object : list) {
                writableArray.pushString((String) object);
            }
        } else if (firstObject instanceof Map) {
            for (Object object : list) {
                writableArray.pushMap(convertHashMapToMap((Map) object));
            }
        } else if (firstObject instanceof List) {
            for (Object object : list) {
                writableArray.pushArray(convertArrayToArrayList((List) object));
            }
        }

        return writableArray;
    }
}
