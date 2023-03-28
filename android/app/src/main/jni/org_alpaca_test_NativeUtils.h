#include <jni.h>

#ifndef _Included_org_alpaca_test_NativeUtils
#define _Included_org_alpaca_test_NativeUtils
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Method:    getCString
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jboolean JNICALL Java_org_alpaca_test_NativeUtils_loadModel
(JNIEnv*, jclass, jstring modelPath);

JNIEXPORT jstring JNICALL Java_org_alpaca_test_NativeUtils_getAnswer
(JNIEnv*, jclass, jstring question, jobject cb);

#ifdef __cplusplus
}
#endif
#endif
