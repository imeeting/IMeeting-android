/*
 * video_encoder.c
 *
 *  Created on: 2012-10-19
 *      Author: star
 */
#include <string.h>
#include <jni.h>
#include "../common.h"
#include "quicklibav.h"
#include "libswscale/swscale.h"

static char rtmp_url[200];
static char live_name[20];
static char group_id[20];
static jint out_img_width = 144;
static jint out_img_height = 192;

static QuickVideoOutput *qvo = NULL;
static AVFrame *raw_picture = NULL;
static AVFrame *tmp_picture = NULL;
static struct SwsContext *img_convert_ctx = NULL;
static enum PixelFormat src_pix_fmt;
static int is_video_encode_ready = 0; // boolean flag

void Java_com_richitec_imeeting_video_ECVideoEncoder_setRtmpUrl(JNIEnv* env,
		jobject thiz, jstring rtmpUrl) {
	const char *str = (*env)->GetStringUTFChars(env, rtmpUrl, 0);
	memset(rtmp_url, 0, sizeof rtmp_url);
	strcpy(rtmp_url, str);
	(*env)->ReleaseStringUTFChars(env, rtmpUrl, str);
}

void Java_com_richitec_imeeting_video_ECVideoEncoder_setLiveName(JNIEnv* env,
		jobject thiz, jstring name) {
	const char *str = (*env)->GetStringUTFChars(env, name, 0);
	memset(live_name, 0, sizeof live_name);
	strcpy(live_name, str);
	(*env)->ReleaseStringUTFChars(env, name, str);
}

void Java_com_richitec_imeeting_video_ECVideoEncoder_setGroupId(JNIEnv* env,
		jobject thiz, jstring groupId) {
	const char *str = (*env)->GetStringUTFChars(env, groupId, 0);
	memset(group_id, 0, sizeof group_id);
	strcpy(group_id, str);
	(*env)->ReleaseStringUTFChars(env, groupId, str);
}

void Java_com_richitec_imeeting_video_ECVideoEncoder_setOutImgWidth(JNIEnv* env,
		jobject thiz, jint outImgWidth) {
	out_img_width = outImgWidth;
}

void Java_com_richitec_imeeting_video_ECVideoEncoder_setOutImgHeight(
		JNIEnv* env, jobject thiz, jint outImgHeight) {
	out_img_height = outImgHeight;
}

void Java_com_richitec_imeeting_video_ECVideoEncoder_setupVideoEncoder(
		JNIEnv* env, jobject thiz) {
	qvo = (QuickVideoOutput*) malloc(sizeof(QuickVideoOutput));
	qvo->width = out_img_width;
	qvo->height = out_img_height;

	char rtmp_full_path[300];
	memset(rtmp_full_path, 0, sizeof rtmp_full_path);
	sprintf(rtmp_full_path, "%s/%s/%s live=1 conn=S:%s", rtmp_url, group_id, live_name, live_name);
	D("rtmp full path: %s", rtmp_full_path);

	int ret = init_quick_video_output(qvo, rtmp_full_path, "flv");
	if (ret < 0) {
		D("quick video output initial failed.");
		Java_com_richitec_imeeting_video_ECVideoEncoder_releaseVideoEncoder(env, thiz);
		return;
	}

	enum PixelFormat dst_pix_fmt = qvo->video_stream->codec->pix_fmt;
	src_pix_fmt = PIX_FMT_NV21;

	raw_picture = alloc_picture(dst_pix_fmt, qvo->width, qvo->height);
	tmp_picture = avcodec_alloc_frame();
	raw_picture->pts = 0;

	is_video_encode_ready = 1;
}

void Java_com_richitec_imeeting_video_ECVideoEncoder_releaseVideoEncoder(
		JNIEnv* env, jobject thiz) {
	is_video_encode_ready = 0;

	if (qvo) {
		close_quick_video_ouput(qvo);
		free(qvo);
		qvo = NULL;
	}

	if (raw_picture) {
		if (raw_picture->data[0]) {
			av_free(raw_picture->data[0]);
		}
		av_free(raw_picture);
		raw_picture = NULL;
	}

	if (tmp_picture) {
		av_free(tmp_picture);
		tmp_picture = NULL;
	}
}

void Java_com_richitec_imeeting_video_ECVideoEncoder_processRawFrame(
		JNIEnv* env, jobject thiz, jbyteArray buffer, jint width, jint height) {
	if (!qvo || !is_video_encode_ready) {
		return;
	}

	D("process raw frame - width: %d height: %d", width, height);

	AVCodecContext *c = qvo->video_stream->codec;

	jbyte *p_buffer_array = (*env)->GetByteArrayElements(env, buffer, 0);
	avpicture_fill((AVPicture *)tmp_picture, p_buffer_array, src_pix_fmt, width, height);
	(*env)->ReleaseByteArrayElements(env, buffer, p_buffer_array, JNI_ABORT);

	img_convert_ctx = sws_getCachedContext(img_convert_ctx, width, height, src_pix_fmt, qvo->width, qvo->height, c->pix_fmt, SWS_BILINEAR, NULL, NULL, NULL);

	sws_scale(img_convert_ctx, tmp_picture->data, tmp_picture->linesize, 0, height, raw_picture->data, raw_picture->linesize);

	int out_size = write_video_frame(qvo, raw_picture);

	D("stream pts val: %lld time base: %d / %d", qvo->video_stream->pts.val, qvo->video_stream->time_base.num, qvo->video_stream->time_base.den);
	double video_pts = (double)qvo->video_stream->pts.val * qvo->video_stream->time_base.num / qvo->video_stream->time_base.den;
	D("write video frame - size: %d video pts: %f", out_size, video_pts);


	raw_picture->pts++;
}
