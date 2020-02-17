package propra.imageconverter.image;


/**
 * The meta data class is responsible for representing information that
 * needs to be transferred to another image format or needs to be known
 * to create the output image (such as compression type of the original image):
 * <p>
 * - compression type
 * - image width
 * - image height
 * - bits per pixel
 * - color sequence of pixels.
 * <p>
 * It offers getter-methods to access this information.
 */
public class MetaData {
    private final CompressionType compressionType;
    private final int imageWidth;
    private final int imageHeight;
    private final int bitsPerPixel;
    private final ColorSequence colorSequence;

    public MetaData(
            CompressionType compressionType,
            int imageWidth,
            int imageHeight,
            int bitsPerPixel,
            ColorSequence colorSequence
    ) {
        this.compressionType = compressionType;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.bitsPerPixel = bitsPerPixel;
        this.colorSequence = colorSequence;
    }

    public CompressionType getCompressionType() {
        return compressionType;
    }

    public int getImageWidth() {
        return imageWidth;
    }

    public int getImageHeight() {
        return imageHeight;
    }

    public int getBitsPerPixel() {
        return bitsPerPixel;
    }

    public ColorSequence getColorSequence() {
        return colorSequence;
    }

}
