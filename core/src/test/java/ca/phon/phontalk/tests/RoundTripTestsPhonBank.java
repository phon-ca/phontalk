package ca.phon.phontalk.tests;

import ca.phon.orthography.Orthography;
import ca.phon.phontalk.*;
import ca.phon.session.Record;
import ca.phon.session.Session;
import ca.phon.session.io.SessionInputFactory;
import ca.phon.session.io.SessionOutputFactory;
import ca.phon.session.io.SessionReader;
import ca.phon.session.io.SessionWriter;
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
@RunWith(Parameterized.class)
public class RoundTripTestsPhonBank {

    final static String GIT_REPO = "git@github.com:phon-ca/phonbank";
    final static String TARGET_TEST_FOLDER = "target/test/RoundTripTestsPhonBank";
    final static String GIT_CLONE_FOLDER = TARGET_TEST_FOLDER + "/phonbank";

    // step 1 - phon -> tb xml
    final static String IN_PHON_FOLDER = GIT_CLONE_FOLDER + "/phon-in-progress/Goad";
    final static String OUT_XML_TB_FOLDER = TARGET_TEST_FOLDER + "/xml";

    // step 2 - tb xml -> chat
    final static String OUT_CHAT_FOLDER = TARGET_TEST_FOLDER + "/chat";

    // step 3 - chat -> tb xml
    final static String OUT_CHAT_XML_FOLDER = TARGET_TEST_FOLDER + "/chat-xml";

    // step 4 - tb xml -> phon
    final static String OUT_PHON_FOLDER = TARGET_TEST_FOLDER + "/chat-xml-phon";

    // step 5 - xml -> chat
    final static String OUT_CHAT_FOLDER_2 = TARGET_TEST_FOLDER + "/chat-xml-chat";


    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> collectFiles() throws IOException, InterruptedException {
        cloneGitRepo();
        List<Object[]> retVal = new ArrayList<>();

        final File phonXmlFolder = new File(IN_PHON_FOLDER);
        if(!phonXmlFolder.exists() || !phonXmlFolder.isDirectory())
            throw new RuntimeException("Invalid input folder");

        final List<Path> xmlPaths = new ArrayList<>();
        final Path baseFolder = phonXmlFolder.toPath();
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
                } else if(path.getFileName().toString().endsWith(".xml") && !path.getFileName().endsWith("project.xml")) {
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
    public void testRoundTrip() throws IOException, XMLStreamException {
        final File origPhonFile = new File(IN_PHON_FOLDER, this.filename);

        System.out.println("Round trip file: " + filename);
        AtomicReference<Integer> intRef = new AtomicReference<>(0);
        final StringBuffer sb = new StringBuffer();
        final PhonTalkListener listener = (msg) -> {
            sb.append(msg.toString());
            sb.append("\n");
            if(!msg.getMessage().contains("adding"))
                intRef.set(intRef.get()+1);
        };

        // read original phon file
        final SessionInputFactory inputFactory = new SessionInputFactory();
        final SessionReader reader = inputFactory.createReaderForFile(origPhonFile);
        final Session session = reader.readSession(new FileInputStream(origPhonFile));

        for(Record r:session.getRecords()) {
            try {
                final Orthography reparsedOrtho = Orthography.parseOrthography(r.getOrthography().toString());
                r.setOrthography(reparsedOrtho);
            } catch (ParseException e) {

            }
        }

        // output talkbank xml
        final File outTbFile = new File(OUT_XML_TB_FOLDER, this.filename);
        File outputFolder = outTbFile.getParentFile();
        if(!outputFolder.exists()) {
            outputFolder.mkdirs();
        }
        final TalkbankWriter writer = new TalkbankWriter();
        writer.addListener(listener);
        writer.writeSession(session, outTbFile.getAbsolutePath());

        // output chat file
        final String chatFilename = FilenameUtils.removeExtension(this.filename) + ".cha";
        final File outChatFile = new File(OUT_CHAT_FOLDER, chatFilename);
        outputFolder = outChatFile.getParentFile();
        if(!outputFolder.exists()) {
            outputFolder.mkdirs();
        }
        final Xml2CHATConverter chatConverter = new Xml2CHATConverter();
        chatConverter.convertFile(outTbFile, outChatFile, listener);

        // chat 2 xml
        final File outChatXmlFile = new File(OUT_CHAT_XML_FOLDER, this.filename);
        outputFolder = outChatXmlFile.getParentFile();
        if(!outputFolder.exists()) {
            outputFolder.mkdirs();
        }
        final CHAT2XmlConverter chat2XmlConverter = new CHAT2XmlConverter();
        chat2XmlConverter.convertFile(outChatFile, outChatXmlFile, listener);

        // check number of reported errors
        Assert.assertEquals(0, (int)intRef.get());

        final String tbXml = FileUtils.readFileToString(outTbFile, StandardCharsets.UTF_8);
        final String phonTbXml = FileUtils.readFileToString(outChatXmlFile, StandardCharsets.UTF_8);

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

        // write log file
        if(!sb.toString().isEmpty()) {
            final File logFile = new File(outTbFile.getParent(), FilenameUtils.removeExtension(outTbFile.getName()) + "-log.txt");
            try (PrintWriter logWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(logFile), StandardCharsets.UTF_8))) {
                logWriter.write(sb.toString());
                logWriter.flush();
            }
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
