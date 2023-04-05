package ca.phon.phontalk.tests;

import ca.phon.app.log.LogUtil;
import ca.phon.phontalk.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.Assert;
import org.junit.Test;
import org.talkbank.ns.talkbank.P;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Test round trip: cha -> xml -> phon -> xml -> cha
 *
 */
public class RoundTripTest {

    private String testName;

    private String inputFile;

    private String outputFolder;

    public RoundTripTest(String testName, String inputFile, String outputFolder) {
        super();

        this.testName = testName;
        this.inputFile = inputFile;
        this.outputFolder = outputFolder;
    }

    public File getOutputFolder() throws IOException {
        final File retVal = new File(this.outputFolder, testName);
        if(retVal.exists()) {
            FileUtils.deleteDirectory(retVal);
        }
        retVal.mkdirs();
        return retVal;
    }

    @Test
    public void testRoundTrip() throws IOException {
        AtomicReference<PhonTalkMessage> lastError = new AtomicReference<>();
        final PhonTalkListener listener = (e) -> {
            System.err.println(e.getMessage());
            if(e.getSeverity() == PhonTalkMessage.Severity.SEVERE) {
                lastError.set(e);
            }
        };

        final String basename = FilenameUtils.getBaseName(inputFile);
        final File outputFolder = getOutputFolder();

        final File origFile = new File(outputFolder, basename + ".cha");
        final File chat2xmlFile = new File(outputFolder, basename + "-tb.xml");
        final File expectedOutputFile = new File(outputFolder, basename + "-tb-cha.cha");
        final File xml2phonFile = new File(outputFolder, basename + "-tb-phon.xml");
        final File phon2xmlFile = new File(outputFolder, basename + "-tb-phon-tb.xml");
        final File roundTripFile = new File(outputFolder, basename + "-tb-phon-tb-cha.cha");

        FileUtils.copyFile(new File(inputFile), origFile);

        final CHAT2XmlConverter chat2XmlConverter = new CHAT2XmlConverter();
        chat2XmlConverter.convertFile(origFile, chat2xmlFile, listener);
        Assert.assertNull(lastError.get());

        final Xml2CHATConverter expectedOutputConverter = new Xml2CHATConverter();
        expectedOutputConverter.convertFile(chat2xmlFile, expectedOutputFile, listener);
        Assert.assertNull(lastError.get());

        final Xml2PhonConverter xml2PhonConverter = new Xml2PhonConverter();
        xml2PhonConverter.convertFile(chat2xmlFile, xml2phonFile, listener);
        Assert.assertNull(lastError.get());

        final Phon2XmlConverter phon2XmlConverter = new Phon2XmlConverter();
        phon2XmlConverter.convertFile(xml2phonFile, phon2xmlFile, listener);
        Assert.assertNull(lastError.get());

        final Xml2CHATConverter xml2CHATConverter = new Xml2CHATConverter();
        xml2CHATConverter.convertFile(phon2xmlFile, roundTripFile, listener);
        Assert.assertNull(lastError.get());

        Path expectedPath = Path.of(expectedOutputFile.toURI());
        Path rtPath = Path.of(roundTripFile.toURI());

        if(testName.equals("dep-tiers") || testName.equals("commenturl") || testName.equals("gem")) {
            final File sortedExpectedFile = new File(outputFolder, basename + "-tb-cha-sorted.cha");
            final File sortedRtFile = new File(outputFolder, basename + "-tb-phon-tb-cha-sorted.cha");
            // ignore ordering in this file
            String[] p1Data = new String[]{"sort", expectedPath.toString()};
            String[] p2Data = new String[]{"sort", rtPath.toString()};

            try {
                ProcessBuilder pb = new ProcessBuilder(p1Data);
                pb.redirectOutput(sortedExpectedFile);
                pb.start().waitFor();

                pb = new ProcessBuilder(p2Data);
                pb.redirectOutput(sortedRtFile);
                pb.start().waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            expectedPath = sortedExpectedFile.toPath();
            rtPath = sortedRtFile.toPath();
        }

        long byteChange = Files.mismatch(expectedPath, rtPath);
        if(byteChange != -1) {
            byteChange = Files.mismatch(origFile.toPath(), rtPath);
            if(byteChange == -1) {
                LogUtil.info("Round-trip file matches original.cha and not -tb-cha.cha file");
            }
        }
        Assert.assertEquals(-1, byteChange);
    }

}
