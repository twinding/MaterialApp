package dk.tw.opencvtest;

import org.opencv.core.Mat;

public class OpenCVDataContainer {

    private Mat canny, morphology, gray, filledContours, bilateral, imageWithGuidingSizes, warpedPerspective;

    public OpenCVDataContainer(Mat gray, Mat bilateral, Mat canny, Mat morphology, Mat filledContours, Mat imageWithGuidingSizes, Mat warpedPerspective) {
        this.gray = gray;
        this.bilateral = bilateral;
        this.canny = canny;
        this.morphology = morphology;
        this.filledContours = filledContours;
        this.imageWithGuidingSizes = imageWithGuidingSizes;
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

    public Mat getImageWithGuidingSizes() {
        return imageWithGuidingSizes;
    }

    public Mat getWarpedPerspective() {
        return warpedPerspective;
    }
}
