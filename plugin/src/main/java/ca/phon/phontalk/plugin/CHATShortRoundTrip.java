package ca.phon.phontalk.plugin;

import ca.phon.orthography.Orthography;
import ca.phon.phontalk.*;
import ca.phon.session.Record;
import ca.phon.session.Session;
import ca.phon.session.io.SessionInputFactory;
import ca.phon.session.io.SessionOutputFactory;
import ca.phon.session.io.SessionReader;
import ca.phon.session.io.SessionWriter;
import ca.phon.worker.PhonWorkerGroup;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.Difference;
import org.xmlunit.diff.ElementSelectors;

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
 * Short round trip test for phon files.  This test will read phon files from a git repository, convert them to
 * talkbank xml, then to chat, then back to talkbank xml.  The talkbank xml is compared to the xml produced by phon
 * in the first step.  If there are any differences, the differences are output to a file.  If there are any errors
 * in the conversion process, the errors are output to a file.
 */
public class CHATShortRoundTrip {

    final static String GIT_REPO = "git@github.com:phon-ca/phonbank";
    final static String TARGET_TEST_FOLDER = "target/test/RoundTripTestsPhonBank";
    final static String GIT_CLONE_FOLDER = TARGET_TEST_FOLDER + "/phonbank";
    final static String OUT_ERROR_FOLDER = TARGET_TEST_FOLDER + "/errors";

    // step 1 - chat -> tb xml
    final static String IN_CHAT_FOLDER = GIT_CLONE_FOLDER + "/chat-todo";
    final static String OUT_XML_TB_FOLDER = TARGET_TEST_FOLDER + "/xml";

    // step 2 - tb xml -> phon
    final static String OUT_PHON_FOLDER = TARGET_TEST_FOLDER + "/phon";

    // step 3 - phon -> tb xml
    final static String OUT_PHON_XML_FOLDER = TARGET_TEST_FOLDER + "/phon-xml";

    // step 4 - tb xml -> chat
    final static String OUT_PHON_XML_CHAT_FOLDER = TARGET_TEST_FOLDER + "/phon-xml-chat";

    // step 5 - chat -> tb xml
    final static String OUT_CHAT_TB_FOLDER = TARGET_TEST_FOLDER + "/phon-xml-chat-xml";

    public static Collection<Object[]> collectFiles() throws IOException, InterruptedException {
        cloneGitRepo();
        List<Object[]> retVal = new ArrayList<>();

        final File phonXmlFolder = new File(IN_CHAT_FOLDER);
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
                } else if(path.getFileName().toString().endsWith(".cha")) {
                    final Path relPath = baseFolder.relativize(path);
                    xmlPaths.add(relPath);
                }
            }
        }
    }

    private String filename;

    public CHATShortRoundTrip(String filename) {
        this.filename = filename;
    }

    public boolean testRoundTrip() throws IOException, XMLStreamException {
        final File origCHATFile = new File(IN_CHAT_FOLDER, this.filename);

        System.out.println("Round trip file: " + filename);
        AtomicReference<Integer> intRef = new AtomicReference<>(0);
        final StringBuffer sb = new StringBuffer();
        final PhonTalkListener listener = (msg) -> {
            System.out.println(msg.getMessage());
            sb.append(msg.getMessage());
            sb.append("\n");
            if(!msg.getMessage().contains("adding") && !msg.getMessage().contains("setting"))
                intRef.set(intRef.get()+1);
        };

        final String xmlFileName = FilenameUtils.removeExtension(this.filename) + ".xml";
        // chat -> tb xml
        final File outTbFile = new File(OUT_XML_TB_FOLDER, xmlFileName);
        File outputFolder = outTbFile.getParentFile();
        if(!outputFolder.exists()) {
            outputFolder.mkdirs();
        }
        final CHAT2XmlConverter chatConverter = new CHAT2XmlConverter();
        chatConverter.convertFile(origCHATFile, outTbFile, listener);

        // tb xml -> phon
        final File outPhonFile = new File(OUT_PHON_FOLDER, xmlFileName);
        outputFolder = outPhonFile.getParentFile();
        if(!outputFolder.exists()) {
            outputFolder.mkdirs();
        }
        final SessionInputFactory inputFactory = new SessionInputFactory();
        final SessionReader reader = inputFactory.createReaderForFile(outTbFile);
        final Session session = reader.readSession(new FileInputStream(outTbFile));
        final SessionOutputFactory outputFactory = new SessionOutputFactory();
        final SessionWriter writer = outputFactory.createWriter();
        final OutputStream out = new FileOutputStream(outPhonFile);
        writer.writeSession(session, out);

        // phon -> tb xml
        final File outPhonXmlFile = new File(OUT_PHON_XML_FOLDER, xmlFileName);
        outputFolder = outPhonXmlFile.getParentFile();
        if(!outputFolder.exists()) {
            outputFolder.mkdirs();
        }
        final Phon2XmlConverter phonConverter = new Phon2XmlConverter();
        phonConverter.convertFile(outPhonFile, outPhonXmlFile, listener);

//        // tb xml -> chat
//        final File outPhonXmlChatFile = new File(OUT_PHON_XML_CHAT_FOLDER, this.filename);
//        outputFolder = outPhonXmlChatFile.getParentFile();
//        if(!outputFolder.exists()) {
//            outputFolder.mkdirs();
//        }
//        final Xml2CHATConverter xml2CHATConverter = new Xml2CHATConverter();
//        xml2CHATConverter.convertFile(outPhonXmlFile, outPhonXmlChatFile, listener);
//
//        // chat -> tb xml
//        final File outChatTbFile = new File(OUT_CHAT_TB_FOLDER, xmlFileName);
//        outputFolder = outChatTbFile.getParentFile();
//        if(!outputFolder.exists()) {
//            outputFolder.mkdirs();
//        }
//        final CHAT2XmlConverter chat2XmlConverter = new CHAT2XmlConverter();
//        chat2XmlConverter.convertFile(outPhonXmlChatFile, outChatTbFile, listener);

        // check number of reported errors
        // write log file
        if(!sb.toString().isEmpty()) {
            final File logFile = new File(outTbFile.getParent(), FilenameUtils.removeExtension(outTbFile.getName()) + "-log.txt");
            try (PrintWriter logWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(logFile), StandardCharsets.UTF_8))) {
                logWriter.write(sb.toString());
                logWriter.flush();
            }
        }
        if(intRef.get() > 0) {
            return false;
        }

        final String tbXml = FileUtils.readFileToString(outTbFile, StandardCharsets.UTF_8);
        final String phonTbXml = FileUtils.readFileToString(outPhonXmlFile, StandardCharsets.UTF_8);

        final Diff xmlDiff = DiffBuilder.compare(phonTbXml).withTest(tbXml)
                .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndAllAttributes))
                .ignoreWhitespace().ignoreComments().build();
        final DiffData diffData = sortCHATDiff(xmlDiff);

        if(!diffData.warnings().isEmpty())
            System.out.println(diffData.getWarningText());
        if(!diffData.errors().isEmpty())
            System.err.println(diffData.getErrorText());

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

        if(diffData.errors().size() > 0) {
            return false;
        }

        return true;
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
            if(d.getComparison().getType().name().equals("CHILD_NODELIST_SEQUENCE")) {
                warnings.add(d);
                continue;
            }
            if(d.getComparison().getType().name().equals("CHILD_LOOKUP") &&
                    d.getComparison().getControlDetails().getXPath() != null &&
                    d.getComparison().getControlDetails().getXPath().startsWith("/CHAT[1]/Participants[1]/participant") &&
                    d.getComparison().getTestDetails().getValue() == null) {
                warnings.add(d);
                continue;
            }
            if(d.getComparison().getType().name().equals("CHILD_LOOKUP") &&
                    d.getComparison().getControlDetails().getValue() == null &&
                    d.getComparison().getTestDetails().getXPath() != null &&
                    d.getComparison().getTestDetails().getXPath().startsWith("/CHAT[1]/Participants[1]/participant")) {
                warnings.add(d);
                continue;
            }
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
                if(d.getComparison().getControlDetails().getValue() != null && d.getComparison().getControlDetails().getValue().toString().contains("Original transcription")) {
                    warnings.add(d);
                    continue;
                }

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
                } else if(d.getComparison().getControlDetails().getXPath().contains("participant")) {
                    warnings.add(d);
                    continue;
                }
            } else if (d.getComparison().getType().name().equals("ATTR_NAME_LOOKUP")) {
                if (d.getComparison().getControlDetails().getXPath().contains("replacement")) {
                    // ignore for now
                    warnings.add(d);
                    continue;
                } else if(d.getComparison().getControlDetails().getXPath().endsWith("@language")) {
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

    public static void performTest(String filename) {
        final CHATShortRoundTrip roundTrip = new CHATShortRoundTrip(filename);
        try {
            if (!roundTrip.testRoundTrip()) {
                final File originalFile = new File(IN_CHAT_FOLDER, filename);
                final File errorFile = new File(OUT_ERROR_FOLDER, filename);
                final File errorFolder = errorFile.getParentFile();
                if (!errorFolder.exists()) {
                    errorFolder.mkdirs();
                }
                FileUtils.copyFile(originalFile, errorFile);
                // copy log file (if it exists)
                final File logFile = new File(OUT_XML_TB_FOLDER, FilenameUtils.removeExtension(filename) + "-log.txt");
                if (logFile.exists()) {
                    final File errorLogFile = new File(OUT_ERROR_FOLDER, FilenameUtils.removeExtension(filename) + "-log.txt");
                    FileUtils.copyFile(logFile, errorLogFile);
                }
                // copy diff file (if it exists)
                final File diffFile = new File(OUT_XML_TB_FOLDER, FilenameUtils.removeExtension(filename) + "-diff.txt");
                if (diffFile.exists()) {
                    final File errorDiffFile = new File(OUT_ERROR_FOLDER, FilenameUtils.removeExtension(filename) + "-diff.txt");
                    FileUtils.copyFile(diffFile, errorDiffFile);
                }
                numErrors.getAndSet(numErrors.get() + 1);
            } else {
                numPassed.getAndSet(numPassed.get() + 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            numErrors.getAndSet(numErrors.get() + 1);
        }
    }

    private static AtomicReference<Integer> numErrors = new AtomicReference<>(0);
    private static AtomicReference<Integer> numPassed = new AtomicReference<>(0);

    public static void main(String[] args) throws Exception {
        final Collection<Object[]> files = collectFiles();
        final PhonWorkerGroup workerGroup = new PhonWorkerGroup(8);
        workerGroup.setFinalTask(() -> {
            System.out.println("--------------------");
            System.out.println("All tasks completed");
            System.out.println("Errors: " + numErrors.get());
            System.out.println("Passed: " + numPassed.get());
        });
        for(Object[] file:files) {
            workerGroup.queueTask(() -> {
                performTest((String)file[0]);
            });
        }
        System.out.println("Starting tasks... (" + files.size() + ")");
        workerGroup.setTotalTasks(files.size());
        workerGroup.begin();
    }

}
