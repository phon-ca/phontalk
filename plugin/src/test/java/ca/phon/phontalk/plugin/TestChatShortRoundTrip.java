package ca.phon.phontalk.plugin;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.Collection;

/**
 * Read test files in FluencyBank (talkbank format) and then write them back in talkbank format.
 * Tests xml equivalence of two files using xmlunit.
 */
// uncomment to run test, will take a long time
@RunWith(Parameterized.class)
public class TestChatShortRoundTrip {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> collectFiles() throws IOException, InterruptedException {
        return CHATShortRoundTrip.collectFiles();
    }

    private String filename;

    public TestChatShortRoundTrip(String filename) {
        this.filename = filename;
    }

    @Test
    public void testRoundTrip() throws IOException, XMLStreamException {
        final CHATShortRoundTrip shortRoundTrip = new CHATShortRoundTrip(filename);
        final boolean rtComplete = shortRoundTrip.testRoundTrip();
        Assert.assertEquals(true, rtComplete);
    }

}
