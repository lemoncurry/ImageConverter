package propra.imageconverter.image;

import java.util.Objects;

/**
 * The pixel class represents pixels (24 bits per pixel) and their color values (RGB).
 * It offers getter-methods to access the pixel information and to compare pixel objects.
 */
public class Pixel {

    private final int R;
    private final int G;
    private final int B;

    Pixel(int r, int g, int b) {
        R = r;
        G = g;
        B = b;
    }

    public int getR() {
        return R;
    }

    public int getG() {
        return G;
    }

    public int getB() {
        return B;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pixel pixel = (Pixel) o;
        return R == pixel.R &&
                G == pixel.G &&
                B == pixel.B;
    }

    @Override
    public String toString() {
        return "{" +
                "R=0x" + Integer.toHexString(R) +
                ", G=0x" + Integer.toHexString(G) +
                ", B=0x" + Integer.toHexString(B) +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(R, G, B);
    }

}
