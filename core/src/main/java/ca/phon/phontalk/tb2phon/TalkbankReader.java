package ca.phon.phontalk.tb2phon;

import ca.phon.phontalk.PhonTalkListener;
import ca.phon.phontalk.PhonTalkMessage;
import ca.phon.session.*;
import ca.phon.session.io.SessionOutputFactory;
import ca.phon.session.io.SessionWriter;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.*;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class TalkbankReader {

    private final static String TB_NS = "http://www.talkbank.org/ns/talkbank";

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

    private boolean readToNextElement(XMLStreamReader reader) throws XMLStreamException {
        while(reader.hasNext()) {
            int eventType = reader.next();
            if(eventType == XMLStreamReader.START_ELEMENT) {
                return true;
            }
        }
        return false;
    }

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

        reader.next();
        while(readToNextElement(reader)) {
            final String eleName = reader.getLocalName();
            switch (eleName) {
                case "Participants":
                    readParticipants(session, reader);
                    break;

                default:
                    reader.next();
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

    public static void main(String[] args) throws IOException, XMLStreamException {
        TalkbankReader reader = new TalkbankReader();
        reader.addListener(msg -> {
            System.out.println(msg);
        });
        final Session session = reader.readFile("core/src/test/resources/ca/phon/phontalk/tests/RoundTripTests/good-xml/participants-duplicate-role.xml");
        final SessionWriter writer = (new SessionOutputFactory()).createWriter();
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        writer.writeSession(session, bout);
        System.out.println(bout.toString("UTF-8"));
    }

}
