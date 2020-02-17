package propra.imageconverter.io.codec.huffman;

import java.util.Objects;


/**
 * Class represents nodes of huffman trees
 */
public class HuffmanNode {

    private byte character;
    private int frequency;

    private HuffmanNode leftChild;
    private HuffmanNode rightChild;

    public HuffmanNode(HuffmanNode leftChild, HuffmanNode rightChild) { // inner node or root
        this.leftChild = leftChild;
        this.rightChild = rightChild;

        if (leftChild == null || rightChild == null) {
            throw new IllegalStateException("An inner node must have two children.");
        }
    }

    HuffmanNode(HuffmanNode leftChild, HuffmanNode rightChild, int weight) { // inner node or root
        this.leftChild = leftChild;
        this.rightChild = rightChild;
        this.frequency = weight;

        if (leftChild == null || rightChild == null) {
            throw new IllegalStateException("An inner node must have two children.");
        }
    }

    public HuffmanNode(byte character) { // leaf node
        this.character = character;
    }

    public HuffmanNode(byte character, int frequency) { // leaf node
        this.character = character;
        this.frequency = frequency;
    }

    boolean isLeaf() {
        return (leftChild == null && rightChild == null);
    }

    int getFrequency() {
        return frequency;
    }

    byte getCharacter() {
        return character;
    }

    HuffmanNode getLeftChild() {
        return leftChild;
    }

    HuffmanNode getRightChild() {
        return rightChild;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HuffmanNode node = (HuffmanNode) o;
        return character == node.character &&
                frequency == node.frequency;
    }

    @Override
    public int hashCode() {
        return Objects.hash(character, frequency);
    }
}
