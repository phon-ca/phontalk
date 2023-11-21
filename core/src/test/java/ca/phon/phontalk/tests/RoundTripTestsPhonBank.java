package ca.phon.phontalk.tests;

import ca.phon.orthography.Orthography;
import ca.phon.phontalk.PhonTalkListener;
import ca.phon.phontalk.TalkbankReader;
import ca.phon.phontalk.TalkbankWriter;
import ca.phon.session.Record;
import ca.phon.session.Session;
import ca.phon.session.io.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.jsoup.select.CombiningEvaluator;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.talkbank.ns.talkbank.P;
import org.talkbank.ns.talkbank.S;
import org.talkbank.ns.talkbank.T;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.Difference;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
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
public class RoundTripTestsPhonBank {

    final static String GIT_REPO = "https://github.com/ghedlund/testphonbank";
    final static String TARGET_TEST_FOLDER = "target/test/RoundTripsTestsPhonBank";
    final static String OUT_XML_TB_FOLDER = TARGET_TEST_FOLDER + "/xml-tb";
    final static String OUT_XML_PHON_FOLDER = TARGET_TEST_FOLDER + "/xml-phon";
    final static String OUT_XML_PHON_TB_FOLDER = TARGET_TEST_FOLDER + "/xml-phon-tb";
    final static String GIT_CLONE_FOLDER = TARGET_TEST_FOLDER + "/testphonbank";
    final static String XML_TB_FOLDER = GIT_CLONE_FOLDER + "/xml-tb";
    final static String XML_PHON_FOLDER = GIT_CLONE_FOLDER + "/xml-phon-1_3";

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
        for(Path xmlRelPath:xmlPaths) {
            retVal.add(new Object[]{xmlRelPath.toString()});
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

    public RoundTripTestsPhonBank(String filename) {
        this.filename = filename;
    }

    @Test
    public void testShortRoundTrip() throws IOException, XMLStreamException {
        final File origTbFile = new File(XML_TB_FOLDER, this.filename);
        final File origPhonFile = new File(XML_PHON_FOLDER, this.filename);
        final File outTbFile = new File(OUT_XML_TB_FOLDER, this.filename);
        final File outPhonFile = new File(OUT_XML_PHON_FOLDER, this.filename);
        final File outPhonTbFile = new File(OUT_XML_PHON_TB_FOLDER, this.filename);

        System.out.println("Round trip file: " + filename);
        AtomicReference<Integer> intRef = new AtomicReference<>(0);
        final PhonTalkListener listener = (msg) -> {
            System.out.println(msg.toString());
            intRef.set(intRef.get()+1);
        };

        // read original tb file
        final TalkbankReader reader = new TalkbankReader();
        reader.addListener(listener);
        final Session tbSession = reader.readFile(origTbFile.getAbsolutePath());

        for(Record r:tbSession.getRecords()) {
            try {
                final Orthography reparsedOrtho = Orthography.parseOrthography(r.getOrthography().toString());
                r.setOrthography(reparsedOrtho);
            } catch (ParseException e) {

            }
        }

        // output talkbank xml
        File outputFolder = outTbFile.getParentFile();
        if(!outputFolder.exists()) {
            outputFolder.mkdirs();
        }
        final TalkbankWriter writer = new TalkbankWriter();
        writer.addListener(listener);
        writer.writeSession(tbSession, outTbFile.getAbsolutePath());

        // read original phon file
        final SessionInputFactory inputFactory = new SessionInputFactory();
        final SessionReader origSessionReader = inputFactory.createReaderForFile(origPhonFile);
        final Session phonSession = origSessionReader.readSession(new FileInputStream(origPhonFile));

        // output new phon file
        if(!outPhonFile.getParentFile().exists()) {
            outPhonFile.getParentFile().mkdirs();
        }
        final SessionOutputFactory sessionOutputFactory = new SessionOutputFactory();
        final SessionWriter sessionWriter = sessionOutputFactory.createWriter();
        sessionWriter.writeSession(phonSession, new FileOutputStream(outPhonFile));

        // output phon-tb file
        if(!outPhonTbFile.getParentFile().exists()) {
            outPhonTbFile.getParentFile().mkdirs();
        }
        final TalkbankWriter phonTbWriter = new TalkbankWriter();
        phonTbWriter.addListener(listener);
        phonTbWriter.writeSession(phonSession, new FileOutputStream(outPhonTbFile));

        // check number of reported errors
        Assert.assertEquals(0, (int)intRef.get());

        final String tbXml = FileUtils.readFileToString(outTbFile, StandardCharsets.UTF_8);
        final String phonTbXml = FileUtils.readFileToString(outPhonTbFile, StandardCharsets.UTF_8);

        final Diff xmlDiff = DiffBuilder.compare(phonTbXml).withTest(tbXml)
                .ignoreWhitespace().ignoreComments().build();
        final DiffData diffData = sortCHATDiff(xmlDiff);

        if(!diffData.warnings().isEmpty())
            System.out.println(diffData.getWarningText());

        // write diff file
        final File diffFile = new File(outTbFile.getParent(), FilenameUtils.removeExtension(outTbFile.getName()) + "-diff.txt");
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
