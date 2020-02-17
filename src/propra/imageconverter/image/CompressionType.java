package propra.imageconverter.image;

import java.io.IOException;
import java.io.OutputStream;

/**
 * CompressionType (enum) class:
 * Consists of two compression types Rle and uncompressed and describes their behaviour with
 * the abstract method writeToOutputStream(...).
 * The implementation is included directly with the respective type.
 * This may be described as a weak form of Strategy Pattern.
 */
public enum CompressionType {
    RLE {
        @Override
        public void writeToOutputStream(OutputStream outputStream, ColorSequence colorSequence,
                                        Packet packet) throws IOException {
            packet.writeToRleOutputStream(outputStream, colorSequence);
        }
    },

    UNCOMPRESSED {
        @Override
        public void writeToOutputStream(OutputStream outputStream, ColorSequence colorSequence,
                                        Packet packet) throws IOException {
            packet.writeToUncompressedOutputStream(outputStream, colorSequence);
        }
    },

    HUFFMAN {
        @Override
        public void writeToOutputStream(OutputStream outputStream, ColorSequence colorSequence, Packet packet) {
            // no implementation needed for huffman compression
            throw new UnsupportedOperationException();
        }
    },

    AUTO {
        @Override
        public void writeToOutputStream(OutputStream outputStream, ColorSequence colorSequence, Packet packet) {
            // no implementation needed for auto compression
        }
    };

    public abstract void writeToOutputStream(OutputStream outputStream, ColorSequence colorSequence,
                                             Packet packet) throws IOException;
}
