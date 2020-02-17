package propra.imageconverter;

import propra.imageconverter.image.CompressionType;
import propra.imageconverter.image.MetaData;
import propra.imageconverter.io.codec.huffman.HuffmanCodec;
import propra.imageconverter.io.reader.MetaDataReader;
import propra.imageconverter.io.reader.ReaderFactory;
import propra.imageconverter.io.reader.image.DataSegmentReader;
import propra.imageconverter.io.writer.ImageWriter;
import propra.imageconverter.io.writer.WriterFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static propra.imageconverter.util.CliHelper.*;
import static propra.imageconverter.util.Validator.ensure;

/**
 * Usage: java propra.imageconverter.ImageConverter [options]
 * <p>
 * Use the option "--help" to get more information on program usage and options.
 * <p>
 * Supported image formats with the following file extensions:
 * - .tga (RGB 24)
 * - .propra (RGB 24)
 * <p>
 * ProPra:
 * - image type 0 (uncompressed) or 1 (RLE) or 2 (Huffman)
 * TGA:
 * - image type 2 (uncompressed) or 10 (RLE, 24 bit RGB)
 * - image descriptor: bit 4 is set to 0, bit 5 to 1 (=origin left upper corner)
 * - no optional fields
 * <p>
 * Supported base N file extensions for decoding:
 * - .base-n (alphabet must be stored in first line of file)
 * - .base-32 (32 hex decoding)
 * <p>
 * Supported operations (image conversion):
 * - .tga: read/write from and to compressed(rle = 10)/uncompressed format
 * - .propra: read/write from and to compressed(rle = 1, huffman = 2)/uncompressed format
 * <p>
 * Supported operations (encoding/decoding base N):
 * - encoding/decoding for base 2, 4, 8, 16, 32, 64
 * <p>
 * Valid alphabets: unique characters, 1 byte per char, length of 2, 4, 8, 16, 32, 64, EOL = '\n'.
 */


final class ImageConverter {

    public static HuffmanCodec huffmanCodec;

    public static void main(String[] args) {

        try {
            ensure(args.length != 0,
                    () -> new IllegalArgumentException("[error] Invalid arguments. " +
                            "Please use --help to view usage."));

            if (args.length == 1 && hasHelpOption(args)) {
                printUsage();
                System.exit(0); // Zero because of successful call of option --help
            }

            String inputPath = getInputPath(args);
            String inputFileExtension = getFileExtension(inputPath);

            if (hasOptionEncodeOrDecode(args)) {
                BaseConverter.encodeOrDecodeFile(args, inputPath);
            } else {
                new ImageConverter().convertImage(args, inputPath, inputFileExtension);
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(123);
        }
    }

    Path prependPrefix(String outputPath, String type) {
        Path op = Paths.get(outputPath);
        String newFilename = "tmp_" + type + "_" + op.getFileName();
        if (op.getParent() != null) {
            return op.getParent().resolve(newFilename);
        } else {
            return Paths.get(newFilename);
        }
    }


    private Optional<File> convertImageToTempFile(String inputPath, String inputFileExtension,
                                                  String outputPath, String type) {
        Path tempOutputPath = prependPrefix(outputPath, type);

        try {
            String[] argsTmp = {"--input=" + inputPath,
                    "--output=" + tempOutputPath.toString(), "--compression=" + type};
            new ImageConverter().convertImage(argsTmp, inputPath, inputFileExtension);
            return Optional.of(tempOutputPath.toFile());
        } catch (IOException e) {
            System.err.println("Exception while generating temp file of type " + type + ": " + e.getMessage());
            try {
                Files.deleteIfExists(tempOutputPath);
            } catch (IOException ignored) {
            }
            return Optional.empty();
        }
    }

    private void convertImage(String[] args,
                              String inputPath,
                              String inputFileExtension) throws IOException {

        String outputPath = getOutputPath(args);
        String outputFileExtension = getFileExtension(outputPath);

        CompressionType outputCompressionType = getCompressionType(args);
        MetaDataReader metaDataReader = ReaderFactory.getReaderFor(inputFileExtension);

        if (outputCompressionType == CompressionType.AUTO) {
            new ImageConverter().convertImageCompressionTypeAuto(inputPath, inputFileExtension, outputPath,
                    outputFileExtension);
            return;
        }

        huffmanCodec = null;
        if (outputCompressionType == CompressionType.HUFFMAN) {
            ensure(outputFileExtension.equals("propra"), () ->
                    new IllegalArgumentException("Huffman compression only supported " +
                            "for propra files. Aborting."));
            new ImageConverter().generateHuffmanCodecWithTempFile(inputPath, inputFileExtension, outputPath);
        }

        ImageWriter imageWriter = WriterFactory.getWriterFor(
                outputFileExtension, outputCompressionType, huffmanCodec
        );

        try (InputStream inputStream = new BufferedInputStream(
                new FileInputStream(new File(inputPath))
        )) {
            // Get image meta data
            MetaData metaDataInput = metaDataReader.readMetaData(inputStream, inputPath);

            Files.deleteIfExists(Paths.get(outputPath)); // remove output file if exists

            // Write new image files
            try (FileOutputStream fileOutputStream = new FileOutputStream(outputPath);
                 BufferedOutputStream bufferedOutputStream =
                         new BufferedOutputStream(fileOutputStream)) {

                // writer is called before packets from the data segment arrive to write the header
                // or a header placeholder
                imageWriter.writeOnInit(metaDataInput, bufferedOutputStream);

                DataSegmentReader.read(metaDataInput, inputStream,
                        imageWriter.getPacketWriter(bufferedOutputStream));

                // in case of placeholder header, write missing header into file
                imageWriter.writeOnEnd(metaDataInput, outputPath, bufferedOutputStream);
            }
        }
    }


    /**
     * Generates huffmanCodec from newly created uncompressed file (to count frequencies, etc....)
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void generateHuffmanCodecWithTempFile(String inputPath, String inputFileExtension,
                                                  String outputPath) throws IOException {
        Optional<File> uncompressedOutput = convertImageToTempFile(inputPath, inputFileExtension,
                outputPath, "uncompressed");
        ensure(uncompressedOutput.isPresent(),
                () -> new IOException("Unable to decompress propra file. Aborting."));

        try (InputStream inputStream = new BufferedInputStream(
                new FileInputStream(uncompressedOutput.get())
        )) {
            ReaderFactory.getReaderFor("propra")
                    .readMetaData(inputStream, uncompressedOutput.get().getPath());
            huffmanCodec = new HuffmanCodec(inputStream);
        }
        uncompressedOutput.get().delete();
    }


    /**
     * Gets the best possible compression from allowed compression types of image format.
     * The approach is simple:
     * Create all possible files in parallel and only keep the one with smallest file size.
     * Since the user wants to use the 'auto' option I assumed that he does not want to be informed
     * about the 'winning' compression type by console print out.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void convertImageCompressionTypeAuto(String inputPath, String inputFileExtension,
                                                 String outputPath, String outputFileExtension) {
        String[] requiredTempTypes;
        switch (outputFileExtension) {
            case "propra":
                requiredTempTypes = new String[]{"huffman", "uncompressed", "rle"};
                break;
            case "tga":
                requiredTempTypes = new String[]{"uncompressed", "rle"};
                break;
            default:
                throw new IllegalArgumentException("Unsupported image type. Aborting.");
        }

        // creates all possible compression type files in parallel, sorts by file size (ascending)
        List<File> files = Stream.of(requiredTempTypes)
                .map(t -> CompletableFuture.supplyAsync(() ->
                        convertImageToTempFile(inputPath, inputFileExtension, outputPath, t)))
                .map(CompletableFuture::join)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .sorted(Comparator.comparingLong(File::length))
                .collect(Collectors.toList());

        ensure(files.size() > 0, () -> new IllegalStateException("There must be a file. Aborting."));

        // file with smallest size
        File winner = files.get(0);

        // rename file to specified outputPath
        ensure(winner.renameTo(new File(outputPath)), () ->
                new IllegalStateException("Must be possible to rename to specified output path. Aborting."));

        // delete other tmp files
        files.stream()
                .skip(1) // first one was winner, and shouldn't be removed
                .forEach(File::delete);
    }

}

