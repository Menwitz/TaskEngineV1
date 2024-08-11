

#include <opencv2/core/types.hpp>

namespace smartautoclicker {

    class DetectionResult {

    public:
        bool isDetected;
        double centerX;
        double centerY;

        double minVal;
        double maxVal;
        cv::Point minLoc;
        cv::Point maxLoc;

        void reset() {
            isDetected = false;
            centerX = 0;
            centerY = 0;
            minVal = 0;
            maxVal = 0;
            minLoc.x = 0;
            minLoc.y = 0;
            maxLoc.x = 0;
            maxLoc.y = 0;
        }
    };
}

