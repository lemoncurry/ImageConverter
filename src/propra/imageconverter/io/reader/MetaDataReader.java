package propra.imageconverter.io.reader;

import propra.imageconverter.image.MetaData;

import java.io.IOException;
import java.io.InputStream;

public interface MetaDataReader {
    MetaData readMetaData(InputStream inputStream, String inputPath) throws IOException;
}
