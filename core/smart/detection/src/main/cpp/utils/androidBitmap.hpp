

#include <android/log.h>
#include <android/bitmap.h>

#include <opencv2/imgproc/imgproc.hpp>

namespace smartautoclicker {

    /** Create a cv::Mat containing the pixels data of an Android Bitmap. */
    std::unique_ptr<cv::Mat> createColorMatFromARGB8888BitmapData(JNIEnv *env, jobject bitmap) {
        try {
            AndroidBitmapInfo info;
            void *pixels = nullptr;

            CV_Assert(AndroidBitmap_getInfo(env, bitmap, &info) >= 0);
            CV_Assert(info.format == ANDROID_BITMAP_FORMAT_RGBA_8888);
            CV_Assert(AndroidBitmap_lockPixels(env, bitmap, &pixels) >= 0);

            auto argbMat = std::make_unique<cv::Mat>(
                info.height,
                info.width,
                CV_8UC4,
                pixels
            );
            AndroidBitmap_unlockPixels(env, bitmap);

            return argbMat;
        } catch (...) {
            AndroidBitmap_unlockPixels(env, bitmap);

            __android_log_print(ANDROID_LOG_ERROR, "androidBitmap",
                                "createColorMatFromARGB8888BitmapData caught an exception");
            jclass je = env->FindClass("java/lang/Exception");
            env->ThrowNew(je, "Android Bitmap exception in JNI code {createColorMatFromARGB8888BitmapData}");

            return nullptr;
        }
    }

}