package propra.imageconverter.io.codec.huffman;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class represents Huffman Codec with possibility of creation of frequency map for each character in
 * the datasegment, creation of a Huffman Tree and respective code map and offers look up methods.
 */
public class HuffmanCodec {
    private static final Comparator<HuffmanNode> NODE_COMPARATOR =
            Comparator.comparingInt(HuffmanNode::getFrequency);
    private final Map<Byte, Integer> freqMap = new HashMap<>();
    private final Map<Byte, String> codeMap = new HashMap<>();
    private Queue<HuffmanNode> leafs;
    private HuffmanNode root;

    public HuffmanCodec(InputStream inputStream) throws IOException {
        createFreqMap(inputStream);
        createLeafs();
        createHuffmanTree();
        createCodeMap(root, "");
    }

    public HuffmanCodec(HuffmanNode root) {
        this.root = root;
        createCodeMap(root, "");
    }

    private void createFreqMap(InputStream inputStream) throws IOException {
        int currentByte = inputStream.read();
        if (currentByte == -1) {
            throw new IllegalArgumentException("Empty data segment. Aborting");
        }

        int[] byteCounts = new int[256];
        int index = 0;
        while (currentByte != -1) {
            byteCounts[Byte.toUnsignedInt((byte) currentByte)]++;
            currentByte = inputStream.read();
            index++;
        }

        // add chars with freq > 0 to freqMap
        index = 0;
        while (index < byteCounts.length) {
            if (byteCounts[index] != 0) {
                freqMap.put((byte) index, byteCounts[index]);
            }
            index++;
        }

        index = 0;
        if (freqMap.size() == 1) { // add filler if only one character in data segment used to create tree
            while (index < byteCounts.length) {
                if (byteCounts[index] == 0) {
                    freqMap.put((byte) index, byteCounts[index]);
                    break;
                }
                index++;
            }
        }
    }

    public Map<Byte, Integer> getFreqMap() {
        return this.freqMap;
    }

    public Queue<HuffmanNode> getLeafs() {
        return leafs;
    }

    private void createLeafs() {
        leafs = freqMap.entrySet().stream()
                .map(e -> new HuffmanNode(e.getKey(), e.getValue()))
                .sorted(NODE_COMPARATOR)
                .collect(Collectors.toCollection(() -> new PriorityQueue<>(NODE_COMPARATOR)));
    }

    private void createHuffmanTree() {
        Queue<HuffmanNode> huffmanNodes = new PriorityQueue<>(leafs);
        while (huffmanNodes.size() > 1) {
            HuffmanNode right = huffmanNodes.remove();
            HuffmanNode left = huffmanNodes.remove();
            HuffmanNode parent = new HuffmanNode(left, right,
                    right.getFrequency() + left.getFrequency());
            huffmanNodes.add(parent);
        }
        root = huffmanNodes.remove();
    }

    private void createCodeMap(HuffmanNode node, String huffmanCode) {
        if (node.isLeaf()) {
            codeMap.put(node.getCharacter(), huffmanCode);
            return;
        }
        createCodeMap(node.getLeftChild(), huffmanCode + "0");
        createCodeMap(node.getRightChild(), huffmanCode + "1");
    }

    public void writeHuffmanTreeToFile(BitStreamWriter huffmanWriter) throws IOException {
        writeHuffmanTreeCode(root, huffmanWriter);
    }

    private void writeHuffmanTreeCode(HuffmanNode node, BitStreamWriter huffmanWriter) throws IOException {
        if (node.isLeaf()) {
            huffmanWriter.writeBit(1);
            huffmanWriter.writeByte(node.getCharacter());
        } else {
            huffmanWriter.writeBit(0);
            writeHuffmanTreeCode(node.getLeftChild(), huffmanWriter);
            writeHuffmanTreeCode(node.getRightChild(), huffmanWriter);
        }
    }

    public Map<Byte, String> getCodeMap() {
        return codeMap;
    }

    Optional<Byte> getCharacter(String code) {
        return codeMap.entrySet().stream().filter(c -> c.getValue().equals(code))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    public void writeCodeAsBits(Byte character, BitStreamWriter huffmanWriter) throws IOException {
        String c = codeMap.get(character);
        for (int i = 0; i < c.length(); i++) {
            huffmanWriter.writeBit((int) c.charAt(i) - '0');
        }
    }

}
