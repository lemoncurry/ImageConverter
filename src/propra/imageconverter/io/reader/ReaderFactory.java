package propra.imageconverter.io.reader;

import propra.imageconverter.io.reader.image.ProPraMetaDataReader;
import propra.imageconverter.io.reader.image.TgaMetaDataReader;

public class ReaderFactory {

    public static MetaDataReader getReaderFor(String fileExtension) {
        switch (fileExtension) {
            case "tga":
                return new TgaMetaDataReader();

            case "propra":
                return new ProPraMetaDataReader();

            default:
                throw new IllegalArgumentException("[error] Unsupported image format. " +
                        "Please use --help to view usage.");

        }
    }
}
