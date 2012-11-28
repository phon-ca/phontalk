

        
grammar Chat;

options {
output=AST;
ASTLabelType=CommonTree;
}

@header {
package ca.phon.phontalk.parser;

import ca.phon.system.logger.*;
}

@members {

private String filename;

public void setFilename(String filename) {
	this.filename = filename;
}

public String getFilename() {
	return this.filename;
}

/** Override the default getErrorMessage() to 
 * also output to PhonLogger
 */
public String getErrorMessage(RecognitionException re, String[] tokens) {
	String retVal = super.getErrorMessage(re, tokens);
	PhonLogger.warning(getFilename() + "(" + 
		re.line + ":" + re.c + ") " + retVal);
		/*
	ByteArrayOutputStream bout = new ByteArrayOutputStream();
	PrintWriter writer = new PrintWriter(new OutputStreamWriter(bout));
	re.printStackTrace(writer);
	PhonLogger.warning(new String(bout.toByteArray(), "UTF-8"));
		*/
	return retVal;
}

/**
 * Print out a warning that we are not supporting the
 * currently active element.
 */
public void unsupportedWarning() {

    

}

}

    

        
chat
	:	CHAT_START chat_attrs* participants? chat_content* CHAT_END 
	->	^(CHAT_START chat_attrs* participants? chat_content*)
	;
	
chat_content
	:	comment	
	|	u
	|	lazy_gem
	;
	
chat_attrs
	:	CHAT_ATTR_MEDIA
	|	CHAT_ATTR_MEDIATYPES
	|	CHAT_ATTR_VERSION
	|	CHAT_ATTR_LANG
	|	CHAT_ATTR_CORPUS
	|	CHAT_ATTR_ID
	|	CHAT_ATTR_DATE
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
	|	PARTICIPANT_ATTR_NAME
	|	PARTICIPANT_ATTR_ROLE
	|	PARTICIPANT_ATTR_LANGUAGE
	|	PARTICIPANT_ATTR_AGE
	|	PARTICIPANT_ATTR_GROUP
	|	PARTICIPANT_ATTR_SEX
	|	PARTICIPANT_ATTR_SES
	|	PARTICIPANT_ATTR_EDUCATION
	|	PARTICIPANT_ATTR_FIRST_LANGUAGE
	|	PARTICIPANT_ATTR_BIRTHPLACE
	|	PARTICIPANT_ATTR_BIRTHDAY
	;

    

        
comment
	:	COMMENT_START COMMENT_ATTR_TYPE? TEXT* COMMENT_END 
	->	^(COMMENT_START COMMENT_ATTR_TYPE? TEXT*)
	;

    

        
u
	:	U_START U_ATTR_WHO? ( uele )* t? postcode? media? a* U_END 
	->	^(U_START U_ATTR_WHO? uele* t? postcode? media? a*)
	;
	catch [RecognitionException re] {
		consumeUntil(input, U_END);
		input.consume(); // consume U_END
	}
	
uele
	:	ugrp
	|	uannotation
	;
	
ugrp
	:	w 
	|	pg 
	|	g
	;
	
uannotation
	: 	s 
	|	pause 
	|	e
	|	linker
	|	tagmarker
	;

    

        
w
	:	W_START wattr* langs? wele* W_END 
	->	^(W_START wattr* langs? wele*)
	;
	
wele
	:	TEXT 
	|	overlappoint
	|	underline
	|	italic
	|	shortening
	|	p
	|	longfeature
	|	wk 
	|	pos
	|	replacement
	|	mor
	|	mk
	;
	
wattr
	:	W_ATTR_SEPARATEDPREFIX
	|	W_ATTR_FORMTYPE
	|	W_ATTR_TYPE
	|	W_ATTR_USERSPECIALFORM
	|	W_ATTR_FORMSUFFIX
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
		

    

        
overlappoint
    :    OVERLAPPOINT_START overlappointattrs+ OVERLAPPOINT_END
    ->    ^(OVERLAPPOINT_START overlappointattrs+)
    ;
    
overlappointattrs
    :    OVERLAPPOINT_ATTR_INDEX
    |    OVERLAPPOINT_ATTR_STARTEND
    |    OVERLAPPOINT_ATTR_TOPBOTTOM
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

    

        
longfeature
    :    LONGFEATURE_START LONGFEATURE_ATTR_TYPE TEXT LONGFEATURE_END
    ->    ^(LONGFEATURE_START LONGFEATURE_ATTR_TYPE TEXT)
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
	|	pause
	|  	e
	|	underline
	|	overlap
	|	tagmarker
	|	r 
	|	k
	|	ga
	|	s
	|	error
	;

    

        
r
	:	R_START R_ATTR_TIMES? R_END 
	->	^(R_START R_ATTR_TIMES?)
	;

    

        
k
	:	K_START K_ATTR_TYPE? K_END 
	-> 	^(K_START K_ATTR_TYPE?)
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

    

        
ga
	:	GA_START GA_ATTR_TYPE? TEXT? GA_END
	->	^(GA_START GA_ATTR_TYPE? TEXT?)
	;


    

        
overlap
	:	OVERLAP_START OVERLAP_ATTR_TYPE? OVERLAP_END?
	->	^(OVERLAP_START OVERLAP_ATTR_TYPE?)
	;	


    

        
tagmarker
	:	TAGMARKER_START TAGMARKER_ATTR_TYPE mor* TAGMARKER_END
	->	^(TAGMARKER_START TAGMARKER_ATTR_TYPE mor*)
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

    

        

otherspokenevent
    :    OTHERSPOKENEVENT_START OTHERSPOKENEVENT_ATTR_WHO w OTHERSPOKENEVENT_END
    ->    ^(OTHERSPOKENEVENT_START OTHERSPOKENEVENT_ATTR_WHO w)
    ;


    

        
error
	:	ERROR_START TEXT? ERROR_END
	->	^(ERROR_START TEXT?)
	;

    

        
duration
    :    DURATION_START TEXT DURATION_END
    ->    ^(DURATION_START TEXT)
    ;

    

        
pause
	:	PAUSE_START PAUSE_ATTR_SYMBOLIC_LENGTH? PAUSE_ATTR_LENGTH? PAUSE_END 
	->	^(PAUSE_START PAUSE_ATTR_SYMBOLIC_LENGTH? PAUSE_ATTR_LENGTH?)
	;

    

        
s
	:	S_START S_ATTR_TYPE? TEXT? S_END 
	->	^(S_START S_ATTR_TYPE? TEXT?)
	;

    

        
g	
	:	G_START gele+ gchoice* G_END 
	->	^(G_START gele+ gchoice*)
	;
	
gele
	:	w
	|	pg
	|	sg
	|	quotation
	|	quotation2
	|	pause
	|	internalmedia
	|	freecode
	|	e
	|	s
	|	tagmarker
	|	longfeature
	|	nonvocal
	|	overlappoint
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
    |    internalmedia
    |    freecode
    |    e
    |    s
    |    tagmarker
    |    longfeature
    |    nonvocal
    |    overlappoint
    |    underline
    |    italic
    ;
    
sw
    :    SW_START TEXT SW_END
    ->    ^(SW_START TEXT)
    ;

    

        
quotation
    :    QUOTATION_START QUOTATION_ATTR_TYPE mor* QUOTATION_END
    ->    ^(QUOTATION_START QUOTATION_ATTR_TYPE mor*)
    ;

    

        
quotation2
    :    QUOTATION2_START QUOTATION2_ATTR_TYPE mor* QUOTATION_END
    ->    ^(QUOTATION2_START QUOTATION2_ATTR_TYPE mor*)
    ;

    

        
internalmedia
    :    INTERNALMEDIA_START internalmedia_attr* INTERNALMEDIA_END
	->	^(INTERNALMEDIA_START internalmedia_attr*)
	;
	
internalmedia_attr
	:	INTERNALMEDIA_ATTR_START
	|	INTERNALMEDIA_ATTR_END
	|	INTERNALMEDIA_ATTR_UNIT
	;

    

        
freecode
    :    FREECODE_START TEXT FREECODE_END
    ->    ^(FREECODE_START TEXT)
    ;

    

        
nonvocal
    :    NONVOCAL_START NONVOCAL_ATTR_TYPE TEXT NONVOCAL_END
    ->    ^(NONVOCAL_START NONVOCAL_ATTR_TYPE TEXT)
    ;

    

        
t
	:	T_START T_ATTR_TYPE? mor? T_END 
	->	^(T_START T_ATTR_TYPE? mor?)
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

    

        
postcode
	:	POSTCODE_START TEXT? POSTCODE_END 
	->	^(POSTCODE_START TEXT?)
	;

    

        
linker
	: 	LINKER_START LINKER_ATTR_TYPE LINKER_END
	->	^(LINKER_START LINKER_ATTR_TYPE)
	;

    

        
lazy_gem
	:	LAZY_GEM_START LAZY_GEM_ATTR_LABEL? LAZY_GEM_END
	->	^(LAZY_GEM_START LAZY_GEM_ATTR_LABEL?)
	;

    