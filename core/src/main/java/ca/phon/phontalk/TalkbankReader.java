package ca.phon.phontalk;

import ca.phon.extensions.UnvalidatedValue;
import ca.phon.formatter.Formatter;
import ca.phon.formatter.FormatterFactory;
import ca.phon.ipa.CompoundWordMarker;
import ca.phon.ipa.IPATranscript;
import ca.phon.ipa.IPATranscriptBuilder;
import ca.phon.ipa.StressType;
import ca.phon.orthography.*;
import ca.phon.orthography.Error;
import ca.phon.orthography.mor.Grasp;
import ca.phon.orthography.mor.GraspTierData;
import ca.phon.orthography.mor.MorTierData;
import ca.phon.session.*;
import ca.phon.session.Record;
import ca.phon.session.io.xml.XMLFragments;
import ca.phon.session.tierdata.TierData;
import ca.phon.session.tierdata.TierLink;
import ca.phon.syllable.SyllabificationInfo;
import ca.phon.syllable.SyllableConstituentType;
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
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Read in a TalkBank xml file as a Phon session.
 *
 */
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

    // region Session utils

    /**
     * Read session records and create tier descriptions for user-defined tiers
     * @param session
     */
    private void updateSessionTierDescriptions(Session session, Map<String, String> tierNameMap) {
        for(Record record:session.getRecords()) {
            for(Tier<?> userTier:record.getUserTiers()) {
                final String tierName = tierNameMap.containsKey(userTier.getName()) ? tierNameMap.get(userTier.getName()) : userTier.getName();
                TierDescription td = session.getUserTiers().get(tierName);
                if(td != null) continue;
                final UserTierType userTierType = UserTierType.fromPhonTierName(tierName);
                if(userTierType != null) {
                    td = factory.createTierDescription(tierName, userTierType.getType(), new HashMap<>(), !userTierType.isAlignable());
                } else {
                    td = factory.createTierDescription(tierName, TierData.class, new HashMap<>(), false);
                }
                session.addUserTier(td);
            }

            for(String abbrvTierName:tierNameMap.keySet()) {
                Tier<Object> tier = (Tier<Object>)record.getTier(abbrvTierName);
                final Tier<Object> newTier = factory.createTier(tierNameMap.get(abbrvTierName), tier.getDeclaredType(), tier.getTierParameters(), tier.isExcludeFromAlignment());
                newTier.setValue(tier.getValue());
                record.removeTier(abbrvTierName);
                record.putTier(newTier);
            }
        }
    }
    // endregion Session utils

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

        // read optional Date
        final String dateText = reader.getAttributeValue(null, "Date");
        if(dateText != null) {
            try {
                final Formatter<LocalDate> dateFormatter = FormatterFactory.createFormatter(LocalDate.class);
                final  LocalDate date = dateFormatter.parse(dateText);
                session.setDate(date);
            } catch (ParseException e) {
                fireWarning("Unable to parse date " + dateText + ", " + e.getLocalizedMessage(), reader);
            }
        }

        final String corpus = reader.getAttributeValue(null, "Corpus");
        if(corpus != null) {
            session.setCorpus(corpus);
        }

        final String videos = reader.getAttributeValue(null, "Videos");
        if(videos != null) {
            session.getMetadata().put("Videos", videos);
        }

        final String media = reader.getAttributeValue(null, "Media");
        if(media != null) {
            session.setMediaLocation(media);
        }

        final String mediaTypes = reader.getAttributeValue(null, "Mediatypes");
        if(mediaTypes != null) {
            session.getMetadata().put("Mediatypes", mediaTypes);
        }

        final String langText = reader.getAttributeValue(null, "Lang");
        if(langText != null) {
            final ArrayList<Language> langs = new ArrayList<>();
            for(String txt:langText.split("\\s")) {
                try {
                    final Language lang = Language.parseLanguage(txt);
                    langs.add(lang);
                } catch (IllegalArgumentException e) {
                    fireWarning("Unable to parse language " + txt + ", " + e.getLocalizedMessage(), reader);
                }
            }
            if(!langs.isEmpty())
                session.setLanguages(langs);
        }

        final String options = reader.getAttributeValue(null, "Options");
        if(options != null) {
            session.getMetadata().put("Options", options);
        }

        final String designType = reader.getAttributeValue(null, "DesignType");
        if(designType != null) {
            session.getMetadata().put("DesignType", designType);
        }

        final String activityType = reader.getAttributeValue(null, "ActivityType");
        if(activityType != null) {
            session.getMetadata().put("ActivityType", activityType);
        }

        final String groupType = reader.getAttributeValue(null, "GroupType");
        if(groupType != null) {
            session.getMetadata().put("GroupType", groupType);
        }

        final String colorwords = reader.getAttributeValue(null, "Colorwords");
        if(colorwords != null) {
            session.getMetadata().put("Colorwords", colorwords);
        }

        final String window = reader.getAttributeValue(null, "Window");
        if(window != null) {
            session.getMetadata().put("Window", window);
        }

        final String pid = reader.getAttributeValue(null, "PID");
        if(pid != null) {
            session.getMetadata().put("PID", pid);
        }

        final String font = reader.getAttributeValue(null, "Font");
        if(font != null) {
            session.getMetadata().put("Font", font);
        }

        final Map<String, String> tierNameMap = new LinkedHashMap<>();

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
                    Comment comment = readComment(reader);
                    final Pattern tierMapPattern = Pattern.compile("tier (.+?)=(.+)");
                    final Matcher matcher = tierMapPattern.matcher(comment.getValue().toString());
                    if(matcher.matches()) {
                        // add to tier name map
                        tierNameMap.put(matcher.group(1), matcher.group(2));
                    } else {
                        session.getTranscript().addComment(comment);
                    }
                    break;

                case "u":
                    readRecord(session, reader);
                    dontSkip = (reader.isStartElement());
                    break;

                case "lazy-gem":
                case "begin-gem":
                case "end-gem":
                    readGem(session, reader);
                    break;

                default:
                    break;
            }
        }

        updateSessionTierDescriptions(session, tierNameMap);
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
                default -> Sex.UNSPECIFIED;
            };
            participant.setSex(s);
        } else {
            participant.setSex(Sex.UNSPECIFIED);
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
        final String bday = reader.getAttributeValue(null, "birthday");
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
    private Comment readComment(XMLStreamReader reader) throws XMLStreamException {
        if(!"comment".equals(reader.getLocalName())) throw new XMLStreamException();
        final Comment comment = factory.createComment();

        // read type
        final String type = reader.getAttributeValue(null, "type");
        CommentType commentType =
                type != null ? CommentType.fromString(type) : CommentType.Generic;
        commentType = commentType == null ? CommentType.Generic : commentType;
        comment.setType(commentType);
        TierData commentData = readTierContent(reader);
        comment.setValue(commentData);

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

        final String who = reader.getAttributeValue("", "who");
        if(who != null) {
            final Participant speaker = session.getParticipants().getParticipantById(who);
            if(speaker == null) {
                fireWarning("Unknown speaker id " + who, reader);
            } else {
                r.setSpeaker(speaker);
            }
        }

        UtteranceTierData utd = readUtterance(reader);
        Orthography ortho = utd.orthography();
        if(ortho.hasUtteranceMedia()) {
            final InternalMedia utteranceMedia = ortho.getUtteranceMedia();
            final MediaSegment mediaSegment = factory.createMediaSegment();
            mediaSegment.setStartValue(utteranceMedia.getStartTime());
            mediaSegment.setEndValue(utteranceMedia.getEndTime());
            mediaSegment.setUnitType(MediaUnit.Second);
            r.setMediaSegment(mediaSegment);
            final List<OrthographyElement> filteredElements = ortho.toList().stream()
                    .filter(ele -> ele != utd.orthography().getUtteranceMedia()).toList();
            ortho = new Orthography(filteredElements);
        }
        r.setOrthography(ortho);

        for(Tier<IPATranscript> ipaTier:utd.ipaTiers()) {
            if(SystemTierType.IPATarget.getName().equals(ipaTier.getName())) {
                r.setIPATarget(ipaTier.getValue());
            } else if(SystemTierType.IPAActual.getName().equals(ipaTier.getName())) {
                r.setIPAActual(ipaTier.getValue());
            } else {
                r.putTier(ipaTier);
            }
        }

        for(Tier<?> depTier:utd.depTiers()) {
            if(SystemTierType.Notes.getName().equals(depTier.getName())) {
                r.setNotes((TierData) depTier.getValue());
            } else {
                r.putTier(depTier);
            }
        }

        for(Tier<MorTierData> morTier:utd.morTiers()) {
            r.putTier(morTier);
        }
        for(Tier<GraspTierData> graTier:utd.graTiers()) {
            r.putTier(graTier);
        }

        for(Tier<IPATranscript> ipaTier:utd.ipaTiers) {
            if(SystemTierType.IPATarget.getName().equals(ipaTier.getName())) {
                r.setIPATarget(ipaTier.getValue());
            } else if(SystemTierType.IPAActual.getName().equals(ipaTier.getName())) {
                r.setIPAActual(ipaTier.getValue());
            } else {
                r.putTier(ipaTier);
            }
        }

        boolean atEnd = false;
        // read remainder of utterance after terminator
        while(!atEnd && readToNextElement(reader)) {
            final String eleName = reader.getLocalName();
            switch (eleName) {
                case "wor":
                    Tier<Orthography> worTier = readWorTier(reader);
                    r.putTier(worTier);
                    break;

                default:
                    // at next transcript element, break loop
                    atEnd = true;
                    break;
            }
        }
        session.getTranscript().addRecord(r);
        return r;
    }

    private Marker readMarker(XMLStreamReader reader) throws XMLStreamException {
        if(!reader.isStartElement()) throwNotStart(reader);
        if(!"k".equals(reader.getLocalName())) throwNotElement(reader, "k", reader.getLocalName());

        final String type = reader.getAttributeValue(null, "type");
        final MarkerType mkType = MarkerType.fromString(type);
        if(mkType == null) throw new XMLStreamException("Unknown marker type " + type);
        return new Marker(mkType);
    }

    private Error readError(XMLStreamReader reader) throws XMLStreamException {
        if (!reader.isStartElement()) throwNotStart(reader);
        if (!"error".equals(reader.getLocalName())) throwNotElement(reader, "error", reader.getLocalName());

        final StringBuilder builder = new StringBuilder();
        while (reader.hasNext()) {
            reader.next();
            if (reader.isCharacters()) {
                builder.append(reader.getText());
            } else if (reader.isStartElement()) {
                throw new XMLStreamException("Invalid content found at element " + reader.getLocalName());
            } else if (reader.isEndElement() && reader.getLocalName().equals("error")) {
                break;
            }
        }
        return new Error(builder.toString());
    }

    private Postcode readPostcode(XMLStreamReader reader) throws XMLStreamException {
        if(!reader.isStartElement()) throwNotStart(reader);
        if(!"postcode".equals(reader.getLocalName())) throwNotElement(reader, "postcode", reader.getLocalName());
        reader.next();
        final String txt = reader.isCharacters() ? reader.getText() : "";
        return new Postcode(txt);
    }

    private MediaSegment readMedia(XMLStreamReader reader) throws XMLStreamException {
        if (!reader.isStartElement()) throwNotStart(reader);
        if(!"media".equals(reader.getLocalName())) throwNotElement(reader, "media", reader.getLocalName());
        final String startTxt = reader.getAttributeValue(null, "start");
        final String endTxt = reader.getAttributeValue(null, "end");
        final String unit = reader.getAttributeValue(null, "unit");
        BigDecimal start = BigDecimal.valueOf(Double.parseDouble(startTxt));
        BigDecimal end = BigDecimal.valueOf(Double.parseDouble(endTxt));

        final MediaUnit mediaUnit = MediaUnit.fromString(unit);
        if (mediaUnit == MediaUnit.Second) {
            start = start.setScale(3, RoundingMode.HALF_UP);
            end = end.setScale(3, RoundingMode.HALF_UP);
        }

        final MediaSegment retVal = SessionFactory.newFactory().createMediaSegment();
        retVal.setStartValue(start.floatValue());
        retVal.setEndValue(end.floatValue());
        retVal.setUnitType(mediaUnit);
        retVal.toString();
        return retVal;
    }



    /**
     * Read utterance dependent tier
     *
     * @param reader
     * @return
     * @throws XMLStreamException
     */
    private Tier<TierData> readDepTier(XMLStreamReader reader) throws XMLStreamException {
        if(!reader.isStartElement()) throwNotStart(reader);
        if(!"a".equals(reader.getLocalName())) throwNotElement(reader, "a", reader.getLocalName());

        final String type = reader.getAttributeValue(null, "type");
        final String flavor = reader.getAttributeValue(null, "flavor");

        if(type != null) {
            String tierName = "undefined";
            if("extension".equals(type)) {
                tierName = flavor;
            } else {
                final UserTierType userTierType = UserTierType.fromTalkbankTierType(type);
                if(userTierType == null) throw new XMLStreamException("Unknown tier 'type' " + type);
                tierName = userTierType.getPhonTierName();
            }
            // one of our pre-defined tiers
            final Tier<TierData> retVal = factory.createTier(tierName, TierData.class);
            retVal.setValue(readTierContent(reader));
            return retVal;
        } else {
            throw new XMLStreamException("Required attribute 'type' missing");
        }
    }

    private TierData readTierContent(XMLStreamReader reader) throws XMLStreamException {
        if(!reader.isStartElement()) throwNotStart(reader);
        final String startEleName = reader.getLocalName();

        final StringBuilder builder = new StringBuilder();
        while(reader.hasNext()) {
            reader.next();
            if(reader.isCharacters()) {
                final String chars = reader.getText();
//                if(!builder.isEmpty() && !chars.isBlank()) builder.append(" ");
                if(!chars.isBlank())
                    builder.append(chars);
            } else if(reader.isStartElement()) {
                final String eleName = reader.getLocalName();
                switch (eleName) {
                    case "media":
                        MediaSegment segment = readMedia(reader);
                        builder.append(InternalMedia.MEDIA_BULLET)
                                .append(segment)
                                .append(InternalMedia.MEDIA_BULLET);
                        break;

                    case "mediaPic":
                        String href = reader.getAttributeValue(null, "href");
                        if(!builder.isEmpty()) builder.append(" ");
                        builder.append(TierLink.LINK_PREFIX).append("pic ").append(href).append(TierLink.LINK_SUFFIX);
                        break;

                    default:
                        throw new XMLStreamException("Unexpected element " + eleName);
                }
            } else if(reader.isEndElement() && reader.getLocalName().equals(startEleName)) {
                break;
            }
        }
        TierData tierData = new TierData();
        try {
            tierData = TierData.parseTierData(builder.toString().trim());
        } catch (ParseException e) {
            fireWarning(e.getMessage(), reader);
            tierData.putExtension(UnvalidatedValue.class, new UnvalidatedValue(builder.toString(), e));
        }
        return tierData;
    }

    /**
     * Read one of: lazy-gem, begin-gem, end-gem
     *
     * @param reader
     * @return
     * @throws XMLStreamException
     */
    private Gem readGem(Session session, XMLStreamReader reader) throws XMLStreamException {
        final String eleName = reader.hasName() ? reader.getLocalName() : "";
        final GemType gemType =  switch(eleName) {
            case "lazy-gem" -> GemType.Lazy;
            case "begin-gem" -> GemType.Begin;
            case "end-gem" -> GemType.End;
            default -> fireWarningOnType("Invalid gem element name " + eleName, reader, GemType.Lazy);
        };
        final Gem retVal =
                factory.createGem(gemType, reader.getAttributeValue(null, "label"));
        session.getTranscript().addGem(retVal);
        return retVal;
    }

    record UtteranceTierData(Orthography orthography, List<Tier<IPATranscript>> ipaTiers, List<Tier<?>> depTiers,
                             List<Tier<MorTierData>> morTiers, List<Tier<GraspTierData>> graTiers) {}
    private UtteranceTierData readUtterance(XMLStreamReader reader) throws XMLStreamException {
        if(!reader.isStartElement()) throwNotStart(reader);
        if(!"u".equalsIgnoreCase(reader.getLocalName())) throwNotElement(reader, "u", reader.getLocalName());

        final StringBuilder builder = new StringBuilder();
        Map<String, StringBuilder> morTierBuilders = new LinkedHashMap<>();
        Map<String, IPATranscriptBuilder> ipaTierBuilders = new LinkedHashMap<>();
        List<Tier<?>> depTierList = new ArrayList<>();

        builder.append("<u xmlns=\"https://phon.ca/ns/session\"");
        final String lang = reader.getAttributeValue("http://www.w3.org/XML/1998/namespace", "lang");
        if(lang != null) {
            builder.append(" xml:lang=\"").append(lang).append("\"");
        }
        builder.append(">");
        readUtteranceContent(reader, builder, depTierList, morTierBuilders, ipaTierBuilders);
        builder.append("</u>");

        final XMLInputFactory inputFactory = XMLInputFactory.newFactory();
        XMLStreamReader morReader = inputFactory.createXMLStreamReader(new ByteArrayInputStream(builder.toString().getBytes(StandardCharsets.UTF_8)));
        final XMLOutputFactory outputFactory = XMLOutputFactory.newFactory();
        final ByteArrayOutputStream bout = new ByteArrayOutputStream();
        XMLStreamWriter morReWriter = new MorRewriter(outputFactory.createXMLStreamWriter(bout, "UTF-8"));

        final StAXSource source = new StAXSource(morReader);
        final StAXResult result = new StAXResult(morReWriter);

        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.transform(source, result);
        } catch (TransformerException tce) {
            throw new XMLStreamException(tce);
        }

        final String xml = bout.toString(StandardCharsets.UTF_8);
        try {
            // main line
            final Orthography ortho = XMLFragments.orthographyFromXml(xml);

            // ipa
            final List<Tier<IPATranscript>> ipaTiers = new ArrayList<>();
            for(String tierName:ipaTierBuilders.keySet()) {
                final SystemTierType ipaTierType = SystemTierType.tierFromString(tierName);
                final Tier<IPATranscript> ipaTier = factory.createTier(ipaTierType.getName(), IPATranscript.class, new LinkedHashMap<>(), false);
                ipaTier.setValue(ipaTierBuilders.get(tierName).toIPATranscript());
                ipaTiers.add(ipaTier);
            }

            // mor
            final List<Tier<MorTierData>> morTiers = new ArrayList<>();
            final List<Tier<GraspTierData>> graTiers = new ArrayList<>();
            for(String tierName:morTierBuilders.keySet()) {
                final UserTierType userTierType = UserTierType.fromPhonTierName(tierName);
                final StringBuilder tierBuilder = morTierBuilders.get(tierName);
                if(userTierType.getType() == MorTierData.class) {
                    tierBuilder.append("</mors>");
                }
                final MorTierData morTierData = XMLFragments.morsFromXml(tierBuilder.toString());
                final Tier<MorTierData> morTier = factory.createTier(tierName, MorTierData.class, new HashMap<>(), true);
                morTier.setValue(morTierData);
                morTiers.add(morTier);

                // read xml with a new stream reader and process the gra tiers (if any)
                final XMLStreamReader graReader = XMLInputFactory.newFactory().createXMLStreamReader(new ByteArrayInputStream(tierBuilder.toString().getBytes(StandardCharsets.UTF_8)));
                final Map<String, List<Grasp>> graMap = new LinkedHashMap<>();
                while(graReader.hasNext()) {
                    graReader.next();
                    if(graReader.isStartElement() && "gra".equals(graReader.getLocalName())) {
                        final String type = graReader.getAttributeValue(null, "type");
                        final UserTierType graTierType = UserTierType.fromChatTierName("%" + type);
                        if(graTierType == null) {
                            fireWarning("Unknown gra tier type " + type, graReader);
                            continue;
                        }
                        Grasp gra = readGra(graReader);
                        List<Grasp> grasps = graMap.computeIfAbsent(graTierType.getPhonTierName(), k -> new ArrayList<>());
                        grasps.add(gra);
                    }
                }
                graReader.close();

                for(String graTierName:graMap.keySet()) {
                    final GraspTierData graspTierData = new GraspTierData(graMap.get(graTierName));
                    final Tier<GraspTierData> graTier = factory.createTier(graTierName, GraspTierData.class, new HashMap<>(), true);
                    graTier.setValue(graspTierData);
                    graTiers.add(graTier);
                }
            }

            return new UtteranceTierData(ortho, ipaTiers, depTierList, morTiers, graTiers);
        } catch (IllegalArgumentException | IOException e) {
            throw new XMLStreamException(e);
        }
    }

    private Tier<Orthography> readWorTier(XMLStreamReader reader) throws XMLStreamException {
        final Tier<Orthography> retVal = factory.createTier(UserTierType.Wor.getPhonTierName(), Orthography.class);

        final StringBuilder builder = new StringBuilder();
        // more/ipa data should not be %wor
        Map<String, StringBuilder> morTierBuilders = new LinkedHashMap<>();
        Map<String, IPATranscriptBuilder> ipaTierBuilders = new LinkedHashMap<>();

        builder.append("<u xmlns=\"https://phon.ca/ns/session\">");
        readUtteranceContent(reader, builder, new ArrayList<>(), morTierBuilders, ipaTierBuilders);
        builder.append("</u>");

        try {
            final Orthography orthography = XMLFragments.orthographyFromXml(builder.toString());
            retVal.setValue(orthography);
        } catch (IOException e) {
            throw new XMLStreamException(e.getMessage(), reader.getLocation(), e);
        }

        return retVal;
    }

    private void readUtteranceElement(XMLStreamReader reader, StringBuilder builder, List<Tier<?>> depTiers,
                                      Map<String, StringBuilder> tierBuilders, Map<String, IPATranscriptBuilder> ipaTierBuilders) throws XMLStreamException {
        if(!reader.isStartElement()) throwNotStart(reader);
        final String eleName = reader.getLocalName();

        builder.append("<").append(eleName);
        appendAttributes(builder, reader);
        builder.append(">");
        readUtteranceContent(reader, builder, depTiers, tierBuilders, ipaTierBuilders);
        builder.append("</").append(eleName).append(">");
    }

    private void readUtteranceContent(XMLStreamReader reader, StringBuilder builder, List<Tier<?>> depTierList,
                                      Map<String, StringBuilder> morTierBuilders, Map<String, IPATranscriptBuilder> ipaTierBuilders) throws XMLStreamException {
        final String originalEleName = reader.getLocalName();
        while(reader.hasNext()) {
            reader.next();
            if(reader.isCharacters()) {
                builder.append(StringEscapeUtils.escapeXml(reader.getText()));
            } else if(reader.isStartElement()) {
                final String eleName = reader.getLocalName();
                switch (eleName) {
                    case "mor":
                        readMor(reader, morTierBuilders);
                        break;

                    case "pg":
                        readPg(reader, builder, morTierBuilders, ipaTierBuilders);
                        break;

                    case "a":
                        Tier<TierData> depTier = readDepTier(reader);
                        depTierList.add(depTier);
                        break;

                    case "wor":
                        final Tier<Orthography> worTier = readWorTier(reader);
                        depTierList.add(worTier);
                        break;

                    default:
                        readUtteranceElement(reader, builder, depTierList, morTierBuilders, ipaTierBuilders);
                        break;
                }
            } else if(reader.isEndElement() && reader.getLocalName().equals(originalEleName)) {
                break;
            }
        }
    }

    private void readPg(XMLStreamReader reader, StringBuilder builder,
                        Map<String, StringBuilder> tierBuilders, Map<String, IPATranscriptBuilder> ipaTierBuilders) throws XMLStreamException {
        builder.append("<pg>");
        while(reader.hasNext()) {
            reader.next();
            if(reader.isStartElement()) {
                final String eleName = reader.getLocalName();
                switch (eleName) {
                    case "model", "actual":
                        readOldPho(reader, ipaTierBuilders);
                        break;

                    case "align":
                        // ignore for now
                        readToEndTag(reader);
                        break;

                    default:
                        readUtteranceElement(reader, builder, new ArrayList<>(), tierBuilders, ipaTierBuilders);
                        break;
                }
            } else if(reader.isEndElement() && reader.getLocalName().equals("pg")) {
                break;
            }
        }
        builder.append("</pg>");
    }

    private void readOldPho(XMLStreamReader reader, Map<String, IPATranscriptBuilder> tierBuilders) throws XMLStreamException {
        if(!reader.isStartElement()) throwNotStart(reader);
        final String eleName = reader.getLocalName();
        if(!"model".equals(eleName) && !"actual".equals(eleName)) throwNotElement(reader, "actual|model", eleName);

        final SystemTierType ipaTier = "model".equals(eleName) ? SystemTierType.IPATarget : SystemTierType.IPAActual;
        IPATranscriptBuilder builder = tierBuilders.computeIfAbsent(ipaTier.getName(), k -> new IPATranscriptBuilder());
        if(builder.size() > 0) builder.appendWordBoundary();

        while(reader.hasNext()) {
            reader.next();
            if(reader.isStartElement()) {
                final String pwEleName = reader.getLocalName();
                if(!"pw".equals(pwEleName)) throwNotElement(reader, "pw", pwEleName);
                readPw(reader, builder);
            } else if(reader.isEndElement()) {
                break;
            }
        }
    }

    private void readPw(XMLStreamReader reader, IPATranscriptBuilder builder) throws XMLStreamException {
        if(!reader.isStartElement()) throwNotStart(reader);
        if(!"pw".equals(reader.getLocalName())) throwNotElement(reader, "pw", reader.getLocalName());
        while(reader.hasNext()) {
            reader.next();
            if(reader.isStartElement()) {
                final String eleName = reader.getLocalName();
                switch (eleName) {
                    case "ss":
                        final String ssType = reader.getAttributeValue(null, "type");
                        final StressType stressType = "1".equals(ssType) ? StressType.PRIMARY : StressType.SECONDARY;
                        builder.appendStress(stressType);
                        break;

                    case "wk":
                        final String wkType = reader.getAttributeValue(null, "type");
                        final CompoundWordMarker wordMarker = "cli".equals(wkType) ? new CompoundWordMarker('~') : new CompoundWordMarker();
                        builder.append(wordMarker);
                        break;

                    case "ph":
                        final String type = reader.getAttributeValue(null, "sctype");
                        final SyllableConstituentType scType = type != null ? SyllableConstituentType.fromString(type) : SyllableConstituentType.UNKNOWN;
                        final String hiatus = reader.getAttributeValue(null, "hiatus");
                        final boolean isHiatus = Boolean.parseBoolean(hiatus);
                        final String eleTxt = reader.getElementText();
                        if(eleTxt.isBlank())
                            throw new XMLStreamException("ph must not be empty", reader.getLocation());
                        builder.append(eleTxt);
                        // setup sc info
                        if(scType != null && scType != SyllableConstituentType.UNKNOWN) {
                            final SyllabificationInfo info = builder.last().getExtension(SyllabificationInfo.class);
                            info.setConstituentType(scType);
                            if(scType == SyllableConstituentType.NUCLEUS)
                                info.setDiphthongMember(!isHiatus);
                        }
                        break;

                    default:
                        throw new XMLStreamException("Invalid pho child element " + eleName, reader.getLocation());
                }
            } else if(reader.isEndElement() && "pw".equals(reader.getLocalName())) {
                break;
            }
        }
    }

    private Grasp readGra(XMLStreamReader reader) throws XMLStreamException {
        final int index = Integer.parseInt(reader.getAttributeValue(null, "index"));
        final int head = Integer.parseInt(reader.getAttributeValue(null, "head"));
        final String relation = reader.getAttributeValue(null, "relation");
        return new Grasp(index, head, relation);
    }

    private void readMor(XMLStreamReader reader, Map<String, StringBuilder> tierBuilders) throws XMLStreamException {
        if(!reader.isStartElement()) throwNotStart(reader);;
        if(!"mor".equals(reader.getLocalName())) throwNotElement(reader, "mor", reader.getLocalName());

        final String type = reader.getAttributeValue(null, "type");
        final UserTierType userTierType = UserTierType.fromChatTierName("%" + type);
        if(userTierType == null) {
            fireWarning("Unknown mor tier type " + type, reader);
            readToEndTag(reader);
            return;
        }
        StringBuilder builder = tierBuilders.get(userTierType.getPhonTierName());
        if(builder == null) {
            builder = new StringBuilder();
            builder.append("<mors>\n");
            tierBuilders.put(userTierType.getPhonTierName(), builder);
        }

        final String eleXml = recreateElementXML(reader, new ArrayList<>(), true);
        final XMLInputFactory inputFactory = XMLInputFactory.newFactory();
        XMLStreamReader morReader = inputFactory.createXMLStreamReader(new ByteArrayInputStream(eleXml.getBytes(StandardCharsets.UTF_8)));
        final XMLOutputFactory outputFactory = XMLOutputFactory.newFactory();
        final ByteArrayOutputStream bout = new ByteArrayOutputStream();
        XMLStreamWriter morReWriter = new MorRewriter(outputFactory.createXMLStreamWriter(bout, "UTF-8"));

        final StAXSource source = new StAXSource(morReader);
        final StAXResult result = new StAXResult(morReWriter);

        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.transform(source, result);
        } catch (TransformerException tce) {
            throw new XMLStreamException(tce);
        }

        builder.append(bout.toString(StandardCharsets.UTF_8));
    }

    /**
     * Class to re-write the {http://www.talkbank.org/ns/talkbank}pos/s elemnt to
     * {https://phon.ca/ns/session}subc
     */
    public static class MorRewriter extends DelegatingXMLStreamWriter {

        private Stack<String> eleStack = new Stack<>();

        private boolean inPos = false;

        public MorRewriter(XMLStreamWriter delegate) {
            super(delegate);
        }

        @Override
        public void writeStartElement(String namespaceURI, String localName) throws XMLStreamException {
            eleStack.push(localName);
            super.writeStartElement(namespaceURI, localName);
        }

        @Override
        public void writeStartElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
            eleStack.push(localName);
            super.writeStartElement(prefix, localName, namespaceURI);
        }

        @Override
        public void writeStartElement(String localName) throws XMLStreamException {
            if("pos".equals(localName)) {
                inPos = true;
            } else if(inPos && "s".equals(localName))  {
                localName = "subc";
            }
            eleStack.push(localName);
            super.writeStartElement(localName);
        }

        @Override
        public void writeEndElement() throws XMLStreamException {
            String eleName = eleStack.pop();
            if("pos".equals(eleName)) {
                inPos = false;
            }
            super.writeEndElement();
        }

        @Override
        public void writeProcessingInstruction(String target) throws XMLStreamException {
        }

        @Override
        public void writeProcessingInstruction(String target, String data) throws XMLStreamException {
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

        @Override
        public void writeEndDocument() throws XMLStreamException {
        }

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
        if(!reader.isStartElement()) throwNotStart(reader);
        final String eleName = reader.getLocalName();
        while(reader.hasNext()) {
            reader.next();
            if(reader.isEndElement() && reader.getLocalName().equals(eleName)) {
                return true;
            }
        }
        return false;
    }

    private void throwNotStart(XMLStreamReader reader) throws XMLStreamException {
        throw new XMLStreamException("Expected start element", reader.getLocation());
    }

    private void throwNotElement(XMLStreamReader reader, String expectedName, String eleName) throws XMLStreamException {
        throw new XMLStreamException(String.format("Expected element '%s' but got '%s'", expectedName, eleName), reader.getLocation());
    }

    private String recreateElementXML(XMLStreamReader reader, List<String> ignoreElements, boolean includeAttributesOfParent)
        throws XMLStreamException{
        if(!reader.isStartElement()) throwNotStart(reader);
        final StringBuilder builder = new StringBuilder();

        appendElementStartXml(builder, reader, includeAttributesOfParent);
        appendElementContent(builder, reader);
        builder.append("</").append(reader.getLocalName()).append(">");

        return builder.toString();
    }

    private void appendElement(StringBuilder builder, XMLStreamReader reader) throws XMLStreamException {
        if(!reader.isStartElement()) throwNotStart(reader);
        String eleName = reader.getLocalName();

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

}
