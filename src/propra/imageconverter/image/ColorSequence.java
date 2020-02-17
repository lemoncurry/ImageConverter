package propra.imageconverter.image;


/**
 * ColorSequence (enum) class:
 * Consists of two color sequences BGR and GBR and specifies their behaviour with
 * the abstract methods formatAsByteArray() and fromByteArray().
 * <p>
 * The implementations are included directly with the respective color sequence.
 * This may be described as a weak form of Strategy Pattern.
 */

public enum ColorSequence {
    BGR {
        @Override
        public byte[] formatAsByteArray(Pixel p) {
            return new byte[]{(byte) p.getB(), (byte) p.getG(), (byte) p.getR()};
        }

        @Override
        public Pixel fromByteArray(byte[] bytes) {
            return new Pixel(Byte.toUnsignedInt(bytes[2]), Byte.toUnsignedInt(bytes[1]),
                    Byte.toUnsignedInt(bytes[0]));
        }
    },

    GBR {
        @Override
        public byte[] formatAsByteArray(Pixel p) {
            return new byte[]{(byte) p.getG(), (byte) p.getB(), (byte) p.getR()};
        }

        @Override
        public Pixel fromByteArray(byte[] bytes) {
            return new Pixel(Byte.toUnsignedInt(bytes[2]), Byte.toUnsignedInt(bytes[0]),
                    Byte.toUnsignedInt(bytes[1]));

        }
    };

    public abstract byte[] formatAsByteArray(Pixel pixel);

    public abstract Pixel fromByteArray(byte[] bytes);
}
