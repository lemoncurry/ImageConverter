package propra.imageconverter.io.writer;

import propra.imageconverter.image.ColorSequence;
import propra.imageconverter.image.CompressionType;
import propra.imageconverter.image.MetaData;
import propra.imageconverter.io.codec.huffman.BitStreamWriter;
import propra.imageconverter.io.codec.huffman.HuffmanCodec;
import propra.imageconverter.util.Checksum;

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;


/**
 * The ProPraWriter includes the behaviour of Writers for the ProPra image format (v. 3.0)
 */
class ProPraImageWriter implements ImageWriter {
    private static final short HEADER_SIZE = 28;
    private static final String FORMAT = "ProPraWS19";

    private final CompressionType compressionType;
    private final HuffmanCodec huffmanCodec;
    private BitStreamWriter huffmanEncoding;

    ProPraImageWriter(CompressionType compressionType, HuffmanCodec huffmancodec) {
        this.compressionType = compressionType;
        this.huffmanCodec = huffmancodec;
    }

    @Override
    public void writeOnInit(MetaData metaDataInput,
                            OutputStream outputStream) throws IOException {
        // write placeholder zeros for header
        for (int i = 0; i < HEADER_SIZE; i++) {
            outputStream.write(0);
        }
    }

    public PacketWriter getPacketWriter(OutputStream outputStream) throws IOException {
        // special case huffman
        if (compressionType.equals(CompressionType.HUFFMAN)) {
            huffmanEncoding = new BitStreamWriter(outputStream);
            huffmanCodec.writeHuffmanTreeToFile(huffmanEncoding);
            return packet -> { // encode each packet to huffman
                byte[] bytes = packet.getAsUncompressedByteArray(ColorSequence.GBR);
                for (byte b : bytes) {
                    huffmanCodec.writeCodeAsBits(b, huffmanEncoding);
                }
            };
        } else { // all other compression types
            return packet ->
                    compressionType.writeToOutputStream(outputStream, ColorSequence.GBR, packet);
        }
    }

    @Override
    public void writeOnEnd(MetaData metaDataInput, String outputPath,
                           OutputStream outputStream) throws IOException {
        if (huffmanEncoding != null)
            huffmanEncoding.flush();
        outputStream.close();

        ByteBuffer header = ByteBuffer.allocate(HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN);

        FORMAT.chars().forEach(c -> header.put((byte) c)); // ProPraWS19
        header.putShort((short) metaDataInput.getImageWidth()); // width
        header.putShort((short) metaDataInput.getImageHeight()); // height
        header.put((byte) metaDataInput.getBitsPerPixel()); // BitsPerPixel

        switch (compressionType) {
            case UNCOMPRESSED:
                header.put((byte) 0);
                break;
            case RLE:
                header.put((byte) 1);
                break;
            case HUFFMAN:
                header.put((byte) 2);
                break;
            default:
                throw new IllegalArgumentException("Unsupported Compression. Aborting.");
        }

        long dataSegmentSize = Files.size(Paths.get(outputPath)) - HEADER_SIZE;
        header.putLong(dataSegmentSize); // data segment size
        header.putInt(Checksum.calculateChecksum( // checksum
                outputPath, HEADER_SIZE, dataSegmentSize
        ));

        header.flip();

        try (RandomAccessFile file = new RandomAccessFile(outputPath, "rw")) {
            file.write(header.array()); // replace placeholder with actual header
        }
    }
}
