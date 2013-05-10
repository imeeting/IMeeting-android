/*
 * jni_util.c
 *
 *  Created on: 2012-10-29
 *      Author: star
 */
#include "../common.h"
#include <jni.h>

jint get_int_field(JNIEnv* env, jobject thiz, const char *field_name) {
	jclass c = (*env)->GetObjectClass(env, thiz);
	jfieldID fid = (*env)->GetFieldID(env, c, field_name, "I");
	if (fid != NULL) {
		jint val = (*env)->GetIntField(env, thiz, fid);
		return val;
	} else {
		return NULL;
	}
}

void set_int_field(JNIEnv* env, jobject thiz, const char *field_name, jint val) {
	jclass c = (*env)->GetObjectClass(env, thiz);
	jfieldID fid = (*env)->GetFieldID(env, c, field_name, "I");
	if (fid != NULL) {
		(*env)->SetIntField(env, thiz, fid, val);
	}
}

jstring get_string_field(JNIEnv* env, jobject thiz, const char *field_name) {
	jclass c = (*env)->GetObjectClass(env, thiz);
	jfieldID fid = (*env)->GetFieldID(env, c, field_name, "Ljava/lang/String;");
	if (fid != NULL) {
		jobject val = (*env)->GetObjectField(env, thiz, fid);
		return val;
	} else {
		return NULL;
	}
}

jobject get_object_field(JNIEnv* env, jobject thiz, const char *field_name,
		const char *field_sig) {
	jclass c = (*env)->GetObjectClass(env, thiz);
	jfieldID fid = (*env)->GetFieldID(env, c, field_name, field_sig);
	if (fid != NULL) {
		jobject obj = (*env)->GetObjectField(env, thiz, fid);
		return obj;
	} else {
		return NULL;
	}
}

void call_void_method(JNIEnv* env, jobject thiz, const char *method_name) {
	jclass clazz = (*env)->GetObjectClass(env, thiz);
	jmethodID mid = (*env)->GetMethodID(env, clazz, method_name, "()V");
	if (mid != NULL) {
		(*env)->CallVoidMethod(env, thiz, mid);
	}
}
