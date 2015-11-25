package dk.tw.opencvtest;

import org.opencv.core.Mat;

import java.io.Serializable;

public class OpenCVDataContainer {

    private Mat canny, morphology, gray, filledContours, bilateral, warpedPerspective;

    public OpenCVDataContainer(Mat gray, Mat bilateral, Mat canny, Mat morphology, Mat filledContours, Mat warpedPerspective) {
        this.gray = gray;
        this.bilateral = bilateral;
        this.canny = canny;
        this.morphology = morphology;
        this.filledContours = filledContours;
        this.warpedPerspective = warpedPerspective;
    }

    public Mat getCanny() {
        return canny;
    }

    public Mat getMorphology() {
        return morphology;
    }

    public Mat getGray() {
        return gray;
    }

    public Mat getFilledContours() {
        return filledContours;
    }

    public Mat getBilateral() {
        return bilateral;
    }

    public Mat getWarpedPerspective() {
        return warpedPerspective;
    }
}
