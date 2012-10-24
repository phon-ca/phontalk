

        
tree grammar ChatTree;

options
{
	ASTLabelType = CommonTree ;
	tokenVocab = Chat ;
	output = AST;
}

@header {
package ca.phon.phontalk.parser;

import ca.phon.phontalk.*;
import ca.phon.application.*;
import ca.phon.application.transcript.*;
import ca.phon.util.*;
import ca.phon.phone.*;
import ca.phon.alignment.*;
import ca.phon.syllable.*;
import ca.phon.engines.syllabifier.*;
import ca.phon.engines.aligner.*;
import ca.phon.system.logger.PhonLogger;

import java.util.Calendar;
import java.util.TimeZone;
import java.text.ParseException;
import java.util.Map;
import java.util.HashMap;
}

@members {

	/** Default IPhonFactory */
	private IPhonFactory phonFactory = 
		IPhonFactory.getFactory("PB1.2");

	/** Session */
	private ITranscript session = 
		phonFactory.createTranscript();
		
	/** The converter 
	       This should be set BEFORE any walker rules are called */
	public XmlConverter converter = null;
	
	public void setConverter(XmlConverter c) { this.converter = c; };

	public ITranscript getSession() { return session; }

	private Syllabifier getSyllabifier() {
		Syllabifier syllabifier = null;
		if(converter != null) {
			Object syllabifierObj =
				converter.getProperty(XmlConverter.CONVERTER_PROP_SYLLABIFIER);
			if(syllabifierObj != null 
			&& syllabifierObj instanceof Syllabifier) {
				syllabifier = (Syllabifier)syllabifierObj;
			}
		}
		return syllabifier;
	}
	
	private boolean isReparsePhones() {
		boolean retVal = false;
		
		if(converter != null) {
			Object reparsePropVal = 
				converter.getProperty(XmlConverter.CONVERTER_PROP_REPARSE_PHONES);
			if(reparsePropVal != null && reparsePropVal instanceof Boolean) {
				retVal = (Boolean)reparsePropVal;
			}
		}
		return retVal;
	}
	
/**
 * Print out a warning that we are not supporting the
 * currently active element.
 */
public void unsupportedWarning() {

    

}
	
	
	private int uttIndex = 0;
}

    

        
chat
	:	^(CHAT_START chat_attrs* participants? chat_content*)
	;
	
chat_content
	:	comment
	|	u
	|	lazy_gem
	;
	
chat_attrs
	:	CHAT_ATTR_MEDIA
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
	|	CHAT_ATTR_VERSION
	|	CHAT_ATTR_LANG
	{
		session.setLanguage($CHAT_ATTR_LANG.text);
	}
	|	CHAT_ATTR_CORPUS
	{
		//session.setCorpus($CHAT_ATTR_CORPUS.text);
	}
	|	CHAT_ATTR_ID
	{
		session.setID($CHAT_ATTR_ID.text);
	}
	|	CHAT_ATTR_DATE
	{
		PhonDateFormat pdf = new PhonDateFormat(PhonDateFormat.YEAR_LONG);
		
		try {
		Calendar c = (Calendar)pdf.parseObject($CHAT_ATTR_DATE.text);
		c.setTimeZone(TimeZone.getTimeZone("GMT-0"));
		session.setDate(c);
		} catch (ParseException e) {}
	}
	;

    

    	
participants 
	:	^(PARTICIPANTS_START participant*)
	;
	
participant
scope {
	IParticipant p;
	boolean bdaySet ;
}
@init {
	$participant::p = session.newParticipant();
	$participant::bdaySet = false;
}
@after {
	if(!$participant::bdaySet && session.getDate() != null) {
		$participant::p.setBirthDate(session.getDate());
	}
}
	:	^(PARTICIPANT_START part_attr*)
	;
	
part_attr
	:	PARTICIPANT_ATTR_ID
	{	$participant::p.setId($PARTICIPANT_ATTR_ID.text);	}
	|	PARTICIPANT_ATTR_NAME
	{	$participant::p.setName($PARTICIPANT_ATTR_NAME.text);	}
	|	PARTICIPANT_ATTR_ROLE
	{	$participant::p.setRole($PARTICIPANT_ATTR_ROLE.text);	}
	|	PARTICIPANT_ATTR_LANGUAGE
	{	$participant::p.setLanguage($PARTICIPANT_ATTR_LANGUAGE.text);	}
	|	PARTICIPANT_ATTR_AGE
	{
		// only set birthday using age if we don't have a birthday set already
		// from age we need to calculate birthday, we can only
		// do that if the session date is set
		if(!$participant::bdaySet) {
			if(session.getDate() != null) {
				Calendar date = session.getDate();
				PhonDurationFormat pdf = new PhonDurationFormat(PhonDurationFormat.XML_FORMAT);
				
				try {
				PhonDuration age = (PhonDuration)pdf.parseObject($PARTICIPANT_ATTR_AGE.text);
				Calendar bDay = PhonDuration.getBeforeDate(date, age);
				bDay.setTimeZone(TimeZone.getTimeZone("GMT-0"));
				$participant::p.setBirthDate(bDay);
				
				$participant::bdaySet = true;
				} catch (ParseException e) {}
			}
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
		PhonLogger.warning("Attribute 'first-language' not supported.");
	}
	|	PARTICIPANT_ATTR_BIRTHPLACE
	{
		PhonLogger.warning("Attribute 'birthplace' not supported.");
	}
	|	PARTICIPANT_ATTR_BIRTHDAY
	{
		PhonDateFormat pdf = new PhonDateFormat(PhonDateFormat.YEAR_LONG);
		try {
			Calendar cal = (Calendar)pdf.parseObject($PARTICIPANT_ATTR_BIRTHDAY.text);
			cal.setTimeZone(TimeZone.getTimeZone("GMT-0"));
			$participant::p.setBirthDate(cal);
			
			$participant::bdaySet = true;
		} catch (ParseException pe) {
			PhonLogger.warning(pe.toString());
		}
	}
	;

    

        
comment
	:	^(COMMENT_START type=COMMENT_ATTR_TYPE? val=TEXT?)
	{
		// if the comment is a date,
		// set the date in the session (overrides CHAT attribute)
		if($type != null && $type.text.equals("Date")) {
			// do nothing
		} else {
			IComment c = session.newComment();
			// create a new comment in the transcript
			if($val != null)
			{
				c.setValue($val.text);
			}

			if($type != null)
			{
				CommentEnum cType =
					CommentEnum.fromString($type.text);
				if(cType == null)
					cType = CommentEnum.Generic;
				c.setType(cType);
			}
		}
	}
	;

    

        
u
scope {
	IUtterance utt;
	
	// when adding annotations, sometimes we need to create
	// the first word-group.  This flag tells the next ugrp
	// element to use the last group instead of making a new one
	boolean useLastGroup;
}
@init {
	$u::utt = session.newUtterance();
	$u::useLastGroup = false;
}
@after {

	if($u::utt.getMedia() == null)
	{
		IMedia m = IPhonFactory.getDefaultFactory().createMedia();
		m.setStartValue(0L);
		m.setEndValue(0L);
		m.setUnitType(MediaUnit.Millisecond);
		$u::utt.setMedia(m);
	}

	uttIndex++;
}
	:	^(U_START who=U_ATTR_WHO? uele* t? postcode? seg=media? a*)
	{
		// setup speaker
		if($who != null) 
		{
			// try to find the speaker
			String partId = $who.text;
			
			// by id first
			for(IParticipant p:session.getParticipants()) {
				if(p.getId().equals(partId)) {
					$u::utt.setSpeaker(p);
					break;
				}
			}
			
		}
		
		// set media if avail
		if($seg.val != null)
		{
			$u::utt.setMedia($seg.val);
		}
	}
	;

uele
	:	ugrp
	|	uannotation
	;

/**
 * Each ugrp object creates a new group in Phon.
 */
 ugrp
 scope {
 	IWord w;
 	
 }
 @init {
 	if($u::useLastGroup) {
 		List<IWord> grps = $u::utt.getWords();
 		$ugrp::w = grps.get(grps.size()-1);
 		
 		// reset flag
 		$u::useLastGroup = false;
 	} else {
 		$ugrp::w = $u::utt.newWordGroup();
 	}
 }
	:	w 
	{	
		$ugrp::w.setWord(
			($ugrp::w.getWord().length() > 0 ? $ugrp::w.getWord() + " " : "") +
			$w.val);
	}
	| 	pg 
	{	
		$ugrp::w.setWord(
			($ugrp::w.getWord().length() > 0 ? $ugrp::w.getWord() + " " : "") +
			$pg.val);
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
	                if(StringUtils.strip(currentGrp).length() > 0) {
	                    grps.add(StringUtils.strip(currentGrp));
	                }
	                currentGrp = "";
	            }
	            currentGrp += c;
	            if(c == ']') {
	                if(StringUtils.strip(currentGrp).length() > 0) {
	                    grps.add(StringUtils.strip(currentGrp));
	                }
	                currentGrp = "";
	            }
					
			}
			if(StringUtils.strip(currentGrp).length() > 0) {
				grps.add(StringUtils.strip(currentGrp));
			}
			// place only an openeing '{' in our first group
			// this tells the tree builder   to create this group surrounding the 
			// next <pg> groups
			$ugrp::w.setWord("{");
			
			// if we have sub-pg groups.  The new words should have been created.
			int phoRepIndex = 0;
			for(String g:grps) {
				IWord nextWord = $u::utt.newWord();
				
				if(g.startsWith("[")) {
					nextWord.setWord(g.substring(1, g.length()-1));
					
					// add phonetic reps generated by pho rule
					
						IPhoneticRep tRep = $g.targetReps.get(phoRepIndex);
						if(tRep != null) {
							IPhoneticRep rtRep = nextWord.getPhoneticRepresentation(Form.Target);
							rtRep.setPhones(tRep.getPhones());
						}
						
						IPhoneticRep aRep = $g.actReps.get(phoRepIndex);
						if(aRep != null) {
							IPhoneticRep raRep = nextWord.getPhoneticRepresentation(Form.Actual);
							raRep.setPhones(aRep.getPhones());
						}
						
						PhoneMap pm = Aligner.getPhoneAlignment(nextWord);
                    	if(pm != null) nextWord.setPhoneAlignment(pm);
						
						phoRepIndex++;
					
				} else {
					nextWord.setWord(g);
				}
				
			}
			
			// finish 'global' group
			IWord endGrpWord = $u::utt.newWord();
			endGrpWord.setWord("}");
		} else {
			$ugrp::w.setWord(
				($ugrp::w.getWord().length() > 0 ? $ugrp::w.getWord() + " " : "") +
				"{" +  $g.val + "}" );
		}
	}
	;
	
uannotation
	:	s
	{
		// add separator to last group
		List<IWord> words = $u::utt.getWords();
		if(words.size() == 0) {
			words.add($u::utt.newWordGroup());
			$u::useLastGroup = true;
		}
		IWord lastWord = 
			words.get(words.size()-1);
		lastWord.setWord(lastWord.getWord() + " " + $s.val);
	}
	|	pause
	{
		// add pause to last group as a comment
		List<IWord> words = $u::utt.getWords();
		if(words.size() == 0) {
			words.add($u::utt.newWordGroup());
			$u::useLastGroup = true;
		}
		IWord lastWord = 
			words.get(words.size()-1);
		lastWord.setWord(lastWord.getWord() + " " + $pause.val);
	}
	|	e
	{
		// add event to last group
		// add pause to last group as a comment
		List<IWord> words = $u::utt.getWords();
		if(words.size() == 0) {
			words.add($u::utt.newWordGroup());
			$u::useLastGroup = true;
		}
		IWord lastWord = 
			words.get(words.size()-1);
		lastWord.setWord(lastWord.getWord() + " " + $e.val);
	}
	|	linker
	{
		List<IWord> words = $u::utt.getWords();
		if(words.size() == 0) {
			words.add($u::utt.newWordGroup());
			$u::useLastGroup = true;
		}
		IWord lastWord = 
			words.get(words.size()-1);
		lastWord.setWord(lastWord.getWord() + " " + $linker.val);
	}
	;

    

        
w returns [String val]
scope {
	String buffer;
	String suffix;
	String prefix;
}
@init {
	$w::buffer = new String();
	$w::suffix = "";
	$w::prefix = "";
}
@after {
	$val = $w::prefix + $w::buffer.toString() + $w::suffix;
}
	:	^(W_START wattr* wele*)
	;
	
wele
	:	TEXT
	{	
		$w::buffer += ($w::buffer.length() > 0 ? "" : "") + $TEXT.text;	
	}
	| 	wk
	{
		$w::buffer += ($w::buffer.length() > 0 ? "" : "") + $wk.val;
	}
	|	p 
	{
		$w::buffer += ($w::buffer.length() > 0 ? "" : "") + $p.val;
	}
	|	shortening
	{
		$w::buffer += ($w::buffer.length() > 0 ? "" : "") + $shortening.val;
	}
	|	f
	{
		$w::buffer += ($w::buffer.length() > 0 ? " " : "") + $f.val;
	}
	|	replacement
	{
		$w::buffer += ($w::buffer.length() > 0 ? " " : "") + $replacement.val;
	}
	|	underline
	{
		$w::buffer += ($w::buffer.length() > 0 ? " " : "") + $underline.val;
	}
	|	langs
	{
		$w::buffer += ($w::buffer.length() > 0 ? " " : "" ) + $langs.val;
	}
	|	mor
	{
		// make sure dep tier exists in session
			IDepTierDesc tierDesc = null;
			for(IDepTierDesc current:session.getDependentTiers())
			{
				if(current.isGrouped() && current.getTierName().equals("Morphology"))
				{
					tierDesc = current;
					break;
				}
			}
			
			if(tierDesc == null) {
				// create the new tier
				tierDesc = session.newDependentTier();
				tierDesc.setTierName("Morphology");
				tierDesc.setIsGrouped(true);
			}
		
		// add more data as a dep tier of the current word(group)
		IDependentTier depTier = $ugrp::w.getDependentTier(tierDesc.getTierName());
		if(depTier == null) {
			depTier = $ugrp::w.newDependentTier();
			depTier.setTierName(tierDesc.getTierName());
		}
		depTier.setTierValue($mor.val);
		
	}
	;
	
wattr
	:	W_ATTR_FORMTYPE
	{
		$w::suffix = "@";
		
		String t = $W_ATTR_FORMTYPE.text;
		if(t.equals("babbling")) {
			$w::suffix += "b";
		} else if(t.equals("child-invented")) {
			$w::suffix += "c";
		} else if(t.equals("dialect")) {
			$w::suffix += "d";
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
		} else if(t.equals("proto-morpheme")) {
			$w::suffix += "pm";
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
		if(t.equals("unintelligible-word")) {
			$w::prefix = "xx";
		} else if(t.equals("unintelligible")) {
			$w::prefix = "xxx";
		} else if(t.equals("unintelligible-word-with-pho")) {
			$w::prefix = "yy";
		} else if(t.equals("unintelligible-with-pho")) {
			$w::prefix = "yyy";
		} else if(t.equals("untranscribed")) {
			$w::prefix = "www";
		} else if(t.equals("action")) {
			$w::prefix = "0";
		} else if(t.equals("omission")) { 
			$w::prefix = "0";
		} else if(t.equals("ellipsis")) {
			$w::prefix = "00";
		} else if(t.equals("fragment")) {
			$w::prefix = "&";
		}
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

    

        
shortening returns [String val]
	:	^(SHORTENING_START v=TEXT?)
	{
		if($v != null) {
			$val = "<" + $v.text + ">";
		}
	}
	;

    

        
f returns [String val]
	:	^(F_START type=F_ATTR_TYPE?)
	{
		if($type != null) 
		{
			$val = "(form:" + $type.text + ")";
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
	

    

        
mor returns [String val]
scope
{
    String morchoicevals;
}
@init
{
    $mor::morchoicevals = new String();
}
    :    ^(MOR_START morattr* morchoice* gra? morseq*)
    {
        // for now, ignoring everything but choice values
        $val = $mor::morchoicevals;
    }
    ;
    
morattr
    :    MOR_ATTR_TYPE
    |    MOR_ATTR_OMITTED
    ;
    
morchoice
    :    mw
    {
        $mor::morchoicevals += ($mor::morchoicevals.length() > 0 ? " " : "") + $mw.val;
    }
    |    mwc
    |    mt
    ;
    
morseq
    :    morpre
    |    morpost
    |    menx
    ;
    
morpre
    :    ^(MORPRE_START morattr* morchoice* gra? morseq*)
    {
        unsupportedWarning();
    }
    ;
    
morpost
    :    ^(MORPOST_START morattr* morchoice* gra? morseq*)
    {
        unsupportedWarning();
    }
    ;
    
menx
    :    MENX_START TEXT MENX_END
    {
        unsupportedWarning();
    }
    ;
    
gra
    :    ^(GRA_START GRA_ATTR_TYPE GRA_ATTR_INDEX GRA_ATTR_HEAD GRA_ATTR_RELATION)
    {
        unsupportedWarning();
    }
    ;
    
mw returns [String val]
scope
{
    String mkvals;
}
@init
{
    $mw::mkvals = new String();
}
    :    ^(MW_START mpfx* pos mwchoice mk*)
    {
        $val = $pos.val + "|" + $mwchoice.val;
        
        if($mw::mkvals.length() > 0) {
            $val += "-" + $mw::mkvals;
        }
    }
    ;
    
mwc
    :    ^(MWC_START mpfx* pos mw+)
    {
        unsupportedWarning();
    }
    ;
    
mt
    :    ^(MT_START MT_ATTR_TYPE)
    {
        unsupportedWarning();
    }
    ;
    
mpfx
    :    ^(MPFX_START TEXT)
    {
        unsupportedWarning();
    }
    ;
   
pos returns [String val]
scope
{
    String svals;
}
@init
{
    $pos::svals = new String();
}
    :    ^(POS_START morposc morposs*)
    {
        $val = $morposc.val;
        if($pos::svals.length() > 0) 
        {
            $val += $pos::svals;
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
    :    ^(S_START TEXT)
    {
        $val = $TEXT.text;
        
        $pos::svals += ($pos::svals.length() > 0 ? " " : "") + ":" + $val;
    }
    ;
    
mwchoice returns [String val]
    :    stem
    {
        $val = $stem.val;
    }
    |    mortagmarker
    ;
    
stem returns [String val]
    :    ^(STEM_START TEXT)
    {
        $val = $TEXT.text;
    }
    ;
    
mortagmarker
    :    ^(MORTAGMARKER_START MORTAGMARKER_ATTR_TYPE)
    ;
    
mk
    :    ^(MK_START TEXT)
    {
        $mw::mkvals += ($mw::mkvals.length() > 0 ? "-" : "") + $TEXT.text;
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
	
pgele returns [String val]
@init {
	$val = new String();
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
		$val = "{" + $g.val + "}";
	}
	|	pause
	{
		$val = $pause.val;
	}
	|  	e
	{
		$val = $e.val;
	}
	|	underline
	{
		$val = $underline.val;
	}
	|	overlap
	{
		$val = "(" + $overlap.val + ")";
	}
	|	r 
	{
		$val = "(x" + $r.val + ")";
	}
	|	k
	{
		$val = "(" +  $k.val + ")";
	}
	|	ga
	{
		$val = $ga.val;
	}
	|	s
	{
		$val = $s.val;
	}
	|	error
	{
		$val = $error.val;
	}
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

    

        
pho
scope
{
	List<Phone> phones;
}
@init
{
	$pho::phones = new ArrayList<Phone>();
}
	:	model
	{
		List<Phone> phones = $pho::phones;
		
		// fix phone list if requested or required
		String txt = "";
		for(Phone p:$pho::phones) txt += p.getPhoneString();

		List<Phone> testPhones = Phone.toPhoneList(txt);
		if(testPhones.size() != phones.size() 
			|| isReparsePhones()) {
			phones = testPhones;
		}
		
		Syllabifier syllabifier = getSyllabifier();
		if(syllabifier != null) syllabifier.syllabify(phones);

		if($g.size() == 0) {
			IPhoneticRep modelRep =
				$ugrp::w.getPhoneticRepresentation(Form.Target);
			if(modelRep == null) {
				modelRep = $ugrp::w.newPhoneticRepresentation();
				modelRep.setForm(Form.Target);
			}
			modelRep.setPhones(phones);
		} else {
			// we are inside a global-g element, add to it's list
			IPhoneticRep tRep = IPhonFactory.getDefaultFactory().createPhoneticRep();
			tRep.setForm(Form.Target);
			tRep.setPhones(phones);

			$g::tReps.put($g::pgCount-1, tRep);
		}
	}
	|	actual
	{
		List<Phone> phones = $pho::phones;
		
		// fix phone list
		String txt = "";
		for(Phone p:$pho::phones) txt += p.getPhoneString();

		List<Phone> testPhones = Phone.toPhoneList(txt);
		
		if(testPhones.size() != phones.size()
			|| isReparsePhones()) {
			phones = testPhones;
		}
		
		Syllabifier syllabifier = getSyllabifier();
		if(syllabifier != null) syllabifier.syllabify(phones);

		if($g.size() == 0) {
			IPhoneticRep actualRep =
				$ugrp::w.getPhoneticRepresentation(Form.Actual);
			if(actualRep == null) {
				actualRep = $ugrp::w.newPhoneticRepresentation();
				actualRep.setForm(Form.Actual);
			}
			actualRep.setPhones(phones);
		} else {
			IPhoneticRep aRep = IPhonFactory.getDefaultFactory().createPhoneticRep();
			aRep.setForm(Form.Actual);
			aRep.setPhones(phones);

			$g::aReps.put($g::pgCount-1, aRep);
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
@init {
	if($pho::phones.size() > 0) {
		Phone wbPhone = new Phone(" ");
		wbPhone.setScType(SyllableConstituentType.WordBoundaryMarker);
		$pho::phones.add(wbPhone);
	}
}
	:	^(PW_START pwele*)
	;

pwele
	:	ss
	{
		Phone ssPhone = new Phone($ss.val);
		ssPhone.setScType(SyllableConstituentType.SyllableStressMarker);
		$pho::phones.add(ssPhone);
	}
	|	wk
	{
		Phone wkPhone = new Phone($wk.val);
		wkPhone.setScType(SyllableConstituentType.SyllableBoundaryMarker);
		$pho::phones.add(wkPhone);
	}
	|	ph
	{
		$pho::phones.add($ph.val);
	}
	;

    

        

ph returns [Phone val]
scope {
	SyllableConstituentType sctype;
	boolean isHiatus;
}
@init {
	$ph::sctype = SyllableConstituentType.Unknown;
	$ph::isHiatus = false;
}
	:	^(PH_START phattr* TEXT)
	{
		$val = new Phone($TEXT.text);
		$val.setScType($ph::sctype);
		$val.setDiphthongMember(!$ph::isHiatus);
	}
	;
	
phattr
	:	PH_ATTR_SCTYPE
	{
		SyllableConstituentType scType =
			SyllableConstituentType.getTypeForIdentifier($PH_ATTR_SCTYPE.text);
		if(scType != null) {
			$ph::sctype = scType;
		} else {
			PhonLogger.warning("Invalid syllable constituent type '" +
				$PH_ATTR_SCTYPE.text + "'");
		}
	}
	|	PH_ATTR_ID
	;


    

        
ss returns [String val]
	:	^(SS_START type=SS_ATTR_TYPE)
	{
			if($type != null)
			{
				if($type.text.equals("1"))
					$val = Syllable.PrimaryStressChar + "";
				else if($type.text.equals("2"))
					$val = Syllable.SecondaryStressChar + "";
			} else {
				$val = "";
			}
	}
	;

    

        
align
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
		IPhoneticRep tRep =
			$ugrp::w.getPhoneticRepresentation(Form.Target);
		IPhoneticRep aRep =
			$ugrp::w.getPhoneticRepresentation(Form.Actual);

		if(tRep != null && aRep != null) {
			PhoneMap pm = new PhoneMap(tRep, aRep);
			pm.setTopAlignment($align::topAlign.toArray(new Integer[0]));
			pm.setBottomAlignment($align::btmAlign.toArray(new Integer[0]));

			$ugrp::w.setPhoneAlignment(pm);
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

    

        
ga returns [String val]
@init{
	$val = "";
}
	:	^(GA_START type=GA_ATTR_TYPE? v=TEXT?)
	{
		$val = "(";
		if($type != null)
		{
		    String t = $type.text;
		    if(t.equals("actions")) {
		        $val += "\045act:";
		    } else if(t.equals("alternative")) {
		        $val += "=?";
		    } else if(t.equals("comments")) {
		        $val += "\045";
		    } else if(t.equals("explanation")) {
		        $val += "=";
		    } else if(t.equals("paralinguistics")) {
		        $val += "!=";
		    } else if(t.equals("standard for dialect")) {
		        $val += "\045sdi:";
		    } else if(t.equals("standard for child")) {
		        $val += "\045sch:";
		    } else if(t.equals("standard for unclear source")) {
		        $val += "\045xxx:";
		    }
		}

		if($v != null)
		{
			$val += " " + $v.text;
		}
		$val += ")";
	}
	;

    

        
overlap returns [String val]
@init {
	$val = "";
}
	:	^(OVERLAP_START type=OVERLAP_ATTR_TYPE?)
	{
		if($type != null) 
		{
			String ovType = $type.text;
			if(ovType.equals("overlap follows")) 
				$val = ">";
			else if(ovType.equals("overlap precedes"))
				$val = "<";
		}
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
	:	^(E_START evtele*)
	;

evtele
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
		$e::buffer += ($e::buffer.length() > 0 ? " " : "") +
				"(happening:";
		if($happening.val != null)
		{
			$e::buffer += $happening.val;
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
	|	overlap
	{
		$e::buffer += ($e::buffer.length() > 0 ? " " : "") +
				"(";
		if($overlap.val != null) {
			 $e::buffer += $overlap.val;	
		}
		$e::buffer += ")";
	}
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

    

        
error returns [String val]
	:	^(ERROR_START et=TEXT?)
	{
		$val = "(error:" + 
			($et != null ? $et.text : "") + ")";
	}
	;

    

        
g returns [String val,  Integer phoRepCount, Map<Integer,IPhoneticRep> targetReps, Map<Integer, IPhoneticRep> actReps, Map<Integer, PhoneMap> phoneMaps]
scope {
	String buffer;
	
	// when g has <pg> children store the
 	// phonetic rep objects generated in the
 	// pho rule
 	Map<Integer, IPhoneticRep> tReps;
 	Map<Integer, IPhoneticRep> aReps;
	Map<Integer, PhoneMap> pMaps;
 	
 	int pgCount;
}
@init {
	$g::buffer = new String();
	
	$g::tReps = new HashMap<Integer, IPhoneticRep>();
	$g::aReps = new HashMap<Integer, IPhoneticRep>();
	$g::pMaps = new HashMap<Integer, PhoneMap>();
	
	$g::pgCount = 0;
}
@after {
	$val = $g::buffer;
	
	$targetReps = $g::tReps;
	$actReps = $g::aReps;
	$phoneMaps = $g::pMaps;
	
	$phoRepCount = $g::pgCount;
}
	:	^(G_START gele*)
	;
	
gele
	:	w 
	{	
		$g::buffer += ($g::buffer.length() > 0 ? " " : "") + $w.val;	
	}
	|	pg
	{
	 	// enclose the pg data in [] so that we know to break up
	 	// this into proper word groups in phon later.
		$g::buffer += ($g::buffer.length() > 0 ? " " : "") + "[" + $pg.val + "]";
	}
	|	pause
	{
		$g::buffer += ($g::buffer.length() > 0 ? " " : "") + $pause.val;
	}
	| 	r 
	{	
		$g::buffer += ($g::buffer.length() > 0 ? " " : "") + 
			"(x" + $r.val + ")";	
	}
	| 	k
	{
		$g::buffer += ($g::buffer.length() > 0 ? " " : "") + 
			"(" + $k.val + ")";
	}
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
	|	e
	{
		$g::buffer += ($g::buffer.length() > 0 ? " " : "") +
			$e.val;
	}
	|	nestedg=g
	{
		$g::buffer += ($g::buffer.length() > 0 ? " " : "") +
			"{]" + $nestedg.val + "}]"; 	
		
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
			$g::pMaps.put(pmapKey, $nestedg.phoneMaps.get(pmapKey));
		}

		$g::pgCount += $nestedg.phoRepCount;
	}
	|	s
	{
		$g::buffer += ($g::buffer.length() > 0 ? " " : "") +
			$s.val;
	}
	|	underline
	{
		$g::buffer += ($g::buffer.length() > 0 ? " " : "") +
			$underline.val;
	}
	|	error
	{
		$g::buffer += ($g::buffer.length() > 0 ? " " : "") +
			$error.val;
	}
	;

    

        
t returns [String val]
	:	^(T_START T_ATTR_TYPE? mor?)
	{
		// add terminator to last wordgroup
		String t = $T_ATTR_TYPE.text;
		List<IWord> words = $u::utt.getWords();
		IWordGroup lastGrp = (IWordGroup)words.get(words.size()-1);
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
		
		List<String> cws = lastGrp.getWords();
		cws.add(append);
		lastGrp.setWords(cws);
	}
	;

    

        
media returns [IMedia val]
scope {
	IMedia m;
	boolean hasSetUnit;
}
@init {
	$media::m = phonFactory.createMedia();
	$media::hasSetUnit = false;
}
@after {
	// convert unit to miliseconds
	if($media::m.getUnitType() == MediaUnit.Second) {
		$val = phonFactory.createMedia();
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
	
		// change type="extension" flavor="pho" to type="phonetic"
		//if($a::t.equals("extension") && $a::f.equals("pho")) {
		//	$a::t = "phonetic";
		//}
		
		// special tiers
		if(	$a::t.equals("addressee") 
			|| $a::t.equals("actions") 
			|| $a::t.equals("situation")
			|| $a::t.equals("intonation")
			|| $a::t.equals("explanation")
			|| $a::t.equals("alternative")
			|| $a::t.equals("coding") 
			|| $a::t.equals("cohesion")
			|| $a::t.equals("english translation")
			|| $a::t.equals("errcoding")
			|| $a::t.equals("flow")
			|| $a::t.equals("facial")
			|| $a::t.equals("target gloss")
			|| $a::t.equals("gesture")
			|| $a::t.equals("language")
			|| $a::t.equals("paralinguistics")
			|| $a::t.equals("SALT")
			|| $a::t.equals("speech act")
			|| $a::t.equals("time stamp") )
		{
			IDepTierDesc tierDesc = null;
			for(IDepTierDesc current:session.getDependentTiers())
			{
				if(!current.isGrouped() && current.getTierName().equals($a::t))
				{
					tierDesc = current;
					break;
				}
			}
			
			if(tierDesc == null) {
				// create the new tier
				tierDesc = session.newDependentTier();
				tierDesc.setTierName($a::t);
				tierDesc.setIsGrouped(false);
			}
			
			IDependentTier depTier = $u::utt.newDependentTier();
			depTier.setTierName($a::t);
			depTier.setTierValue($a::buffer);
		}
		
		// set notes if type is 'comments'
		else if($a::t.equals("comments")) 
		{
			// set notes in utterance
			$u::utt.setNotes($a::buffer);
		}
		
		// if type is 'extension' create a new dep tier (if necessary)
		// and then add the data to the utterance
		else if($a::t.equals("extension"))
		{
			String tierName = $a::f;
			String tierVal = StringUtils.strip($a::buffer);
			
			List<IDepTierDesc> allTiers = new ArrayList<IDepTierDesc>();
			allTiers.addAll(session.getDependentTiers());
			allTiers.addAll(session.getWordAlignedTiers());
			IDepTierDesc tierDesc = null;
			for(IDepTierDesc current:allTiers)
			{
				if(current.getTierName().equals(tierName))
				{
					tierDesc = current;
					break;
				}
			}
			
			if(tierDesc == null) {
				// create the new tier
				tierDesc = session.newDependentTier();
				tierDesc.setTierName(tierName);
				
				boolean isGrouped =
				 	(tierVal.startsWith("[") && tierVal.endsWith("]"));
				tierDesc.setIsGrouped(isGrouped);
			}
			
			//IDependentTier tier = $u::utt.newDependentTier();
			//tier.setTierName(tierName);
			//tier.setTierValue($a::buffer);
			try {
				$u::utt.setTierString(tierName, tierVal);
			} catch (ca.phon.exceptions.ParserException e) {
				e.printStackTrace();
			}
		}
		
		// if type is 'phonetic' we have an old-style pho (instead)
		// of npho) try to import the data as an IPA transcript
		else if($a::t.equals("phonetic"))
		{
			String[] splitVals = $a::buffer.split("\\p{Space}");
			List<IWord> words = $u::utt.getWords();
			Syllabifier syllabifier = getSyllabifier();
			
			if(splitVals.length != words.size()) {
				PhonLogger.warning("[Record " + (uttIndex+1) + "] Misaligned \%xpho '" + $a::buffer + "'");
			}
			
			int sIdx = 0;
			for(int wIdx = 0; wIdx < words.size() && sIdx < splitVals.length; wIdx++) {
				IWord word = words.get(wIdx);
				String v = splitVals[sIdx++];
				
				IPhoneticRep phoRep =
					word.getPhoneticRepresentation(Form.Actual);
				List<Phone> phones = Phone.toPhoneList(v);
				if(syllabifier != null)
					syllabifier.syllabify(phones);
				phoRep.setPhones(phones);
				phoRep.setForm(Form.Actual);
			}
			
			if(sIdx < splitVals.length) {
				// add remainder to last word
				IWord word = words.get(words.size()-1);
				
				String remainder = "";
				do {
					remainder += (remainder.length() == 0 ? "" : " ") + splitVals[sIdx++];
				} while(sIdx < splitVals.length);
				
				IPhoneticRep phoRep =
					word.getPhoneticRepresentation(Form.Actual);
					
				List<Phone> phones = phoRep.getPhones();
				List<Phone> newphones = Phone.toPhoneList(
					(phones.size() == 0 ? "" : " ") + remainder);
				phones.addAll(newphones);
				if(syllabifier != null)
					syllabifier.syllabify(phones);
				phoRep.setPhones(phones);
				phoRep.setForm(Form.Actual);
			}
		}
	}
	;
	
aele
	:	TEXT
	{
		$a::buffer += ($a::buffer.length() > 0 ? " " : "") +
			$TEXT.text;
	}
	|	media
	{
		IMedia m = $media.val;
		if(m != null) {
			String addVal = "(" + 
				StringUtils.msToDisplayString((long)Math.round(m.getStartValue())) + "-" +
				StringUtils.msToDisplayString((long)Math.round(m.getEndValue())) + ")";
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

    

        
postcode
	:	^(POSTCODE_START v=TEXT?)
	{
		// make a new tier in the session if necessary
		IDepTierDesc postcodeDesc = null;
		for(IDepTierDesc tierDesc:session.getDependentTiers())
		{
			if(tierDesc.getTierName().equals("Postcode")) {
				postcodeDesc = tierDesc;
				break;
			}
		}
		if(postcodeDesc == null) {
			postcodeDesc = session.newDependentTier();
			postcodeDesc.setTierName("Postcode");
			postcodeDesc.setIsGrouped(false);
		}
		
		// get the tier in the current record
		IDependentTier depTier = 
			$u::utt.getDependentTier(postcodeDesc.getTierName());
		if(depTier == null) {
			depTier = $u::utt.newDependentTier();
			depTier.setTierValue("");
			depTier.setTierName(postcodeDesc.getTierName());
		}
		depTier.setTierValue(
			StringUtils.strip(depTier.getTierValue() + " " + $v.text));
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
		else if(lkType.equals("TCU completion"))
			$val += "+\u224b";
		else if(lkType.equals("no break completion"))
			$val += "+\u2248";
	}
	;	

    

        
lazy_gem
	:	^(LAZY_GEM_START label=LAZY_GEM_ATTR_LABEL?)
	{
		IComment gem = session.newComment();
		gem.setType(CommentEnum.fromString("LazyGem"));
		
		// create a new 'lazy-gem' comment in Phon
		if($label != null)
		{
			gem.setValue($label.text);
		}
	}
;

    