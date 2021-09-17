//
// Created by Administrator on 2021/9/15.
//

#include <jni.h>
#include <assert.h>

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    if (vm->GetEnv((void **) (&env), JNI_VERSION_1_6) != JNI_OK) {
        return -1;
    }
    assert(env != NULL);

//    if (!register_native_api(env)) {//注册接口
//        return -1;
//    }
    return JNI_VERSION_1_6;
}
