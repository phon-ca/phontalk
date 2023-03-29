package ca.phon.phontalk.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class RoundTripTests extends RoundTripTest {

    public RoundTripTests(String inputFile, String outputFolder) {
        super(inputFile, outputFolder);
    }

    @Parameters
    public static Collection<Object[]> testData() {
        List<Object[]> retVal = new ArrayList<>();

        final File testFolder = new File("src/test/resources/ca/phon/phontalk/tests/RoundTripTests/");
        final String outputFolder = "target/test/ca/phon/phontalk/tests/RoundTripTests/";

        for(File testFile:testFolder.listFiles()) {
            if(!testFile.getName().endsWith(".cha")) continue;
            retVal.add(new Object[]{testFile.getAbsolutePath(), outputFolder});
        }
        retVal.sort(Comparator.comparing(c -> c[0].toString()));

        return retVal;
    }

}
