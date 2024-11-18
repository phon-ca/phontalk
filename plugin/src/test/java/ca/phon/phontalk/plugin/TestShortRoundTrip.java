package ca.phon.phontalk.plugin;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.util.Collection;

/**
 * Read test files in FluencyBank (talkbank format) and then write them back in talkbank format.
 * Tests xml equivalence of two files using xmlunit.
 */
// uncomment to run test, will take a long time
@RunWith(Parameterized.class)
public class TestShortRoundTrip {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> collectFiles() throws IOException, InterruptedException {
        return ShortRoundTrip.collectFiles();
    }

    private String filename;

    public TestShortRoundTrip(String filename) {
        this.filename = filename;
    }

    @Test
    public void testRoundTrip() throws IOException, XMLStreamException {
        final ShortRoundTrip shortRoundTrip = new ShortRoundTrip(filename);
        final boolean rtComplete = shortRoundTrip.testRoundTrip();
        Assert.assertEquals(true, rtComplete);
    }

}
