//
//  RNSpatialConnect.m
//  RNSpatialConnect
//
//  Created by Frank Rowe on 2/13/17.
//  Copyright © 2017 Boundless Spatial. All rights reserved.
//

#import "RNSpatialConnect.h"
#import "RCTBridge.h"
#import "RCTEventDispatcher.h"
#import "RCTUIManager.h"

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


RCT_EXPORT_METHOD(bindMapView:(nonnull NSNumber *)reactTag)
{
  dispatch_async(RCTGetUIManagerQueue(), ^{
    [self.bridge.uiManager addUIBlock:^(__unused RCTUIManager *uiManager, NSDictionary<NSNumber *, UIView *> *viewRegistry) {
      id view = viewRegistry[reactTag];
      if (![view isKindOfClass:[AIRMap class]]) {
        RCTLogError(@"Invalid view returned from registry, expecting AIRMap, got: %@", view);
      } else {
        mapView = (AIRMap *)view;
      }
    }];
  });
}

RCT_EXPORT_METHOD(addRasterLayers:(NSArray *)storeIds)
{
  [mapView removeOverlays:mapView.overlays];
  NSArray *stores = [[[SpatialConnect sharedInstance] dataService] storesByProtocol:@protocol(SCRasterStore)];
  [[[[[[stores rac_sequence] signal] filter:^BOOL(SCDataStore *store) {
    return [storeIds containsObject:store.storeId] && [((id<SCRasterStore>)store).rasterLayers count] > 0;
  }] map:^RACTuple*(SCDataStore *store) {
    return [RACTuple tupleWithObjects:store.storeId, ((id<SCRasterStore>)store).rasterLayers, nil];
  }] deliverOn:[RACScheduler mainThreadScheduler]] subscribeNext:^(RACTuple *t) {
    id<SCRasterStore> rs =
    (id<SCRasterStore>)[[[SpatialConnect sharedInstance] dataService] storeByIdentifier:[t first]];
    for (id layer in [t second]) {
      [rs overlayFromLayer:layer mapview:(AIRMap *)mapView];
    }
  }];
}

RCT_EXPORT_METHOD(handler:(NSDictionary *)action)
{
  NSLog(@"action %@", action);
  [[bridgeAPI parseJSAction:action] subscribeNext:^(NSDictionary *payload) {
    NSDictionary *newAction =
    @{ @"type" : action[@"type"],
       @"payload" : payload };
    [self sendEvent:newAction status:SCJSSTATUS_NEXT];
  }
                                            error:^(NSError *error) {
                                              NSDictionary *newAction = @{
                                                                          @"type" : action[@"type"],
                                                                          @"payload" : [error localizedDescription]
                                                                          };
                                              [self sendEvent:newAction status:SCJSSTATUS_ERROR];
                                            }
                                        completed:^{
                                          NSDictionary *completed = @{ @"type" : action[@"type"] };
                                          [self sendEvent:completed status:SCJSSTATUS_COMPLETED];
                                        }];
}

-(void)sendEvent:(NSDictionary *)newAction status:(NSInteger)status {
  NSString *type = newAction[@"responseId"] != nil ? newAction[@"responseId"] : [newAction[@"type"] stringValue];
  if (status == SCJSSTATUS_COMPLETED) {
    type = [type stringByAppendingString:@"_completed"];
  }
  if (status == SCJSSTATUS_ERROR) {
    type = [type stringByAppendingString:@"_error"];
  }
  //NSLog(@"newAction %@", newAction);
  //NSLog(@"type %@", type);
  [self.bridge.eventDispatcher sendAppEventWithName:type body:newAction];
}


@end
