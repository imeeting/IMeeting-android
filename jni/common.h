/*
 * common.h
 *
 *  Created on: 2012-10-15
 *      Author: star
 */

#ifndef COMMON_H_
#define COMMON_H_

#define DEBUG 1

#if DEBUG
#include <android/log.h>
#  define  D(x...)  __android_log_print(ANDROID_LOG_DEBUG, "imeeting", x)
#endif

#ifndef NULL
#define NULL	0
#endif

#endif /* COMMON_H_ */
