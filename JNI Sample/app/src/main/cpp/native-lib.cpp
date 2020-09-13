#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_me_tommy_jnisample_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_me_tommy_jnisample_MainActivity_stringFromJNI2(
        JNIEnv* env, jobject
        ){
    std::string  hello = "Hello World";
    return env->NewStringUTF(hello.c_str());
}