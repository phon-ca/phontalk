

        
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

/** Override the default getErrorMessage() to 
 * also output to PhonLogger
 */
public String getErrorMessage(RecognitionException re, String[] tokens) {
	String retVal = super.getErrorMessage(re, tokens);
	PhonLogger.warning("line " + 
		re.line + ":" + re.c + " " + retVal);
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
	:	COMMENT_START COMMENT_ATTR_TYPE? TEXT? COMMENT_END 
	->	^(COMMENT_START COMMENT_ATTR_TYPE? TEXT?)
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
	;

    

        
w
	:	W_START wattr* wele* W_END 
	->	^(W_START wattr* wele*)
	;
	
wele
	:	TEXT 
	|	wk 
	|	p 
	|	shortening 
	|	f 
	|	replacement
	|	underline
	|	langs
	|	mor
	;
	
wattr
	:	W_ATTR_FORMTYPE
	|	W_ATTR_TYPE
	;

    

        
wk
	:	WK_START WK_ATTR_TYPE? WK_END
	->	^(WK_START WK_ATTR_TYPE?)
	;

    

        
p
	:	P_START P_ATTR_TYPE? P_END 
	->	^(P_START P_ATTR_TYPE?)
	;

    

        
shortening
	:	SHORTENING_START TEXT? SHORTENING_END 
	->	^(SHORTENING_START TEXT?)
	;

    

        
f
	:	F_START F_ATTR_TYPE? F_END 
	->	^(F_START F_ATTR_TYPE?)
	;

    

        
replacement
	:	REPLACEMENT_START replacementele* REPLACEMENT_END 
	->	^(REPLACEMENT_START replacementele*)
	;
	
replacementele
    :    w
    ;

    

        
underline
	:	UNDERLINE_START UNDERLINE_ATTR_TYPE UNDERLINE_END
	->	^(UNDERLINE_START UNDERLINE_ATTR_TYPE)
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
		

    

        
mor
    :    MOR_START morattr* morchoice* gra? morseq* MOR_END
    ->    ^(MOR_START morattr* morchoice* gra? morseq*)
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
    :    morpre
    |    morpost
    |    menx
    ;
    
morpre
    :    MORPRE_START morattr* morchoice* gra? morseq* MORPRE_END
    ->    ^(MORPRE_START morattr* morchoice* gra? morseq*)
    ;
    
morpost
    :    MORPOST_START morattr* morchoice* gra? morseq* MORPOST_END
    ->    ^(MORPOST_START morattr* morchoice* gra? morseq*)
    ;
    
menx
    :    MENX_START TEXT MENX_END
    ;
    
gra
    :    GRA_START GRA_ATTR_TYPE GRA_ATTR_INDEX GRA_ATTR_HEAD GRA_ATTR_RELATION GRA_END
    ->    ^(GRA_START GRA_ATTR_TYPE GRA_ATTR_INDEX GRA_ATTR_HEAD GRA_ATTR_RELATION)
    ;
    
mw
    :    MW_START mpfx* pos mwchoice mk* MW_END
    ->    ^(MW_START mpfx* pos mwchoice mk*)
    ;
    
mwc
    :    MWC_START mpfx* pos mw mw+ MWC_END
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
    
mwchoice
    :    stem
    |    mortagmarker
    ;
    
stem
    :    STEM_START TEXT STEM_END
    ->    ^(STEM_START TEXT);
    
mortagmarker
    :    MORTAGMARKER_START MORTAGMARKER_ATTR_TYPE MORTAGMARKER_END
    ->    ^(MORTAGMARKER_START MORTAGMARKER_ATTR_TYPE)
    ;
    
mk
    :    MK_START TEXT MK_END
    ->    ^(MK_START TEXT)
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


    

        
e
	:	E_START evtele* E_END 
	->	^(E_START evtele*)
	;

evtele
	:	action
	|	happening
	|	ga
	|	overlap
	;

    

        
action
	:	ACTION_START TEXT? ACTION_END 
	->	^(ACTION_START TEXT?)
	;

    

        
happening
	:	HAPPENING_START TEXT? HAPPENING_END 
	->	^(HAPPENING_START TEXT?)
	;

    

        
pause
	:	PAUSE_START PAUSE_ATTR_SYMBOLIC_LENGTH? PAUSE_ATTR_LENGTH? PAUSE_END 
	->	^(PAUSE_START PAUSE_ATTR_SYMBOLIC_LENGTH? PAUSE_ATTR_LENGTH?)
	;

    

        
s
	:	S_START S_ATTR_TYPE? TEXT? S_END 
	->	^(S_START S_ATTR_TYPE? TEXT?)
	;

    

        
error
	:	ERROR_START TEXT? ERROR_END
	->	^(ERROR_START TEXT?)
	;

    

        
g	
	:	G_START gele* G_END 
	->	^(G_START gele*)
	;
	
gele
	:	w
	-> {pg_stack != null && !pg_stack.isEmpty() && $pg::inPg}? w
	-> ^(PG_START w)
	|	pg
	|	pause
	|	r 
	|	k
	|	ga
	|	overlap
	|	e
	|	g
	|	s
	| 	underline
	|	error
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

    