package propra.imageconverter.io.reader.image;

import propra.imageconverter.image.ColorSequence;
import propra.imageconverter.image.CompressionType;
import propra.imageconverter.image.MetaData;
import propra.imageconverter.io.exceptions.UnsupportedFormatException;
import propra.imageconverter.io.reader.MetaDataReader;
import propra.imageconverter.util.Checksum;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import static propra.imageconverter.image.ColorSequence.GBR;
import static propra.imageconverter.util.Validator.ensure;


/**
 * Reads .propra image meta data (Spec 2.0), compression type 0 or 1.
 */

public class ProPraMetaDataReader implements MetaDataReader {
    private static final short HEADER_SIZE = 28;
    private static final ColorSequence colorSequence = GBR;
    private CompressionType compressionType;

    @Override
    public MetaData readMetaData(InputStream inputStream, String inputPath) throws IOException {
        byte[] metaData = new byte[HEADER_SIZE];

        for (int i = 0; i < HEADER_SIZE; i++) {
            metaData[i] = (byte) inputStream.read();
        }

        String format = new String(Arrays.copyOfRange(metaData, 0, 10));
        ensure(format.equals("ProPraWS19"),
                () -> new UnsupportedFormatException("[error] ProPra image " +
                        "file does not start with 'ProPraWS19'. Aborting.")
        );

        ByteBuffer header = ByteBuffer.wrap(metaData, 10, metaData.length - 10)
                .order(ByteOrder.LITTLE_ENDIAN);

        int width = Short.toUnsignedInt(header.getShort());
        int height = Short.toUnsignedInt(header.getShort());
        ensure(width != 0 && height != 0,
                () -> new UnsupportedFormatException("[error] File contains " +
                        "inconsistent dimensions. Aborting.")
        );

        int bitsPerPixel = header.get();
        ensure(bitsPerPixel == 24,
                () -> new UnsupportedFormatException("[error] File contains " +
                        "unsupported format <> 24 bits/pixel. Aborting.")
        );

        // 0 = uncompressed, 1 = compressed, perPixel, 2 = huffman, per byte
        int compression = header.get();
        ensure(compression == 0 || compression == 1 || compression == 2,
                () -> new UnsupportedFormatException("[error] File contains unsupported compression. " +
                        "Please use --help to view usage."));

        long dataSegmentSize = header.getLong();
        int checkSum = header.getInt();

        switch (compression) {
            case 0:
                compressionType = CompressionType.UNCOMPRESSED;
                if (dataSegmentSize != (long) width * height * bitsPerPixel / 8) {
                    throw new UnsupportedFormatException("[error] Inconsistent height/width. Aborting.");
                }
                break;
            case 1:
                compressionType = CompressionType.RLE;
                break;
            case 2:
                compressionType = CompressionType.HUFFMAN;
                break;
        }

        if (compressionType == CompressionType.UNCOMPRESSED) {
            ensure(Files.size(Paths.get(inputPath)) == dataSegmentSize + HEADER_SIZE, () ->
                    new UnsupportedFormatException("[error] Unexpected file size. Aborting."));

            ensure(checkSum == Checksum.calculateChecksum(inputPath, HEADER_SIZE, dataSegmentSize),
                    () -> new UnsupportedFormatException(
                            "[error] Unexpected " + "checksum in source file. Aborting.")
            );
        }
        return new MetaData(compressionType, width, height, bitsPerPixel, colorSequence);
    }

}
