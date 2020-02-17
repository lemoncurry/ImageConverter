package propra.imageconverter.io.writer;

import propra.imageconverter.image.ColorSequence;
import propra.imageconverter.image.CompressionType;
import propra.imageconverter.image.MetaData;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * The TgaWriter includes the behaviour of Writers for the tga image format.
 */
class TgaImageWriter implements ImageWriter {
    private static final int HEADER_SIZE = 18;

    private final CompressionType compressionType;

    TgaImageWriter(CompressionType compressionType) {
        this.compressionType = compressionType;
    }

    @Override
    public void writeOnInit(MetaData metaDataInput,
                            OutputStream outputStream) throws IOException {

        ByteBuffer header = ByteBuffer.allocate(HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN);

        // start header
        header.put((byte) 0); // ID
        header.put((byte) 0); // Color Map Type

        // imageType 2, or 10
        if (compressionType == CompressionType.HUFFMAN) {
            throw new IllegalArgumentException("Huffman compression not supported. Aborting.");
        }
        if (compressionType == CompressionType.RLE) {
            header.put((byte) 10);
        } else {
            header.put((byte) 2);
        }

        header.putShort((short) 0); // start Color Map
        header.putShort((short) 0); // length Color Map
        header.put((byte) 0); // length entry color map
        header.putShort((short) 0); // xCoordinate origin
        header.putShort((short) metaDataInput.getImageHeight()); // yCoordinate origin
        header.putShort((short) metaDataInput.getImageWidth()); // width
        header.putShort((short) metaDataInput.getImageHeight()); // height
        header.put((byte) metaDataInput.getBitsPerPixel()); // color depth
        header.put((byte) 32); // attribute byte, bit 5 is set to 1, all other to 0

        // go back to beginning of buffer
        header.flip();

        outputStream.write(header.array());
    }

    @Override
    public PacketWriter getPacketWriter(OutputStream outputStream) {
        return packet -> compressionType.writeToOutputStream(outputStream, ColorSequence.BGR, packet);
    }

    @Override
    public void writeOnEnd(MetaData metaDataInput, String outputPath, OutputStream outputStream) {
        // this method is empty because all meta data information for tga is available before
        // the data segment is written (e.g. no checksum calculation necessary).
    }
}
