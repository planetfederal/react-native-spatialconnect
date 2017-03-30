//
//  RNSpatialConnect.m
//  RNSpatialConnect
//
//  Created by Frank Rowe on 2/13/17.
//  Copyright © 2017 Boundless Spatial. All rights reserved.
//

#import <React/RNSpatialConnect.h>
#import <React/RCTBridge.h>
#import <React/RCTEventDispatcher.h>
#import <React/RCTUIManager.h>
#import <SpatialConnect/SCTileOverlay.h>

@implementation RNSpatialConnect

@synthesize bridge = _bridge;

- (id)init {
  self = [super init];
  bridgeAPI = [[SCJavascriptBridgeAPI alloc] init];
  sc = [SpatialConnect sharedInstance];
  return self;
}

RCT_EXPORT_MODULE();

RCT_EXPORT_METHOD(addConfigFilepath:(NSString *)p)
{
  NSString *cfgPath = [SCFileUtils filePathFromMainBundle:p];
  [sc.configService addConfigFilepath:cfgPath];
}


RCT_EXPORT_METHOD(bindMapView:(nonnull NSNumber *)reactTag callback:(RCTResponseSenderBlock)callback)
{
  dispatch_async(RCTGetUIManagerQueue(), ^{
    [self.bridge.uiManager addUIBlock:^(__unused RCTUIManager *uiManager, NSDictionary<NSNumber *, UIView *> *viewRegistry) {
      id view = viewRegistry[reactTag];
      if (![view isKindOfClass:[MKMapView class]]) {
        RCTLogError(@"Invalid view returned from registry, expecting MKMapView, got: %@", view);
        callback(@[@"Invalid view returned from registry, expecting MKMapView"]);
      } else {
        mapView = (MKMapView *)view;
        callback(@[[NSNull null], @"success"]);
      }
    }];
  });
}

RCT_EXPORT_METHOD(addRasterLayers:(NSArray *)storeIds)
{
  NSArray *overlays = [[[[mapView.overlays rac_sequence] signal] filter:^BOOL(id overlay) {
    return [overlay isKindOfClass:[SCTileOverlay class]];
  }] toArray];
  [mapView removeOverlays:overlays];
  NSArray *stores = [[[SpatialConnect sharedInstance] dataService] storesByProtocolArray:@protocol(SCRasterStore)];
  [[[[[stores rac_sequence] signal] filter:^BOOL(SCDataStore *store) {
    return [storeIds containsObject:store.storeId] && [((id<SCRasterStore>)store).rasterLayers count] > 0;
  }] deliverOn:[RACScheduler mainThreadScheduler]] subscribeNext:^(SCDataStore *store) {
    id<SCRasterStore> rs =
    (id<SCRasterStore>)[[[SpatialConnect sharedInstance] dataService] storeByIdentifier:store.storeId];
    for (id layer in rs.rasterLayers) {
      [rs overlayFromLayer:layer mapview:mapView];
    }
  }];
}

RCT_EXPORT_METHOD(handler:(NSDictionary *)action)
{
  NSString *type = action[@"responseId"] != nil ? action[@"responseId"] : [action[@"type"] stringValue];
  [[bridgeAPI parseJSAction:action] subscribeNext:^(NSDictionary *payload) {
    NSDictionary *newAction = @{
                                @"type" : action[@"type"],
                                @"payload" : payload
                                };
    [self sendEvent:newAction status:SCJSSTATUS_NEXT type:type];
  } error:^(NSError *error) {
    NSDictionary *newAction = @{
                                @"type" : action[@"type"],
                                @"payload" : [error localizedDescription]
                                };
    [self sendEvent:newAction status:SCJSSTATUS_ERROR type:type];
  } completed:^{
    NSDictionary *completed = @{
                                @"type" : action[@"type"]
                                };
    [self sendEvent:completed status:SCJSSTATUS_COMPLETED type:type];
  }];
}

-(void)sendEvent:(NSDictionary *)newAction status:(NSInteger)status type:(NSString *)type {
  if (status == SCJSSTATUS_COMPLETED) {
    type = [type stringByAppendingString:@"_completed"];
  }
  if (status == SCJSSTATUS_ERROR) {
    type = [type stringByAppendingString:@"_error"];
  }
  [self.bridge.eventDispatcher sendAppEventWithName:type body:newAction];
}


@end
