package ca.phon.phontalk.tests;

import ca.phon.phontalk.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Test round trip: cha -> xml -> phon -> xml -> cha
 *
 */
public class RoundTripTest {

    private String inputFile;

    private String outputFolder;

    public RoundTripTest(String inputFile, String outputFolder) {
        super();

        this.inputFile = inputFile;
        this.outputFolder = outputFolder;
    }

    public File getOutputFolder() throws IOException {
        final File retVal = new File(this.outputFolder);
        if(!retVal.exists())
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
        final File chat2xmlFile = new File(outputFolder, basename + "-xml.xml");
        final File xml2phonFile = new File(outputFolder, basename + "-xml-phon.xml");
        final File phon2xmlFile = new File(outputFolder, basename + "-xml-phon-xml.xml");
        final File roundTripFile = new File(outputFolder, basename + "-xml-phon-xml-cha.cha");

        FileUtils.copyFile(new File(inputFile), origFile);

        final CHAT2XmlConverter chat2XmlConverter = new CHAT2XmlConverter();
        chat2XmlConverter.convertFile(origFile, chat2xmlFile, listener);
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
    }

}
