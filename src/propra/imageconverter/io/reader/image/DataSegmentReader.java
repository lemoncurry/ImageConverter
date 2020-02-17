package propra.imageconverter.io.reader.image;

import propra.imageconverter.image.ColorSequence;
import propra.imageconverter.image.MetaData;
import propra.imageconverter.image.Packet;
import propra.imageconverter.image.Pixel;
import propra.imageconverter.io.codec.huffman.HuffmanInputStream;
import propra.imageconverter.io.writer.ImageWriter;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.ArrayList;
import java.util.List;


/**
 * The DataSegmentReader-Class reads data from the inputStream,
 * and in case it receives uncompressed data,
 * compresses the data by creating Rle- and Raw-Packets.
 * Packets are handed to the packetWriter.
 */
public class DataSegmentReader {
    private static final int BYTES_PER_PIXEL = 3; // 24 bits per pixel

    public static void read(MetaData metaData, InputStream inputStream,
                            ImageWriter.PacketWriter packetWriter) throws IOException {

        switch (metaData.getCompressionType()) {

            case RLE:
                readRleCompressedData(metaData, inputStream, packetWriter);
                break;

            case UNCOMPRESSED:
                readUncompressedData(metaData, inputStream, packetWriter);
                break;

            case HUFFMAN: // data gets uncompressed by HuffmanInputStream read() method
                readUncompressedData(metaData, new HuffmanInputStream(inputStream), packetWriter);
                break;

            default:
                throw new IllegalArgumentException("[error] Unsupported " +
                        "compression type. Aborting.");
        }
    }

    private static void readUncompressedData(MetaData metaData, InputStream inputStream,
                                             ImageWriter.PacketWriter packetWriter) throws IOException {
        int scanLineLength = metaData.getImageWidth();
        int numberOfScanLines = metaData.getImageHeight();
        PushbackInputStream pushbackInputStream = new PushbackInputStream(inputStream,
                2 * metaData.getBitsPerPixel() / 8);

        // traverse through all scanLines in image
        for (int lineID = 0; lineID < numberOfScanLines; lineID++) {
            int pixelsReadInThisScanLine = 0;

            while (pixelsReadInThisScanLine < scanLineLength) {

                // only look for Rle packets if there are at least two
                // pixels left on this line
                if (pixelsReadInThisScanLine < scanLineLength - 1) {
                    Packet rlePacket = readRlePacket(pushbackInputStream,
                            (scanLineLength - pixelsReadInThisScanLine),
                            metaData.getColorSequence());
                    if (rlePacket != null) {
                        pixelsReadInThisScanLine += rlePacket.getUncompressedPixelCount();
                        packetWriter.writePacket(rlePacket);

                        // We do not know why the Rle packet stopped
                        // accepting new pixels. The following reasons come to
                        // mind:
                        // * The Rle packet is full, but new repeated pixels are still in this line.
                        // * The packet is followed by other repeated pixels.
                        // * The packet is followed by other unrepeated pixels.
                        // * There are no pixels left on this scanline.
                        // In all of the above cases, starting a new Rle packet
                        // is preferable, if at all possible. We can achieve
                        // this trivially with a new iteration of the loop.
                        // If a new Rle packet is not possible, we will
                        // fallback to the Raw packet.
                        continue;
                    }
                }

                Packet rawPacket = readRawPacket(pushbackInputStream,
                        (scanLineLength - pixelsReadInThisScanLine),
                        metaData.getColorSequence());
                packetWriter.writePacket(rawPacket);
                pixelsReadInThisScanLine += rawPacket.getUncompressedPixelCount();
            }
        }
    }

    private static void readRleCompressedData(MetaData metaData, InputStream inputStream,
                                              ImageWriter.PacketWriter packetWriter) throws IOException {
        int currentByte;
        long readPixels = 0;
        final long expectedPixels = metaData.getImageHeight() * metaData.getImageWidth();
        while ((currentByte = inputStream.read()) != -1 && readPixels < expectedPixels) {
            int packetTypeIndicator = currentByte & 0x80;
            int sizeField = (currentByte & 0x7F) + 1;

            if (packetTypeIndicator > 0) { // Is Rle Packet, create RlePackets
                Pixel referencePixel = readPixel(inputStream, metaData.getColorSequence());
                packetWriter.writePacket(Packet.createRlePacket(sizeField, referencePixel));
                readPixels += sizeField;
            } else { // create RawPackets
                Pixel[] pixels = new Pixel[sizeField];
                readPixels += sizeField;
                for (int i = 0; i < pixels.length; i++) {
                    pixels[i] = readPixel(inputStream, metaData.getColorSequence());
                }
                packetWriter.writePacket(Packet.createRawPacket(pixels));
            }
        }
    }

    private static Packet readRawPacket(PushbackInputStream pushbackInputStream,
                                        long remainingPixelsInThisLine,
                                        ColorSequence colorSequence) throws IOException {
        List<Pixel> pixels = new ArrayList<>();
        Pixel currentPixel = readPixel(pushbackInputStream, colorSequence);
        pixels.add(currentPixel);
        remainingPixelsInThisLine--;

        while (remainingPixelsInThisLine > 0) {
            Pixel nextPixel = readPixel(pushbackInputStream, colorSequence);
            if (nextPixel.equals(currentPixel)) {
                // We started a new Raw Packet, but we noticed that the next pixel has the same
                // value as the current one.
                // We need to stop the process of creating a Raw Packet, unread the current and
                // next Pixel, so a Rle Packet may be started and can process those pixels.
                pixels.remove(pixels.size() - 1);
                unreadPixel(pushbackInputStream, nextPixel, colorSequence);
                unreadPixel(pushbackInputStream, currentPixel, colorSequence);
                break;
            }
            // in case the (actual) pixel count is >= 128, we need to finish the packet.
            // because we already have read the next pixel (which would be pixel 129), we need to
            // unread it and make sure it will be included in the next packet.
            if (pixels.size() >= 128) {
                unreadPixel(pushbackInputStream, nextPixel, colorSequence);
                break;
            }
            pixels.add(nextPixel);
            currentPixel = nextPixel;
            remainingPixelsInThisLine--;
        }

        return Packet.createRawPacket(pixels.toArray(new Pixel[0]));
    }

    private static Packet readRlePacket(PushbackInputStream pushbackInputStream,
                                        long remainingPixelsInThisLine,
                                        ColorSequence colorSequence) throws IOException {
        Pixel referencePixel = readPixel(pushbackInputStream, colorSequence);
        remainingPixelsInThisLine--;

        int repetitionCount = 1;

        Pixel nextPixel = readPixel(pushbackInputStream, colorSequence);
        remainingPixelsInThisLine--;

        // case: rep < 2
        if (!referencePixel.equals(nextPixel)) {
            unreadPixel(pushbackInputStream, nextPixel, colorSequence);
            unreadPixel(pushbackInputStream, referencePixel, colorSequence);
            return null;
        }

        repetitionCount++;

        while (remainingPixelsInThisLine > 0
                && repetitionCount < 128) {
            nextPixel = readPixel(pushbackInputStream, colorSequence);
            remainingPixelsInThisLine--;
            if (!nextPixel.equals(referencePixel)) {
                unreadPixel(pushbackInputStream, nextPixel, colorSequence);
                break;
            } else {
                repetitionCount++;
            }
        }
        return Packet.createRlePacket(repetitionCount, referencePixel);
    }

    // transforms bytes into pixels and sets color sequence according to input format
    private static Pixel readPixel(InputStream inputStream,
                                   ColorSequence colorSequence) throws IOException {
        byte[] pixel = new byte[BYTES_PER_PIXEL];
        for (int i = 0; i < BYTES_PER_PIXEL; i++) {
            int value = inputStream.read();
            if (value < 0) {
                throw new EOFException("[error] Unexpected end-of-file. Aborting.");
            }
            pixel[i] = (byte) value;
        }
        return colorSequence.fromByteArray(pixel);
    }

    private static void unreadPixel(PushbackInputStream pushbackInputStream, Pixel pixel,
                                    ColorSequence colorSequence) throws IOException {
        pushbackInputStream.unread(colorSequence.formatAsByteArray(pixel));
    }
}
