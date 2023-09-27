package ca.phon.phontalk.tb2phon;

import ca.phon.extensions.UnvalidatedValue;
import ca.phon.ipa.IPATranscript;
import ca.phon.orthography.Orthography;
import ca.phon.phontalk.PhonTalkListener;
import ca.phon.phontalk.PhonTalkMessage;
import ca.phon.session.*;
import ca.phon.session.Record;
import ca.phon.session.io.SessionOutputFactory;
import ca.phon.session.io.SessionWriter;
import ca.phon.session.io.xml.XMLFragments;
import ca.phon.session.tierdata.TierData;
import org.apache.commons.lang3.StringEscapeUtils;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.*;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class TalkbankReader {

    private final static String TBNS = "http://www.talkbank.org/ns/talkbank";

    private final List<PhonTalkListener> listeners = new ArrayList<>();

    private final SessionFactory factory = SessionFactory.newFactory();

    private String file;

    /**
     * Read talkbank xml file
     *
     * @param file
     * @throws IOException
     */
    public Session readFile(String file) throws IOException, XMLStreamException {
        try(InputStream in = new FileInputStream(file)) {
            this.file = file;
            return readStream(in);
        }
    }

    /**
     * Read stream as talkbank xml file
     *
     * @param stream
     * @throws IOException
     */
    public Session readStream(InputStream stream) throws XMLStreamException {
        final XMLInputFactory inputFactory = XMLInputFactory.newFactory();
        final XMLStreamReader reader = inputFactory.createXMLStreamReader(stream, "UTF-8");
        while(readToNextElement(reader)) {
            if(reader.hasName() && "CHAT".equals(reader.getLocalName())) {
                return readCHAT(reader);
            }
        }
        return null;
    }

    // region XML Processing

    /**
     * Read CHAT element and return Session object
     *
     * @param reader
     * @return
     * @throws XMLStreamException
     */
    private Session readCHAT(XMLStreamReader reader) throws XMLStreamException {
        if(!"CHAT".equalsIgnoreCase(reader.getLocalName())) throw new XMLStreamException();

        final Session session = factory.createSession();
        // TODO attributes

        // flag used to avoid skipping elements
        boolean dontSkip = false;
        while(dontSkip || readToNextElement(reader)) {
            if(dontSkip) dontSkip = false;
            final String eleName = reader.getLocalName();
            switch (eleName) {
                case "Participants":
                    readParticipants(session, reader);
                    dontSkip = true;
                    break;

                case "comment":
                    readComment(session, reader);
                    break;

                case "u":
                    readRecord(session, reader);
                    break;

                case "lazy-gem":
                case "begin-gem":
                case "end-gem":
                    readGem(reader);
                    break;

                default:
                    break;
            }
        }

        return session;
    }

    /**
     * Read participants and add them to given session
     *
     * @param session
     * @param reader
     * @throws XMLStreamException
     */
    private void readParticipants(Session session, XMLStreamReader reader) throws XMLStreamException {

        while(readToNextElement(reader) && "participant".equals(reader.getLocalName())) {
            final Participant participant = readParticipant(reader);
            session.addParticipant(participant);
        }
    }

    /**
     * Read participant
     *
     * @param reader
     * @return participant
     * @throws XMLStreamException
     */
    public Participant readParticipant(XMLStreamReader reader) throws XMLStreamException {
        if(!"participant".equals(reader.getLocalName())) throw new XMLStreamException();
        final Participant participant = factory.createParticipant();

        // ensure we have a valid id and role
        String id = reader.getAttributeValue(null, "id");
        if(id == null) {
            fireWarning("No id specified for participant", reader);
        }
        String role = reader.getAttributeValue(null, "role");
        if(role == null) {
            fireWarning("No role specified for participant", reader);
            role = "Target_Child";
        }
        ParticipantRole participantRole = ParticipantRole.fromString(role);
        if(participantRole == null) {
            participantRole = fireWarningOnType("Unsupported role type " + role, reader, ParticipantRole.PARTICIPANT);
        }
        if(id == null) {
            id = participantRole.getId();
            // TODO update id based on number of participants with role
        }
        participant.setId(id);
        participant.setRole(participantRole);

        // name (if any)
        final String name = reader.getAttributeValue(null, "name");
        if(name != null)
            participant.setName(name);

        // age
        final String ageTxt = reader.getAttributeValue(null, "age");
        if(ageTxt != null) {
            try {
                final Period duration = Period.parse(ageTxt);
                participant.setAge(duration);
            } catch (DateTimeParseException e) {
                fireWarning(e.getLocalizedMessage(), reader);
            }
        }

        // group
        final String group = reader.getAttributeValue(null, "group");
        if(group != null)
            participant.setGroup(group);

        // sex
        final String sex = reader.getAttributeValue(null, "sex");
        if(sex != null) {
            Sex s = switch (sex) {
                case "male" -> Sex.MALE;
                case "female" -> Sex.FEMALE;
                default -> null;
            };
            participant.setSex(s);
        }

        // SES
        final String SES = reader.getAttributeValue(null, "SES");
        if(SES != null)
            participant.setSES(SES);

        // education
        final String education = reader.getAttributeValue(null, "education");
        if(education != null)
            participant.setEducation(education);

        // custom-field
        final String custom = reader.getAttributeValue(null, "custom-field");
        if(custom != null) {
            participant.setOther(custom);
        }

        // birthday
        final String bday = reader.getAttributeValue(null, "date");
        if(bday != null) {
            try {
                LocalDate date = LocalDate.parse(bday);
                participant.setBirthDate(date);
            } catch (DateTimeParseException e) {
                fireWarning(e.getLocalizedMessage(), reader);
            }
        }

        // language
        final String lang = reader.getAttributeValue(null, "language");
        if(lang != null) {
            participant.setLanguage(lang);
        }

        // first-language
        final String firstLang = reader.getAttributeValue(null, "first-language");
        if(firstLang != null) {
            participant.setFirstLanguage(firstLang);
        }

        // birthplace
        final String birthplace = reader.getAttributeValue(null, "birthplace");
        if(birthplace != null) {
            participant.setBirthplace(birthplace);
        }

        return participant;
    }

    /**
     * Read comment
     *
     * @param reader
     * @return comment
     * @throws XMLStreamException
     */
    private Comment readComment(Session session, XMLStreamReader reader) throws XMLStreamException {
        if(!"comment".equals(reader.getLocalName())) throw new XMLStreamException();
        final Comment comment = factory.createComment();

        // read type
        final String type = reader.getAttributeValue(null, "type");
        CommentType commentType =
                type != null ? CommentType.fromString(type) : CommentType.Generic;
        commentType = commentType == null ? CommentType.Generic : commentType;
        comment.setType(commentType);

        StringBuilder builder = new StringBuilder();
        while(reader.hasNext()) {
            reader.next();
            if(reader.isCharacters()) {
                if(!builder.isEmpty()) builder.append(" ");
                builder.append(reader.getText());
            } else if(reader.isStartElement() && "media".equals(reader.getLocalName())) {
                // TODO read media
            } else if(reader.isStartElement() && "mediaPic".equals(reader.getLocalName())) {
                // TODO read mediaPic
            } else {
                break;
            }
        }
        TierData commentData = new TierData();
        try {
            commentData = TierData.parseTierData(builder.toString());
        } catch (ParseException pe) {
            fireWarning(pe.getMessage(), reader);
            commentData.putExtension(UnvalidatedValue.class, new UnvalidatedValue(builder.toString(), pe));
        }
        comment.setValue(commentData);

        session.getTranscript().addComment(comment);

        return comment;
    }

    /**
     * Read record
     *
     * @param reader
     * @return record
     * @throws XMLStreamException
     */
    private Record readRecord(Session session, XMLStreamReader reader) throws XMLStreamException {
        if(!"u".equals(reader.getLocalName())) throw new XMLStreamException();
        final Record r = factory.createRecord();

        UtteranceTierData utd = readUtterance(reader);
        r.setOrthography(utd.orthography);

        session.getTranscript().addRecord(r);

        return r;
    }

    /**
     * Read one of: lazy-gem, begin-gem, end-gem
     *
     * @param reader
     * @return
     * @throws XMLStreamException
     */
    private Gem readGem(XMLStreamReader reader) throws XMLStreamException {
        final String eleName = reader.hasName() ? reader.getLocalName() : "";
        final GemType gemType =  switch(eleName) {
            case "lazy-gem" -> GemType.Lazy;
            case "begin-gem" -> GemType.Begin;
            case "end-gem" -> GemType.End;
            default -> fireWarningOnType("Invalid gem element name " + eleName, reader, GemType.Lazy);
        };
        return factory.createGem(gemType, reader.getText());
    }

    // endregion XML Processing

    // region XML Utils
    private boolean readToNextElement(XMLStreamReader reader) throws XMLStreamException {
        while(reader.hasNext()) {
            int eventType = reader.next();
            if(eventType == XMLStreamReader.START_ELEMENT) {
                return true;
            }
        }
        return false;
    }

    private boolean readToEndTag(XMLStreamReader reader) throws XMLStreamException {
        if(!reader.isStartElement()) throwNotStart();
        final String eleName = reader.getLocalName();
        while(reader.hasNext()) {
            reader.next();
            if(reader.isEndElement() && reader.getLocalName().equals(eleName)) {
                return true;
            }
        }
        return false;
    }

    record UtteranceTierData(Orthography orthography, List<Tier<IPATranscript>> ipaTiers, List<Tier<TierData>> morTiers) {}
    private UtteranceTierData readUtterance(XMLStreamReader reader) throws XMLStreamException {
        if(!reader.isStartElement() && !"u".equalsIgnoreCase(reader.getLocalName())) throw new XMLStreamException("Not a start element");

        final StringBuilder builder = new StringBuilder();
        List<Tier<IPATranscript>> ipaTiers = new ArrayList<>();
        List<Tier<TierData>> morTiers = new ArrayList<>();

        builder.append("<u xmlns=\"https://phon.ca/ns/session\"");
        final String lang = reader.getAttributeValue("http://www.w3.org/XML/1998/namespace", "lang");
        if(lang != null) {
            builder.append(" xml:lang=\"").append(lang).append("\"");
        }
        builder.append(">");
        readUtteranceContent(reader, builder, ipaTiers, morTiers);
        builder.append("</u>");

        try {
            final Orthography ortho = XMLFragments.orthographyFromXml(builder.toString());
            return new UtteranceTierData(ortho, ipaTiers, morTiers);
        } catch (IllegalArgumentException | IOException e) {
            throw new XMLStreamException(e);
        }
    }

    private void readUtteranceElement(XMLStreamReader reader, StringBuilder builder, List<Tier<IPATranscript>> ipaTiers, List<Tier<TierData>> morTiers) throws XMLStreamException {
        if(!reader.isStartElement()) throw new XMLStreamException();
        final String eleName = reader.getLocalName();

        builder.append("<").append(eleName);
        appendAttributes(builder, reader);
        builder.append(">");
        readUtteranceContent(reader, builder, ipaTiers, morTiers);
        builder.append("</").append(eleName).append(">");
    }

    private void readUtteranceContent(XMLStreamReader reader, StringBuilder builder, List<Tier<IPATranscript>> ipaTiers, List<Tier<TierData>> morTiers) throws XMLStreamException {
        boolean readTerminator = false;
        final String originalEleName = reader.getLocalName();
        while(!readTerminator && reader.hasNext()) {
            reader.next();
            if(reader.isCharacters()) {
                builder.append(reader.getText());
            } else if(reader.isStartElement()) {
                final String eleName = reader.getLocalName();
                switch (eleName) {
                    case "mor":
                        // TODO handle mor tiers
                        readToEndTag(reader);
                        break;

                    case "actual", "model":
                        // TODO handle ipa tiers
                        readToEndTag(reader);
                        break;

                    case "align":
                        // old-data, ignore
                        readToEndTag(reader);
                        break;

                    case "t":
                        readTerminator = true;
                    default:
                        readUtteranceElement(reader, builder, ipaTiers, morTiers);
                        break;
                }
            } else if(reader.isEndElement() && reader.getLocalName().equals(originalEleName)) {
                break;
            }
        }
    }

    private void readMor(XMLStreamReader reader, StringBuilder builder, List<Tier<TierData>> morTiers) throws XMLStreamException {
        if(!reader.isStartElement() || !"mor".equals(reader.getLocalName())) throwNotElement("mor", reader.getLocalName());

    }

    private void throwNotStart() throws XMLStreamException {
        throw new XMLStreamException("Expected start element");
    }

    private void throwNotElement(String expectedName, String eleName) throws XMLStreamException {
        throw new XMLStreamException(String.format("Expected element '%s' but got '%s'", expectedName, eleName));
    }

    private String recreateElementXML(XMLStreamReader reader, List<String> ignoreElements, boolean includeAttributesOfParent)
        throws XMLStreamException{
        if(!reader.isStartElement()) throw new XMLStreamException("Not a start element");
        final StringBuilder builder = new StringBuilder();

        appendElementStartXml(builder, reader, includeAttributesOfParent);
        appendElementContent(builder, reader);
        builder.append("</").append(reader.getLocalName()).append(">");

        return builder.toString();
    }

    private void appendElement(StringBuilder builder, XMLStreamReader reader) throws XMLStreamException {
        if(!reader.isStartElement()) throw new XMLStreamException();
        final String eleName = reader.getLocalName();

        builder.append("<").append(eleName);
        appendAttributes(builder, reader);
        builder.append(">");
        appendElementContent(builder, reader);
        builder.append("</").append(eleName).append(">");
    }

    private void appendElementContent(StringBuilder builder, XMLStreamReader reader) throws XMLStreamException {
        while(reader.hasNext()) {
            reader.next();
            if(reader.isCharacters()) {
                builder.append(reader.getText());
            } else if(reader.isStartElement()) {
                appendElement(builder, reader);
            } else if(reader.isEndElement()) {
                break;
            }
        }
    }

    private void appendAttributes(StringBuilder builder, XMLStreamReader reader) {
        for(int i = 0; i < reader.getAttributeCount(); i++) {
            builder.append(" ");
            builder.append(reader.getAttributeLocalName(i));
            builder.append("=\"");
            builder.append(StringEscapeUtils.escapeXml(reader.getAttributeValue(i)));
            builder.append("\"");
        }
    }

    private void appendElementStartXml(StringBuilder builder, XMLStreamReader reader, boolean includeAttributes) {
        builder.append("<").append(reader.getLocalName());
        if(includeAttributes) {
            appendAttributes(builder, reader);
        }
        builder.append(">");
    }

    // endregion XML Utils

    // region Listeners

    /**
     * Fire warning and return the given value.  Useful for switch statements
     * when you want to both report the warning/recovery information and
     * return a default value
     *
     * @param message
     * @param reader
     * @param retVal
     * @return the provided value
     * @param <T>
     */
    private <T> T fireWarningOnType(String message, XMLStreamReader reader, T retVal) {
        fireWarning(message, reader);
        return retVal;
    }

    private void fireWarning(String message, XMLStreamReader reader) {
        final PhonTalkMessage msg = new PhonTalkMessage(message, PhonTalkMessage.Severity.WARNING);
        msg.setLineNumber(reader.getLocation().getLineNumber());
        msg.setColNumber(reader.getLocation().getColumnNumber());
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

    public static void main(String[] args) throws IOException, XMLStreamException {
        TalkbankReader reader = new TalkbankReader();
        reader.addListener(msg -> {
            System.out.println(msg);
        });
        final Session session = reader.readFile("core/src/test/resources/ca/phon/phontalk/tests/RoundTripTests/good-xml/language-utterance-code.xml");
        final SessionWriter writer = (new SessionOutputFactory()).createWriter();
        writer.writeSession(session, System.out);
    }

}
