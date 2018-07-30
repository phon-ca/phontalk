

        
grammar TalkBank2AST;

options {
	output=AST;
	ASTLabelType=CommonTree;
	superClass=PhonTalkParser;
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

	public void reportError(RecognitionException e) {
		throw new TreeWalkerError(e);
	}
}

    

        
chat
	:	CHAT_START chat_attrs+ metadata? participants chat_content* CHAT_END 
	->	^(CHAT_START chat_attrs+ metadata? participants chat_content*)
	;
	
chat_content
	:	comment
	|	tcu
	|	begin_gem
	|	end_gem
	|	lazy_gem
	|	u
	;
	
chat_attrs
	:	CHAT_ATTR_VERSION
	|	CHAT_ATTR_DATE
	|	CHAT_ATTR_CORPUS
	|	CHAT_ATTR_MEDIA
	|	CHAT_ATTR_MEDIATYPES
	|	CHAT_ATTR_LANG
	|	CHAT_ATTR_OPTIONS
	|	CHAT_ATTR_COLORWORDS
	|	CHAT_ATTR_PID
	|	CHAT_ATTR_FONT
	;

    

        
metadata
    :    METADATA_START dcelementtype* METADATA_END
    ->    ^(METADATA_START dcelementtype*)
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
    :    TITLE_START TITLE_ATTR_LANG? TEXT TITLE_END
    ->    ^(TITLE_START TITLE_ATTR_LANG? TEXT)
    ;
    
dc_creator
    :    CREATOR_START CREATOR_ATTR_LANG? TEXT CREATOR_END
    ->    ^(CREATOR_START CREATOR_ATTR_LANG? TEXT)
    ;
    
dc_subject
    :    SUBJECT_START SUBJECT_ATTR_LANG? TEXT SUBJECT_END
    ->    ^(SUBJECT_START SUBJECT_ATTR_LANG? TEXT)
    ;
    
dc_description
    :    DESCRIPTION_START DESCRIPTION_ATTR_LANG? TEXT DESCRIPTION_END
    ->    ^(DESCRIPTION_START DESCRIPTION_ATTR_LANG? TEXT)
    ;
    
dc_publisher
    :    PUBLISHER_START PUBLISHER_ATTR_LANG? TEXT PUBLISHER_END
    ->    ^(PUBLISHER_START PUBLISHER_ATTR_LANG? TEXT)
    ;
    
dc_contributor
    :    CONTRIBUTOR_START CONTRIBUTOR_ATTR_LANG? TEXT CONTRIBUTOR_END
    ->    ^(CONTRIBUTOR_START CONTRIBUTOR_ATTR_LANG? TEXT)
    ;
    
dc_date
    :    DATE_START DATE_ATTR_LANG? TEXT DATE_END
    ->    ^(DATE_START DATE_ATTR_LANG? TEXT)
    ;
    
dc_type
    :    TYPE_START DATE_ATTR_LANG? TEXT TYPE_END
    ->    ^(TYPE_START DATE_ATTR_LANG? TEXT)
    ;
    
dc_format
    :    FORMAT_START DATE_ATTR_LANG? TEXT FORMAT_END
    ->    ^(FORMAT_START DATE_ATTR_LANG? TEXT)
    ;
    
dc_identifier
    :    IDENTIFIER_START IDENTIFIER_ATTR_LANG? TEXT IDENTIFIER_END
    ->    ^(IDENTIFIER_START IDENTIFIER_ATTR_LANG? TEXT)
    ;
    
dc_source
    :    SOURCE_START SOURCE_ATTR_LANG? TEXT SOURCE_END
    ->    ^(SOURCE_START SOURCE_ATTR_LANG? TEXT)
    ;
    
dc_language
    :    LANGUAGE_START LANGUAGE_ATTR_LANG? TEXT LANGUAGE_END
    ->    ^(LANGUAGE_START LANGUAGE_ATTR_LANG? TEXT)
    ;

dc_relation
    :    RELATION_START RELATION_ATTR_LANG? TEXT RELATION_END
    ->    ^(RELATION_START RELATION_ATTR_LANG? TEXT)
    ;
    
dc_coverage
    :    COVERAGE_START COVERAGE_ATTR_LANG? TEXT COVERAGE_END
    ->    ^(COVERAGE_START COVERAGE_ATTR_LANG? TEXT)
    ;
    
dc_rights
    :    RIGHTS_START RIGHTS_ATTR_LANG? TEXT RIGHTS_END
    ->    ^(RIGHTS_START RIGHTS_ATTR_LANG? TEXT)
    ;
    
dc_appId
    :    APPID_START APPID_ATTR_LANG? TEXT APPID_END
    ->    ^(APPID_START APPID_ATTR_LANG? TEXT)
    ;


    

        
participants 
	:	PARTICIPANTS_START participant* PARTICIPANTS_END 
	->	^(PARTICIPANTS_START participant*)
	;
	
participant 
	:	PARTICIPANT_START part_attr* PARTICIPANT_END 
	->	^(PARTICIPANT_START part_attr*)
	;
	
part_attr
	:	PARTICIPANT_ATTR_ID
	|	PARTICIPANT_ATTR_ROLE
	|	PARTICIPANT_ATTR_NAME
	|	PARTICIPANT_ATTR_AGE
	|	PARTICIPANT_ATTR_GROUP
	|	PARTICIPANT_ATTR_SEX
	|	PARTICIPANT_ATTR_SES
	|	PARTICIPANT_ATTR_EDUCATION
	|	PARTIICPANT_ATTR_CUSTOM_FIELD
	|	PARTICIPANT_ATTR_BIRTHDAY
	|	PARTICIPANT_ATTR_LANGUAGE
	|	PARTICIPANT_ATTR_FIRST_LANGUAGE
	|	PARTICIPANT_ATTR_BIRTHPLACE
	;

    

        
comment
	:	COMMENT_START COMMENT_ATTR_TYPE? commentele* COMMENT_END 
	->	^(COMMENT_START COMMENT_ATTR_TYPE? commentele*)
	;
	
commentele
	:	media
	|	TEXT
	;

    

        
tcu
    :    TCU_START u+ TCU_END
    ->    ^(TCU_START u+)
    ;

    

        
u
	:	U_START u_attrs* linker* ( uele )* t postcode* media? uendele* U_END 
	->	^(U_START u_attrs* linker* uele* t postcode* media? uendele*)
	;
	catch [RecognitionException re] {
		consumeUntil(input, U_END);
		input.consume(); // consume U_END
	}
	
u_attrs
	:	U_ATTR_WHO
	|	U_ATTR_LANG
	;
	
uele
	:	ugrp
	|	uannotation
	;
	
ugrp
	:	w 
	|	pg 
	|	g
	|	sg
	;
	
uannotation
	: 	blob
	|	quotation
	|	quotation2
	|	pause 
	|	internal_media
	|	freecode
	|	e
	|	s
	|	tagmarker
	|	overlap_point
	|	underline
	|	italic
	|	long_feature
	|	nonvocal
	;
	
uendele
	:	k
	|	error
	|	r
	|	a
	;

    

        
linker
	: 	LINKER_START LINKER_ATTR_TYPE LINKER_END
	->	^(LINKER_START LINKER_ATTR_TYPE)
	;

    

        
w
	:	W_START wattr* langs? wele* W_END 
	->	^(W_START wattr* langs? wele*)
	;
	
wele
	:	TEXT 
	|	overlap_point
	|	underline
	|	italic
	|	shortening
	|	p
	|	long_feature
	|	wk 
	|	pos
	|	replacement
	|	mor
	|	mk
	;
	
wattr
	:	W_ATTR_SEPARATED_PREFIX
	|	W_ATTR_USER_SPECIAL_FORM
	|	W_ATTR_FORMSUFFIX
	|	W_ATTR_FORMTYPE
	|	W_ATTR_TYPE
	|	W_ATTR_UNTRANSCRIBED
	;

    

        
langs	
	:	LANGS_START langsEle LANGS_END
	->	^(LANGS_START langsEle)
	;
	
langsEle	
	:	singleLang
	|	multipleLang
	|	ambiguousLang
	;
	
singleLang
	:	SINGLE_START TEXT SINGLE_END
	->	^(SINGLE_START TEXT)
	;
	
multipleLang
	:	MULTIPLE_START TEXT MULTIPLE_END
	->	^(MULTIPLE_START TEXT)
	;
	
ambiguousLang
	:	AMBIGUOUS_START TEXT AMBIGUOUS_END
	->	^(AMBIGUOUS_START TEXT)
	;
		

    

        
overlap_point
    :    OVERLAP_POINT_START overlap_pointattrs+ OVERLAP_POINT_END
    ->    ^(OVERLAP_POINT_START overlap_pointattrs+)
    ;
    
overlap_pointattrs
    :    OVERLAP_POINT_ATTR_INDEX
    |    OVERLAP_POINT_ATTR_START_END
    |    OVERLAP_POINT_ATTR_TOP_BOTTOM
    ;

    

        
underline
	:	UNDERLINE_START UNDERLINE_ATTR_TYPE UNDERLINE_END
	->	^(UNDERLINE_START UNDERLINE_ATTR_TYPE)
	;

    

        
italic
	:	ITALIC_START ITALIC_ATTR_TYPE ITALIC_END
	->	^(ITALIC_START ITALIC_ATTR_TYPE)
	;

    

        
shortening
	:	SHORTENING_START TEXT? SHORTENING_END 
	->	^(SHORTENING_START TEXT?)
	;

    

        
p
	:	P_START P_ATTR_TYPE? P_END 
	->	^(P_START P_ATTR_TYPE?)
	;

    

        
long_feature
    :    LONG_FEATURE_START LONG_FEATURE_ATTR_TYPE TEXT LONG_FEATURE_END
    ->    ^(LONG_FEATURE_START LONG_FEATURE_ATTR_TYPE TEXT)
    ;

    

        
wk
	:	WK_START WK_ATTR_TYPE? WK_END
	->	^(WK_START WK_ATTR_TYPE?)
	;

    

        
replacement
	:	REPLACEMENT_START replacementele* REPLACEMENT_END 
	->	^(REPLACEMENT_START replacementele*)
	;
	
replacementele
    :    w
    ;

    

        
mor
    :    MOR_START morattr+ morchoice menx* gra* morseq* MOR_END
    ->    ^(MOR_START morattr+ morchoice menx* gra* morseq*)
    ;
    
morattr
    :    MOR_ATTR_TYPE
    |    MOR_ATTR_OMITTED
    ;
    
morchoice
    :    mw
    |    mwc
    |    mt
    ;
    
morseq
    :    mor_pre
    |    mor_post
    ;
    
mor_pre
    :    MOR_PRE_START morchoice menx* gra* MOR_PRE_END
    ->    ^(MOR_PRE_START morchoice menx* gra*)
    ;
    
mor_post
    :    MOR_POST_START morchoice menx* gra* MOR_POST_END
    ->    ^(MOR_POST_START morchoice menx* gra*)
    ;
    
menx
    :    MENX_START TEXT MENX_END
    ->    ^(MENX_START TEXT)
    ;
    
gra
    :    GRA_START graattrs+ GRA_END
    ->    ^(GRA_START graattrs+)
    ;
    
graattrs
    :    GRA_ATTR_TYPE
    |    GRA_ATTR_INDEX
    |    GRA_ATTR_HEAD
    |    GRA_ATTR_RELATION
    ;
    
mw
    :    MW_START mpfx* pos stem mk* MW_END
    ->    ^(MW_START mpfx* pos stem mk*)
    ;
    
mwc
    :    MWC_START mpfx* pos mw+ MWC_END
    ->    ^(MWC_START mpfx* pos mw+)
    ;
    
mt
    :    MT_START MT_ATTR_TYPE MT_END
    ->    ^(MT_START MT_ATTR_TYPE)
    ;
    
mpfx
    :    MPFX_START TEXT MPFX_END
    ->    ^(MPFX_START TEXT)
    ;
   
pos
    :    POS_START morposc morposs* POS_END
    ->    ^(POS_START morposc morposs*)
    ;
    
morposc
    :    C_START TEXT C_END
    ->    ^(C_START TEXT)
    ;
    
morposs
    :    S_START TEXT S_END
    ->    ^(S_START TEXT)
    ;
    
stem
    :    STEM_START TEXT STEM_END
    ->    ^(STEM_START TEXT);
    
mk
    :    MK_START MK_ATTR_TYPE TEXT MK_END
    ->    ^(MK_START MK_ATTR_TYPE TEXT)
    ;
    


    

        
blob
    :    BLOB_START TEXT* BLOB_END
    ->    ^(BLOB_START TEXT*)
    ;

    

        
g	
	:	G_START gele+ gchoice* G_END 
	->	^(G_START gele+ gchoice*)
	;
	
gele
	:	w
	|	g
	|	pg
	|	sg
	|	quotation
	|	quotation2
	|	pause
	|	internal_media
	|	freecode
	|	e
	|	s
	|	tagmarker
	|	long_feature
	|	nonvocal
	|	overlap_point
	|	underline
	|	italic
	;
	
gchoice
	:	k
	|	error
	|	r
	|	duration
	|	ga
	|	overlap
	;

    

        
pg
scope {
	boolean inPg;
}
@init {
	$pg::inPg = true;
}
@after {
	$pg::inPg = false;
}
	:	PG_START ( pgele )* ( pho )* align? PG_END
	->	^(PG_START pgele* pho* align?)
	;
	
pgele
	:	w
	|	g
	|	quotation
	|	quotation2
	|	pause
	|	internal_media
	|	freecode
	|  	e
	|	s
	|	tagmarker
	|	long_feature
	|	nonvocal
	|	overlap_point
	|	underline
	|	italic
	;

    

        
quotation
    :    QUOTATION_START QUOTATION_ATTR_TYPE mor* QUOTATION_END
    ->    ^(QUOTATION_START QUOTATION_ATTR_TYPE mor*)
    ;

    

        
quotation2
    :    QUOTATION2_START QUOTATION2_ATTR_TYPE mor* QUOTATION_END
    ->    ^(QUOTATION2_START QUOTATION2_ATTR_TYPE mor*)
    ;

    

        
pause
	:	PAUSE_START PAUSE_ATTR_SYMBOLIC_LENGTH? PAUSE_ATTR_LENGTH? PAUSE_END 
	->	^(PAUSE_START PAUSE_ATTR_SYMBOLIC_LENGTH? PAUSE_ATTR_LENGTH?)
	;

    

        
internal_media
    :    INTERNAL_MEDIA_START internal_media_attr* INTERNAL_MEDIA_END
	->	^(INTERNAL_MEDIA_START internal_media_attr*)
	;
	
internal_media_attr
	:	INTERNAL_MEDIA_ATTR_START
	|	INTERNAL_MEDIA_ATTR_END
	|	INTERNAL_MEDIA_ATTR_UNIT
	;

    

        
freecode
    :    FREECODE_START TEXT FREECODE_END
    ->    ^(FREECODE_START TEXT)
    ;

    

        
e
	:	E_START echoice1 echoice2* E_END 
	->	^(E_START echoice1 echoice2*)
	;
	
echoice1
	:	action
	|	happening
	|	otherspokenevent
	;

echoice2
	:	k
	|	error
	|	r
	|	overlap
	|	ga
	|	duration
	;

    

        
action
	:	ACTION_START TEXT? ACTION_END 
	->	^(ACTION_START TEXT?)
	;

    

        
happening
	:	HAPPENING_START TEXT? HAPPENING_END 
	->	^(HAPPENING_START TEXT?)
	;

    

        
ga
	:	GA_START GA_ATTR_TYPE? TEXT? GA_END
	->	^(GA_START GA_ATTR_TYPE? TEXT?)
	;


    

        
overlap
	:	OVERLAP_START overlap_attr* OVERLAP_END?
	->	^(OVERLAP_START overlap_attr*)
	;	
	
overlap_attr
	:	OVERLAP_ATTR_TYPE
	|	OVERLAP_ATTR_INDEX
	;


    

        

otherspokenevent
    :    OTHERSPOKENEVENT_START otherspokenevent_attr* OTHERSPOKENEVENT_END
    ->    ^(OTHERSPOKENEVENT_START otherspokenevent_attr*)
    ;
    
otherspokenevent_attr
	:	OTHERSPOKENEVENT_ATTR_WHO
	|	OTHERSPOKENEVENT_ATTR_SAID
	;


    

        
k
	:	K_START K_ATTR_TYPE? K_END 
	-> 	^(K_START K_ATTR_TYPE?)
	;

    

        
error
	:	ERROR_START TEXT? ERROR_END
	->	^(ERROR_START TEXT?)
	;

    

        
duration
    :    DURATION_START TEXT DURATION_END
    ->    ^(DURATION_START TEXT)
    ;

    

        
s
	:	S_START S_ATTR_TYPE? TEXT? S_END 
	->	^(S_START S_ATTR_TYPE? TEXT?)
	;

    

        
tagmarker
	:	TAGMARKER_START TAGMARKER_ATTR_TYPE mor* TAGMARKER_END
	->	^(TAGMARKER_START TAGMARKER_ATTR_TYPE mor*)
	;

    

        
nonvocal
    :    NONVOCAL_START NONVOCAL_ATTR_TYPE TEXT NONVOCAL_END
    ->    ^(NONVOCAL_START NONVOCAL_ATTR_TYPE TEXT)
    ;

    

        
pho
	:	model
	|	actual
	;

model
	:	MODEL_START pw* MODEL_END
	->	^(MODEL_START  pw*)
	;

actual
	:	ACTUAL_START pw* ACTUAL_END
	->	^(ACTUAL_START pw*)
	;

pw
	:	PW_START pwele* PW_END
	->	^(PW_START pwele*)
	;
	
pwele
	:	ss
	|	wk
	|	ph
	;

    

        
ph
	:	PH_START phattr* TEXT PH_END
	->	^(PH_START phattr* TEXT)
	;
	
phattr
	:	PH_ATTR_ID
	|	PH_ATTR_SCTYPE
	|	PH_ATTR_HIATUS
	;

    

        
ss
	:	SS_START SS_ATTR_TYPE SS_END
	->	^(SS_START SS_ATTR_TYPE)
	;

    

        
align
	:	ALIGN_START alignCol* ALIGN_END
	->	^(ALIGN_START alignCol*)
	;

alignCol
	:	COL_START phref+ COL_END
	->	^(COL_START phref+)
	;

phref
	:	modelref
	|	actualref
	;

modelref
	:	MODELREF_START TEXT MODELREF_END
	->	^(MODELREF_START TEXT)
	;

actualref
	:	ACTUALREF_START TEXT ACTUALREF_END
	->	^(ACTUALREF_START TEXT)
	;

    

        
sg
    :    SG_START sgchoice+ sw+ SG_END
    ->    ^(SG_START sgchoice+ sw+)
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
    :    SW_START TEXT SW_END
    ->    ^(SW_START TEXT)
    ;

    

        
r
	:	R_START R_ATTR_TIMES? R_END 
	->	^(R_START R_ATTR_TIMES?)
	;

    

        
t
	:	T_START T_ATTR_TYPE? mor? T_END 
	->	^(T_START T_ATTR_TYPE? mor?)
	;

    

        
postcode
	:	POSTCODE_START TEXT? POSTCODE_END 
	->	^(POSTCODE_START TEXT?)
	;

    

        
media
	:	MEDIA_START media_attr* MEDIA_END
	->	^(MEDIA_START media_attr*)
	;
	
media_attr
	:	MEDIA_ATTR_START
	|	MEDIA_ATTR_END
	|	MEDIA_ATTR_UNIT
	;

    

        
a
	:	A_START a_attr* (aele)* A_END 
	->	^(A_START a_attr* aele*)
	;

aele
	:	TEXT
	|	media
	;
	
a_attr
	:	A_ATTR_TYPE
	|	A_ATTR_FLAVOR
	;

    

        
begin_gem
    :    BEGIN_GEM_START BEGIN_GEM_ATTR_LABEL BEGIN_GEN_END
    ->    ^(BEGIN_GEM_START BEGIN_GEM_ATTR_LABEL)
    ;

    

        
end_gem
    :    END_GEM_START END_GEM_ATTR_LABEL END_GEN_END
    ->    ^(END_GEM_START END_GEM_ATTR_LABEL)
    ;

    

        
lazy_gem
	:	LAZY_GEM_START LAZY_GEM_ATTR_LABEL? LAZY_GEM_END
	->	^(LAZY_GEM_START LAZY_GEM_ATTR_LABEL?)
	;

    