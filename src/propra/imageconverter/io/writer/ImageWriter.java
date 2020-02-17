package propra.imageconverter.io.writer;

import propra.imageconverter.image.MetaData;
import propra.imageconverter.image.Packet;

import java.io.IOException;
import java.io.OutputStream;

/**
 * The Writer interface specifies behavior of writers.
 * <p>
 * It also includes one inner interface PacketWriter to describe the behavior
 * of PacketWriters, which may be accessed via the getPacketWriter method.
 */
public interface ImageWriter {
    void writeOnInit(MetaData metaDataInput, OutputStream outputStream) throws IOException;

    PacketWriter getPacketWriter(OutputStream outputStream) throws IOException;

    void writeOnEnd(MetaData metaDataInput, String outputPath, OutputStream outputStream) throws IOException;

    // Packets are handed from the DataSegmentReader to the respective Writer.
    // This way the DataSegmentReader does not need further information on the Writer.
    // Although it would have been possible to create an extra Interface class
    // for the PacketWriter it was included here because of the getPacketWriter
    // method of the enclosing interface which justifies this grouping.
    @FunctionalInterface
    interface PacketWriter {
        void writePacket(Packet packet) throws IOException;
    }
}
