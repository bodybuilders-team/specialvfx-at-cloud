package pt.ulisboa.tecnico.cnv.imageproc;

import boofcv.alg.filter.blur.GBlurImageOps;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.Planar;

import java.awt.image.BufferedImage;

public class BlurImageHandler extends ImageProcessingHandler {

    public static void main(String[] args) {

        if (args.length != 2) {
            System.err.println("Syntax BlurImage <input image path> <output image path>");
            return;
        }

        String inputImagePath = args[0];
        String outputImagePath = args[1];
        BufferedImage bufferedInput = UtilImageIO.loadImageNotNull(inputImagePath);
        BufferedImage bufferedOutput = new BlurImageHandler().process(bufferedInput);
        UtilImageIO.saveImage(bufferedOutput, outputImagePath);
    }

    public BufferedImage process(BufferedImage bi) {
        Planar<GrayU8> input = ConvertBufferedImage.convertFrom(bi, true, ImageType.pl(3, GrayU8.class));
        Planar<GrayU8> output = input.createSameShape();
        GBlurImageOps.gaussian(input, output, -1, 32, null);
        return ConvertBufferedImage.convertTo(output, null, true);
    }
}
