package propra.imageconverter.io.codec.huffman;

import java.io.IOException;
import java.io.OutputStream;

import static propra.imageconverter.util.Validator.ensure;

public class BitStreamWriter {
    private final OutputStream outputStream;
    private int bitBuffer;
    private int bitsInBuffer;

    public BitStreamWriter(OutputStream outputStream) {
        this.outputStream = outputStream;
        this.bitBuffer = 0;
        this.bitsInBuffer = 0;
    }

    public void writeByte(byte b) throws IOException {
        for (int position = 7; position >= 0; position--) {
            int currentBit = (b >> position) & 1;
            writeBit(currentBit);
        }
    }

    public void flush() throws IOException {
        if (bitsInBuffer == 0) {
            return;
        }
        bitBuffer <<= (8 - bitsInBuffer);
        outputStream.write(bitBuffer);
        bitBuffer = 0;
        bitsInBuffer = 0;
    }

    public void writeBit(int bit) throws IOException {
        ensure((bit == 1 || bit == 0), () -> new IllegalArgumentException("Bit must be 0 or 1."));
        bitBuffer = (bitBuffer << 1) | bit;
        bitsInBuffer++;
        if (bitsInBuffer == 8) {
            flush();
        }
    }


}
