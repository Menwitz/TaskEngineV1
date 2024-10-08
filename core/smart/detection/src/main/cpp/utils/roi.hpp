
#include <opencv2/imgproc/imgproc.hpp>

namespace smartautoclicker {

    bool isRoiNotContainedInImage(const cv::Rect& roi, const cv::Mat& image) {
        return 0 > roi.x || 0 > roi.width || roi.x + roi.width > image.cols
                || 0 > roi.y || 0 > roi.height || roi.y + roi.height > image.rows;
    }

    bool isRoiNotContainingImage(const cv::Rect& roi, const cv::Mat& image) {
        return roi.x < 0 || roi.width <= 0 || roi.width < image.cols
                || roi.y < 0 || roi.height <= 0 || roi.y + roi.height < image.rows;
    }

    bool isImageNotContainingImage(const cv::Mat& container, const cv::Mat& contained) {
        return container.size().height < contained.size().height ||
                container.size().width < contained.size().width;
    }

    cv::Rect getScaledRoi(const cv::Rect& roi, const double scaleRatio) {
        return {
                cvRound(roi.x * scaleRatio),
                cvRound(roi.y * scaleRatio),
                cvRound(roi.width * scaleRatio),
                cvRound(roi.height * scaleRatio)
        };
    }

    cv::Rect getRoiForResult(const cv::Point& resultLoc, const cv::Mat& expectedImage) {
        return {
                resultLoc.x,
                resultLoc.y,
                expectedImage.cols,
                expectedImage.rows
        };
    }

    void markRoiAsInvalidInResults(const cv::Rect& roi, const cv::Mat& results) {
        cv::rectangle(results, roi, cv::Scalar(0), CV_FILLED);
    }

    void logInvalidRoiInImage(const cv::Rect& roi, const cv::Mat& image) {
        __android_log_print(
                ANDROID_LOG_ERROR, "Detector",
                "ROI is invalid, %1d/%2d %3d/%4d in %5d/%6d",
                roi.x, roi.y, roi.width, roi.height,
                image.cols, image.rows
        );
    }
}