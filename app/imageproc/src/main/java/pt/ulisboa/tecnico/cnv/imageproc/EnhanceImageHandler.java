package pt.ulisboa.tecnico.cnv.imageproc;

import boofcv.alg.enhance.EnhanceImageOps;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayU8;

import java.awt.image.BufferedImage;

public class EnhanceImageHandler extends ImageProcessingHandler {

    public static void main(String[] args) {

        if (args.length != 2) {
            System.err.println("Syntax EnhanceImageHandler <input image path> <output image path>");
            return;
        }

        String inputImagePath = args[0];
        String outputImagePath = args[1];
        BufferedImage bufferedInput = UtilImageIO.loadImageNotNull(inputImagePath);
        ImageProcessingRequest request = new ImageProcessingRequest(0, bufferedInput);
        BufferedImage bufferedOutput = new EnhanceImageHandler().process(request);
        UtilImageIO.saveImage(bufferedOutput, outputImagePath);
    }

    public BufferedImage process(ImageProcessingRequest request) {
        BufferedImage bi = request.getImage();
        GrayU8 gray = ConvertBufferedImage.convertFrom(bi, (GrayU8) null);
        GrayU8 adjusted = gray.createSameShape();
        EnhanceImageOps.equalizeLocal(gray, 50, adjusted, 256, null);
        return ConvertBufferedImage.convertTo(adjusted, null);
    }
}
