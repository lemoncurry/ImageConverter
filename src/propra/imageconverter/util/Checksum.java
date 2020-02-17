package propra.imageconverter.util;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class Checksum {

    /**
     * Calculates the Checksum in accordance with the ProPraSpecs 2.0.
     * Variables are named to mirror the naming in the Spec.
     * <p>
     * Description of the Algorithm:
     * <p>
     * Checksum = A_n*2^16 + B_n
     * A_n = sum(i + data[i]) mod X, for all i element {1, 2, ..., n}
     * B_0 = 1
     * B_i = (B_i-1 + A_i) mod X, for all i element {1, 2, ..., n}
     * <p>
     * X = 65513, n = number of bytes in data segment
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static int calculateChecksum(String path, int headerSize, long dataSegmentSize) throws IOException {
        try (InputStream inputStream = new BufferedInputStream(new FileInputStream(path))) {
            inputStream.skip(headerSize); // go to data segment

            int X = 65513;
            int A_n;
            int A_sum = 0;
            int A_i;
            int B_i_1;
            int B_i = 1;
            int B_n;

            for (int i = 1; i <= dataSegmentSize; i++) {
                byte b = (byte) inputStream.read();
                A_i = (i + Byte.toUnsignedInt(b));
                A_sum = (A_sum + A_i) % X; // reduction in every step to avoid integer overflow
                B_i_1 = B_i;
                B_i = (B_i_1 + A_sum) % X;
            }

            A_n = A_sum;
            B_n = B_i;

            return (A_n * (int) Math.pow(2, 16) + B_n);
        }
    }


}
