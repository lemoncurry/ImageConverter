package propra.imageconverter.image;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * The abstract Packet class describes the behaviour of packets in three abstract methods
 * - writeToCompressedOutputStream(...)
 * - writeToUncompressedOutputStream(...)
 * - getUncompressedPixelCount()
 * - getAsUncompressedByteArray(...)
 * <p>
 * and two static methods
 * - createRlePacket(...)
 * - createRawPacket(...).
 * <p>
 * It also includes two static inner classes RlePacket and RawPacket that
 * implement Packet and its abstract methods.
 */
public abstract class Packet {

    public static Packet createRlePacket(int pixelCount, Pixel pixel) {
        return new RlePacket(pixelCount, pixel);
    }

    public static Packet createRawPacket(Pixel[] pixels) {
        return new RawPacket(pixels);
    }

    public abstract void writeToRleOutputStream(OutputStream outputStream,
                                                ColorSequence colorSequence) throws IOException;

    public abstract byte[] getAsUncompressedByteArray(ColorSequence colorSequence);

    public abstract void writeToUncompressedOutputStream(OutputStream outputStream,
                                                         ColorSequence colorSequence) throws IOException;

    public abstract int getUncompressedPixelCount();

    private static class RlePacket extends Packet {

        // NOTE: in binary representation, repetition count is encoded minus one of this number.
        // This field stores the actual pixel count!
        private final int repetitionCount;
        private final Pixel pixel;

        RlePacket(int repetitionCount, Pixel pixel) {
            this.repetitionCount = repetitionCount;
            this.pixel = pixel;
        }

        @Override
        public String toString() {
            return "RlePacket{" +
                    "repetitionCount=" + repetitionCount +
                    ", pixel=" + pixel +
                    '}';
        }

        @Override
        public void writeToRleOutputStream(OutputStream outputStream,
                                           ColorSequence colorSequence) throws IOException {
            outputStream.write((byte) (128 + repetitionCount - 1));
            outputStream.write(colorSequence.formatAsByteArray(pixel));
        }

        @Override
        public byte[] getAsUncompressedByteArray(ColorSequence colorSequence) {
            byte[] bytes = new byte[repetitionCount * 3];
            byte[] p = colorSequence.formatAsByteArray(pixel);

            for (int i = 0; i < repetitionCount * 3; i += 3) {
                bytes[i] = p[0];
                bytes[i + 1] = p[1];
                bytes[i + 2] = p[2];
            }
            return bytes;
        }

        @Override
        public void writeToUncompressedOutputStream(OutputStream outputStream,
                                                    ColorSequence colorSequence) throws IOException {
            byte[] p = colorSequence.formatAsByteArray(pixel);
            for (int i = 0; i < repetitionCount; i++) {
                outputStream.write(p);
            }
        }

        @Override
        public int getUncompressedPixelCount() {
            return repetitionCount;
        }


    }


    private static class RawPacket extends Packet {
        private final Pixel[] pixels;

        RawPacket(Pixel[] pixels) {
            this.pixels = pixels;
        }

        @Override
        public String toString() {
            return "RawPacket{" +
                    "pixels=" + Arrays.toString(pixels) +
                    '}';
        }

        @Override
        public void writeToRleOutputStream(OutputStream outputStream,
                                           ColorSequence colorSequence) throws IOException {
            outputStream.write((byte) (pixels.length - 1)); // header for raw packet
            for (Pixel pixel : pixels) {
                outputStream.write(colorSequence.formatAsByteArray(pixel));
            }

        }

        @Override
        public void writeToUncompressedOutputStream(OutputStream outputStream,
                                                    ColorSequence colorSequence) throws IOException {
            for (Pixel pixel : pixels) {
                outputStream.write(colorSequence.formatAsByteArray(pixel));
            }
        }

        @Override
        public byte[] getAsUncompressedByteArray(ColorSequence colorSequence) {
            byte[] bytes = new byte[pixels.length * 3];
            int pixelIndex = 0;
            for (int i = 0; i < pixels.length * 3; i += 3) {
                byte[] p = colorSequence.formatAsByteArray(pixels[pixelIndex]);
                pixelIndex++;
                bytes[i] = p[0];
                bytes[i + 1] = p[1];
                bytes[i + 2] = p[2];
            }
            return bytes;
        }

        @Override
        public int getUncompressedPixelCount() {
            return pixels.length;
        }
    }
}
