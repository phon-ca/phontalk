

        
tree grammar AST2Phon;

options
{
	ASTLabelType = CommonTree ;
	tokenVocab = TalkBank2AST ;
	output = AST;
	superClass=PhonTalkTreeParser;
}

@header {
package ca.phon.phontalk.parser;

import ca.phon.phontalk.*;
import ca.phon.phontalk.parser.*;

import ca.phon.util.*;
import ca.phon.ipa.*;
import ca.phon.ipa.alignment.*;
import ca.phon.syllable.*;
import ca.phon.syllabifier.*;
import ca.phon.orthography.*;
import ca.phon.project.*;
import ca.phon.session.*;
import ca.phon.extensions.*;
import ca.phon.formatter.*;
import ca.phon.visitor.*;
import ca.phon.worker.*;
import ca.phon.plugin.*;

import java.util.Calendar;
import java.util.TimeZone;
import java.text.ParseException;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.logging.*;
import java.util.stream.*;
import java.util.regex.*;

import java.time.*;

import javax.xml.datatype.*;

import org.apache.commons.lang3.*;
}

@members {
	private final static Logger LOGGER = Logger.getLogger("ca.phon.phontalk.parser");

	/** Default IPhonFactory */
	private SessionFactory sessionFactory = 
		SessionFactory.newFactory();

	/** Session */
	private Session session = 
		sessionFactory.createSession();

	public Session getSession() { 
		// fix tier view
		sessionFactory.setupDefaultTierView(session);
		return session;
	}
	
	/** List of comments which are to be added 
	    to the next record */
	private List<Comment> nextRecordComments = new ArrayList<Comment>();
	
	private int recordIndex = 0;
	
	public void reportError(RecognitionException e) {
		throw new TreeWalkerError(e);
	}
	
	private TierDescription ensureTierExists(String tierName, boolean grouped) {
		return ensureTierExists(tierName, grouped, null);
	}
	
	private TierDescription ensureTierExists(String tierName, boolean grouped, Record record) {
		TierDescription tierDesc = null;
		for(TierDescription current:session.getUserTiers())
		{
			if(current.isGrouped() == grouped && current.getName().equals(tierName))
			{
				tierDesc = current;
				break;
			}
		}
		
		if(tierDesc == null) {
			// create the new tier
			tierDesc = sessionFactory.createTierDescription(tierName, grouped, TierString.class);
			session.addUserTier(tierDesc);
			
			TierViewItem tvi = sessionFactory.createTierViewItem(tierName, true, "default");
			List<TierViewItem> tierView = new ArrayList<>(session.getTierView());
			tierView.add(tvi);
			session.setTierView(tierView);
		}
		
		if(record != null) {
			if(!record.hasTier(tierName)) {
				Tier<TierString> tier = sessionFactory.createTier(tierName, TierString.class, grouped);
				record.putTier(tier);
			}
		}
		
		return tierDesc;
	}
	
	/**
	 * Some rules reference their parent, to avoid this when processing fragments
	 * set this to <code>true</code>.
	 *
	 * NOTE: Currently only works for pho->IPATranscript and align->PhoneMap
	 */
	private boolean processFragments = false;
	
	public boolean isProcessFragments() { return processFragments; }
	public void setProcessFragments(boolean processFragments) { this.processFragments = processFragments; }
	
	public Map<String, Object> properties = new HashMap<>();
	public Object getProperty(String key, Object defaultVal) {
		return (properties.containsKey(key) ? properties.get(key) : defaultVal);
	}
	
	public void setProperty(String key, Object value) {
		properties.put(key, value);
	}
	
	private boolean syllabifyAndAlign = false;
	public boolean isSyllabifyAndAlign() {
		return this.syllabifyAndAlign;
	}
	
	public void setSyllabifyAndAlign(boolean syllabifyAndAlign) {
		this.syllabifyAndAlign = syllabifyAndAlign;
	}
	
	// syllabifier
	private Syllabifier syllabifier = SyllabifierLibrary.getInstance().getSyllabifierForLanguage(Language.parseLanguage("eng-simple"));
	
	// set syllabifier, done during CHAT attribute processing (if possible)
	public void setSyllabifier(Syllabifier syllabifier) {
		this.syllabifier = syllabifier;
	}
	
	private Syllabifier getSyllabifier() {
		return this.syllabifier;
	}
}

    

        
chat
	:	^(CHAT_START chat_attrs+ metadata? participants chat_content*)
	;
	
chat_content
	:	comment
	{
		// add comment to last record
		if(session.getRecordCount() == 0)
			session.getMetadata().addComment($comment.val);
		else
			nextRecordComments.add($comment.val);
	}
	|	u
	{
		session.addRecord($u.record);
	}
	|	lazy_gem
	{
		// add comment to next record
		nextRecordComments.add($lazy_gem.lazyGem);
	}
	|	begin_gem
	{
		nextRecordComments.add($begin_gem.beginGem);
	}
	|	end_gem
	{
		nextRecordComments.add($end_gem.endGem);
	}
	|	tcu
	{
		$tcu.records.forEach( r -> session.addRecord(r) );
	}
	;
	
chat_attrs
	:	CHAT_ATTR_VERSION
	|	CHAT_ATTR_DATE
	{
		LocalDate date = DateFormatter.stringToDateTime($CHAT_ATTR_DATE.text);
		session.setDate(date);
	}
	|	CHAT_ATTR_CORPUS
	{
		session.setCorpus($CHAT_ATTR_CORPUS.text);
	}
	|	CHAT_ATTR_MEDIA
	{	
		session.setMediaLocation($CHAT_ATTR_MEDIA.text + 
			(session.getMediaLocation() != null ? session.getMediaLocation() : ""));
	}
	|	CHAT_ATTR_MEDIATYPES
	{
		String suffix = ".wav";  // default media type
		String type = $CHAT_ATTR_MEDIATYPES.text;
		if(type.equals("aif") || type.equals("aiff"))
		{
			suffix = ".aiff";
		} else if(type.equals("mov") || type.equals("video"))
		{
			suffix = ".mov";
		}
		session.setMediaLocation(
			(session.getMediaLocation() != null ? session.getMediaLocation() : "") + suffix);
	}
	|	CHAT_ATTR_LANG
	{
		session.setLanguage($CHAT_ATTR_LANG.text);
		
		// attempt to load appropriate syllabifier
		try {
			Language syllabifierLang = Language.parseLanguage($CHAT_ATTR_LANG.text.split("\\p{Space}")[0]);
			SyllabifierLibrary library = SyllabifierLibrary.getInstance();
			Syllabifier syllabifier = library.getSyllabifierForLanguage(syllabifierLang);
			if(syllabifier != null) {
				setSyllabifier(syllabifier);
			}
		} catch (IllegalArgumentException e) {
			LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
		}
	}
	|	CHAT_ATTR_OPTIONS
	{
		// TODO
	}
	|	CHAT_ATTR_COLORWORDS
	{
		// TODO
	}
	|	CHAT_ATTR_PID
	{
		Comment comment = sessionFactory.createComment();
		comment.setType(CommentEnum.Code);
		comment.setValue("pid " + $CHAT_ATTR_PID.text);
		session.getMetadata().addComment(comment);
	}
	|	CHAT_ATTR_FONT
	{
		// TODO
	}
	;

    

        
metadata
    :    ^(METADATA_START dcelementtype*)
    ;
    
dcelementtype
    :    dc_title
    |    dc_creator
    |    dc_subject
    |    dc_description
    |    dc_publisher
    |    dc_contributor
    |    dc_date
    |    dc_type
    |    dc_format
    |    dc_identifier
    |    dc_relation
    |    dc_coverage
    |    dc_rights
    |    dc_appId
    ;
    
dc_title
    :    ^(TITLE_START TITLE_ATTR_LANG? TEXT)
    ;
    
dc_creator
    :    ^(CREATOR_START CREATOR_ATTR_LANG? TEXT)
    ;
    
dc_subject
    :    ^(SUBJECT_START SUBJECT_ATTR_LANG? TEXT)
    ;
    
dc_description
    :    ^(DESCRIPTION_START DESCRIPTION_ATTR_LANG? TEXT)
    ;
    
dc_publisher
    :    ^(PUBLISHER_START PUBLISHER_ATTR_LANG? TEXT)
    ;
    
dc_contributor
    :    ^(CONTRIBUTOR_START CONTRIBUTOR_ATTR_LANG? TEXT)
    ;
    
dc_date
    :    ^(DATE_START DATE_ATTR_LANG? TEXT)
    ;
    
dc_type
    :    ^(TYPE_START DATE_ATTR_LANG? TEXT)
    ;
    
dc_format
    :    ^(FORMAT_START DATE_ATTR_LANG? TEXT)
    ;
    
dc_identifier
    :    ^(IDENTIFIER_START IDENTIFIER_ATTR_LANG? TEXT)
    ;
    
dc_source
    :    ^(SOURCE_START SOURCE_ATTR_LANG? TEXT)
    ;
    
dc_language
    :    ^(LANGUAGE_START LANGUAGE_ATTR_LANG? TEXT)
    ;

dc_relation
    :    ^(RELATION_START RELATION_ATTR_LANG? TEXT)
    ;
    
dc_coverage
    :    ^(COVERAGE_START COVERAGE_ATTR_LANG? TEXT)
    ;
    
dc_rights
    :    ^(RIGHTS_START RIGHTS_ATTR_LANG? TEXT)
    ;
    
dc_appId
    :    ^(APPID_START APPID_ATTR_LANG? TEXT)
    ;

    

    	
participants 
	:	^(PARTICIPANTS_START (participant { session.addParticipant($participant.val); })*)
	;
	
participant returns [Participant val]
scope {
	Participant p;
}
@init {
	$participant::p = sessionFactory.createParticipant();
}
@after {
	$val = $participant::p;
}
	:	^(PARTICIPANT_START part_attr*)
	;
	
part_attr
	:	PARTICIPANT_ATTR_ID
	{	$participant::p.setId($PARTICIPANT_ATTR_ID.text);	}
	|	PARTICIPANT_ATTR_NAME
	{	$participant::p.setName($PARTICIPANT_ATTR_NAME.text.replaceAll("_", " "));	}
	|	PARTICIPANT_ATTR_ROLE
	{	$participant::p.setRole(
			ParticipantRole.fromString($PARTICIPANT_ATTR_ROLE.text.replaceAll("_", " ")));	}
	|	PARTICIPANT_ATTR_LANGUAGE
	{	$participant::p.setLanguage($PARTICIPANT_ATTR_LANGUAGE.text);	}
	|	PARTICIPANT_ATTR_AGE
	{
		LocalDate date = session.getDate();
		
		try {
			DatatypeFactory dtFactory = DatatypeFactory.newInstance();
			javax.xml.datatype.Duration duration = dtFactory.newDuration($PARTICIPANT_ATTR_AGE.text);
			Period age = Period.of(duration.getYears(), duration.getMonths(),
										 duration.getDays());
			$participant::p.setAge(age);
		} catch (javax.xml.datatype.DatatypeConfigurationException e) {
			LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
		}
		
	}
	|	PARTICIPANT_ATTR_GROUP
	{	$participant::p.setGroup($PARTICIPANT_ATTR_GROUP.text);		}
	|	PARTICIPANT_ATTR_SEX
	{
		Sex pSex = ($PARTICIPANT_ATTR_SEX.text.equalsIgnoreCase("male") ? Sex.MALE : Sex.FEMALE);
		$participant::p.setSex(pSex);
	}
	|	PARTICIPANT_ATTR_SES
	{	$participant::p.setSES($PARTICIPANT_ATTR_SES.text);		}
	|	PARTICIPANT_ATTR_EDUCATION
	{	$participant::p.setEducation($PARTICIPANT_ATTR_EDUCATION.text);		}
	|	PARTICIPANT_ATTR_FIRST_LANGUAGE
	{	
		LOGGER.warning("Attribute 'first-language' not supported.");
	}
	|	PARTICIPANT_ATTR_BIRTHPLACE
	{
		LOGGER.warning("Attribute 'birthplace' not supported.");
	}
	|	PARTICIPANT_ATTR_BIRTHDAY
	{
		DateFormatter formatter = new DateFormatter();
		
		try {
			LocalDate bday = formatter.parse($PARTICIPANT_ATTR_BIRTHDAY.text);
			$participant::p.setBirthDate(bday);
		} catch (ParseException e) {
			LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
		}
	}
	|	PARTICIPANT_ATTR_CUSTOM_FIELD
	{
		// TODO
	}
	;

    

        
comment returns [Comment val]
scope {
	StringBuffer buffer;
	MediaSegment media;
}
@init {
	$comment::buffer = new StringBuffer();
}
	:	^(COMMENT_START type=COMMENT_ATTR_TYPE? commentele*)
	{
		CommentEnum cType = CommentEnum.Generic;
		if($type != null)
		{
			CommentEnum t =
				CommentEnum.fromString($type.text);
			if(t != null) cType = t;
		}
		$val = sessionFactory.createComment(cType, $comment::buffer.toString());
		
		if($media != null) {
			$val.putExtension(MediaSegment.class, $comment::media);
		}
	}
	;
	
commentele
	:	media
	{
		$comment::media = $media.val;
	}
	|	TEXT
	{
		$comment::buffer.append($TEXT.text);
	}
	;

    

        
tcu returns [List<Record> records]
@before {
    $records = new ArrayList<Record>();
}
    :    ^(TCU_START (tcuGrp+=u{$records.add($u.record);})+)
    {
        // add TCU comments to appropriate records
        Record firstRecord = (Record)$tcuGrp.get(0);
        Record lastRecord = (Record)$tcuGrp.get($tcuGrp.size()-1);
        
        Comment tcuStart = sessionFactory.createComment(CommentEnum.BeginTcu, "");
        firstRecord.addComment(tcuStart);
        
        Comment tcuEnd = sessionFactory.createComment(CommentEnum.EndTcu, "");
        lastRecord.addComment(tcuEnd);
    }
    ;

    

        
u returns [Record record]
scope {
	Record r;
	
	// when adding annotations, sometimes we need to create
	// the first word-group.  This flag tells the next ugrp
	// element to use the last group instead of making a new one
	boolean useLastGroup;
	
	String who;
	String recordLang;
	
	List<String> links;
}
@init {
	$u::r = sessionFactory.createRecord();
	$u::useLastGroup = false;
	$u::who = null;
	$u::recordLang = null;
	$u::links = new ArrayList<>();
}
@after {
	$u.record = $u::r;
	++recordIndex;
}
	:	^(U_START u_attrs* (linker{$u::links.add($linker.val);})* uele* t postcode* seg=media? uendele*)
	{
		// setup speaker
		if($u::who != null) 
		{
			// try to find the speaker
			String partId = $u::who;
			
			// by id first
			for(Participant p:session.getParticipants()) {
				if(p.getId().equals(partId)) {
					$u::r.setSpeaker(p);
					break;
				}
			}
			
		}
		
		if($u::links.size() > 0) {
			OrthographyBuilder builder = new OrthographyBuilder();
			$u::links.forEach( l -> builder.append(l) );
			builder.append($u::r.getOrthography().getGroup(0));
			$u::r.getOrthography().setGroup(0, builder.toOrthography());
		}
		
		// set media if avail
		if($seg.val != null)
		{
			$u::r.getSegment().setGroup(0, $seg.val);
		}
		
		if(isSyllabifyAndAlign()) {
			Syllabifier syllabifier = getSyllabifier();
			if($u::recordLang != null) {
				// make sure the uttlang tier exists in session
				TierDescription langTierDesc = ensureTierExists("uttlang", false);
				
				Tier<String> langTier = $u::r.getTier(langTierDesc.getName(), String.class);
				if(langTier == null) {
					langTier = sessionFactory.createTier(langTierDesc.getName(), String.class, false);
					$u::r.putTier(langTier);
				}
				langTier.setGroup(0, $u::recordLang);
				
				try {
					Language lang = Language.parseLanguage($u::recordLang);
					SyllabifierLibrary library = SyllabifierLibrary.getInstance();
					Syllabifier s = library.getSyllabifierForLanguage(lang);
					if(s != null) syllabifier = s;
				} catch (IllegalArgumentException e) {
					LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
				}
			}
			
			// syllabify/align IPA as necessary
			for(int gIdx = 0; gIdx < $u::r.numberOfGroups(); gIdx++) {
				Group g = $u::r.getGroup(gIdx);
				
				IPATranscript model = g.getIPATarget();
				boolean isSyllabifyModel = 
						model != null && model.length() > 0;
				if(isSyllabifyModel) {
					syllabifier.syllabify(model.toList());
				}
				
				IPATranscript actual = g.getIPAActual();
				boolean isSyllabifyActual = 
						actual != null && actual.length() > 0;
				if(isSyllabifyActual) {
					syllabifier.syllabify(actual.toList());
				}
				
				PhoneMap pm = g.getPhoneAlignment();
				if(pm == null || pm.getAlignmentLength() == 0) {
					pm = (new PhoneAligner()).calculatePhoneAlignment(model, actual);
					g.setPhoneAlignment(pm);
				}
			}
		}
		
		// add comments if necessary
		if(nextRecordComments.size() > 0) {
			nextRecordComments.forEach( c -> $u::r.addComment(c) );
			nextRecordComments.clear();
		}
		
		$u.record = $u::r;
	}
	;
	
u_attrs
	:	U_ATTR_WHO
	{
		$u::who = $U_ATTR_WHO.text;
	}
	|	U_ATTR_LANG
	{
		$u::recordLang = $U_ATTR_LANG.text;
	}
	;

uele
	:	ugrp
	|	uannotation
	;

/**
 * Each ugrp object creates a new group in Phon.
 */
 ugrp returns [Group group]
 scope {
 	Group g;
 }
 @init {
 	if($u::useLastGroup) {
 		$ugrp::g = $u::r.getGroup($u::r.numberOfGroups()-1);
 		
 		// reset flag
 		$u::useLastGroup = false;
 	} else {
 		$ugrp::g = $u::r.addGroup();
 	}
 }
@after {
 	$ugrp.group = $ugrp::g;
}
	:	w 
	{	
		OrthographyBuilder builder = new OrthographyBuilder();
		builder.append($ugrp::g.getOrthography()).append($w.val);
		$ugrp::g.setOrthography(builder.toOrthography());
	}
	| 	pg 
	{	
		OrthographyBuilder builder = new OrthographyBuilder();
		builder.append($ugrp::g.getOrthography()).append($pg.val);
		$ugrp::g.setOrthography(builder.toOrthography());
	}
	| 	g
	{	
		// we may need to break data up into more words
		// check to see if we have sub-[] enclosures
		String gData = $g.val;
		if(gData.contains("[") || gData.contains("]")) {
			// break up data
			List<String> grps = new ArrayList<String>();
			String currentGrp = "";
			for(int i = 0; i < gData.length(); i++) {
				char c = gData.charAt(i);
				
				if(c == '[') {
	                if(currentGrp.trim().length() > 0) {
	                    grps.add(currentGrp.trim());
	                }
	                currentGrp = "";
	            }
	            currentGrp += c;
	            if(c == ']') {
	                if(currentGrp.trim().length() > 0) {
	                    grps.add(currentGrp.trim());
	                }
	                currentGrp = "";
	            }
					
			}
			if(currentGrp.trim().length() > 0) {
				grps.add(currentGrp.trim());
			}
			// place only an openeing '{' in our first group
			// this tells the tree builder   to create this group surrounding the 
			// next <pg> groups
			$ugrp::g.setOrthography(
				(new OrthographyBuilder()).append("{").toOrthography());
			
			// if we have sub-pg groups.  The new words should have been created.
			int phoRepIndex = 1;
			for(String g:grps) {
				Group nextGroup = $u::r.addGroup();
				
				if(g.startsWith("[")) {
					nextGroup.setOrthography(
						(new OrthographyBuilder()).append(g.substring(1, g.length()-1))
							.toOrthography());
					
					// add phonetic reps generated by pho rule
					IPATranscript tRep = $g.targetReps.get(phoRepIndex);
					if(tRep == null) tRep = new IPATranscript();
					nextGroup.setIPATarget(tRep);

					IPATranscript aRep = $g.actReps.get(phoRepIndex);
					if(aRep == null) aRep = new IPATranscript();
					nextGroup.setIPAActual(aRep);
					
					PhoneMap pm = $g.phoneMaps.get(phoRepIndex);
					if(pm == null) 
						pm = (new PhoneAligner()).calculatePhoneMap(tRep, aRep);
					nextGroup.setPhoneAlignment(pm);
					
					for(String tierName:$g.tierMaps.keySet()) {
						Tier<String> tier = $u::r.getTier(tierName, String.class);
						if(tier == null) {
							tier = sessionFactory.createTier(tierName, String.class, true);
							for(int i = 0; i < $u::r.numberOfGroups(); i++) tier.addGroup();
							$u::r.putTier(tier);
						}
						String grpVal = $g.tierMaps.get(tierName).get(phoRepIndex);
						if(grpVal == null) grpVal = "";
						nextGroup.setTier(tierName, String.class, grpVal);
					}
						
					++phoRepIndex;
				} else {
					nextGroup.setOrthography(
						(new OrthographyBuilder()).append(g).toOrthography());
				}
			}
			
			// finish 'global' group
			Group endGrp = $u::r.addGroup();
			endGrp.setOrthography(
				(new OrthographyBuilder()).append("}").toOrthography());
		} else {
			$ugrp::g.setOrthography(
				(new OrthographyBuilder()).append($ugrp::g.getOrthography())
					.append("{").append($g.val).append("}")
					.toOrthography());
					
			// IPA Transcriptions
			IPATranscript ipaT = $g.targetReps.get(0);
			if(ipaT != null) $ugrp::g.setIPATarget(ipaT);
			
			IPATranscript ipaA = $g.actReps.get(0);
			if(ipaA != null) $ugrp::g.setIPAActual(ipaA);
			
			PhoneMap pm = $g.phoneMaps.get(0);
			if(pm != null) $ugrp::g.setPhoneAlignment(pm);
			
			// other tiers
			for(String tierName:$g.tierMaps.keySet()) {
				Tier<String> tier = $u::r.getTier(tierName, String.class);
				if(tier == null) {
					tier = sessionFactory.createTier(tierName, String.class, true);
					for(int i = 0; i < $u::r.numberOfGroups(); i++) tier.addGroup();
					$u::r.putTier(tier);
				}
				String grpVal = $g.tierMaps.get(tierName).get(0);
				if(grpVal == null) grpVal = "";
				$ugrp::g.setTier(tierName, String.class, grpVal);
			}
		}
	}
	|	sg
	{
		// TODO support sign groups
		LOGGER.warning("Sign groups are not supported");
	}
	;
	
uannotation
	:	blob
	{
		Group lastGroup = 
			($u::r.numberOfGroups() > 0 ? $u::r.getGroup($u::r.numberOfGroups()-1) : $u::r.addGroup());
		lastGroup.setOrthography(
			(new OrthographyBuilder()).append(lastGroup.getOrthography())
				.append($blob.val).toOrthography());
	}
	|	quotation
	{
		// TODO quotation
		LOGGER.warning("quotation not supported");
	}
	|	quotation2
	{
		// TODO quotation2
		LOGGER.warning("quotation2 not supported");
	}
	|	pause
	{
		// add pause to last group as a comment
		Group lastGroup = 
			($u::r.numberOfGroups() > 0 ? $u::r.getGroup($u::r.numberOfGroups()-1) : $u::r.addGroup());
		lastGroup.setOrthography(
			(new OrthographyBuilder()).append(lastGroup.getOrthography())
				.append($pause.val).toOrthography());
	}
	|	internal_media
	{
		Group lastGroup = 
			($u::r.numberOfGroups() > 0 ? $u::r.getGroup($u::r.numberOfGroups()-1) : $u::r.addGroup());
		lastGroup.setOrthography(
			(new OrthographyBuilder()).append(lastGroup.getOrthography())
				.append($internal_media.val).toOrthography());
	}
	|	freecode
	{
		Group lastGroup = 
			($u::r.numberOfGroups() > 0 ? $u::r.getGroup($u::r.numberOfGroups()-1) : $u::r.addGroup());
		lastGroup.setOrthography(
			(new OrthographyBuilder()).append(lastGroup.getOrthography())
				.append($freecode.val).toOrthography());
	}
	|	e
	{
		// add event to last group
		// add pause to last group as a comment
		Group lastGroup = 
			($u::r.numberOfGroups() > 0 ? $u::r.getGroup($u::r.numberOfGroups()-1) : $u::r.addGroup());
		lastGroup.setOrthography(
			(new OrthographyBuilder()).append(lastGroup.getOrthography())
				.append($e.val).toOrthography());
	}
	|	s
	{
		// add separator to last group
		Group lastGroup = 
			($u::r.numberOfGroups() > 0 ? $u::r.getGroup($u::r.numberOfGroups()-1) : $u::r.addGroup());
		lastGroup.setOrthography(
			(new OrthographyBuilder()).append(lastGroup.getOrthography())
				.append($s.val).toOrthography());
	}
	|	tagmarker
	{
		Group lastGroup = 
			($u::r.numberOfGroups() > 0 ? $u::r.getGroup($u::r.numberOfGroups()-1) : $u::r.addGroup());
		lastGroup.setOrthography(
			(new OrthographyBuilder()).append(lastGroup.getOrthography())
				.append($tagmarker.val).toOrthography());
	}
	|	overlap_point
	{
		Group lastGroup = 
			($u::r.numberOfGroups() > 0 ? $u::r.getGroup($u::r.numberOfGroups()-1) : $u::r.addGroup());
		lastGroup.setOrthography(
			(new OrthographyBuilder()).append(lastGroup.getOrthography())
				.append($overlap_point.val).toOrthography());
	}
	|	underline
	{
		Group lastGroup = 
			($u::r.numberOfGroups() > 0 ? $u::r.getGroup($u::r.numberOfGroups()-1) : $u::r.addGroup());
		lastGroup.setOrthography(
			(new OrthographyBuilder()).append(lastGroup.getOrthography())
				.append($underline.val).toOrthography());
	}
	|	italic
	{
		Group lastGroup = 
			($u::r.numberOfGroups() > 0 ? $u::r.getGroup($u::r.numberOfGroups()-1) : $u::r.addGroup());
		lastGroup.setOrthography(
			(new OrthographyBuilder()).append(lastGroup.getOrthography())
				.append($italic.val).toOrthography());
	}
	|	long_feature
	{
		Group lastGroup = 
			($u::r.numberOfGroups() > 0 ? $u::r.getGroup($u::r.numberOfGroups()-1) : $u::r.addGroup());
		lastGroup.setOrthography(
			(new OrthographyBuilder()).append(lastGroup.getOrthography())
				.append($long_feature.val).toOrthography());
	}
	|	nonvocal
	{
		Group lastGroup = 
			($u::r.numberOfGroups() > 0 ? $u::r.getGroup($u::r.numberOfGroups()-1) : $u::r.addGroup());
		lastGroup.setOrthography(
			(new OrthographyBuilder()).append(lastGroup.getOrthography())
				.append($nonvocal.val).toOrthography());
	}
	;
	
uendele
	:	k
	{
		TierDescription tierDesc = ensureTierExists("Markers", false);
		
		Tier<String> markersTier = $u::r.getTier(tierDesc.getName(), String.class);
		if(markersTier == null) {
			markersTier = sessionFactory.createTier(tierDesc.getName(), String.class, tierDesc.isGrouped());
			$u::r.putTier(markersTier);
		}
		
		StringBuffer buffer = new StringBuffer();
		if(markersTier.getGroup(0) != null) {
			buffer.append(markersTier.getGroup(0)).append(" ");
		}
		buffer.append($k.val);
		markersTier.setGroup(0, buffer.toString());
	}
	|	error
	{
		TierDescription tierDesc = ensureTierExists("Errors", false);
		
		Tier<String> errorsTier = $u::r.getTier(tierDesc.getName(), String.class);
		if(errorsTier == null) {
			errorsTier = sessionFactory.createTier(tierDesc.getName(), String.class, tierDesc.isGrouped());
			$u::r.putTier(errorsTier);
		}
		
		StringBuffer buffer = new StringBuffer();
		if(errorsTier.getGroup(0) != null) {
			buffer.append(errorsTier.getGroup(0)).append(" ");
		}
		buffer.append($error.val);
		errorsTier.setGroup(0, buffer.toString());
	}
	|	r
	{
		TierDescription tierDesc = ensureTierExists("Repetition", false);
		
		Tier<String> repTier = $u::r.getTier(tierDesc.getName(), String.class);
		if(repTier == null) {
			repTier = sessionFactory.createTier(tierDesc.getName(), String.class, tierDesc.isGrouped());
			$u::r.putTier(repTier);
		}
		
		StringBuffer buffer = new StringBuffer();
		if(repTier.getGroup(0) != null) {
			buffer.append(repTier.getGroup(0)).append(" ");
		}
		buffer.append($r.val);
		repTier.setGroup(0, buffer.toString());
	}
	|	a
	{
		// sub-rule a performs necessary tier management
	}
	;

    

        
linker returns [String val]
@init {
	$val = "(";
}
@after {
	$val += ")";
}
	:	^(LINKER_START type=LINKER_ATTR_TYPE)
	{
		String lkType = $type.text;
		if(lkType.equals("quoted utterance next"))
			$val += "+\"";
		else if(lkType.equals("quick uptake"))
			$val += "+^";
		else if(lkType.equals("lazy overlap mark"))
			$val += "+<";
		else if(lkType.equals("self completion"))
			$val += "+,";
		else if(lkType.equals("other completion"))
			$val += "++";
		else if(lkType.equals("technical break TCU completion"))
			$val += "+\u224b";
		else if(lkType.equals("no break TCU completion"))
			$val += "+\u2248";
	}
	;	

    

        
w returns [String val]
scope {
	String word;
	String elements;
	String suffix;
	String prefix;
}
@init {
	$w::word = new String();
	$w::elements = new String();
	$w::suffix = "";
	$w::prefix = "";
}
@after {
	$val = $w::prefix + $w::word + $w::suffix + ($w::elements.length() > 0 ? " " + $w::elements : "");
}
	:	^(W_START wattr* ls=langs? wele*)
	{
		if($ls.val != null) {
			$w::word = $ls.val + $w::word;
		}
	}
	;
	
wele
	:	TEXT
	{	
		$w::word += ($w::word.length() > 0 ? "" : "") + $TEXT.text;	
	}
	|	overlap_point
	{
		$w::elements += ($w::elements.length() > 0 ? " " : "") + $overlap_point.val;
	}
	|	underline
	{
		$w::elements += ($w::elements.length() > 0 ? " " : "") + $underline.val;
	}
	|	italic
	{
		$w::elements += ($w::elements.length() > 0 ? " " : "") + $italic.val;
	}
	|	shortening
	{
		$w::word += $shortening.val;
	}
	|	p 
	{
		$w::word += $p.val;
	}
	|	long_feature
	{
		$w::elements += ($w::elements.length() > 0 ? " " : "") + $long_feature.val;
	}
	| 	wk
	{
		$w::word += $wk.val;
	}
	|	pos
	|	replacement
	{
		$w::elements += ($w::elements.length() > 0 ? " " : "") + $replacement.val;
	}
	|	mor
	{
		// mor is handled as a seperate dependent tier
		String morVal = $mor.val;
		String tierName = $mor.tierName;
		
		if($g.size() > 0) {
			// add to group data
			Map<Integer, String> trMap = $g::trMaps.get(tierName);
			if(trMap == null) {
				trMap = new LinkedHashMap<>();
				$g::trMaps.put(tierName, trMap);
			}
			String val = (trMap.containsKey($g::pgCount) ? trMap.get($g::pgCount) : "");
			val += (val.length() > 0 ? " " : "") + morVal;
			trMap.put($g::pgCount, val);
		} else {
			// make sure dep tier exists in session
			TierDescription tierDesc = ensureTierExists(tierName, true); 
			
			Tier<String> morTier = $u::r.getTier(tierName, String.class);
			if(morTier == null) {
				morTier = sessionFactory.createTier(tierName, String.class, true);
				for(int i = 0; i < $u::r.numberOfGroups(); i++) morTier.addGroup();
				$u::r.putTier(morTier);
			}
			
			// add mor data as a dep tier of the current group
			Group group = null;
			// get the correct word group holder
			if($t.size() > 0 || $ugrp.size() > 0) {
			    group = ($t.size() > 0 ? $t::g : $ugrp::g);
			} else if($u.size() > 0) {
			    group = $u::r.getGroup($u::r.numberOfGroups()-1);
			}
			
			String tierValue = 
				(group.getTier(tierName) != null ? group.getTier(tierName, String.class) : "");
			tierValue += 
			    (tierValue.length() == 0 ? "" : " ") + morVal;
			group.setTier(tierName, String.class, tierValue);
		}
	}
	|	mk
	{
		$w::elements += ($w::elements.length() > 0 ? " " : "") 
			+ "(mk," + $mk.type + ":" + $mk.val + ")";
	}
	;
	
wattr
	:	W_ATTR_SEPARATED_PREFIX
	{
		// word#
		if(Boolean.parseBoolean($W_ATTR_SEPARATED_PREFIX.text))
			$w::suffix = "#";
	}
	|	W_ATTR_USER_SPECIAL_FORM
	{
		$w::suffix = "@z:" + $W_ATTR_USER_SPECIAL_FORM.text;
	}
	|	W_ATTR_FORMSUFFIX
	{
		$w::suffix += "-" + $W_ATTR_FORMSUFFIX.text;
	}
	|	W_ATTR_FORMTYPE
	{
		$w::suffix = "@";
		
		String t = $W_ATTR_FORMTYPE.text;
		if(t.equals("addition")) {
			$w::suffix += "a";
		} else if(t.equals("babbling")) {
			$w::suffix += "b";
		} else if(t.equals("child-invented")) {
			$w::suffix += "c";
		} else if(t.equals("dialect")) {
			$w::suffix += "d";
		} else if(t.equals("echolalia")) {
			$w::suffix += "e";
		} else if(t.equals("family-specific")) {
			$w::suffix += "f";
		} else if(t.equals("filled pause")) {
			$w::suffix += "fp";
		} else if(t.equals("filler syllable")) {
			$w::suffix += "fs";
		} else if(t.equals("generic")) {
			$w::suffix += "g";
		} else if(t.equals("interjection")) {
			$w::suffix += "i";
		} else if(t.equals("kana")) {
			$w::suffix += "k";
		} else if(t.equals("letter")) {
			$w::suffix += "l";
		} else if(t.equals("neologism")) {
			$w::suffix += "n";
		} else if(t.equals("no voice")) {
			$w::suffix += "nv";
		} else if(t.equals("onomatopoeia")) {
			$w::suffix += "o";
		} else if(t.equals("phonology consistent")) {
			$w::suffix += "p";
		} else if(t.equals("quoted metareference")) {
			$w::suffix += "q";
		} else if(t.equals("sign speech")) {
			$w::suffix += "sas";
		} else if(t.equals("singing")) {
			$w::suffix += "si";
		} else if(t.equals("signed language")) {
			$w::suffix += "sl";
		} else if(t.equals("test")) {
			$w::suffix += "t";
		} else if(t.equals("UNIBET")) {
			$w::suffix += "u";
		} else if(t.equals("words to be excluded")) {
			$w::suffix += "x";
		} else if(t.equals("word play")) {
			$w::suffix += "wp";
		}
	}
	|	W_ATTR_TYPE
	{
		String t = $W_ATTR_TYPE.text;
		if(t.equals("omission")) { 
			$w::prefix = "0";
		} else if(t.equals("fragment")) {
			$w::prefix = "&";
		} else if(t.equals("filler")) {
			$w::prefix = "&-";
		} else if(t.equals("incomplete")) {
			$w::prefix = "&+";
		}
	}
	|	W_ATTR_UNTRANSCRIBED
	;

    

        
langs returns [String val]
scope {
	String buffer;
}
@init {
	$langs::buffer = new String();
}
@after {
	$val = $langs::buffer.toString();
}
	:	^(LANGS_START langsEle)
	;
	
langsEle	
	:	singleLang
	|	multipleLang
	|	ambiguousLang
	;
	
singleLang
	:	^(SINGLE_START TEXT)
	{
		$langs::buffer = "(langs:single," + $TEXT.text + ")";
	}
	;
	
multipleLang
	:	^(MULTIPLE_START TEXT)
	{
		$langs::buffer = "(langs:multiple," + $TEXT.text + ")";
	}
	;
	
ambiguousLang
	:	^(AMBIGUOUS_START TEXT)
	{
		$langs::buffer = "(langs:ambiguous," + $TEXT.text + ")";
	}
	;
	

    

        
overlap_point returns [String val]
scope {
    String index;
    String startEnd;
    String topBottom;
}
@init {
    $overlap_point::index = null;
    $overlap_point::startEnd = null;
    $overlap_point::topBottom = null;
}
    :    ^(OVERLAP_POINT_START overlap_pointattrs+)
    {
        $val = "(overlap-point:";
        
        String attrs = 
            ($overlap_point::index != null ?  $overlap_point::index : "");
        attrs += (attrs.length() > 0 ? "," : "") +
            ($overlap_point::startEnd != null ? $overlap_point::startEnd : "");
        attrs += (attrs.length() > 0 ? "," : "") + 
            ($overlap_point::topBottom != null ? $overlap_point::topBottom : "");
            
        $val += attrs + ")";
    }
    ;
    
overlap_pointattrs
    :    OVERLAP_POINT_ATTR_INDEX
    {
        $overlap_point::index = $OVERLAP_POINT_ATTR_INDEX.text;
    }
    |    OVERLAP_POINT_ATTR_START_END
    {
        $overlap_point::startEnd = $OVERLAP_POINT_ATTR_START_END.text;
    }
    |    OVERLAP_POINT_ATTR_TOP_BOTTOM
    {
        $overlap_point::topBottom = $OVERLAP_POINT_ATTR_TOP_BOTTOM.text;
    }
    ;

    

        
underline returns [String val]
@init {
$val = "";
}
	:	^(UNDERLINE_START type=UNDERLINE_ATTR_TYPE)
	{
		String uType = $type.text;
		$val = "*underline:" + uType + "*";
	}
	;	

    

        
italic returns [String val]
@init {
$val = "";
}
	:	^(ITALIC_START type=ITALIC_ATTR_TYPE)
	{
		String uType = $type.text;
		$val = "*italic:" + uType + "*";
	}
	;	

    

        
shortening returns [String val]
	:	^(SHORTENING_START v=TEXT?)
	{
		if($v != null) {
			$val = "<" + $v.text + ">";
		}
	}
	;

    

        
p returns [String val]
	:	^(P_START type=P_ATTR_TYPE?)
	{
		if($type != null)
		{
			String pt = $type.text;
			
			if(pt.equals("stress"))
				$val = "/";
			else if(pt.equals("accented nucleus"))
				$val = "//";
			else if(pt.equals("contrastive stress"))
				$val = "///";
			else if(pt.equals("drawl"))
				$val = ":";
			else if(pt.equals("pause"))
				$val = "^";
			else if(pt.equals("blocking"))
				$val = "^";
		}
	}
	;

    

        
long_feature returns [String val]
    :    ^(LONG_FEATURE_START LONG_FEATURE_ATTR_TYPE TEXT)
    {
        $val = "(long-feature," + $LONG_FEATURE_ATTR_TYPE.text + ":" + $TEXT.text + ")";
    }
    ;

    

        
wk returns [String val]
	:	^(WK_START type=WK_ATTR_TYPE?)
	{
		// return value based on type
		if($type != null) {
			String wkt = $type.text;
			
			if(wkt.equals("cmp"))
				$val = "+";
			else if(wkt.equals("cli"))
				$val = "~";
		} else {
			$val = "";
		}
	}
	;

    

        
replacement returns [String val]
scope {
    String buffer;
}
@init {
    $replacement::buffer = "";
}
@after {
    $val = "(replacement:" + $replacement::buffer.toString() + ")";
}
	:	^(REPLACEMENT_START replacementele*)
	;
	
replacementele
    :    w
    {
        $replacement::buffer += ($replacement::buffer.length() > 0 ? " " : "") + $w.val;
    }
    ;

    

        
mor returns [String tierName, String val]
scope
{
    List<String> morPres;
    List<String> morPosts;
    List<String> menxVals;
    
    String morType;
    Boolean morOmitted;
}
@init
{
    $mor::morPres = new ArrayList<String>();
    $mor::morPosts = new ArrayList<String>();
    $mor::menxVals = new ArrayList<String>();
    
    $mor::morType = "mor";
    $mor::morOmitted = Boolean.FALSE;
}
    :    ^(MOR_START morattr+ morchoice menx* gra* morseq*)
    {
        $tierName = ($mor::morType.equals("mor") ? "Morphology" : "trn");
 
        // build mor-string
		String v = $morchoice.val;
		for(String menxVal:$mor::menxVals) {
		    v += "=" + menxVal;
		}
		for(String morPre:$mor::morPres) {
		    v = morPre + "$" + v;
		}
		for(String morPost:$mor::morPosts) {
		    v += "~" + morPost;
		}
		
		// if omitted, add the '0'
		if($mor::morOmitted) {
		    v = "0" + v;
		}
		
		$val = v;
    }
    ;
    
morattr
    :    MOR_ATTR_TYPE
    {    $mor::morType = $MOR_ATTR_TYPE.text;    }
    |    MOR_ATTR_OMITTED
    {    $mor::morOmitted = new Boolean($MOR_ATTR_OMITTED.text);    }
    ;
    
morchoice returns [String val]
    :    mw
    {    $val = $mw.val;    }
    |    mwc
    {    $val = $mwc.val;    }
    |    mt
    {    $val = $mt.val;    }
    ;
    
morseq returns [String val]
    :    mor_pre
    {    $val = $mor_pre.val;    }
    |    mor_post
    {    $val = $mor_post.val;    }
    ;
    
mor_pre returns [String val]
scope
{
    List<String> menxVals;
}
@init
{
    $mor_pre::menxVals = new ArrayList<String>();
}
@after
{
    $mor::morPres.add($val);
}
    :    ^(MOR_PRE_START morchoice menx* gra*)
    {
        $val = $morchoice.val;
        for(String menxVal:$mor_pre::menxVals) {
            $val += "=" + menxVal;
        }
    }
    ;
    
mor_post returns [String val]
scope
{
    List<String> menxVals;
}
@init
{
    $mor_post::menxVals = new ArrayList<String>();
}
@after
{
    $mor::morPosts.add($val);
}
    :    ^(MOR_POST_START morchoice menx* gra*)
    {
        $val = $morchoice.val;
        for(String menxVal:$mor_post::menxVals) {
            $val += "=" + menxVal;
        }
    }
    ;
    
menx returns [String val]
@after
{
    if($mor_pre.size() > 0) {
        $mor_pre::menxVals.add($val);
    } else if($mor_post.size() > 0) {
        $mor_post::menxVals.add($val);
    } else if($mor.size() > 0) {
        $mor::menxVals.add($val);
    }
}
    :    ^(MENX_START TEXT)
    {
        $val = $TEXT.text;
    }
    ;

gra returns [String val]
scope
{
    String type;
    String index;
    String head;
    String relation;
}
    :    ^(GRA_START graattrs+)
    {
        // make sure dep tier exists in session
		TierDescription tierDesc = ensureTierExists("GRASP", true, $u::r);
		
		// value
		$val = $gra::index + "|" + $gra::head + "|" + $gra::relation;
		
		// add mor data as a dep tier of the current word(group)
		Group group = null;
		// get the correct word group holder
		if($t.size() > 0 || $ugrp.size() > 0) {
		    group = ($t.size() > 0 ? $t::g : $ugrp::g);
		} else if($u.size() > 0) {
		    group = $u::r.getGroup($u::r.numberOfGroups()-1);
		}
		
		String tierValue = group.getTier("GRASP", String.class);
		if(tierValue == null) tierValue = new String();
		tierValue += 
		    (tierValue.length() == 0 ? "" : " ") + $val;
		group.setTier("GRASP", TierString.class, new TierString(tierValue));
    }
    ;
    
graattrs
    :    GRA_ATTR_TYPE
    {    $gra::type = $GRA_ATTR_TYPE.text;    }
    |    GRA_ATTR_INDEX
    {    $gra::index = $GRA_ATTR_INDEX.text;    }
    |    GRA_ATTR_HEAD
    {    $gra::head = $GRA_ATTR_HEAD.text;    }
    |    GRA_ATTR_RELATION
    {    $gra::relation = $GRA_ATTR_RELATION.text;    }
    ;
    
mw returns [String val]
scope
{
    List<String> mpfxVals;
    List<Tuple<String, String>> mkVals;
}
@init
{
    $mw::mpfxVals = new ArrayList<String>();
    $mw::mkVals = new ArrayList<Tuple<String, String>>();
}
@after {
    // add to compound word if we are inside a <mwc> container
    if($mwc.size() > 0) {
        $mwc::mwVals.add($val);
    }
}
    :   ^(MW_START mpfx* pos stem mk*)
    {
        // simplest case, pos + single choice 
        $val = $pos.val + "|" + $stem.val;
        
        // add mpfx vals if any
        for(String v:$mw::mpfxVals) {
            $val = v + "#" + $val;
        }
        
        // add mk vals if any
        for(Tuple<String, String> v:$mw::mkVals) {
            String suffix = v.getObj1();
            String type = v.getObj2();
            
            String prefix = "";
            if(type.equals("sfx")) {
                prefix = "-";
            } else if(type.equals("sfxf")) {
                prefix = "&";
            } else if(type.equals("mc")) {
                prefix = ":";
            }
            
            $val += prefix + suffix;
        }
    }
    ;
    
mwc returns [String val]
scope
{
    List<String> mwVals;
    List<String> mpfxVals;
    boolean inMwc;
}
@init
{
    $mwc::inMwc = true;
    $mwc::mpfxVals = new ArrayList<String>();
    $mwc::mwVals = new ArrayList<String>();
}
@after
{
    $mwc::inMwc = false;
}
    :    ^(MWC_START mpfx* pos mw+)
    {
        $val = $pos.val + "|";
        for(String v:$mwc::mwVals) {
            $val += "+" + v;
        }
        
        for(String v:$mwc::mpfxVals) {
            $val = v + "#" + $val;
        }
    }
    ;
    
mt returns [String val]
    :    ^(MT_START MT_ATTR_TYPE)
    {
        String t = $MT_ATTR_TYPE.text;
        if(t.equals("p")) {
		   $val = ".";
		} else if(t.equals("q")) {
		    $val = "?";
		} else if(t.equals("e")) {
		    $val = "!";
		} else {
		    // wrap in paren
		    $val = "(mt:" + t + ")";
		}
    }
    ;
    
mpfx returns [String val]
@after {
    if($mw.size() > 0) {
        $mw::mpfxVals.add($val);
    } else if($mwc.size() > 0) {
        $mwc::mpfxVals.add($val);
    }
}
    :    ^(MPFX_START TEXT)
    {
        $val = $TEXT.text;
    }
    ;
   
pos returns [String val]
scope
{
    List<String> sVals;
}
@init
{
    $pos::sVals = new ArrayList<String>();
}
    :    ^(POS_START morposc morposs*)
    {
        $val = $morposc.val;
        for(String s:$pos::sVals) {
            $val += ":" + s;
        }
    }
    ;
    
morposc returns [String val]
    :    ^(C_START TEXT)
    {
        $val = $TEXT.text;
    }
    ;
    
morposs returns [String val]
@after {
    $pos::sVals.add($val);
}
    :    ^(S_START TEXT)
    {
        $val = $TEXT.text;
    }
    ;
    
stem returns [String val]
    :    ^(STEM_START TEXT)
    {
        $val = $TEXT.text;
    }
    ;
    
mk returns [String val, String type]
@after {
    if($mw.size() > 0) {
        $mw::mkVals.add(new Tuple<String, String>($val, $type));
    }
}
    :    ^(MK_START MK_ATTR_TYPE TEXT)
    {
        $val = $TEXT.text;
        $type = $MK_ATTR_TYPE.text;
    }
    ;

    

        
blob returns [String val]
scope {
    StringBuffer buffer;
}
@init {
    $blob::buffer = new StringBuffer();
}
    :    ^(BLOB_START (TEXT{$blob::buffer.append($TEXT.text.replaceAll("\\(", "\\\\(").replaceAll("\\)", "\\\\)"));})*)
    {
        $val = "(blob:" + $blob::buffer.toString() + ")";
    }
    ;

    

        
g returns [String val,  Integer phoRepCount, Map<Integer,IPATranscript> targetReps, Map<Integer, IPATranscript> actReps, Map<Integer, PhoneMap> phoneMaps, Map<String, Map<Integer, String>> tierMaps]
scope {
	String buffer;
	
	// when g has <pg> children store the
 	// phonetic rep objects generated in the
 	// pho rule
 	Map<Integer, IPATranscript> tReps;
 	Map<Integer, IPATranscript> aReps;
	Map<Integer, PhoneMap> pMaps;
	Map<String, Map<Integer, String>> trMaps;
 	
 	int pgCount;
}
@init {
	$g::buffer = new String();
	
	$g::tReps = new HashMap<Integer, IPATranscript>();
	$g::aReps = new HashMap<Integer, IPATranscript>();
	$g::pMaps = new HashMap<Integer, PhoneMap>();
	$g::trMaps = new HashMap<String, Map<Integer, String>>();
	
	$g::pgCount = 0;
}
@after {
	$val = $g::buffer;
	
	$targetReps = $g::tReps;
	$actReps = $g::aReps;
	$phoneMaps = $g::pMaps;
	$tierMaps = $g::trMaps;
	
	$phoRepCount = $g::pgCount;
}
	:	^(G_START gele+ gchoice*)
	;
	
gele
	:	w 
	{	
		$g::buffer += ($g::buffer.length() > 0 ? " " : "") + $w.val;	
	}
	|	nestedg=g
	{
		$g::buffer += ($g::buffer.length() > 0 ? " " : "") +
			"{" + $nestedg.val + "}"; 	
		
		Integer[] targetKeys = $nestedg.targetReps.keySet().toArray(new Integer[0]);
		for(Integer targetKey:targetKeys) {
			int newKey = targetKey + $g::pgCount;
			$g::tReps.put(newKey, $nestedg.targetReps.get(targetKey));
		}
		
		Integer[] actualKeys = $nestedg.actReps.keySet().toArray(new Integer[0]);
		for(Integer actualKey:actualKeys) {
			int newKey = actualKey + $g::pgCount;
			$g::aReps.put(newKey, $nestedg.actReps.get(actualKey));
		}
		
		Integer[] pMapKeys = $nestedg.phoneMaps.keySet().toArray(new Integer[0]);
		for(Integer pmapKey:pMapKeys) {
			int newKey = pmapKey + $g::pgCount;
			$g::pMaps.put(newKey, $nestedg.phoneMaps.get(pmapKey));
		}
		
		for(String tierName:$nestedg.tierMaps.keySet()) {
			Map<Integer, String> trMap = $nestedg.tierMaps.get(tierName);
			if(trMap == null) trMap = new HashMap<>();
			
			Integer[] trMapKeys = trMap.keySet().toArray(new Integer[0]);
			for(Integer trMapKey:trMapKeys) {
				int newKey = trMapKey + $g::pgCount;
				
				Map<Integer, String> newTrMap = $g::trMaps.get(tierName);
				if(newTrMap == null) {
					newTrMap = new LinkedHashMap<>();
					$g::trMaps.put(tierName, newTrMap);
				}
				newTrMap.put(newKey, trMap.get(trMapKey));
			}
		}

		$g::pgCount += $nestedg.phoRepCount;
	}
	|	pg
	{
	 	// enclose the pg data in [] so that we know to break up
	 	// this into proper word groups in phon later.
		$g::buffer += ($g::buffer.length() > 0 ? " " : "") + "[" + $pg.val + "]";
	}
	|	sg
	{
		// TODO sg
		LOGGER.warning("sg not supported");
	}
	|	quotation
	{
		// TODO quotation
		LOGGER.warning("quotation not supported");
	}
	|	quotation2
	{
		// TODO quotation2
		LOGGER.warning("quotation2 not supported");
	}
	|	pause
	{
		$g::buffer += ($g::buffer.length() > 0 ? " " : "") + $pause.val;
	}
	|	internal_media
	{
		$g::buffer += ($g::buffer.length() > 0 ? " " : "") + $internal_media.val;
	}
	|	freecode
	|	e
	{
		$g::buffer += ($g::buffer.length() > 0 ? " " : "") +
			$e.val;
	}
	|	s
	{
		$g::buffer += ($g::buffer.length() > 0 ? " " : "") +
			$s.val;
	}
	|	tagmarker
	{
		$g::buffer += ($g::buffer.length() > 0 ? " " : "") +
			"(" + $tagmarker.val + ")";
	}
	|	long_feature
	{
		$g::buffer += ($g::buffer.length() > 0 ? " " : "") +
			"(" + $long_feature.val + ")";
	}
	|	nonvocal
	{
		$g::buffer += ($g::buffer.length() > 0 ? " " : "") +
			"(" + $nonvocal.val + ")";
	}
	|	overlap_point
	{
		$g::buffer += ($g::buffer.length() > 0 ? " " : "") +
			$overlap_point.val;
	}
	|	underline
	{
		$g::buffer += ($g::buffer.length() > 0 ? " " : "") +
			$underline.val;
	}
	|	italic
	{
		 $g::buffer += ($g::buffer.length() > 0 ? " " : "") +
			"(" + $italic.val + ")";
	}
	;
	
gchoice
	: 	k
	{
		$g::buffer += ($g::buffer.length() > 0 ? " " : "") + 
			"(" + $k.val + ")";
	}
	|	error
	{
		$g::buffer += ($g::buffer.length() > 0 ? " " : "") +
			$error.val;
	}
	| 	r 
	{	
		$g::buffer += ($g::buffer.length() > 0 ? " " : "") + 
			"(x" + $r.val + ")";	
	}
	|	duration
	|	ga
	{
		$g::buffer += ($g::buffer.length() > 0 ? " " : "") +
			$ga.val;
	}
	|	overlap
	{
		$g::buffer += ($g::buffer.length() > 0 ? " " : "") +
			"(" + $overlap.val + ")";
	}
	;

    

        
pg returns [String val]
scope {
	String buffer;
	int phoCount;
}
@init {
	$pg::buffer = new String();
	if($g.size() > 0) {
		$g::pgCount++;
	}
}
@after {
	$val = $pg::buffer.toString();
}
	:	^(PG_START pgele* pho* align?)
	;
	
pgele returns [String val, Map<String, Map<Integer, String>> tierMaps]
@init {
	$val = new String();
	$tierMaps = new HashMap<>();
}
@after {
	$pg::buffer += ($pg::buffer.length() > 0 ? " " : "") + $val;
}
	:	w 
	{
		$val = $w.val;
	}
	|	g
	{
		$val = "{" + $g.val + " }";
		
		for(String tierName:$g.tierMaps.keySet()) {
			Map<Integer, String> trMap = $g.tierMaps.get(tierName);
			String grpVal = trMap.values().stream().collect(Collectors.joining(" "));
			
			Tier<String> tier = $u::r.getTier(tierName, String.class);
			if(tier == null) {
				tier = sessionFactory.createTier(tierName, String.class, true);
				for(int i = 0; i < $u::r.numberOfGroups(); i++) tier.addGroup();
				$u::r.putTier(tier);
			}
			$ugrp::g.setTier(tierName, String.class, grpVal);
		}
		$tierMaps = $g.tierMaps;
	}
	|	quotation
	{
		// TODO quotation
		LOGGER.warning("quotation not supported");
	}
	|	quotation2
	{
		// TODO quotation2
		LOGGER.warning("quotation2 not supported");
	}
	|	pause
	{
		$val = $pause.val;
	}
	|	internal_media
	{
		$val = $internal_media.val;
	}
	|	freecode
	{
		$val = $freecode.val;
	}
	|  	e
	{
		$val = $e.val;
	}
	|	s
	{
		$val = $s.val;
	}
	|	tagmarker
	{
		$val = $tagmarker.val;
	}
	|	long_feature
	{
		$val = $long_feature.val;
	}
	|	nonvocal
	{
		$val = $nonvocal.val;
	}
	|	overlap_point
	{
		$val = $overlap_point.val;
	}
	|	underline
	{
		$val = $underline.val;
	}
	|	italic
	{
		$val = $italic.val;
	}
	;
	

    

        
quotation
    :    ^(QUOTATION_START QUOTATION_ATTR_TYPE mor*)
    ;

    

        
quotation2
    :    ^(QUOTATION2_START QUOTATION2_ATTR_TYPE mor*)
    ;

    

        
pause returns [String val]
	:	^(PAUSE_START sl=PAUSE_ATTR_SYMBOLIC_LENGTH? len=PAUSE_ATTR_LENGTH?)
	{
		$val = "(pause:";
		if($len != null) {
		    $val = "(pause:" + $len.text + ")";
		} else {
		    String t = $sl.text;
		    if(t.equals("simple"))  {
		        $val = "(.)";
		    } else if(t.equals("long")) {
		        $val = "(..)";
		    } else if(t.equals("very long")) {
		        $val = "(...)";
		    }
		}
	}
	;

    

        
internal_media returns [String val]
scope {
	String startAttr;
	String endAttr;
}
@init {
	$internal_media::startAttr = null;
	$internal_media::endAttr = null;
}
    :   ^(INTERNAL_MEDIA_START internal_media_attr*)
    {
    	if($internal_media::startAttr != null && $internal_media::endAttr != null) {
    		$val = "(internal-media:" + $internal_media::startAttr + "-" + $internal_media::endAttr + ")";
    	}
    }
	;
	
internal_media_attr
	:	INTERNAL_MEDIA_ATTR_START
	{
		$internal_media::startAttr = 
			$INTERNAL_MEDIA_ATTR_START.text;
	}
	|	INTERNAL_MEDIA_ATTR_END
	{
		$internal_media::endAttr = 
			$INTERNAL_MEDIA_ATTR_END.text;
	}
	|	INTERNAL_MEDIA_ATTR_UNIT
	;

    

        
freecode returns [String val]
    :    ^(FREECODE_START TEXT)
    {
        $val = "(freecode:" + $TEXT.text + ")";
    }
    ;

    

        
e returns [String val]
scope {
	String buffer;
}
@init {
	$e::buffer = new String();
}
@after {
	$val = "*" + $e::buffer + "*";
}
	:	^(E_START echoice1 echoice2*)
	;
	

echoice1
	:	action
	{
		$e::buffer += ($e::buffer.length() > 0 ? " " : "") + "(action:";
		
		if($action.val != null)
		{
			$e::buffer += $action.val;
		}
		$e::buffer += ")";
	}
	|	happening
	{
		// default event option, no parenthesis needed
		$e::buffer += ($e::buffer.length() > 0 ? " " : "");
		if($happening.val != null)
		{
			$e::buffer += $happening.val;
		}
	}
	|	otherspokenevent
	{
		$e::buffer += ($e::buffer.length() > 0 ? " " : "");
		if($otherspokenevent.val != null) {
			$e::buffer += $otherspokenevent.val;
		}
	}
	;

echoice2
	:	k
	{
		if($k.val != null) {
			$e::buffer += ($e::buffer.length() > 0 ? " " : "") +
				$k.val;
		}
	}
	|	error
	{
		if($error.val != null) {
			$e::buffer += ($e::buffer.length() > 0 ? " " : "") +
					$error.val;
		}
	}
	|	r
	{
		if($r.val != null) {
			$e::buffer += ($e::buffer.length() > 0 ? " " : "") +
					"(x" + $r.val + ")";
		}
	}
	|	overlap
	{
		$e::buffer += ($e::buffer.length() > 0 ? " " : "") +
				"(";
		if($overlap.val != null) {
			 $e::buffer += $overlap.val;	
		}
		$e::buffer += ")";
	}
	|	ga
	{
		if($ga.val != null)
		{
			$e::buffer += ($e::buffer.length() > 0 ? " " : "") +
				$ga.val;
		}
	}
	|	duration
	;

    

        
action returns [String val]
@init {
	$val = null;
}
	:	^(ACTION_START v=TEXT?)
	{
		if($v != null)
		{
			$val = $v.text;
		}
	}
	;

    

        
happening returns [String val]
@init {
	$val = null;
}
	:	^(HAPPENING_START v=TEXT?)
	{
		if($v != null) 
		{
			$val = $v.text;
		}
	}
	;

    

        
ga returns [String val]
@init{
	$val = "";
}
	:	^(GA_START type=GA_ATTR_TYPE? v=TEXT?)
	{
		final StringBuffer buffer = new StringBuffer();
		if($type != null)
		{
		    String t = $type.text;
		    if(t.equals("actions")) {
		        buffer.append("\045act:");
		    } else if(t.equals("alternative")) {
		        buffer.append("=?");
		    } else if(t.equals("comments")) {
		        buffer.append("\045");
		    } else if(t.equals("explanation")) {
		        buffer.append("=");
		    } else if(t.equals("paralinguistics")) {
		        buffer.append("=!");
		    } else if(t.equals("standard for dialect")) {
		        buffer.append("\045sdi:");
		    } else if(t.equals("standard for child")) {
		        buffer.append("\045sch:");
		    } else if(t.equals("standard for unclear source")) {
		        buffer.append("\045xxx:");
		    }
		}

		if($v != null)
		{
			buffer.append(" " + $v.text);
		}
		
		// escape parenthesis
		$val = "(" + EscapeUtils.escapeParenthesis(buffer.toString()) + ")";
	}
	;

    

        
overlap returns [String val]
scope {
	String type;
	String index;
}
@init {
	$overlap::type = "";
	$overlap::index = "";
}
@after {
	$val = $overlap::type + $overlap::index;
}
	:	^(OVERLAP_START overlap_attr*)
	;
	
overlap_attr
	:	type=OVERLAP_ATTR_TYPE
	{
		if($type != null) {
			String ovType = $type.text;
			if(ovType.equals("overlap follows")) 
				$overlap::type = ">";
			else if(ovType.equals("overlap precedes"))
				$overlap::type = "<";
		}
	}
	|	index=OVERLAP_ATTR_INDEX
	{
		if($index != null) {
			$overlap::index = $index.text;
		}
	}
	;

    

        

otherspokenevent returns [String val]
scope {
	String who;
	String said;
}
@init {
	$otherspokenevent::who = new String();
	$otherspokenevent::said = new String();
}
    :    ^(OTHERSPOKENEVENT_START otherspokenevent_attr*)
    {
        $val = $otherspokenevent::who + "=" + $otherspokenevent::said;
    }
    ;
    
otherspokenevent_attr
	:	OTHERSPOKENEVENT_ATTR_WHO
	{
		$otherspokenevent::who = $OTHERSPOKENEVENT_ATTR_WHO.text;
	}
	|	OTHERSPOKENEVENT_ATTR_SAID
	{
		$otherspokenevent::said = $OTHERSPOKENEVENT_ATTR_SAID.text;
	}
	;


    

        
k returns [String val]
	:	^(K_START type=K_ATTR_TYPE?)
	{
		if($type != null)
		{
		    String t = $K_ATTR_TYPE.text;
		    
		    if(t.equals("stressing")) {
		        $val = "!";
		    } else if(t.equals("contrastive stressing")) {
		        $val = "!!";
		    } else if(t.equals("best guess")) {
		        $val = "?";
		    } else if(t.equals("retracing")) {
		        $val = "/";
		    } else if(t.equals("retracing with correction")) {
		        $val = "//";
		    } else if(t.equals("retracing reformulation")) {
		        $val = "///";
		    } else if(t.equals("retracing unclear")) {
		        $val = "/?";
		    } else if(t.equals("false start")) {
		        $val = "/-";
		    }
		}
	}
	;

    

        
error returns [String val]
	:	^(ERROR_START et=TEXT?)
	{
		$val = "(error:" + 
			($et != null ? $et.text : "") + ")";
	}
	;

    

        
duration
    :    DURATION_START TEXT DURATION_END
    ->    ^(DURATION_START TEXT)
    ;

    

        
s returns [String val]
	:	^(S_START S_ATTR_TYPE? TEXT?)
	{
		$val = "";
		if($S_ATTR_TYPE != null)
		{
			String type = $S_ATTR_TYPE.text;
			if(type.equals("comma"))
			{
				$val = ",";
			} else if(type.equals("tag question"))
			{
				$val = ",,";
			} else if(type.equals("semicolon"))
			{
				$val = ";";
			} else if(type.equals("colon")) 
			{
				$val = ":";
			} else if(type.equals("clause delimiter"))
			{
				$val = "(clause delimiter:" + $TEXT.text + ")";
			} else if(type.equals("rising to high"))
			{
				$val = "0x21D7";
			} else if(type.equals("rising to mid"))
			{
				$val = "0x2197";
			} else if(type.equals("level"))
			{
				$val = "0x2192";
			} else if(type.equals("falling to mid"))
			{
				$val = "0x2198";
			} else if(type.equals("falling to low"))
			{
				$val = "0x21D8";
			} else if(type.equals("latching"))
			{
				$val = "0x2261";
			}
		}
	}
	;

    

        
tagmarker returns [String val]
@init {
	List<Tuple<String, String>> morvals = new ArrayList<Tuple<String, String>>();
}
	:	^(TAGMARKER_START TAGMARKER_ATTR_TYPE (mor {morvals.add(new Tuple($mor.tierName, $mor.val));})*)
	{
	    String tmType = $TAGMARKER_ATTR_TYPE.text;
	    if(tmType.equals("comma")) {
	        $val = ",";
	    } else if(tmType.equals("tag")) {
	        $val = "\u201E";
	    } else if(tmType.equals("vocative")) {
	        $val = "\u2021";
	    }
	    
	    // add mor to mor tier as necessary
	    for(Tuple<String, String> morData:morvals) {
	    	// mor is handled as a seperate dependent tier
			String morVal = morData.getObj2();
			String tierName = morData.getObj1();
			
			// make sure dep tier exists in session
			TierDescription tierDesc = ensureTierExists(tierName, true);
			
			Tier<String> morTier = $u::r.getTier(tierName, String.class);
			if(morTier == null) {
				morTier = sessionFactory.createTier(tierName, String.class, true);
				for(int i = 0; i < $u::r.numberOfGroups(); i++) morTier.addGroup();
				$u::r.putTier(morTier);
			}
			
			Group group = null;
			// get the correct word group holder
			if($t.size() > 0 || $ugrp.size() > 0) {
			    group = ($t.size() > 0 ? $t::g : $ugrp::g);
			} else if($u::r.numberOfGroups() > 0) {
			    group = $u::r.getGroup($u::r.numberOfGroups()-1);
			}
			
			String tierValue = group.getTier(tierName, String.class);
			if(tierValue == null) tierValue = new String();
			tierValue += 
			    (tierValue.length() == 0 ? "" : " ") + morVal;
			group.setTier(tierName, String.class, tierValue);
	    }
	}
	;

    

        
nonvocal returns [String val]
    :    ^(NONVOCAL_START NONVOCAL_ATTR_TYPE TEXT)
    {
        $val = "(nonvocal," + $NONVOCAL_ATTR_TYPE.text + ":" + $TEXT.text + ")";
    }
    ;

    

        
pho returns [IPATranscript ipa]
scope
{
	StringBuffer buffer;
}
@init
{
	$pho::buffer = new StringBuffer();
}
	:	model
	{
		$pho.ipa = ($pho::buffer.length() > 0 
					?	(new IPATranscriptBuilder()).append($pho::buffer.toString()).toIPATranscript()
					:	new IPATranscript());
		if(!isProcessFragments()) {
			if($g.size() == 0) {
				$ugrp::g.setIPATarget($pho.ipa);
			} else {
				$g::tReps.put($g::pgCount, $pho.ipa);
			}
		}
	}
	|	actual
	{
		$pho.ipa = ($pho::buffer.length() > 0
					?	(new IPATranscriptBuilder()).append($pho::buffer.toString()).toIPATranscript()
					:	new IPATranscript());
		if(!isProcessFragments()) {
			if($g.size() == 0) {
				$ugrp::g.setIPAActual($pho.ipa);
			} else {
				$g::aReps.put($g::pgCount, $pho.ipa);
			}
		}
	}
	;

model
	:	^(MODEL_START  pw*)
	;

actual
	:	^(ACTUAL_START pw*)
	;

pw
scope {
	SyllableConstituentType lastType;
}
@init {
	if($pho::buffer.length() > 0) {
		$pho::buffer.append(" ");
	}
	$pw::lastType = SyllableConstituentType.UNKNOWN;
}
	:	^(PW_START pwele*)
	;

pwele
	:	ss
	{
		$pho::buffer.append($ss.val);
	}
	|	wk
	{
		$pho::buffer.append($wk.val);
	}
	|	ph
	{
		if($ph.val != null) {
			$pho::buffer.append($ph.val);
			if($ph.type != null && $ph.type != SyllableConstituentType.UNKNOWN) {
				
				if($pw::lastType == SyllableConstituentType.NUCLEUS &&
					$ph.type == SyllableConstituentType.NUCLEUS && !$ph.hiatus) {
					
					$pho::buffer.replace($pho::buffer.length() - $ph.val.length() - 1,
						$pho::buffer.length(), "D" + $ph.val);
					$pho::buffer.append(':').append("D");
				} else {
					$pho::buffer.append(':').append($ph.type.getMnemonic());
				}
				$pw::lastType = $ph.type;
			}
		}
	}
	;

    

        

ph returns [String val, SyllableConstituentType type, boolean hiatus]
scope {
	IPAElementFactory factory;
	SyllableConstituentType sctype;
	boolean isHiatus;
}
@init {
	$ph::factory = new IPAElementFactory();
	$ph::sctype = SyllableConstituentType.UNKNOWN;
	$ph::isHiatus = false;
}
	:	^(PH_START phattr* TEXT)
	{
		$val = $TEXT.text;
		$type = $ph::sctype;
		$hiatus = $ph::isHiatus;
	}
	;
	
phattr
	:	PH_ATTR_SCTYPE
	{
		SyllableConstituentType scType =
			SyllableConstituentType.fromString($PH_ATTR_SCTYPE.text);
		if(scType != null) {
			$ph::sctype = scType;
		} else {
			LOGGER.warning("Invalid syllable constituent type '" +
				$PH_ATTR_SCTYPE.text + "'");
		}
	}
	|	PH_ATTR_ID
	|	PH_ATTR_HIATUS
	{
		$ph::isHiatus = Boolean.parseBoolean($PH_ATTR_HIATUS.text);
	}
	;


    

        
ss returns [String val]
	:	^(SS_START type=SS_ATTR_TYPE)
	{
			if($type != null)
			{
				if($type.text.equals("1"))
					$val = SyllableStress.PrimaryStress.getIpa() + "";
				else if($type.text.equals("2"))
					$val = SyllableStress.SecondaryStress.getIpa() + "";
			} else {
				$val = "";
			}
	}
	;

    

        
align returns [PhoneMap val]
scope {
	List<Integer> topAlign;
	List<Integer> btmAlign;
}
@init {
	$align::topAlign = new ArrayList<Integer>();
	$align::btmAlign = new ArrayList<Integer>();
}
	:	^(ALIGN_START alignCol*)
	{
		IPATranscript tRep = 
			(isProcessFragments() ? (IPATranscript)getProperty("model", new IPATranscript()) : $ugrp::g.getIPATarget());
		IPATranscript aRep = 
			(isProcessFragments() ? (IPATranscript)getProperty("actual", new IPATranscript()) : $ugrp::g.getIPAActual());

		if(tRep != null && aRep != null) {
			PhoneMap pm = new PhoneMap(tRep, aRep);
			pm.setTopAlignment($align::topAlign.toArray(new Integer[0]));
			pm.setBottomAlignment($align::btmAlign.toArray(new Integer[0]));
			
			$align.val = pm;

			if(!isProcessFragments())
				$ugrp::g.setPhoneAlignment(pm);
		}
		
	}
	;

alignCol
scope {
	boolean hasModel;
	boolean hasActual;
}
@init {
	$alignCol::hasModel = false;
	$alignCol::hasActual = false;
}
	:	^(COL_START phref+)
	{
		if($alignCol::hasModel) {
			int pIdx = 0;
			for(Integer aIdx:$align::topAlign) {
				if(aIdx >= 0)
					pIdx++;
			}
			$align::topAlign.add(pIdx);
		} else {
			$align::topAlign.add(-1);
		}

		if($alignCol::hasActual) {
			int pIdx = 0;
			for(Integer aIdx:$align::btmAlign) {
				if(aIdx >= 0)
					pIdx++;
			}
			$align::btmAlign.add(pIdx);
		} else {
			$align::btmAlign.add(-1);
		}
	}
	;

phref
	:	modelref
	{
		$alignCol::hasModel = true;
	}
	|	actualref
	{
		$alignCol::hasActual = true;
	}
	;

modelref
	:	^(MODELREF_START TEXT)
	;

actualref
	:	^(ACTUALREF_START TEXT)
	;

    

        
sg
    :    ^(SG_START sgchoice+ sw+)
    ;
    
sgchoice
    :    w
    |    g
    |    quotation
    |    quotation2
    |    pause
    |    internal_media
    |    freecode
    |    e
    |    s
    |    tagmarker
    |    long_feature
    |    nonvocal
    |    overlap_point
    |    underline
    |    italic
    ;
    
sw
    :    ^(SW_START TEXT)
    ;

    

        
r returns [String val]
	:	^(R_START times=R_ATTR_TIMES?)
	{
		if($times != null)
		{
			$val = $times.text;
		}
	}
	;

    

        
t returns [String val]
scope {
 	Group g; 	
}
@init {
 	$t::g = 
 		($u::r.numberOfGroups() > 0 ? $u::r.getGroup($u::r.numberOfGroups()-1) : $u::r.addGroup());
 }
	:	^(T_START T_ATTR_TYPE? mor?)
	{
		// add terminator to last wordgroup
		String t = $T_ATTR_TYPE.text;
		String append = "";
		if(t.equals("p")) {
		   append = ".";
		} else if(t.equals("q")) {
		    append = "?";
		} else if(t.equals("e")) {
		    append = "!";
		} else {
		    // wrap in paren
		    append = "(t:" + t + ")";
		}
		$t::g.setOrthography(
			(new OrthographyBuilder()).append($t::g.getOrthography()).append(append).toOrthography());
		
		if($mor.val != null) {
			// mor is handled as a seperate dependent tier
			String morVal = $mor.val;
			String tierName = $mor.tierName;
			
			// make sure dep tier exists in session
			TierDescription tierDesc = ensureTierExists(tierName, true);
			
			Tier<String> morTier = $u::r.getTier(tierName, String.class);
			if(morTier == null) {
				morTier = sessionFactory.createTier(tierName, String.class, true);
				for(int i = 0; i < $u::r.numberOfGroups(); i++) morTier.addGroup();
				$u::r.putTier(morTier);
			}
			
			// add mor data as a dep tier of the current word(group)
			Group group = null;
			// get the correct word group holder
			if($t.size() > 0 || $ugrp.size() > 0) {
			    group = ($t.size() > 0 ? $t::g : $ugrp::g);
			} else if($u.size() > 0) {
			    group = $u::r.getGroup($u::r.numberOfGroups()-1);
			}
			
			String tierValue = group.getTier(tierDesc.getName(), String.class);
			if(tierValue == null) tierValue = new String();
			tierValue += 
			    (tierValue.length() == 0 ? "" : " ") + morVal;
			group.setTier(tierDesc.getName(), String.class, tierValue);
		}
	}
	;

    

        
postcode
	:	^(POSTCODE_START v=TEXT?)
	{
		// make a new tier in the session if necessary
		TierDescription postcodeDesc = ensureTierExists("Postcode", false);
		Tier<String> postcodeTier = $u::r.getTier("Postcode", String.class);
		if(postcodeTier == null) {
			postcodeTier = sessionFactory.createTier("Postcode", String.class, false);
			$u::r.putTier(postcodeTier);
		}
		
		postcodeTier.setGroup(0,
			(new String(postcodeTier.getGroup(0) + " " + $v.text)).trim());
	}
	;

    

        
media returns [MediaSegment val]
scope {
	MediaSegment m;
	boolean hasSetUnit;
}
@init {
	$media::m = sessionFactory.createMediaSegment();
	$media::hasSetUnit = false;
}
@after {
	// convert unit to miliseconds
	if($media::m.getUnitType() == MediaUnit.Second) {
		$val = sessionFactory.createMediaSegment();
		$val.setUnitType(MediaUnit.Millisecond);
		$val.setStartValue($media::m.getStartValue() * 1000);
		$val.setEndValue($media::m.getEndValue() * 1000);
	} else {
		$val = $media::m;
	}
}
	:	^(MEDIA_START media_attr*)
	;
	
media_attr
	:	MEDIA_ATTR_START
	{
		String startText = $MEDIA_ATTR_START.text;
		Float startVal = Float.parseFloat(startText);
		$media::m.setStartValue(startVal);
	}
	|	MEDIA_ATTR_END
	{
		String endText = $MEDIA_ATTR_END.text;
		Float endVal = Float.parseFloat(endText);
		$media::m.setEndValue(endVal);
	}
	|	MEDIA_ATTR_UNIT
	{
		String unitType = $MEDIA_ATTR_UNIT.text;
		if(unitType.equalsIgnoreCase("s"))
		{
			$media::m.setUnitType(MediaUnit.Second);
		} else if(unitType.equalsIgnoreCase("ms"))
		{
			$media::m.setUnitType(MediaUnit.Millisecond);
		}
	}
	;

    

        
a returns [String type, String flavor, String val]
scope {
	String t;
	String f;
	String buffer;
}
@init {
	$a::t = new String();
	$a::f = new String();
	$a::buffer = new String();
}
@after {
	$type = $a::t;
	$flavor = $a::f;
	$val = $a::buffer;
}
	:	^(A_START a_attr* eles+=aele*)
	{
		String tierVal = $a::buffer.trim();
		// special tiers
		if(	$a::t.equals("addressee") 
			|| $a::t.equals("actions") 
			|| $a::t.equals("alternative")
			|| $a::t.equals("coding") 
			|| $a::t.equals("cohesion")
			|| $a::t.equals("english translation")
			|| $a::t.equals("errcoding")
			|| $a::t.equals("explanation")
			|| $a::t.equals("flow")
			|| $a::t.equals("facial")
			|| $a::t.equals("target gloss")
			|| $a::t.equals("gesture")
			|| $a::t.equals("intonation")
			|| $a::t.equals("language")
			|| $a::t.equals("ort")
			|| $a::t.equals("orthography")
			|| $a::t.equals("paralinguistics")
			|| $a::t.equals("SALT")
			|| $a::t.equals("situation")
			|| $a::t.equals("speech act")
			|| $a::t.equals("time stamp") )
		{
			String tierName = $a::t;
			if(tierName.equals("orthography")) tierName = "ort"; // change tier name for orthpgraphy to avoid collisions
			
			TierDescription tierDesc = ensureTierExists(tierName, false);
			Tier<TierString> userTier = $u::r.getTier(tierName, TierString.class);
			if(userTier == null) {
				userTier = sessionFactory.createTier(tierName, TierString.class, false);
				$u::r.putTier(userTier);
			}
			userTier.setGroup(0, new TierString(tierVal));
		}
		
		// set notes if type is 'comments'
		else if($a::t.equals("comments")) 
		{
			// set notes in utterance
			$u::r.getNotes().setGroup(0, new TierString(tierVal));
		}
		
		// if type is 'extension' create a new dep tier (if necessary)
		// and then add the data to the utterance
		else if($a::t.equals("extension"))
		{
			String tierName = $a::f;
			
			boolean isGrouped =
			 	(tierVal.startsWith("[") && tierVal.endsWith("]"));
			TierDescription tierDesc = ensureTierExists(tierName, isGrouped);
			
			Tier<TierString> depTier = $u::r.getTier(tierName, TierString.class);
			if(depTier == null) {
				depTier = sessionFactory.createTier(tierName, TierString.class, isGrouped);
				$u::r.putTier(depTier);
			}
			
			if(isGrouped) {
				final Pattern pattern = Pattern.compile("\\[(.*?)\\]");
				final Matcher matcher = pattern.matcher(tierVal);
				int numDepGroups = 0;
				while(matcher.find()) {
					final String grpVal = matcher.group(1).trim();
					depTier.addGroup(new TierString(grpVal));
					++numDepGroups;
				}
				
				if(numDepGroups > $u::r.getOrthography().numberOfGroups()) {
					final PhonTalkMessage msg = new PhonTalkMessage(
						"Record #" + (session.getRecordCount()+1) + ". Tier " + tierName + " has more groups than Orthography.",
						PhonTalkMessage.Severity.WARNING);
					msg.setFile(new java.io.File(this.getFile()));
					if(getPhonTalkListener() != null)
						getPhonTalkListener().message(msg);
				}
			} else {
				depTier.setGroup(0, new TierString(tierVal));
			}
		}
		
		// if type is 'phonetic' we have an old-style pho (instead)
		// of npho) try to import the data as an IPA transcript
		else if($a::t.equals("phonetic"))
		{
			String[] splitVals = $a::buffer.split("\\p{Space}");
			
			if(splitVals.length != $u::r.numberOfGroups()) {
				LOGGER.warning("[Record " + (recordIndex+1) + "] Misaligned \%xpho '" + $a::buffer + "'");
			}
			
			Tier<IPATranscript> ipaA = $u::r.getIPAActual();
			int sIdx = 0;
			for(int gIdx = 0; gIdx < $u::r.numberOfGroups() && sIdx < splitVals.length; gIdx++) {
				String v = splitVals[sIdx++].trim();
				
				IPATranscriptBuilder builder = new IPATranscriptBuilder();
				builder.append(v);
				ipaA.setGroup(gIdx, builder.toIPATranscript());
			}
			
			if(sIdx < splitVals.length) {
				IPATranscriptBuilder builder = new IPATranscriptBuilder();
				builder.append(ipaA.getGroup(ipaA.numberOfGroups()-1)).appendWordBoundary().append(splitVals[sIdx]);
				ipaA.setGroup(ipaA.numberOfGroups()-1, builder.toIPATranscript());
			}
		}
	}
	;
	
aele
	:	TEXT
	{
		$a::buffer += $TEXT.text;
	}
	|	media
	{
		MediaSegment m = $media.val;
		if(m != null) {
			String addVal = "(" + 
				MsFormatter.msToDisplayString((long)Math.round(m.getStartValue())) + "-" +
				MsFormatter.msToDisplayString((long)Math.round(m.getEndValue())) + ")";
			$a::buffer += ($a::buffer.length() > 0 ? " " : "") +
				addVal;
		}
	}
	;

a_attr
	:	A_ATTR_TYPE
	{
		$a::t = $A_ATTR_TYPE.text;
	}
	|	A_ATTR_FLAVOR
	{ 
		$a::f = $A_ATTR_FLAVOR.text;
	}
	;

    

        
begin_gem returns [Comment beginGem]
    :   ^(BEGIN_GEM_START BEGIN_GEM_ATTR_LABEL)
    {
        $beginGem = sessionFactory.createComment(
            CommentEnum.BeginGem, $BEGIN_GEM_ATTR_LABEL.text);
    }
    ;        

    

        
end_gem returns [Comment endGem]
    :   ^(END_GEM_START END_GEM_ATTR_LABEL)
    {
        $endGem = sessionFactory.createComment(
            CommentEnum.EndGem, $END_GEM_ATTR_LABEL.text);
    }
    ;        

    

        
lazy_gem returns [Comment lazyGem]
	:	^(LAZY_GEM_START label=LAZY_GEM_ATTR_LABEL?)
	{
		$lazyGem = sessionFactory.createComment();
		$lazyGem.setType(CommentEnum.fromString("LazyGem"));
		
		if($label != null)
		{
			$lazyGem.setValue($label.text);
		}
	}
;

    