package ca.phon.phontalk;

import ca.phon.formatter.Formatter;
import ca.phon.formatter.FormatterFactory;
import ca.phon.session.*;
import ca.phon.session.Record;
import ca.phon.session.io.xml.OneToOne;
import ca.phon.session.io.xml.SessionXMLStreamWriter;
import ca.phon.session.io.xml.XMLFragments;
import ca.phon.session.tierdata.*;
import ca.phon.util.Language;
import ca.phon.xml.DelegatingXMLStreamWriter;
import com.sun.xml.txw2.output.IndentingXMLStreamWriter;

import javax.xml.stream.*;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stax.StAXResult;
import javax.xml.transform.stax.StAXSource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

public class TalkbankWriter {

    private final static String TBNS = "http://www.talkbank.org/ns/talkbank";

    private final static String TBNS_SCHEMA_LOCATION = "http://www.talkbank.org/ns/talkbank https://talkbank.org/software/talkbank.xsd";

    private final List<PhonTalkListener> listeners = new ArrayList<>();

    private String file;

    public final static boolean DEFAULT_FORMATTED_OUTPUT = true;
    private boolean formattedOutput  = DEFAULT_FORMATTED_OUTPUT;

    public boolean isFormattedOutput() {
        return formattedOutput;
    }

    public void setFormattedOutput(boolean formattedOutput) {
        this.formattedOutput = formattedOutput;
    }

    /**
     * Write to file
     *
     * @param filename
     * @throws XMLStreamException
     */
    public void writeSession(Session session, String filename) throws IOException {
        try(final FileOutputStream out = new FileOutputStream(filename)) {
            writeSession(session, out);
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
    }

    /**
     * Write to stream
     *
     * @param stream
     * @throws XMLStreamException
     */
    public void writeSession(Session session, OutputStream stream) throws XMLStreamException {
        final XMLOutputFactory outputFactory = XMLOutputFactory.newFactory();
        XMLStreamWriter writer = outputFactory.createXMLStreamWriter(stream, "UTF-8");
        writer = new SessionXMLStreamWriter(writer, isFormattedOutput());
        writer.writeStartDocument();
        writeCHAT(session, writer);
    }

    // region XML Writing
    public void writeCHAT(Session session, XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement("CHAT");
        writer.writeDefaultNamespace(TBNS);
        writer.writeNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        writer.writeAttribute("http://www.w3.org/2001/XMLSchema-instance", "schemaLocation", TBNS_SCHEMA_LOCATION);

        // setup attributes
        writer.writeAttribute("Version", "2.21.0");

        if(session.getDate() != null) {
            final Formatter<LocalDate> dateFormatter = FormatterFactory.createFormatter(LocalDate.class);
            writer.writeAttribute("Date", dateFormatter.format(session.getDate()));
        }

        if(session.getCorpus() != null && !session.getCorpus().isBlank()) {
            writer.writeAttribute("Corpus", session.getCorpus());
        }

        if(session.getMetadata().get("Videos") != null) {
            writer.writeAttribute("Videos", session.getMetadata().get("Videos"));
        }

        if(session.getMediaLocation() != null) {
            // TODO CHAT only wants name of file without extension
            writer.writeAttribute("Media", session.getMediaLocation());
        }

        if(session.getMetadata().get("Mediatypes") != null) {
            writer.writeAttribute("Mediatypes", session.getMetadata().get("Mediatypes"));
        }

        if(!session.getLanguages().isEmpty()) {
            final String langTxt = session.getLanguages().stream()
                    .map(Language::toString)
                    .collect(Collectors.joining(" "));
            writer.writeAttribute("Lang", langTxt);
        }

        if(session.getMetadata().containsKey("Options")) {
            writer.writeAttribute("Options", session.getMetadata().get("Options"));
        }

        if(session.getMetadata().containsKey("DesignType")) {
            writer.writeAttribute("DesignType", session.getMetadata().get("DesignType"));
        }

        if(session.getMetadata().containsKey("ActivityType")) {
            writer.writeAttribute("ActivityType", session.getMetadata().get("ActivityType"));
        }

        if(session.getMetadata().containsKey("GroupType")) {
            writer.writeAttribute("GroupType", session.getMetadata().get("GroupType"));
        }

        if(session.getMetadata().containsKey("Colorwords")) {
            writer.writeAttribute("Colorwords", session.getMetadata().get("Colorwords"));
        }

        if(session.getMetadata().containsKey("Window")) {
            writer.writeAttribute("Window", session.getMetadata().get("Window"));
        }

        if(session.getMetadata().containsKey("PID")) {
            writer.writeAttribute("PID", session.getMetadata().get("PID"));
        }

        if(session.getMetadata().containsKey("Font")) {
            writer.writeAttribute("Font", session.getMetadata().get("Font"));
        }

        writer.writeStartElement("Participants");
        // write participants
        for(Participant participant:session.getParticipants()) {
            writeParticipant(participant, writer);
        }
        writer.writeEndElement();

        int uid = 0;
        // write transcript
        for(Transcript.Element element:session.getTranscript()) {
            if(element.isComment()) {
                writeComment(element.asComment(), writer);
            } else if(element.isGem()) {
                writeGem(element.asGem(), writer);
            } else if(element.isRecord()) {
                writeRecord(element.asRecord(), uid++, element.asRecord().getSpeaker().getId(), writer);
            }
        }

        writer.writeEndElement();
        writer.writeEndDocument();
    }

    private void writeParticipant(Participant participant, XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement("participant");
        // required
        writer.writeAttribute("id", participant.getId());
        writer.writeAttribute("role", participant.getRole().getTitle().replaceAll("\\s", "_"));

        if(participant.getName() != null) {
            writer.writeAttribute("name", participant.getName());
        }
        if(participant.getAge(null) != null) {
            writer.writeAttribute("age", participant.getAge(null).toString());
        }
        if(participant.getGroup() != null) {
            writer.writeAttribute("group", participant.getGroup());
        }
        if(participant.getSex() != Sex.UNSPECIFIED) {
            writer.writeAttribute("sex", participant.getSex().getText().toLowerCase());
        }
        if(participant.getSES() != null) {
            writer.writeAttribute("SES", participant.getSES());
        }
        if(participant.getEducation() != null) {
            writer.writeAttribute("education", participant.getEducation());
        }
        if(participant.getBirthDate() != null) {
            final Formatter<LocalDate> dateFormatter = FormatterFactory.createFormatter(LocalDate.class);
            writer.writeAttribute("birthday", dateFormatter.format(participant.getBirthDate()));
        }
        if(participant.getLanguage() != null) {
            writer.writeAttribute("language", participant.getLanguage());
        }
        if(participant.getFirstLanguage() != null) {
            writer.writeAttribute("first-language", participant.getFirstLanguage());
        }
        if(participant.getBirthplace() != null) {
            writer.writeAttribute("birthplace", participant.getBirthplace());
        }
        if(participant.getOther() != null) {
            writer.writeAttribute("custom-field", participant.getOther());
        }
        writer.writeEndElement();
    }

    private void writeComment(Comment comment, XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement("comment");
        writer.writeAttribute("type", comment.getType().getLabel());
        writeTierData(comment.getValue(), writer);
        writer.writeEndElement();
    }

    private void writeTierData(TierData tierData, XMLStreamWriter writer) throws XMLStreamException {
        for(int i = 0; i < tierData.length(); i++) {
            final TierElement ele = tierData.elementAt(i);
            if (ele instanceof TierString ts) {
                if(i > 0) writer.writeCharacters(" ");
                writer.writeCharacters(ts.text());
            } else if (ele instanceof TierInternalMedia tim) {
                writer.writeStartElement("media");
                writer.writeAttribute("start", Integer.toString((int) (tim.getStartTime() * 1000.0f)));
                writer.writeAttribute("end", Integer.toString((int) (tim.getEndTime() * 1000.0f)));
                writer.writeAttribute("unit", "ms");
                writer.writeEndElement();
            } else if (ele instanceof TierLink tl) {
                if ("pic".equals(tl.getLabel())) {
                    writer.writeStartElement("mediaPic");
                    writer.writeAttribute("href", tl.getHref());
                    writer.writeEndElement();
                } else {
                    writer.writeCharacters(tl.text());
                }
            }
        }
    }

    private void writeGem(Gem gem, XMLStreamWriter writer) throws XMLStreamException {
        switch (gem.getType()) {
            case Begin -> writer.writeStartElement("begin-gem");
            case End -> writer.writeStartElement("end-gem");
            case Lazy -> writer.writeStartElement("lazy-gem");
        };
        writer.writeAttribute("label", gem.getLabel());
        writer.writeEndElement();
    }

    private void writeRecord(Record record, int uid, String who, XMLStreamWriter writer) throws XMLStreamException {
        OneToOne.annotateRecord(record);
        try {
            final String utteranceXml = XMLFragments.toXml(record.getOrthography(), false, false);
            final XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();
            final XMLStreamReader uReader = xmlInputFactory.createXMLStreamReader(new ByteArrayInputStream(utteranceXml.getBytes(StandardCharsets.UTF_8)));
            final XMLStreamWriter userTierWriter = new RecordXmlStreamWriter(writer, record, uid, who);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer t = tf.newTransformer();
            StAXSource source = new StAXSource(uReader);
            StAXResult result = new StAXResult(userTierWriter);
            t.transform(source, result);
        } catch (IOException | TransformerException e) {
            throw new XMLStreamException(e);
        } finally {
            OneToOne.removeAnnotations(record);
        }
    }
    // endregion XML Writing

    // region Listeners
    private void fireWarning(String message, XMLStreamWriter writer) {
        final PhonTalkMessage msg = new PhonTalkMessage(message, PhonTalkMessage.Severity.WARNING);
        msg.setLineNumber(-1);
        msg.setColNumber(-1);
        if(file != null)
            msg.setFile(new File(file));
        fireMessage(msg);
    }

    private void fireMessage(PhonTalkMessage message) {
        listeners.forEach(l -> l.message(message));
    }

    public void addListener(PhonTalkListener listener) {
        if(!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    // endregion Listeners

    private class RecordXmlStreamWriter extends DelegatingXMLStreamWriter {

        private final Record record;

        private final int uid;

        private final String who;

        private final Stack<String> eleStack = new Stack<>();

        public RecordXmlStreamWriter(XMLStreamWriter delegate, Record record, int uid, String who) {
            super(delegate);
            this.record = record;
            this.uid = uid;
            this.who = who;
        }

        @Override
        public void writeStartElement(String localName) throws XMLStreamException {
            super.writeStartElement(localName);
            if("u".equals(localName)) {
                writeAttribute("uID", "u" + uid);
                writeAttribute("who", who);
            }
            eleStack.push(localName);
        }

        @Override
        public void writeStartElement(String namespaceURI, String localName) throws XMLStreamException {
            super.writeStartElement(namespaceURI, localName);
            eleStack.push(localName);
        }

        @Override
        public void writeStartElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
            super.writeStartElement(prefix, localName, namespaceURI);
            eleStack.push(localName);
        }

        @Override
        public void writeEndElement() throws XMLStreamException {
            final String eleName = eleStack.pop();
            if("u".equals(eleName)) {
                // write user tiers
                // notes first
                if(record.getNotesTier().hasValue() && record.getNotesTier().getValue().length() > 0) {
                    writeStartElement("a");
                    writeAttribute("type", "comments");
                    writeTierData(record.getNotes(), this);
                    writeCharacters(record.getNotes().toString());
                    writeEndElement();
                }

                // other user tiers
                for(String tierName:record.getUserDefinedTierNames()) {
                    final UserTierType userTierType = UserTierType.fromPhonTierName(tierName);
                    if(userTierType == UserTierType.Mor || userTierType == UserTierType.Trn
                        || userTierType == UserTierType.Gra || userTierType == UserTierType.Grt)
                        continue;
                    final Tier<?> tier = record.getTier(tierName);
                    if(tier == null || !tier.hasValue()) continue;
                    writeStartElement("a");
                    if(userTierType != null) {
                        final String type = userTierType.getTierName();
                        writeAttribute("type", type);
                    } else {
                        writeAttribute("type", "extension");
                        writeAttribute("flavor", tierName);
                    }
                    if(tier.getDeclaredType() == TierData.class) {
                        writeTierData((TierData) tier.getValue(), this);
                    } else {
                        writeCharacters(tier.toString());
                    }
                    writeEndElement();
                }

                // TODO IPA syllabification and alignment
            }
            super.writeEndElement();
        }

        @Override
        public void writeEndDocument() throws XMLStreamException {
        }

        @Override
        public void close() throws XMLStreamException {
        }

        @Override
        public void writeStartDocument() throws XMLStreamException {
        }

        @Override
        public void writeStartDocument(String version) throws XMLStreamException {
        }

        @Override
        public void writeStartDocument(String encoding, String version) throws XMLStreamException {
        }
    }

}
