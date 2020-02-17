package propra.imageconverter;

import propra.imageconverter.io.codec.base.BaseN;
import propra.imageconverter.io.codec.base.CodecType;
import propra.imageconverter.util.CliHelper;
import propra.imageconverter.util.Validator;

import java.io.*;
import java.nio.charset.StandardCharsets;

import static propra.imageconverter.util.CliHelper.hasBaseNDecodeOption;
import static propra.imageconverter.util.CliHelper.hasBaseNEncodeOption;

class BaseConverter {
    private static final String BASE32_HEX_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUV";

    static void encodeOrDecodeFile(String[] args,
                                   String inputPath) throws IOException {

        String outputFilePath;

        String alphabet = "";
        CodecType codecType;

        // Set outputFilePath, alphabet and codecType
        // case Base 32 hex encode
        if (CliHelper.hasEncodeBase32HexOption(args)) {
            Validator.ensure(!CliHelper.getFileExtension(inputPath).equals("base-32"),
                    () -> new IllegalArgumentException("[error] Unexpected file extension or encoding option. " +
                            "Please use --help to view usage."));

            outputFilePath = inputPath + ".base-32";
            alphabet = BASE32_HEX_ALPHABET;
            codecType = CodecType.ENCODE;

            // case Base 32 hex decode
        } else if (CliHelper.hasDecodeBase32HexOption(args)) {
            Validator.ensure(CliHelper.getFileExtension(inputPath).equals("base-32"),
                    () -> new IllegalArgumentException("[error] Unexpected file extension or decoding option. " +
                            "Please use --help to view usage."));

            outputFilePath = inputPath.substring(0, inputPath.length() - ".base-32".length());
            alphabet = BASE32_HEX_ALPHABET;
            codecType = CodecType.DECODE;

            // case Base N encode
        } else if (CliHelper.hasBaseNEncodeOption(args)) {
            Validator.ensure(!CliHelper.getFileExtension(inputPath).equals("base-n"),
                    () -> new IllegalArgumentException("[error] Unexpected file extension or encoding option. " +
                            "Please use --help to view usage."));
            outputFilePath = inputPath + ".base-n";
            alphabet = CliHelper.getAlphabet(args);
            codecType = CodecType.ENCODE;

            // case Base N decode
        } else if (CliHelper.hasBaseNDecodeOption(args)) {
            Validator.ensure(CliHelper.getFileExtension(inputPath).equals("base-n"),
                    () -> new IllegalArgumentException("[error] Unexpected file extension or decoding option. " +
                            "Please use --help to view usage."));

            outputFilePath = inputPath.substring(0, inputPath.length() - ".base-n".length());
            codecType = CodecType.DECODE;
            // get alphabet from first line of file
        } else {
            throw new IllegalArgumentException("[error] No valid options for encoding or decoding. " +
                    "Please use --help to view usage.");
        }

        // Encode or decode file, write new file
        switch (codecType) {
            case DECODE:
                decode(args, inputPath, outputFilePath, alphabet);
                break;

            case ENCODE:
                encode(args, inputPath, outputFilePath, alphabet);
                break;
        }
    }

    private static void decode(String[] args, String inputPath, String outputFilePath,
                               String alphabet) throws IOException {
        try (OutputStream outputStream = new BufferedOutputStream(
                new FileOutputStream(outputFilePath)
        );
             Reader reader = new BufferedReader(
                     new InputStreamReader(new FileInputStream(
                             new File(inputPath)), StandardCharsets.UTF_8)
             )) {

            // case base N decode option
            if (hasBaseNDecodeOption(args)) {
                alphabet = getAlphabetFrom(reader);
            }
            new BaseN(alphabet).decodeFromBaseN(reader, outputStream);
        }
    }

    private static String getAlphabetFrom(Reader reader) throws IOException {
        StringBuilder alphabetBuilder = new StringBuilder();
        int readByte;
        // Read alphabet in first line byte by byte
        while ((readByte = reader.read()) >= 0) {
            if (readByte == 0x0A) { // check for eol
                break;
            } else {
                alphabetBuilder.append((char) readByte);
            }
        }
        return alphabetBuilder.toString();
    }

    private static void encode(String[] args, String inputPath, String outputFilePath,
                               String alphabet) throws IOException {
        try (Writer writer = new BufferedWriter(
                new OutputStreamWriter(
                        new FileOutputStream(outputFilePath), StandardCharsets.UTF_8)
        );
             InputStream inputStream = new BufferedInputStream(
                     new FileInputStream(new File(inputPath))
             )) {

            // case base N encoding option
            if (hasBaseNEncodeOption(args)) {
                writer.write(alphabet);
                writer.write((byte) 0x0A); // write eol
            }
            new BaseN(alphabet).encodeToBaseN(inputStream, writer);
        }
    }
}
