package ca.phon.phontalk.tests;

import ca.phon.phontalk.PhonTalkListener;
import ca.phon.phontalk.TalkbankReader;
import ca.phon.phontalk.TalkbankWriter;
import ca.phon.session.Session;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.talkbank.ns.talkbank.P;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.Difference;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Period;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Read files in 'good-xml' (talkbank format) and then write them back in talkbank format.
 * Tests xml equivalence of two files using xmlunit.
 */
@RunWith(Parameterized.class)
public class TestTb2Tb {

    final static String GOOD_XML = "src/test/resources/ca/phon/phontalk/tests/RoundTripTests/good-xml";

    final static String OUTPUT_FOLDER = "target/test/TestTb2Tb/good-xml";

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> collectFiles() {
        List<Object[]> retVal = new ArrayList<>();

        final File goodXmlFolder = new File(GOOD_XML);
        if(!goodXmlFolder.exists() || !goodXmlFolder.isDirectory())
            throw new RuntimeException("Invalid input folder");

        for(File xmlFile:goodXmlFolder.listFiles((dir, name) -> name.endsWith(".xml"))) {
            final File outFile = new File(OUTPUT_FOLDER, xmlFile.getName());
            retVal.add(new Object[]{xmlFile.getName(), xmlFile, outFile});
        }

        retVal.sort(Comparator.comparing(a -> a[0].toString()));

        return retVal;
    }

    private String filename;
    private File inputXmlFile;
    private File outputXmlFile;

    public TestTb2Tb(String filename, File inputXmlFile, File outputXmlFile) {
        this.filename = filename;
        this.inputXmlFile = inputXmlFile;
        this.outputXmlFile = outputXmlFile;
    }

    @Test
    public void testShortRoundTrip() throws IOException, XMLStreamException {
        // ensure output folder exists
        final File outputFolder = new File(OUTPUT_FOLDER);
        if(!outputFolder.exists()) {
            outputFolder.mkdirs();
        }

        System.out.println("Round trip file: " + filename);
        AtomicReference<Integer> intRef = new AtomicReference<>(0);
        final PhonTalkListener listener = (msg) -> {
            System.out.println(msg.toString());
            intRef.set(intRef.get()+1);
        };

        // read in talkbank file
        final TalkbankReader reader = new TalkbankReader();
        reader.addListener(listener);
        final Session session = reader.readFile(inputXmlFile.getAbsolutePath());

        final TalkbankWriter writer = new TalkbankWriter();
        writer.addListener(listener);
        writer.writeSession(session, outputXmlFile.getAbsolutePath());

        // check number of reported errors
        Assert.assertEquals(0, (int)intRef.get());

        final String origXml = FileUtils.readFileToString(inputXmlFile, StandardCharsets.UTF_8);
        final String testXml = FileUtils.readFileToString(outputXmlFile, StandardCharsets.UTF_8);

        final Diff xmlDiff = DiffBuilder.compare(origXml).withTest(testXml)
                .ignoreWhitespace().ignoreComments().build();

        final Iterator<Difference> iter = xmlDiff.getDifferences().iterator();
        int numSignificantDiffs = 0;
        while(iter.hasNext()) {
            Difference d = iter.next();
            System.out.println(d.toString());
            if(d.getComparison().getType().name().equals("ATTR_VALUE")) {
                if (d.getComparison().getControlDetails().getXPath().equals("/CHAT[1]/@Version")) {
                    // ignore version differences for now
                    continue;
                } else if(d.getComparison().getControlDetails().getXPath().endsWith("@age")) {
                    // also ignore differences in period if they are the same
                    final Period cdur = Period.parse(d.getComparison().getControlDetails().getValue().toString());
                    final Period tdur = Period.parse(d.getComparison().getTestDetails().getValue().toString());
                    if(cdur.equals(tdur)) {
                        continue;
                    }
                }
            }
            ++numSignificantDiffs;
        }
        Assert.assertEquals(0, numSignificantDiffs);
    }

}
