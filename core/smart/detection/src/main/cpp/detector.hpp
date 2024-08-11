

#include <jni.h>
#include <opencv2/imgproc/imgproc.hpp>

#include "types/detectionResult.hpp"

namespace smartautoclicker {

    class Detector {

    private:
        double scaleRatio = 1;

        std::unique_ptr<cv::Mat> fullSizeColorCurrentImage = nullptr;
        std::unique_ptr<cv::Mat> scaledGrayCurrentImage = std::make_unique<cv::Mat>();

        DetectionResult detectionResult;

        std::unique_ptr<cv::Mat> scaleAndChangeToGray(const cv::Mat &fullSizeColored) const;

        static std::unique_ptr<cv::Mat> matchTemplate(const cv::Mat& image, const cv::Mat& condition);
        static void locateMinMax(const cv::Mat& matchingResult, DetectionResult& results);
        static bool isResultAboveThreshold(const DetectionResult& results, const int threshold);
        static double getColorDiff(const cv::Mat& image, const cv::Mat& condition);

        cv::Rect getDetectionResultFullSizeRoi(const cv::Rect& detectionRoi, int fullSizeWidth, int fullSizeHeight) const;

        DetectionResult detectCondition(JNIEnv *env, jobject conditionImage, cv::Rect fullSizeDetectionRoi, int threshold);

    public:

        Detector() = default;

        void setScreenMetrics(JNIEnv *env, jstring metricsTag, jobject screenImage, double detectionQuality);

        void setScreenImage(JNIEnv *env, jobject screenImage);

        DetectionResult detectCondition(JNIEnv *env, jobject conditionImage, int threshold);
        DetectionResult detectCondition(JNIEnv *env, jobject conditionImage, int x, int y, int width, int height, int threshold);
    };
}


