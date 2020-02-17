package propra.imageconverter.io.codec.huffman;

import java.io.IOException;
import java.io.InputStream;

public class BitStreamReader {
    private final InputStream inputStream;
    private int bitBuffer;
    private short remainingBitsInBuffer;

    public BitStreamReader(InputStream inputStream) {
        this.inputStream = inputStream;
        this.bitBuffer = 0;
        this.remainingBitsInBuffer = 0;
    }

    public int readBit() throws IOException {
        if (remainingBitsInBuffer == 0) {
            bitBuffer = (short) inputStream.read();
            if (bitBuffer == -1) {
                return -1; //EOF reached
            }
            remainingBitsInBuffer = 8;
        }
        remainingBitsInBuffer -= 1;
        return (bitBuffer >>> remainingBitsInBuffer) & 1;
    }

    public byte readByte() throws IOException {
        int result = 0;
        for (int i = 7; i >= 0; i--) {
            result |= readBit() << i;
        }
        return (byte) result;
    }

}
