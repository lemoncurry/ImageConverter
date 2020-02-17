package propra.imageconverter.io.codec.huffman;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;


/**
 * Class for decoding propra files that are compressed with huffman according to ProPra-Spec 3.0
 */
public class HuffmanInputStream extends InputStream {
    public final HuffmanCodec huffmanCodec;
    private final BitStreamReader huffmanFileReader;

    public HuffmanInputStream(InputStream huffmanFileStream) throws IOException {
        huffmanFileReader = new BitStreamReader(huffmanFileStream);
        HuffmanNode huffmanTreeRoot = readHuffmanTree(huffmanFileReader);
        huffmanCodec = new HuffmanCodec(huffmanTreeRoot);
    }


    private HuffmanNode readHuffmanTree(BitStreamReader huffmanFileReader) throws IOException {
        HuffmanNode left;
        HuffmanNode right;
        if ((huffmanFileReader.readBit() == 1)) { // = leaf
            return new HuffmanNode(huffmanFileReader.readByte());
        } else {
            left = readHuffmanTree(huffmanFileReader);
            right = readHuffmanTree(huffmanFileReader);
            return new HuffmanNode(left, right);
        }
    }

    /**
     * Decodes data segment of Huffman encoded propra file.
     */
    @Override
    public int read() throws IOException {
        // First read bit by bit
        // compare to codes in HashMap
        String code = "";
        int currentBit;
        Optional<Byte> character;
        while ((currentBit = huffmanFileReader.readBit()) != -1) {
            code = code.concat(Integer.toString(currentBit));
            character = huffmanCodec.getCharacter(code);
            if (character.isPresent()) {
                return Byte.toUnsignedInt(character.get());
            }
        }
        return -1;
    }
}
