package propra.imageconverter.io.writer;

import propra.imageconverter.image.CompressionType;
import propra.imageconverter.io.codec.huffman.HuffmanCodec;

import java.io.IOException;

public class WriterFactory {

    public static ImageWriter getWriterFor(String outputFileExtension,
                                           CompressionType compression, HuffmanCodec huffmanCodec) throws IOException {
        switch (outputFileExtension) {
            case "tga":
                return new TgaImageWriter(compression);

            case "propra":
                return new ProPraImageWriter(compression, huffmanCodec);

            default:
                throw new IOException("[error] Unsupported image format. " +
                        "Please use --help to view usage.");
        }
    }

}
