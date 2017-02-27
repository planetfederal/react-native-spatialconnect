//
//  RNSpatialConnect.h
//  RNSpatialConnect
//
//  Created by Frank Rowe on 2/13/17.
//  Copyright © 2017 Boundless Spatial. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "RCTBridgeModule.h"
#import <MapKit/MapKit.h>
#import <ReactiveCocoa/RACSignal.h>
#import <SpatialConnect/SCJavascriptBridgeAPI.h>
#import <SpatialConnect/SpatialConnect.h>

@interface RNSpatialConnect : NSObject <RCTBridgeModule> {
  SpatialConnect *sc;
  SCJavascriptBridgeAPI *bridgeAPI;
  MKMapView *mapView;
}

@end
