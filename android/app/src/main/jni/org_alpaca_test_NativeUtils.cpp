#include <jni.h>
#include "Logger.h"
#include "org_alpaca_test_NativeUtils.h"
#include "lib.h"

#define TAG "Alpaca-NativeUtils"

std::shared_ptr<ChatBot> gChatBot;

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jboolean JNICALL Java_org_alpaca_test_NativeUtils_loadModel(
        JNIEnv* env, jclass obj, jstring modelPath)
{
    gpt_params params;
    params.temp = 0.1f;
    params.top_p = 0.95f;
    params.n_ctx = 128;
    ALOGD(TAG, "params.n_threads=%d", params.n_threads);
    jboolean  isCopy;
    const char* utfStr = env->GetStringUTFChars(modelPath, &isCopy);
    params.model = utfStr;
    if (isCopy) {
        env->ReleaseStringUTFChars(modelPath, utfStr);
    }
    gChatBot = std::make_shared<ChatBot>(params);
    if(gChatBot->status() == ST_FAILED) {
        return JNI_FALSE;
    }
    if (gChatBot->load_model() != ST_OK) {
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

jstring utf8_decode(JNIEnv* env, const std::string& str) {
    const char* cstr = str.c_str();
    jobject byteBuf = env->NewDirectByteBuffer((void *) cstr, strlen(cstr));
    jclass cls_Charset = env->FindClass("java/nio/charset/Charset");
    jmethodID mid_Charset_forName = env->GetStaticMethodID(cls_Charset, "forName", "(Ljava/lang/String;)Ljava/nio/charset/Charset;");
    jobject charset = env->CallStaticObjectMethod(cls_Charset, mid_Charset_forName, env->NewStringUTF("UTF-8"));
    if (env->ExceptionCheck()) {
        env->ExceptionClear();
        return nullptr;
    }

    jmethodID mid_Charset_decode = env->GetMethodID(cls_Charset, "decode", "(Ljava/nio/ByteBuffer;)Ljava/nio/CharBuffer;");
    jobject charBuf = env->CallObjectMethod(charset, mid_Charset_decode, byteBuf);
    env->DeleteLocalRef(byteBuf);
    if (env->ExceptionCheck()) {
        env->ExceptionClear();
        return nullptr;
    }

    jclass cls_CharBuffer = env->FindClass("java/nio/CharBuffer");
    jmethodID mid_CharBuffer_toString = env->GetMethodID(cls_CharBuffer, "toString", "()Ljava/lang/String;");
    jstring jstr = static_cast<jstring>(env->CallObjectMethod(charBuf, mid_CharBuffer_toString));
    if (env->ExceptionCheck()) {
        env->ExceptionClear();
        return nullptr;
    }
    return jstr;
}

JNIEXPORT jstring JNICALL Java_org_alpaca_test_NativeUtils_getAnswer
        (JNIEnv* env, jclass obj, jstring question, jobject jniCb)
{
    std::function<void(const std::string&, const BotStatus&)> cb = nullptr;
    if (jniCb != nullptr) {
        jclass objClass = env->GetObjectClass(jniCb);//onEmit(String newStr, int status)
        jmethodID onEmitId = env->GetMethodID(objClass, "onEmit", "(Ljava/lang/String;I)V");
        cb = [&](const std::string& str, const BotStatus& st) {
            if (env->ExceptionOccurred()) {
                return;
            }
            jstring newStr = utf8_decode(env, str);
            env->CallVoidMethod(jniCb, onEmitId, newStr, (jint)st);
        };
    }
    jboolean  isCopy;
    const char* utfStr = env->GetStringUTFChars(question, &isCopy);
    auto q = std::string(utfStr);
    if (isCopy) {
        env->ReleaseStringUTFChars(question, utfStr);
    }
    BotStatus st;
    auto answer = gChatBot->get_answer(q, st, cb);
    jstring ret;
    if (st == ST_OK) {
        ret = utf8_decode(env, answer);
    } else {
        char buf[32];
        sprintf(buf, "error: %d", st);
        ret = env->NewStringUTF(buf);
    }
    return ret;
}

#ifdef __cplusplus
}
#endif
