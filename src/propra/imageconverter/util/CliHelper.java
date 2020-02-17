package propra.imageconverter.util;

import propra.imageconverter.image.CompressionType;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Stream;

import static propra.imageconverter.util.Validator.ensure;

/**
 * The CliHelper Class includes static methods to process
 * arguments passed to the main-method in ImageConverter.java
 * and to provide a usage print out.
 */
public final class CliHelper {

    public static boolean hasHelpOption(String[] args) {
        return Arrays.asList(args).contains("--help");
    }

    public static String getFileExtension(String path) {
        String fileName = getFileName(path);
        return Stream.of(fileName)
                .filter(f -> f.contains("."))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("[error] Missing argument or invalid " +
                        "file extension. Please use --help to view usage."))
                .substring(fileName.lastIndexOf(".") + 1);
    }

    public static String getInputPath(String[] args) throws IOException {
        long count = Arrays.stream(args).filter(arg -> arg.startsWith("--input=")).count();
        ensure(count == 1,
                () -> new IOException("[error] Unexpected use of options. " +
                        "Please use --help to view usage.")
        );

        return Arrays.stream(args)
                .filter(arg -> arg.startsWith("--input="))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("[error] Missing input parameter. " +
                        "Please use --help to view usage."))
                .substring("--input=".length());
    }

    public static String getOutputPath(String[] args) {
        long count = Arrays.stream(args).filter(arg -> arg.startsWith("--output=")).count();
        ensure(count == 1,
                () -> new IllegalArgumentException("[error] Unexpected use of options. " +
                        "Please use --help to view usage.")
        );

        return Arrays.stream(args)
                .filter(arg -> arg.startsWith("--output="))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("[error] Missing output parameter. " +
                        "Please use --help to view usage."))
                .substring("--output=".length());
    }

    private static String getFileName(String path) {
        return new File(path).getName();
    }

    public static boolean hasEncodeBase32HexOption(String[] args) {
        return Arrays.asList(args).contains("--encode-base-32");
    }

    public static boolean hasDecodeBase32HexOption(String[] args) {
        return Arrays.asList(args).contains("--decode-base-32");
    }

    public static boolean hasBaseNEncodeOption(String[] args) {
        return Arrays.stream(args)
                .anyMatch(arg -> arg.startsWith("--encode-base-n="));
    }

    public static boolean hasBaseNDecodeOption(String[] args) {
        return Arrays.asList(args).contains("--decode-base-n");
    }

    private static boolean hasBaseNOption(String[] args) {
        return hasBaseNEncodeOption(args) || hasBaseNDecodeOption(args);
    }

    public static boolean hasOptionEncodeOrDecode(String[] args) {
        return (hasEncodeBase32HexOption(args) || hasDecodeBase32HexOption(args)
                || hasBaseNOption(args));
    }

    public static String getAlphabet(String[] args) {
        long count = Arrays.stream(args).filter(arg -> arg.startsWith("--encode-base-n=")).count();
        ensure(count == 1,
                () -> new IllegalArgumentException("[error] Unexpected use of options. " +
                        "Please use --help to view usage.")
        );
        return Arrays.stream(args).filter(arg -> arg.startsWith("--encode-base-n="))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("[error] Missing '--encode-base-n' option. " +
                        "Please use --help to view usage."))
                .substring("--encode-base-n=".length());
    }

    public static CompressionType getCompressionType(String[] args) {
        long count = Arrays.stream(args).filter(arg -> arg.startsWith("--compression=")).count();
        ensure(count == 1,
                () -> new IllegalArgumentException("[error] Unexpected use of options. " +
                        "Please use --help to view usage.")
        );
        String compressionType = Arrays.stream(args)
                .filter(arg -> arg.startsWith("--compression="))
                .findAny()
                .orElse("--compression=uncompressed") // uncompressed = default
                .substring("--compression=".length());
        switch (compressionType) {
            case "rle":
                return CompressionType.RLE;
            case "uncompressed":
                return CompressionType.UNCOMPRESSED;
            case "huffman":
                return CompressionType.HUFFMAN;
            case "auto":
                return CompressionType.AUTO;
            default:
                throw new IllegalArgumentException(
                        "[error] Compression type not supported. " +
                                "Please use --help to view usage."
                );
        }
    }

    public static void printUsage() {
        System.out.println("----------------------------------------------------------");
        System.out.println();
        System.out.println("Usage: java propra.imageconverter.ImageConverter [options]");
        System.out.println();
        System.out.println("Options: Overview");
        System.out.println("\t" + "--input=<path/to/file>");
        System.out.println("\t" + "--output=<path/to/file>");
        System.out.println("\t" + "--encode-base-32");
        System.out.println("\t" + "--decode-base-32");
        System.out.println("\t" + "--encode-base-n=<alphabet>");
        System.out.println("\t" + "--decode-base-n");
        System.out.println("\t" + "--help");
        System.out.println();
        System.out.println("(1) File base N encoding/decoding options");
        System.out.println();

        System.out.println("\t" + "--input=<path/to/file> --encode-base-32");
        System.out.println("\t" + "--input=<path/to/file.base-32> --decode-base-32");
        System.out.println("\t" + "--input=<path/to/file> --encode-base-n=<alphabet>");
        System.out.println("\t" + "--input=<path/to/file.base-n> --decode-base-n");
        System.out.println();
        System.out.println("\t" + "Base N decoding: Alphabet is stored in first line of file. " +
                "EOL is indicated by '\\n'. ");
        System.out.println("\t" + "Alphabets are not allowed to include '\\n' as character. ");
        System.out.println("\t" + "Alphabet characters need to be unique.");
        System.out.println();
        System.out.println("(2) Image file conversion options");
        System.out.println();
        System.out.println("\t" + "--input=<path/to/file> --output=<path/to/file> " +
                "[--compression=<compression_type>]");
        System.out.println("\t" + "Compression types: auto, uncompressed (default), rle, huffman(propra only!)");
        System.out.println();
        System.out.println("\t" + "Supported image formats: .tga, .propra");
        System.out.println();
        System.out.println("----------------------------------------------------------");
    }
}
