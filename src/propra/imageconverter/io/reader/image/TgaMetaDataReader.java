package propra.imageconverter.io.reader.image;

import propra.imageconverter.image.ColorSequence;
import propra.imageconverter.image.CompressionType;
import propra.imageconverter.image.MetaData;
import propra.imageconverter.io.exceptions.UnsupportedFormatException;
import propra.imageconverter.io.reader.MetaDataReader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static propra.imageconverter.util.Validator.ensure;

public class TgaMetaDataReader implements MetaDataReader {

    private static final int HEADER_SIZE = 18;
    private static final ColorSequence colorSequence = ColorSequence.BGR;
    private CompressionType compressionType;

    @SuppressWarnings("unused")
    @Override
    public MetaData readMetaData(InputStream inputStream, String inputPath) throws IOException {
        byte[] metaData = new byte[HEADER_SIZE];

        for (int i = 0; i < HEADER_SIZE; i++) {
            metaData[i] = (byte) inputStream.read();
        }

        ByteBuffer header = ByteBuffer.wrap(metaData).order(ByteOrder.LITTLE_ENDIAN);

        // image ID = 0 supported
        int imageID = header.get();
        ensure(imageID == 0,
                () -> new UnsupportedFormatException("[error] File contains unsupported image ID. Aborting."));

        // Color Map Type
        int colorMapType = header.get();

        // Image Type 2 or 10 supported
        int imageType = header.get();
        ensure(imageType == 2 || imageType == 10,
                () -> new UnsupportedFormatException("[error] File contains unsupported image type. Aborting."));

        switch (imageType) {
            case 2:
                compressionType = CompressionType.UNCOMPRESSED;
                break;
            case 10:
                compressionType = CompressionType.RLE;
                break;
        }

        // Color Map Specs
        int startColorMap = header.getShort();
        int lengthColorMap = header.getShort();
        int lengthColorMapEntry = header.get();

        // Image Specs
        int xCoordinate = header.getShort();
        int yCoordinate = header.getShort();
        ensure(xCoordinate == 0 || yCoordinate != 0,
                () -> new UnsupportedFormatException("[error] File contains inconsistent dimensions. Aborting.")
        );

        int width = Short.toUnsignedInt(header.getShort());
        int height = Short.toUnsignedInt(header.getShort());
        ensure((width > 0 && height > 0) && xCoordinate == 0 && (yCoordinate == 0 || yCoordinate == height),
                () -> new UnsupportedFormatException("[error] File contains inconsistent dimensions. Aborting."));

        int bitsPerPixel = header.get();
        ensure(bitsPerPixel == 24,
                () -> new UnsupportedFormatException("[error] File contains unsupported format <> 24 bits/pixel. Aborting.")
        );

        int imageDescriptor = header.get();

        return new MetaData(compressionType, width, height, bitsPerPixel, colorSequence);
    }

}

