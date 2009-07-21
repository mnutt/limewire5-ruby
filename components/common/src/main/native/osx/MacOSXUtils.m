//
//  MacOSXUtils.m
//  GURL
//
//  Created by Curtis Jones on 2008.04.08.
//  Copyright 2008 __MyCompanyName__. All rights reserved.
//

#import <JavaVM/jni.h>
#import <Foundation/Foundation.h>

#ifdef __cplusplus
extern "C" {
#endif

#define OS_NATIVE(func) Java_org_limewire_ui_swing_util_MacOSXUtils_##func

JNIEXPORT jstring JNICALL OS_NATIVE(GetCurrentFullUserName)
	(JNIEnv *env, jobject clazz)
{
	NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
	NSString *usernameNSString = NSFullUserName();
	jstring usernameJString = (*env)->NewStringUTF(env, [usernameNSString UTF8String]);
	[pool release];
	
	return usernameJString;
}

JNIEXPORT void JNICALL OS_NATIVE(SetLoginStatusNative)
	(JNIEnv *env, jobject obj, jboolean onoff)
{
	NSMutableArray *loginItems;
	NSDictionary *appDict;
	NSEnumerator *appEnum;
	NSString *agentAppPath = @"/Applications/LimeWire.app";
	
	// Make a mutable copy (why a copy?)
	loginItems = (NSMutableArray *)CFPreferencesCopyValue((CFStringRef)@"AutoLaunchedApplicationDictionary", 
																												(CFStringRef)@"loginwindow", kCFPreferencesCurrentUser, 
																												kCFPreferencesAnyHost);
	loginItems = [[loginItems autorelease] mutableCopy];
	appEnum = [loginItems objectEnumerator];
	
	while ((appDict = [appEnum nextObject])) {
		if ([[[appDict objectForKey:@"Path"] stringByExpandingTildeInPath] isEqualToString:agentAppPath])
			break;
	}
	
	// register the item
	if (onoff == JNI_TRUE) {
		if (!appDict)
			[loginItems addObject:[NSDictionary dictionaryWithObjectsAndKeys:[NSNumber numberWithBool:NO], @"Hide", agentAppPath, @"Path", nil]];
	}
	// unregister the item
	else if (appDict)
		[loginItems removeObject:appDict];
	
	CFPreferencesSetValue((CFStringRef)@"AutoLaunchedApplicationDictionary", 
												loginItems, (CFStringRef)@"loginwindow", 
												kCFPreferencesCurrentUser, kCFPreferencesAnyHost);
	
	CFPreferencesSynchronize((CFStringRef)@"loginwindow", kCFPreferencesCurrentUser, kCFPreferencesAnyHost);
	
	[loginItems release];
}

#ifdef __cplusplus
}
#endif
