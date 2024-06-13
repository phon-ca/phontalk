package ca.phon.phontalk;

import ca.phon.formatter.Formatter;
import ca.phon.formatter.FormatterFactory;
import ca.phon.orthography.Orthography;
import ca.phon.orthography.OrthographyBuilder;
import ca.phon.orthography.Terminator;
import ca.phon.orthography.TerminatorType;
import ca.phon.session.*;
import ca.phon.session.Record;
import ca.phon.session.alignment.CrossTierAlignment;
import ca.phon.session.alignment.TierAligner;
import ca.phon.session.io.xml.OneToOne;
import ca.phon.session.io.xml.SessionXMLStreamWriter;
import ca.phon.session.io.xml.XMLFragments;
import ca.phon.session.tierdata.*;
import ca.phon.util.Language;
import ca.phon.xml.DelegatingXMLStreamWriter;
import org.apache.commons.lang3.StringEscapeUtils;

import javax.xml.stream.*;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stax.StAXResult;
import javax.xml.transform.stax.StAXSource;
import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class TalkbankWriter {

    private final static String TBNS = "http://www.talkbank.org/ns/talkbank";

    private final static String TBNS_SCHEMA_LOCATION = "http://www.talkbank.org/ns/talkbank https://talkbank.org/software/talkbank.xsd";

    private final static String TB_VERSION = "3.0.0";

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
        writer.writeStartDocument("UTF-8", "1.0");
        writeCHAT(session, writer);
    }

    // region XML Writing
    public void writeCHAT(Session session, XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement("CHAT");
        writer.writeDefaultNamespace(TBNS);
        writer.writeNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        writer.writeAttribute("http://www.w3.org/2001/XMLSchema-instance", "schemaLocation", TBNS_SCHEMA_LOCATION);

        // setup attributes
        writer.writeAttribute("Version", TB_VERSION);

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

        if(session.getMediaLocation() != null && !session.getMediaLocation().isBlank()) {
            final String mediaName = new File(session.getMediaLocation()).getName();
            final String mediaWithoutExt = mediaName.lastIndexOf('.') > 0 ? mediaName.substring(0, mediaName.lastIndexOf('.')) : mediaName;
            writer.writeAttribute("Media", mediaWithoutExt);

            // get extension
            final String ext = mediaName.lastIndexOf('.') > 0 ? mediaName.substring(mediaName.lastIndexOf('.')) : "";
            if(ext.isBlank() || ".wav".equalsIgnoreCase(ext) || ".mp3".equalsIgnoreCase(ext) || ".m4a".equalsIgnoreCase(ext)) {
                writer.writeAttribute("Mediatypes", "audio");
            } else {
                writer.writeAttribute("Mediatypes", "video");
            }
        }

        if(session.getMetadata().get("Mediatypes") != null) {
            writer.writeAttribute("Mediatypes", session.getMetadata().get("Mediatypes"));
        }

        if(!session.getLanguages().isEmpty()) {
            final String langTxt = session.getLanguages().stream()
                    .map(Language::toString)
                    .collect(Collectors.joining(" "));
            writer.writeAttribute("Lang", langTxt);
        } else {
            final String lang = Locale.getDefault().getISO3Country();
            writer.writeAttribute("Lang",  lang);
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

        // write tier name mappings
        for(TierDescription userTierDesc:session.getUserTiers()) {
            final String abbrvTierName = "%x" + UserTierType.abbreviateTierName(userTierDesc.getName());
            final String chatTierName = UserTierType.determineCHATTierName(session, userTierDesc.getName());
            // only write when necessary
            if(userTierDesc.getName().length() < 7 && abbrvTierName.equals(chatTierName)) continue;
            if(chatTierName.startsWith("%x")) {
                final Comment tierComment = SessionFactory.newFactory().createComment(CommentType.Generic);
                try {
                    tierComment.setValue(TierData.parseTierData(String.format("%s = %s", chatTierName, userTierDesc.getName())));
                    writeComment(tierComment, writer);
                } catch (ParseException pe) {}
            }
        }

        int uid = 0;
        // write transcript
        for(Transcript.Element element:session.getTranscript()) {
            if(element.isComment()) {
                writeComment(element.asComment(), writer);
            } else if(element.isGem()) {
                writeGem(element.asGem(), writer);
            } else if(element.isRecord()) {
                writeRecord(session, element.asRecord(), uid++, writer);
            }
        }

        writer.writeEndElement();
        writer.writeEndDocument();
    }

    private void writeParticipant(Participant participant, XMLStreamWriter writer) throws XMLStreamException {
        writer.writeEmptyElement("participant");
        // required
        writer.writeAttribute("id", participant.getId());
        writer.writeAttribute("role", participant.getRole().getTitle().replaceAll("\\s", "_"));

        if(participant.getName() != null && !participant.getName().isBlank()) {
            writer.writeAttribute("name", participant.getName());
        }
        if(participant.getAge(null) != null) {
            writer.writeAttribute("age", participant.getAge(null).toString());
        }
        if(participant.getGroup() != null && !participant.getGroup().isBlank()) {
            writer.writeAttribute("group", participant.getGroup());
        }
        if(participant.getSex() != Sex.UNSPECIFIED) {
            writer.writeAttribute("sex", participant.getSex().getText().toLowerCase());
        }
        if(participant.getSES() != null && !participant.getSES().isBlank()) {
            writer.writeAttribute("SES", participant.getSES());
        }
        if(participant.getEducation() != null && !participant.getEducation().isBlank()) {
            writer.writeAttribute("education", participant.getEducation());
        }
        if(participant.getBirthDate() != null) {
            final Formatter<LocalDate> dateFormatter = FormatterFactory.createFormatter(LocalDate.class);
            writer.writeAttribute("birthday", dateFormatter.format(participant.getBirthDate()));
        }
        if(participant.getLanguage() != null && !participant.getLanguage().isBlank()) {
            writer.writeAttribute("language", participant.getLanguage());
        }
        if(participant.getFirstLanguage() != null && !participant.getFirstLanguage().isBlank()) {
            writer.writeAttribute("first-language", participant.getFirstLanguage());
        }
        if(participant.getBirthplace() != null && !participant.getBirthplace().isBlank()) {
            writer.writeAttribute("birthplace", participant.getBirthplace());
        }
        if(participant.getOther() != null && !participant.getOther().isBlank()) {
            writer.writeAttribute("custom-field", participant.getOther());
        }
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
                if (i > 0) writer.writeCharacters(" ");
                writer.writeCharacters(ts.text());
            } else if(ele instanceof TierComment tc) {
                if(i > 0) writer.writeCharacters(" ");
                writer.writeCharacters(tc.toString());
            } else if (ele instanceof TierInternalMedia tim) {
                writer.writeEmptyElement("media");
                writer.writeAttribute("start", String.format("%.3f", tim.getStartTime()));
                writer.writeAttribute("end", String.format("%.3f", tim.getEndTime()));
                writer.writeAttribute("unit", "s");
            } else if (ele instanceof TierLink tl) {
                if ("pic".equals(tl.getLabel())) {
                    writer.writeEmptyElement("mediaPic");
                    writer.writeAttribute("href", tl.getHref());
                } else {
                    if (i > 0) writer.writeCharacters(" ");
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

    private void writeRecord(Session session, Record record, int uid, XMLStreamWriter writer) throws XMLStreamException {
        try {
            if(!checkRecord(session, record, uid, writer)) {
                fireWarning("Skipping record #" + (uid+1), writer);
                return;
            }
            OneToOne.annotateRecord(record);
            final String utteranceXml = XMLFragments.toXml(record.getOrthography(), false, false);
            final XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();
            final XMLStreamReader uReader = xmlInputFactory.createXMLStreamReader(new ByteArrayInputStream(utteranceXml.getBytes(StandardCharsets.UTF_8)));
            final XMLStreamWriter userTierWriter = new RecordXmlStreamWriter(writer, session, record, uid, record.getSpeaker().getId());
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer t = tf.newTransformer();
            StAXSource source = new StAXSource(uReader);
            StAXResult result = new StAXResult(userTierWriter);
            t.transform(source, result);
        } catch (IOException | TransformerException e) {
            throw new XMLStreamException("Error writing record #" + (uid+1), e);
        } finally {
            OneToOne.removeAnnotations(record);
        }
    }

    private boolean checkRecord(Session session, Record record, int uid, XMLStreamWriter writer) {
        if(record.getSpeaker() == Participant.UNKNOWN) {
            if(session.getParticipantCount() > 0) {
                final Participant firstParticipant = session.getParticipant(0);
                fireWarning("Record #" + (uid + 1) + " has no speaker, setting to '"
                        + firstParticipant.getId() + "'", writer);
                record.setSpeaker(firstParticipant);
            }
        }

        if(record.getOrthographyTier().isUnvalidated()) {
            fireWarning("Record #" + (uid +1) + " has invalid orthography: " +
                    record.getOrthographyTier().getUnvalidatedValue().getParseError().toString(), writer);
            return false;
        } else {

            if (record.getOrthography() == null || record.getOrthography().length() == 0) {
                fireWarning("Record #" + (uid + 1) + " has no orthography, setting to 'xxx .'", writer);
                final OrthographyBuilder builder = new OrthographyBuilder();
                builder.append("xxx");
                builder.append(new Terminator(TerminatorType.PERIOD));
                record.setOrthography(builder.toOrthography());
            }

            if (record.getOrthography().length() > 0 && record.getOrthography().elementAt(0) instanceof Terminator) {
                fireWarning("Record #" + (uid + 1) + " has no orthography, setting to 'xxx " + record.getOrthography().toString() + "'", writer);
                final OrthographyBuilder builder = new OrthographyBuilder();
                builder.append("xxx");
                builder.append(record.getOrthography());
                record.setOrthography(builder.toOrthography());
            }
        }

        // check for missing CA terminator
        if(record.getOrthography().getTerminator() == null) {
            fireWarning("Record #" + (uid +1) + " is missing a CA terminator, adding '.'", writer);
            final OrthographyBuilder builder = new OrthographyBuilder();
            builder.append(record.getOrthography());
            builder.append(new Terminator(TerminatorType.PERIOD));
            record.setOrthography(builder.toOrthography());
        }

        if(record.getIPATargetTier().isUnvalidated()) {
            fireWarning("Record #" + (uid +1) + " has invalid IPA target: " +
                    record.getIPATargetTier().getUnvalidatedValue().getParseError().toString(), writer);
        }

        if(record.getIPAActualTier().isUnvalidated()) {
            fireWarning("Record #" + (uid +1) + " has invalid IPA actual: " +
                    record.getIPAActualTier().getUnvalidatedValue().getParseError().toString(), writer);
        }

        for(Tier<?> tier:record.getUserTiers()) {
            if(tier.isUnvalidated()) {
                fireWarning("Record #" + (uid +1) + " has invalid tier " + tier.getName() + ": " +
                        tier.getUnvalidatedValue().getParseError().toString(), writer);
            }
        }

        // check x-tier alignment
        CrossTierAlignment xTierAlignment = TierAligner.calculateCrossTierAlignment(record);
        if(!xTierAlignment.isComplete()) {
            fireWarning("Record #" + (uid +1) + " has incomplete cross-tier alignment", writer);
        }

        // check media
        if(record.getMediaSegment() != null) {
            final MediaSegment segment = record.getMediaSegment();
            if(segment.isPoint() && !segment.isPointAtOrigin()) {
                fireWarning("Record #" + (uid +1) + " media start and end cannot be the same, adding 1ms to end", writer);
                segment.setEndTimeMs(segment.getEndValueMs() + 1);
            }
        }

        return true;
    }

    private void writeWorTier(Orthography worData, XMLStreamWriter writer) throws XMLStreamException {
        try {
            final String xml = XMLFragments.toXml(worData, false, false);
            final XMLInputFactory inputFactory = XMLInputFactory.newFactory();
            final XMLStreamReader reader = inputFactory.createXMLStreamReader(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            final XMLStreamWriter w = new DelegatingXMLStreamWriter(writer) {
                @Override
                public void writeStartElement(String localName) throws XMLStreamException {
                    if ("u".equals(localName)) {
                        localName = "wor";
                    }
                    super.writeStartElement(localName);
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
            };
            final StAXSource source = new StAXSource(reader);
            final StAXResult result = new StAXResult(w);

            final TransformerFactory transformerFactory = TransformerFactory.newInstance();
            final Transformer transformer = transformerFactory.newTransformer();
            transformer.transform(source, result);
        } catch(IOException | TransformerException e) {
            throw new XMLStreamException(e);
        }
    }

    private void writeMedia(MediaSegment segment, XMLStreamWriter writer) throws XMLStreamException {
        writer.writeEmptyElement("media");
        float start = segment.getStartValue();
        start = segment.getUnitType() == MediaUnit.Millisecond ? start / 1000.0f : start;
        float end = segment.getEndValue();
        end = segment.getUnitType() == MediaUnit.Millisecond ? end / 1000.0f : end;
        writer.writeAttribute("start",
                BigDecimal.valueOf(start).setScale(3, RoundingMode.HALF_UP).toString());
        writer.writeAttribute("end",
                BigDecimal.valueOf(end).setScale(3, RoundingMode.HALF_UP).toString());
        writer.writeAttribute("unit", "s");
    }

    /**
     * Writes record data including any user-defined tiers
     */
    private class RecordXmlStreamWriter extends DelegatingXMLStreamWriter {

        private final Session session;

        private final Record record;

        private final int uid;

        private final String who;

        private final Stack<String> eleStack = new Stack<>();

        private boolean foundTerminator = false;

        private boolean inPos = false;

        public RecordXmlStreamWriter(XMLStreamWriter delegate, Session session, Record record, int uid, String who) {
            super(delegate);
            this.record = record;
            this.session = session;
            this.uid = uid;
            this.who = who;
        }


        @Override
        public void writeStartElement(String localName) throws XMLStreamException {
            if(inPos && "subc".equals(localName)) {
                localName = "s";
            }
            super.writeStartElement(localName);
            if("u".equals(localName)) {
                writeAttribute("who", who);
                writeAttribute("uID", "u" + uid);
            } else if("t".equals(localName)) {
                foundTerminator = true;
            } else if("pos".equals(localName)) {
                inPos = true;
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
        public void writeEmptyElement(String localName) throws XMLStreamException {
            super.writeEmptyElement(localName);
            if("t".equals(localName)) {
                foundTerminator = true;
            }
        }

        @Override
        public void writeEndElement() throws XMLStreamException {
            final String eleName = eleStack.pop();
            if("u".equals(eleName)) {
                if(!foundTerminator) {
                    writeEmptyElement("t");
                    writeAttribute("type", "missing CA terminator");
                }

                if(record.getSegmentTier().hasValue() && !record.getSegmentTier().getValue().isPointAtOrigin())
                    writeMedia(record.getMediaSegment(), this);

                // other user tiers
                for(String tierName:record.getUserDefinedTierNames()) {
                    final UserTierType userTierType = UserTierType.fromPhonTierName(tierName);
                    if(userTierType == UserTierType.Mor || userTierType == UserTierType.Trn
                            || userTierType == UserTierType.Gra || userTierType == UserTierType.Grt)
                        continue;
                    final Tier<?> tier = record.getTier(tierName);
                    if (tier == null || !tier.hasValue() || tier.getValue().toString().length() == 0) continue;
                    if(userTierType == UserTierType.Wor) {
                        writeWorTier((Orthography)tier.getValue(), this);
                    } else {
                        writeStartElement("a");
                        if (userTierType != null) {
                            final String type = userTierType.getTalkbankTierType();
                            writeAttribute("type", type);
                        } else {
                            String flavor = UserTierType.determineCHATTierName(session, tierName);
                            if (flavor.startsWith("%x")) {
                                flavor = flavor.substring(2);
                            }
                            writeAttribute("type", "extension");
                            writeAttribute("flavor", flavor);
                        }
                        if (tier.getDeclaredType() == TierData.class) {
                            writeTierData((TierData) tier.getValue(), this);
                        } else {
                            writeCharacters(tier.toString());
                        }
                        writeEndElement();
                    }
                }

                if(record.getNotesTier().hasValue() && record.getNotesTier().getValue().length() > 0) {
                    writeStartElement("a");
                    writeAttribute("type", "extension");
                    writeAttribute("flavor", "Notes");
                    writeTierData(record.getNotes(), this);
                    writeEndElement();
                }
                // TODO IPA syllabification and alignment
            } else if("pos".equals(eleName)) {
                inPos = false;
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

}
