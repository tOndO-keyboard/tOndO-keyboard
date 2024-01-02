#import <Foundation/Foundation.h>
#import "NativeCallProxy.h"


@implementation FrameworkLibAPI

id<NativeCallsProtocol> api = NULL;
+(void) registerAPIforNativeCalls:(id<NativeCallsProtocol>) aApi
{
    api = aApi;
}

@end

// Converts C style string to NSString
NSString* CreateNSString (const char* string)
{
    if (string)
        return [NSString stringWithUTF8String: string];
    else
        return [NSString stringWithUTF8String: ""];
}

// Helper method to create C string copy
char* MakeStringCopy (const char* string)
{
    if (string == NULL)
        return NULL;
    
    char* res = (char*)malloc(strlen(string) + 1);
    strcpy(res, string);
    return res;
}

extern "C" {
    const char* _GetStartingData ()
    {
        // By default mono string marshaler creates .Net string for returned UTF-8 C string 
        // and calls free for returned value, thus returned strings should be allocated on heap
        return MakeStringCopy([[api getStartingData] UTF8String]);
    }
    
    void _SendEndMessage (const char* jsonMessage)
    {
        // By default mono string marshaler creates .Net string for returned UTF-8 C string 
        // and calls free for returned value, thus returned strings should be allocated on heap
        [api sendEndMessage : CreateNSString(jsonMessage)];
    }
}

