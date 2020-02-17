package propra.imageconverter.io.codec.base;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static propra.imageconverter.util.Validator.ensure;

/**
 * The BaseN class is responsible for encoding any file to Base N and for decoding .base-n or
 * .base-32 files. The alphabet for decoding is handed over by the ImageConverter-class.
 * Supported base N codecs are Base 2, 4, 8, 16, 32, 32_hex, and 64.
 */
public class BaseN {
    private static final int BITS_PER_CHAR = 8;

    // alphabet
    private final String alphabet;
    private final char[] alphabetChars;

    // base N variables needed for encoding/decoding
    private final int bitsPerEncodedByte;
    private final int bytesPerChunk;
    private final int mask;


    public BaseN(String alphabet) {
        // check alphabet is non-empty
        ensure(alphabet != null,
                () -> new IllegalArgumentException("[error] No valid alphabet. Aborting.")
        );

        this.alphabet = alphabet;
        this.alphabetChars = alphabet.toCharArray();

        // check if alphabet has unique characters
        for (char character : alphabetChars) {
            long count = alphabet.chars().filter(c -> c == character).count();
            ensure(count == 1,
                    () -> new IllegalArgumentException("[error] Invalid alphabet. Aborting.")
            );
        }

        // case: base of n
        switch (alphabetChars.length) {
            case 2: // base 2
                this.bitsPerEncodedByte = 1;
                this.bytesPerChunk = 1;
                break;
            case 4: //base 4
                this.bitsPerEncodedByte = 2;
                this.bytesPerChunk = 1;
                break;
            case 8: // base 8
                this.bitsPerEncodedByte = 3;
                this.bytesPerChunk = 3;
                break;
            case 16: // base 16
                this.bitsPerEncodedByte = 4;
                this.bytesPerChunk = 1;
                break;
            case 32: // base 32
                this.bitsPerEncodedByte = 5;
                this.bytesPerChunk = 5;
                break;
            case 64: // base 64
                this.bitsPerEncodedByte = 6;
                this.bytesPerChunk = 3;
                break;
            default:
                throw new IllegalArgumentException("[error] Codec is not supported. Aborting.]");
        }

        // get bitmask, correct value for 0 indexing
        this.mask = alphabetChars.length - 1;
    }

    private static long addZeroBits(long buffer, int fillerBits) {
        buffer <<= fillerBits; // fill with additional zeros
        return buffer;
    }

    public void encodeToBaseN(InputStream inputStream, java.io.Writer writer) throws IOException {
        byte[] chunk = new byte[bytesPerChunk];

        int readBytes;
        while ((readBytes = inputStream.read(chunk)) != -1) {

            // buffer idea from  https://stackoverflow.com/a/12456674
            // copy bytes to buffer for easier traversal.
            long buffer = 0; // buffer of max. 64 bits


            // The aim of the following loop is to write the bytes into the buffer as shown in
            // https://de.wikipedia.org/wiki/Base32 (Grundprinzip).
            // Byte after byte will be read and after each byte is copied into the buffer,
            // a shift of 8 bits occurs to the left, so the next byte will be placed to
            // to the right (index 0 to 7). The shift does usually not occur after the last byte.
            // But in cases where (chunkBitCount % bitsPerEncodedByte != 0),
            // additional zeros are needed to guarantee a correct chunk size
            // (= last shift with filler bits) and an additional shift occurs.
            int bufferLength = 0;
            int fillerBits = bitsPerEncodedByte - 1; // max. needed filler bits
            int copiedBitsInBuffer = readBytes * BITS_PER_CHAR;

            int processedByteIndex = 0;
            while (readBytes > processedByteIndex) {
                buffer |= chunk[processedByteIndex] & 0xFF; // take byte and copy set bits to buffer
                bufferLength += BITS_PER_CHAR;
                if (processedByteIndex + 1 == readBytes) { // case last byte
                    if ((copiedBitsInBuffer % bitsPerEncodedByte) != 0) {
                        buffer = addZeroBits(buffer, fillerBits);
                        bufferLength += fillerBits;
                        break;
                    }
                } else { // case all other bytes before last byte
                    buffer = addZeroBits(buffer, BITS_PER_CHAR);
                    // byte is put from index 0 to index 7
                }
                processedByteIndex++;
            }

            int bitsUsedForEncoding = 0;
            while (bitsUsedForEncoding < copiedBitsInBuffer) {
                int lengthOfShiftToRight = getLengthOfShiftToRight(bufferLength, bitsUsedForEncoding);
                int index = getIndex(buffer, lengthOfShiftToRight);
                writer.write(alphabetChars[index]);
                bitsUsedForEncoding += bitsPerEncodedByte;
            }
        }
    }

    public void decodeFromBaseN(java.io.Reader reader, OutputStream outputStream) throws IOException {
        short buffer = 0; // buffer of max. 16 bits
        int bufferLength = 0;

        int inputByte;
        while ((inputByte = reader.read()) != -1) {

            char c = (char) inputByte; // get char from input byte

            buffer |= alphabet.indexOf(c) & mask; // copy set bits from read character to buffer
            bufferLength += bitsPerEncodedByte;
            if (bufferLength >= BITS_PER_CHAR) { // always extract 8 bits when possible
                bufferLength -= BITS_PER_CHAR; // minus 8 bits
                outputStream.write((byte) ((buffer >> bufferLength) & 0xFF)); // extract first 8 bits
            }
            buffer = (short) addZeroBits(buffer, bitsPerEncodedByte); // space for new bits starting at index 0
        }
    }

    private int getLengthOfShiftToRight(int bufferLength, int bitsUsedForEncoding) {
        return bufferLength - bitsPerEncodedByte - bitsUsedForEncoding;
    }

    private int getIndex(long buffer, int lengthOfShiftToRight) {
        return (int) (buffer >> lengthOfShiftToRight) & mask;
    }

}
