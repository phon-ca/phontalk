package ca.phon.phontalk.plugin;

import ca.phon.orthography.Orthography;
import ca.phon.phontalk.*;
import ca.phon.session.Record;
import ca.phon.session.Session;
import ca.phon.session.io.SessionInputFactory;
import ca.phon.session.io.SessionReader;
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

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> collectFiles() throws IOException, InterruptedException {
        return ShortRoundTrip.collectFiles();
    }

    private String filename;

    public RoundTripTestsPhonBank(String filename) {
        this.filename = filename;
    }

    @Test
    public void testRoundTrip() throws IOException, XMLStreamException {
        final ShortRoundTrip shortRoundTrip = new ShortRoundTrip(filename);
        final boolean rtComplete = shortRoundTrip.testRoundTrip();
        Assert.assertEquals(true, rtComplete);
    }

}
