package ca.phon.phontalk.tests;

import ca.phon.phontalk.PhonTalkListener;
import ca.phon.phontalk.TalkbankReader;
import ca.phon.phontalk.TalkbankWriter;
import ca.phon.session.Session;
import ca.phon.session.io.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.Difference;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Read test files in FluencyBank (talkbank format) and then write them back in talkbank format.
 * Tests xml equivalence of two files using xmlunit.
 */
// uncomment to run test, will take a long time
//@RunWith(Parameterized.class)
public class RoundTripTestsFluencyBank {

    final static String GIT_REPO = "https://github.com/ghedlund/testfluencybank";
    final static String TARGET_TEST_FOLDER = "target/test/RoundTripsTestsFluencyBank";
    final static String OUT_XML_TB_FOLDER = TARGET_TEST_FOLDER + "/xml-tb";
    final static String OUT_XML_PHON_FODLER = TARGET_TEST_FOLDER + "/xml-phon";
    final static String GIT_CLONE_FOLDER = TARGET_TEST_FOLDER + "/testfluencybank";
    final static String XML_TB_FOLDER = GIT_CLONE_FOLDER + "/xml-tb";

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> collectFiles() throws IOException, InterruptedException {
        cloneGitRepo();
        List<Object[]> retVal = new ArrayList<>();

        final File tbXmlFolder = new File(XML_TB_FOLDER);
        if(!tbXmlFolder.exists() || !tbXmlFolder.isDirectory())
            throw new RuntimeException("Invalid input folder");

        final List<Path> xmlPaths = new ArrayList<>();
        final Path baseFolder = tbXmlFolder.toPath();
        collectFiles(baseFolder, baseFolder, xmlPaths);

        final File outXmlFolder = new File(OUT_XML_TB_FOLDER);
        if(!outXmlFolder.exists()) {
            outXmlFolder.mkdirs();
        }
        final Path outFolder = outXmlFolder.toPath();
        for(Path xmlRelPath:xmlPaths) {
            final File xmlFile = baseFolder.resolve(xmlRelPath).toFile();
            final File outFile = outFolder.resolve(xmlRelPath).toFile();
            retVal.add(new Object[]{xmlRelPath.toString(), xmlFile, outFile });
        }

        retVal.sort(Comparator.comparing(a -> a[0].toString()));

        return retVal;
    }

    public static int cloneGitRepo() throws IOException, InterruptedException {
        final File targetTestFolder = new File(TARGET_TEST_FOLDER);
        if(!targetTestFolder.exists()) targetTestFolder.mkdirs();
        final File gitRepoFolder = new File(GIT_CLONE_FOLDER);
        if(!gitRepoFolder.exists()) {
            final String cmd = String.format("git clone %s %s", GIT_REPO, GIT_CLONE_FOLDER);
            Process p = Runtime.getRuntime().exec(cmd);
            int exitVal = p.waitFor();
            return exitVal;
        } else {
            return 0;
        }
    }

    public static void collectFiles(Path baseFolder, Path folder, List<Path> xmlPaths) throws IOException {
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(folder)) {
            for(Path path:directoryStream) {
                if(Files.isDirectory(path)) {
                    collectFiles(baseFolder, path, xmlPaths);
                } else if(path.getFileName().toString().endsWith(".xml")) {
                    final Path relPath = baseFolder.relativize(path);
                    xmlPaths.add(relPath);
                }
            }
        }
    }

    private String filename;
    private File inputXmlFile;
    private File outputXmlFile;

    public RoundTripTestsFluencyBank(String filename, File inputXmlFile, File outputXmlFile) {
        this.filename = filename;
        this.inputXmlFile = inputXmlFile;
        this.outputXmlFile = outputXmlFile;
    }

    @Test
    public void testShortRoundTrip() throws IOException, XMLStreamException {
        // ensure output folder exists
        final File outputFolder = outputXmlFile.getParentFile();
        if(!outputFolder.exists()) {
            outputFolder.mkdirs();
        }

        final File phonXmlFile = new File(OUT_XML_PHON_FODLER, filename);
        if(!phonXmlFile.getParentFile().exists()) {
            phonXmlFile.getParentFile().mkdirs();
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

        final SessionWriter writer = (new SessionOutputFactory()).createWriter();
        writer.writeSession(session, new FileOutputStream(phonXmlFile));

        final TalkbankWriter phonWriter = new TalkbankWriter();
        phonWriter.addListener(listener);
        phonWriter.writeSession(session, outputXmlFile.getAbsolutePath());

        // check number of reported errors
        Assert.assertEquals(0, (int)intRef.get());

        final String origXml = FileUtils.readFileToString(inputXmlFile, StandardCharsets.UTF_8);
        final String testXml = FileUtils.readFileToString(outputXmlFile, StandardCharsets.UTF_8);

        final Diff xmlDiff = DiffBuilder.compare(origXml).withTest(testXml)
                .ignoreWhitespace().ignoreComments().build();
        final DiffData diffData = sortCHATDiff(xmlDiff);

        if(!diffData.warnings().isEmpty())
            System.out.println(diffData.getWarningText());

        // write diff file
        final File diffFile = new File(outputXmlFile.getParent(), FilenameUtils.removeExtension(outputXmlFile.getName()) + "-diff.txt");
        try (PrintWriter diffWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(diffFile), StandardCharsets.UTF_8))) {
            if(!diffData.warnings().isEmpty()) {
                diffWriter.write(diffData.getWarningText());
                diffWriter.write("\n");
            }
            if(!diffData.errors().isEmpty()) {
                diffWriter.write(diffData.getErrorText());
                diffWriter.write("\n");
            }
            diffWriter.flush();
        }

        Assert.assertEquals(diffData.getErrorText(), 0, diffData.errors().size());
    }

//    @Test
    public void testLongRoundTrip() throws IOException, XMLStreamException {
        // ensure output folder exists
        final File outputFolder = new File(OUT_XML_PHON_FODLER);
        if(!outputFolder.exists()) {
            outputFolder.mkdirs();
        }

        System.out.println("Round trip file: " + filename);
        AtomicReference<Integer> intRef = new AtomicReference<>(0);
        final PhonTalkListener listener = (msg) -> {
            System.out.println(msg.toString());
            intRef.set(intRef.get()+1);
        };

        final File inputFile = new File(OUT_XML_TB_FOLDER, this.filename);

        // read in talkbank file
        final TalkbankReader reader = new TalkbankReader();
        reader.addListener(listener);
        final Session session = reader.readFile(inputFile.getAbsolutePath());

        // write phon file
        final File phonFile = new File(OUT_XML_PHON_FODLER, this.filename);
        phonFile.getParentFile().mkdirs();
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
