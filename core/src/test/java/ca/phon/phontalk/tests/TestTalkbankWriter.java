package ca.phon.phontalk.tests;

import ca.phon.phontalk.TalkbankWriter;
import ca.phon.session.Session;
import ca.phon.session.io.SessionInputFactory;
import ca.phon.session.io.SessionReader;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RunWith(JUnit4.class)
public class TestTalkbankWriter {

    @Test
    public void testNewPhoWithMod() throws IOException, XMLStreamException {
        final String xml = """
                <session xmlns="https://phon.ca/ns/session" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" corpus="IDO" version="2.0" xsi:schemaLocation="https://phon.ca/ns/session https://phon.ca/xml/xsd/session/v2_0/session.xsd">
                    <date>2000-06-23</date>
                    <participants>
                        <participant id="CHI">
                            <role>Target_Child</role>
                            <name>Ido</name>
                            <sex>male</sex>
                            <age>P4Y5M21DT0H0M0S</age>
                            <languages>ind jav</languages>
                        </participant>
                    </participants>
                    <transcript>
                        <comment type="Date">
                            <tierData>
                                <tw>23-JUN-2000</tw>
                            </tierData>
                        </comment>
                        <comment type="Generic">
                            <tierData>
                                <tw>Test</tw>
                                <tw>new</tw>
                                <tw>pho</tw>
                                <tw>with</tw>
                                <tw>mod</tw>
                            </tierData>
                        </comment>
                        <r uuid="82234547-c1a8-421c-843f-4e87f6db5bfb" speaker="CHI" excludeFromSearches="false">
                            <orthography>
                                <u>
                                    <pg>
                                        <w>A</w>
                                    </pg>
                                    <pg>
                                        <w>B</w>
                                    </pg>
                                    <pg>
                                        <w>C</w>
                                    </pg>
                                    <t type="p"/>
                                </u>
                            </orthography>
                            <ipaTarget>
                                <pho>
                                    <pw>
                                        <ph>
                                            <base>d</base>
                                        </ph>
                                    </pw>
                                    <pw>
                                        <ph>
                                            <base>e</base>
                                        </ph>
                                    </pw>
                                    <pw>
                                        <ph>
                                            <base>f</base>
                                        </ph>
                                    </pw>
                                </pho>
                            </ipaTarget>
                            <ipaActual>
                                <pho>
                                    <pw>
                                        <ph>
                                            <base>a</base>
                                        </ph>
                                    </pw>
                                    <pw>
                                        <ph>
                                            <base>b</base>
                                        </ph>
                                    </pw>
                                    <pw>
                                        <ph>
                                            <base>c</base>
                                        </ph>
                                    </pw>
                                </pho>
                            </ipaActual>
                            <segment start="0.0" end="0.0" unit="ms"/>
                        </r>
                    </transcript>
                </session>""";
        final TalkbankWriter writer = new TalkbankWriter();
        final SessionInputFactory inputFactory = new SessionInputFactory();
        final SessionReader reader = inputFactory.createReader(inputFactory.availableReaders().get(0));
        final Session session = reader.readSession(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        final ByteArrayOutputStream bout = new ByteArrayOutputStream();
        writer.writeSession(session, bout);

        System.out.println(bout.toString(StandardCharsets.UTF_8));
    }
}
