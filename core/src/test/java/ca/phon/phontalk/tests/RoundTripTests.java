package ca.phon.phontalk.tests;

import ca.phon.phontalk.PhonTalkListener;
import ca.phon.phontalk.TalkbankReader;
import ca.phon.phontalk.TalkbankWriter;
import ca.phon.session.Session;
import ca.phon.session.io.*;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.Difference;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Period;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Read files in 'good-xml' (talkbank format) and then write them back in talkbank format.
 * Tests xml equivalence of two files using xmlunit.
 */
@RunWith(Parameterized.class)
public class RoundTripTests {

    final static String GOOD_XML = "src/test/resources/ca/phon/phontalk/tests/RoundTripTests/good-xml";

    final static String GOOD_XML_OUTPUT = "target/test/RoundTripTests/good-xml";

    final static String GOOD_XML_PHON_OUTPUT = "target/test/RoundTripTests/good-xml-phon";

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> collectFiles() {
        List<Object[]> retVal = new ArrayList<>();

        final File goodXmlFolder = new File(GOOD_XML);
        if(!goodXmlFolder.exists() || !goodXmlFolder.isDirectory())
            throw new RuntimeException("Invalid input folder");

        for(File xmlFile:goodXmlFolder.listFiles((dir, name) -> name.endsWith(".xml"))) {
            final File outFile = new File(GOOD_XML_OUTPUT, xmlFile.getName());
            retVal.add(new Object[]{xmlFile.getName(), xmlFile, outFile});
        }

        retVal.sort(Comparator.comparing(a -> a[0].toString()));

        return retVal;
    }

    private String filename;
    private File inputXmlFile;
    private File outputXmlFile;

    public RoundTripTests(String filename, File inputXmlFile, File outputXmlFile) {
        this.filename = filename;
        this.inputXmlFile = inputXmlFile;
        this.outputXmlFile = outputXmlFile;
    }

    @Test
    public void testShortRoundTrip() throws IOException, XMLStreamException {
        // ensure output folder exists
        final File outputFolder = new File(GOOD_XML_OUTPUT);
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
        final DiffData diffData = sortCHATDiff(xmlDiff);

        if(!diffData.warnings().isEmpty())
            System.out.println(diffData.getWarningText());

        Assert.assertEquals(diffData.getErrorText(), 0, diffData.errors().size());
    }

    @Test
    public void testLongRoundTrip() throws IOException, XMLStreamException {
        // ensure output folder exists
        final File outputFolder = new File(GOOD_XML_PHON_OUTPUT);
        if(!outputFolder.exists()) {
            outputFolder.mkdirs();
        }

        System.out.println("Round trip file: " + filename);
        AtomicReference<Integer> intRef = new AtomicReference<>(0);
        final PhonTalkListener listener = (msg) -> {
            System.out.println(msg.toString());
            intRef.set(intRef.get()+1);
        };

        final File inputFile = new File(GOOD_XML_OUTPUT, inputXmlFile.getName());

        // read in talkbank file
        final TalkbankReader reader = new TalkbankReader();
        reader.addListener(listener);
        final Session session = reader.readFile(inputFile.getAbsolutePath());

        // write phon file
        final File phonFile = new File(GOOD_XML_PHON_OUTPUT, inputFile.getName());
        final SessionOutputFactory outputFactory = new SessionOutputFactory();
        final SessionWriter sessionWriter = outputFactory.createWriter();
        sessionWriter.writeSession(session, new FileOutputStream(phonFile));

        // read Phon file
        final SessionInputFactory inputFactory = new SessionInputFactory();
        final SessionReader sessionReader = inputFactory.createReader(sessionWriter.getClass().getAnnotation(SessionIO.class));
        final Session testSession = sessionReader.readSession(new FileInputStream(phonFile));

        final TalkbankWriter writer = new TalkbankWriter();
        writer.addListener(listener);
        writer.writeSession(testSession, outputXmlFile.getAbsolutePath());

        // check number of reported errors
        Assert.assertEquals(0, (int)intRef.get());

        final String origXml = FileUtils.readFileToString(inputFile, StandardCharsets.UTF_8);
        final String testXml = FileUtils.readFileToString(outputXmlFile, StandardCharsets.UTF_8);

        final Diff xmlDiff = DiffBuilder.compare(origXml).withTest(testXml)
                .ignoreWhitespace().ignoreComments().build();
        final DiffData diffData = sortCHATDiff(xmlDiff);

        if(!diffData.warnings().isEmpty())
            System.out.println(diffData.getWarningText());

        Assert.assertEquals(diffData.getErrorText(), 0, diffData.errors().size());
    }

    private record DiffData(List<Difference> warnings, List<Difference> errors) {
        public String getErrorText() {
            return errors().stream().map(Difference::toString).collect(Collectors.joining("\n"));
        }

        public String getWarningText() {
            return warnings().stream().map(Difference::toString).collect(Collectors.joining("\n"));
        }
    }

    private DiffData sortCHATDiff(Diff diff) {
        final List<Difference> errors = new ArrayList<>();
        final List<Difference> warnings = new ArrayList<>();

        for (Difference d : diff.getDifferences()) {
            if(d.getComparison().getControlDetails().getXPath() != null && (d.getComparison().getControlDetails().getXPath().contains("model") || d.getComparison().getControlDetails().getXPath().contains("actual"))) {
                warnings.add(d);
                continue;
            }
            if(d.getComparison().getTestDetails().getXPath() != null && (d.getComparison().getTestDetails().getXPath().contains("mod") || d.getComparison().getTestDetails().getXPath().contains("pho"))) {
                warnings.add(d);
                continue;
            }
            if (d.getComparison().getType().name().equals("ATTR_VALUE") ||
                    d.getComparison().getType().name().equals("TEXT_VALUE")) {
                final String controlText = d.getComparison().getControlDetails().getValue().toString();
                final String testText = d.getComparison().getTestDetails().getValue().toString();
                if (d.getComparison().getControlDetails().getXPath().equals("/CHAT[1]/@Version")) {
                    // ignore version differences for now
                    warnings.add(d);
                    continue;
                } else if (d.getComparison().getControlDetails().getXPath().endsWith("@age")) {
                    // also ignore differences in period if they are the same
                    final Period cdur = Period.parse(d.getComparison().getControlDetails().getValue().toString());
                    final Period tdur = Period.parse(d.getComparison().getTestDetails().getValue().toString());
                    if (cdur.equals(tdur)) {
                        warnings.add(d);
                        continue;
                    }
                } else if (controlText.matches("[0-9]+(\\.[0-9]*)?") && testText.matches("[0-9]+(\\.[0-9]*)?")) {
                    // ignore differences in floating point number output
                    Float control = Float.parseFloat(controlText);
                    Float test = Float.parseFloat(testText);
                    if (control.equals(test)) {
                        warnings.add(d);
                        continue;
                    }
                } else if (controlText.endsWith("].") && testText.endsWith("] .")) {
                    final String cmp = controlText.substring(0, testText.length() - 2) + " .";
                    if (cmp.equals(testText)) {
                        warnings.add(d);
                        continue;
                    }
                }
            } else if (d.getComparison().getType().name().equals("ELEMENT_NUM_ATTRIBUTES")) {
                if (d.getComparison().getControlDetails().getXPath().contains("replacement")) {
                    // ignore for now
                    warnings.add(d);
                    continue;
                } else if(d.getComparison().getControlDetails().getXPath().contains("ph")) {
                    warnings.add(d);
                    continue;
                }
            } else if (d.getComparison().getType().name().equals("ATTR_NAME_LOOKUP")) {
                if (d.getComparison().getControlDetails().getXPath().contains("replacement")) {
                    // ignore for now
                    warnings.add(d);
                    continue;
                }
            } else if(d.getComparison().getType().name().equals("CHILD_NODELIST_LENGTH")) {
                int oldLen = (int)d.getComparison().getControlDetails().getValue();
                int newLen = (int)d.getComparison().getTestDetails().getValue();
                int diffLen = newLen - oldLen;
                if(d.getComparison().getControlDetails().getXPath().contains("pg")
                    && d.getComparison().getTestDetails().getXPath().contains("pg") && diffLen == -2) {
                    warnings.add(d);
                    continue;
                } else if(d.getComparison().getControlDetails().getXPath().contains("w")
                        && d.getComparison().getTestDetails().getXPath().contains("w") && diffLen == 2) {
                    warnings.add(d);
                    continue;
                } else if(d.getComparison().getControlDetails().getXPath().contains("pw")
                    || d.getComparison().getControlDetails().getXPath().contains("ph")) {
                    warnings.add(d);
                    continue;
                }
            } else if(d.getComparison().getType().name().equals("CHILD_LOOKUP")) {
                if(d.getComparison().getControlDetails().getXPath() == null &&
                        (d.getComparison().getTestDetails().getXPath().contains("mod") || d.getComparison().getTestDetails().getXPath().contains("pho"))) {
                    warnings.add(d);
                    continue;
                } else if(d.getComparison().getTestDetails().getXPath() == null &&
                        (d.getComparison().getControlDetails().getXPath().contains("model") || d.getComparison().getControlDetails().getXPath().contains("actual"))) {
                    warnings.add(d);
                    continue;
                }
            }
            errors.add(d);
        }

        return new DiffData(warnings, errors);
    }

}
