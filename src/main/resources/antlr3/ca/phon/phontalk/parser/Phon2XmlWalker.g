

        
tree grammar Phon2XmlWalker;

options
{
	ASTLabelType = CommonTree ;
	tokenVocab = Chat ;
	output = template;
}

@header {
package ca.phon.phontalk.parser;

import ca.phon.system.logger.*;
}

@members {
private int uttIndex = 0;

/** Override the default getErrorMessage() to 
 * also output to PhonLogger
 */
public String getErrorMessage(RecognitionException re, String[] tokens) {
    String retVal = super.getErrorMessage(re, tokens);
   
    PhonLogger.severe("Record " + (uttIndex+1) + " (" +
        re.line + ":" + re.c + ") " + retVal);

    if(re instanceof MismatchedTreeNodeException) {
    	MismatchedTreeNodeException mte = (MismatchedTreeNodeException)re;
    	PhonLogger.severe((new ChatTokens()).getTokenName(mte.expecting));
    }
        re.printStackTrace();
    return retVal;
}

public int getRecordIndex() {
	return uttIndex+1;
}

/**
 * Print out a warning that we are not supporting the
 * currently active element.
 */
public void unsupportedWarning() {

    

}

}


    

		
chat
	:	^(CHAT_START (attrlist+=chat_attrs)* (partlist=participants)? (contentlist+=chat_content)*)
	->	template(
			attrs={$attrlist},
			parts={$partlist.st},
			content={$contentlist}
		)
	<<\<?xml version="1.0" encoding="UTF-8"?\>
\<CHAT 
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
	xmlns="http://www.talkbank.org/ns/talkbank"  
	xsi:schemaLocation="http://www.talkbank.org/ns/talkbank http://talkbank.org/software/talkbank.xsd" 
	<attrs; separator=""> \>
	<parts>
	<content; separator="">
\</CHAT\> >>
	;

chat_content
	:	comment
	->	template( v={$comment.st} )
		"<v>"
	|	u
	->	template( v={$u.st} )
		"<v>"
	|	lazy_gem
	->	template( v={$lazy_gem.st} )
		"<v>"
	;
	
chat_attrs
	:	CHAT_ATTR_MEDIA
	->	template(media={$CHAT_ATTR_MEDIA.text})
	<<
Media="<media>" >>
	|	CHAT_ATTR_MEDIATYPES
	->	template(types={$CHAT_ATTR_MEDIATYPES.text})
	<<
Mediatypes="<types>" >>
	|	CHAT_ATTR_VERSION
	->	template(version={$CHAT_ATTR_VERSION.text})
	<<
Version="<version>" >>
	|	CHAT_ATTR_LANG
	->	template(lang={$CHAT_ATTR_LANG.text})
	<<
Lang="<lang>" >>
	|	CHAT_ATTR_CORPUS
	->	template(corpus={$CHAT_ATTR_CORPUS.text})
	<<
Corpus="<corpus>" >>
	|	CHAT_ATTR_ID
	->	template(id={$CHAT_ATTR_ID.text})
	<<
Id="<id>" >>
	|	CHAT_ATTR_DATE
	->	template(date={$CHAT_ATTR_DATE.text})
	<<
Date="<date>" >>
	;
	

	

		
participants 
	:	^(PARTICIPANTS_START (partlist+=participant)*)
	->	template(parts={$partlist})
	<<
\<Participants\>
<parts; separator="">
\</Participants\>
	>>
	;
		
participant
	:	^(PARTICIPANT_START (attrlist+=part_attr)*)
	->	template(
			attrs={$attrlist}
		)
	<<
\<participant
	<attrs; separator="\n"> /\>
	>>
	;
	
part_attr
	:	PARTICIPANT_ATTR_ID
	->	template(id={$PARTICIPANT_ATTR_ID.text})
		"id=\"<id>\""
	|	PARTICIPANT_ATTR_NAME
	->	template(name={$PARTICIPANT_ATTR_NAME.text})
		"name=\"<name>\""
	|	PARTICIPANT_ATTR_ROLE
	->	template(role={$PARTICIPANT_ATTR_ROLE.text})
		"role=\"<role>\""
	|	PARTICIPANT_ATTR_LANGUAGE
	->	template(language={$PARTICIPANT_ATTR_LANGUAGE.text})
		"language=\"<language>\""
	|	PARTICIPANT_ATTR_AGE
	->	template(age={$PARTICIPANT_ATTR_AGE.text})
		"age=\"<age>\""
	|	PARTICIPANT_ATTR_GROUP
	->	template(grp={$PARTICIPANT_ATTR_GROUP.text})
		"group=\"<grp>\""
	|	PARTICIPANT_ATTR_SEX
	->	template(sex={$PARTICIPANT_ATTR_SEX.text})
		"sex=\"<sex>\""
	|	PARTICIPANT_ATTR_SES
	->	template(ses={$PARTICIPANT_ATTR_SES.text})
		"ses=\"<ses>\""
	|	PARTICIPANT_ATTR_EDUCATION
	->	template(edu={$PARTICIPANT_ATTR_EDUCATION.text})
		"education=\"<edu>\""
	|	PARTICIPANT_ATTR_FIRST_LANGUAGE
	->	template(flang={$PARTICIPANT_ATTR_FIRST_LANGUAGE.text})
		"first-language=\"<flang>\""
	|	PARTICIPANT_ATTR_BIRTHPLACE
	->	template(birthplace={$PARTICIPANT_ATTR_BIRTHPLACE.text})
		"birthplace=\"<birthplace>\""
	|	PARTICIPANT_ATTR_BIRTHDAY
	->	template(birthday={$PARTICIPANT_ATTR_BIRTHDAY.text})
		"birthday=\"<birthday>\""
	;

	

		
comment
	:	^(COMMENT_START ctype=COMMENT_ATTR_TYPE? cval=TEXT?)
	->	template(
			type={$ctype.text},
			val={$cval.text}
		)
	<<\<comment type="<type>"\><val>\</comment\> >>
	;

	

		
u
	:	^(U_START U_ATTR_WHO? (contentlist+=uele)* t? postcode? media? (annotationlist+=a)*)
	->	template(
			who={$U_ATTR_WHO.text},
			ucontent={$contentlist},
			terminator={$t.st},
			pc={$postcode.st},
			segment={$media.st},
			annotations={$annotationlist},
			uttid={uttIndex++}
		)
	<<
\<u who="<who>" uID="u<uttid>"\>
	<ucontent; separator="">
	<terminator>
	<pc>
	<segment>
	<annotations; separator="">
\</u\>
	>>
	;
	
uele
	:	ugrp
	->	template( v={$ugrp.st} )
		"<v>"
	|	uannotation
	->	template( v={$uannotation.st} )
		"<v>"
	;
	
uannotation
	: 	s 
	->	template( v={$s.st} )
		"<v>"
	|	pause
	->	template( v={$pause.st} )
		"<v>"
	|	e
	->	template( v={$e.st} )
		"<v>"
	|	linker
	->	template( v={$linker.st} )
		"<v>"
	;
	

ugrp
	:	w 
	->	template( v={$w.st} )
		"<v>"
	|	pg
	->	template( v={$pg.st} )
		"<v>"
	|	g
	-> 	template( v={$g.st} )
		"<v>"
	;

	

		
w
	:	^(W_START (wattrlist+=wattr)* (contentlist+=wele)* )
	->	{$wattrlist != null}? template( content={$contentlist}, wattrs={$wattrlist} )
		"\<w <wattrs; separator=\"\">\><content; separator=\"\">\</w\>"
	->	template( content={$contentlist} )
		"\<w\><content; separator=\"\">\</w\>"
	;
	
wele
	:	TEXT
	->	template( v={$TEXT.text} )
		"<v>"
	|	wk
	->	template( v={$wk.st} )
		"<v>"
	|	p
	->	template( v={$p.st} )
		"<v>"
	|	shortening
	->	template( v={$shortening.st} )
		"<v>"
	|	f
	->	template( v={$f.st} )
		"<v>"
	|	replacement
	->	template( v={$replacement.st} )
		"<v>"
	|	underline
	->	template( v={$underline.st} )
		"<v>"
	|	langs
	->	template ( v={$langs.st} )
		"<v>"
	|	mor
	;
	
wattr
	:	W_ATTR_FORMTYPE
	->	template( v={$W_ATTR_FORMTYPE.text} )
	<<formType="<v>" >>
	|	W_ATTR_TYPE
	->	template( v={$W_ATTR_TYPE.text} )
	<<type="<v>" >>
	;

	

        
wk
    :    ^(WK_START WK_ATTR_TYPE?)
    ->    template( type={$WK_ATTR_TYPE.text} )
    "\<wk type=\"<type>\"/\>"
    ;

    

		
p
	:	^(P_START P_ATTR_TYPE?)
	->	template( type={$P_ATTR_TYPE} )
	<<\<p type="<type>"/\> >>
	;

	

        
shortening
    :    ^(SHORTENING_START TEXT?)
    ->    template( v={$TEXT.text} )
    "\<shortening\><v>\</shortening\>"
    ;

    

        
f
    :    ^(F_START F_ATTR_TYPE?)
    ->    template( type={$F_ATTR_TYPE.text} )
    <<\<f type="<type>"/\> >>
    ;

    

        
replacement
    :    ^(REPLACEMENT_START (rcontentlist+=replacementele)*)
    ->    template( rcontent={$rcontentlist} )
    "\<replacement\><rcontent; separator=\"\">\</replacement\>"
    ;
    
replacementele
    :    w
    ->    template( v={$w.st} )
	    "<v>"
    ;

    

        
underline
	:	^(UNDERLINE_START type=UNDERLINE_ATTR_TYPE)
	->	template( type={$UNDERLINE_ATTR_TYPE} )
	"\<underline type=\"<type>\"/\>"
	;	

    

        
langs	
	:	^(LANGS_START langsEle)
	->	template( v={$langsEle.st} )
		"\<langs\><v>\</langs\>"
	;
	
langsEle	
	:	singleLang
	->	template( v={$singleLang.st} )
		"<v>"
	|	multipleLang
	->	template( v={$multipleLang.st} )
		"<v>"
	|	ambiguousLang
	->	template( v={$ambiguousLang.st} )
		"<v>"
	;
	
singleLang
	:	^(SINGLE_START singt=TEXT)
	->	template( v={$singt.text} )
		"\<single\><v>\</single\>"
	;
	
multipleLang
	:	^(MULTIPLE_START multt=TEXT)
	->	template( v={$multt.text} )
		"\<multiple\><v>\</multiple\>"
	;
	
ambiguousLang
	:	^(AMBIGUOUS_START ambt=TEXT)
	->	template( v={$ambt.text} )
		"\<ambiguous\><v>\</ambiguous\>"
	;

    

        
mor
    :    ^(MOR_START morattr* morchoice* gra? morseq*)
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
    
mw
    :    ^(MW_START mpfx* pos mwchoice mk*)
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
   
pos
    :    ^(POS_START morposc morposs*)
    ;
    
morposc
    :    ^(C_START TEXT)
    ;
    
morposs
    :    ^(S_START TEXT)
    ;
    
mwchoice
    :    stem
    |    mortagmarker
    ;
    
stem
    :    ^(STEM_START TEXT);
    
mortagmarker
    :    ^(MORTAGMARKER_START MORTAGMARKER_ATTR_TYPE)
    ;
    
mk
    :    ^(MK_START TEXT)
    ;

    

        
pg
	:	^(PG_START (contentlist+=pgele)* ( phoreps += pho )* align?)
	->    template(
	        content={$contentlist},
	        ipas={$phoreps},
			alignment={$align.st}
	      )
	<<
\<pg\>
	<content; separator="">
	<ipas; separator="\n">
	<alignment>
\</pg\> >>
	;
	
pgele
	:	w 
	->	template( v={$w.st} )
		"<v>"
	|	g
	->	template( v={$g.st} )
		"<v>"
	|	pause
	->	template( v={$pause.st} )
		"<v>"
	|  	e
	->	template( v={$e.st} )
		"<v>"
	|	underline
	->	template( v={$underline.st} )
		"<v>"
	|	overlap
	->	template( v={$overlap.st} )
		"<v>"
	|	r 
	->	template( v={$r.st} )
		"<v>"
	|	k
	->	template( v={$k.st} )
		"<v>"
	|	ga
	->	template( v={$ga.st} )
		"<v>"
	|	s
	->	template( v={$s.st} )
		"<v>"
	|	error
	->	template( v={$error.st} )
		"<v>"
	;

    

        
r
	:	^(R_START R_ATTR_TIMES?)
	->    template( times={$R_ATTR_TIMES.text} )
	<<\<r times="<times>"/\> >>
	;

    

        
k
	:    ^(K_START K_ATTR_TYPE?)
	->    template( type={$K_ATTR_TYPE} )
	<<\<k type="<type>"/\> >>
	;

    

		
pho
	:	model
	->	template( v = {$model.st} )
	"<v>"
	|	actual
	->	template( v = {$actual.st} )
	"<v>"
	;

model
	:	^(MODEL_START  (pws+=pw)*)
	->	template( content={$pws} )
	<<\<model\>
	<content; separator="\n">
\</model\> >>
	;

actual
	:	^(ACTUAL_START (pws+=pw)*)
	->	template( content={$pws} )
	<<\<actual\>
	<content; separator="\n">
\</actual\> >>
	;

pw
	:	^(PW_START (pweles+=pwele)*)
	->	template( content={$pweles} )
	<<\<pw\>
	<content; separator="\n">
\</pw\> >>
	;

pwele
	:	ss
	->	template( v = {$ss.st} )
	"<v>"
	|	wk
	->	template( v = {$wk.st} )
	"<v>"
	|	ph
	->	template(v = {$ph.st} )
	"<v>"
	;

	

        
ph
	:	^(PH_START (phattrs+=phattr)* TEXT)
	->	template( attributes = {$phattrs}, v = {$TEXT.text} )
	<<\<ph <attributes; separator=" ">\><v>\</ph\> >>
	;

phattr
	:	PH_ATTR_SCTYPE
	->	template( v = {$PH_ATTR_SCTYPE.text} )
	<<sctype="<v>">>
	|	PH_ATTR_ID
	->	template( v = {$PH_ATTR_ID.text} )
	<<id="<v>">>
	;

    

        
ss
	:	^(SS_START SS_ATTR_TYPE)
	->	template( type={$SS_ATTR_TYPE.text} )
	<<\<ss type="<type>"/\> >>
	;

    

        
align
	:	^(ALIGN_START (cols+=alignCol)*)
	->	template( columns = {$cols} )
	<<\<align\>
	<columns; separator="\n">
\</align\> >>
	;

alignCol
	:	^(COL_START (phs+=phref)+)
	->	template( phrefs = {$phs} )
	<<\<col\><phrefs; separator=" ">\</col\> >>
	;

phref
	:	modelref
	->	template( v = {$modelref.st} )
	"<v>"
	|	actualref
	->	template( v = {$actualref.st} )
	"<v>"
	;

modelref
	:	^(MODELREF_START TEXT)
	->	template( v = {$TEXT.text} )
	<<\<modelref\><v>\</modelref\> >>
	;

actualref
	:	^(ACTUALREF_START TEXT)
	->	template( v = {$TEXT.text} )
	<<\<actualref\><v>\</actualref\> >>
	;

    

        
ga
	:	^(GA_START GA_ATTR_TYPE? TEXT?)
	->    template(
	        type={$GA_ATTR_TYPE.text},
	        val={$TEXT.text}
	      )
	<<\<ga type="<type>"\><val>\</ga\> >>
	;


    

        
overlap
	:	^(OVERLAP_START OVERLAP_ATTR_TYPE?)
	->    template( type={$OVERLAP_ATTR_TYPE.text} )
	<<\<overlap type="<type>"/\> >>
	;	

    

		
e
	:	^(E_START (contentlist+=evtele)*)
	->	template( content={$contentlist} )
	<<\<e\><content; separator="">\</e\> >>
	;

evtele
	:	action
	->	template( v={$action.st} )
		"<v>"
	|	happening
	->	template( v={$happening.st} )
		"<v>"
	|	ga
	->	template( v={$ga.st} )
		"<v>"
	|	overlap
	->	template( v={$overlap.st} )
		"<v>"
	;

	

        
action
    :    ^(ACTION_START TEXT?)
    ->    template( v={$TEXT.text} )
    <<\<action\><v>\</action\> >>
    ;

    

        
happening
	:	^(HAPPENING_START TEXT?)
	->    template( v={$TEXT.text} )
	<<\<happening\><v>\</happening\> >>
	;

    

        
pause
	:	^(PAUSE_START PAUSE_ATTR_SYMBOLIC_LENGTH?)
	->    template( len={$PAUSE_ATTR_SYMBOLIC_LENGTH} )
	"\<pause symbolic-length=\"<len>\"/\>"
	;

    

		
s
	:	^(S_START S_ATTR_TYPE? TEXT?)
	->	template(
			type={$S_ATTR_TYPE.text},
			val={$TEXT.text}
		)
	"\<s type=\"<type>\"\><val>\</s\>"
	;

	

        
error
	:	^(ERROR_START et=TEXT?)
	->	template( errtext={$et.text} )
		"\<error\><errtext>\</error\>"
	;

    

		
g	
	:	^(G_START (contentlist+=gele)*)
	->	template( content={$contentlist} )
	<<\<g\><content; separator="">\</g\> >>
	;
	
gele
	:	w 
	->	template( v={$w.st} )
		"<v>"
	|	pg
	->	template( v={$pg.st} )
		"<v>"
	|	pause
	->	template( v={$pause.st} )
		"<v>"
	|	r 
	->	template( v={$r.st} )
		"<v>"
	|	k
	->	template( v={$k.st} )
		"<v>"
	|	ga
	->	template( v={$ga.st} )
		"<v>"
	|	overlap
	->	template( v={$overlap.st} )
		"<v>"
	|	e
	->	template( v={$e.st} )
		"<v>"
	|	g
	->	template( v={$g.st} )
		"<v>"
	|	s
	->	template( v={$s.st} )
		"<v>"
	|	underline
	->	template( v={$underline.st} )
		"<v>"
	|	error
	->	template( v={$error.st} )
		"<v>"
	;

	

        
t
	:	^(T_START T_ATTR_TYPE? mor?)
	->    template( type={$T_ATTR_TYPE} )
	<<\<t type="<type>"/\> >>
	;

    

		
media
	:	^(MEDIA_START (attrlist+=media_attr)*)
	->	template( attrs={$attrlist} )
	<<\<media <attrs; separator=" ">/\> >>
	;
	
media_attr
	:	MEDIA_ATTR_START
	->	template( v={$MEDIA_ATTR_START} )
	<<start="<v>">>
	|	MEDIA_ATTR_END
	->	template( v={$MEDIA_ATTR_END} )
	<<end="<v>">>
	|	MEDIA_ATTR_UNIT
	->	template( v={$MEDIA_ATTR_UNIT} )
	<<unit="<v>">>
	;

	

		
a
	:	^(A_START (attrlist+=a_attr)* (contentlist+=aele)*)
	->	template(
			attrs={$attrlist},
			content={$contentlist}
		)
	<<\<a <attrs; separator=" ">\>\<![CDATA[<content; separator="">]]\>\</a\> >>
	;

aele
	:	TEXT
	->	template( v={$TEXT.text} )
		"<v>"
	|	media
	->	template( v={$media.st} )
		"<v>"
	;
	
a_attr
	:	A_ATTR_TYPE
	->	template( v={$A_ATTR_TYPE} )
	<<type="<v>">>
	|	A_ATTR_FLAVOR
	->	template( v={$A_ATTR_FLAVOR} )
	<<flavor="<v>">>
	;

	

		
postcode
	:	^(POSTCODE_START TEXT?)
	->	template( v={$TEXT.text} )
	<<\<postcode\><v>\</postcode\> >>
	;

	

		
linker
	: 	^(LINKER_START LINKER_ATTR_TYPE)
	->	template( type={$LINKER_ATTR_TYPE.text} )
	<<\<linker type="<type>"/\> >>
	;

	

        
lazy_gem
	:	^(LAZY_GEM_START LAZY_GEM_ATTR_LABEL?)
	->    template( label={$LAZY_GEM_ATTR_LABEL.text} )
	<<\<lazy-gem label="<label>"/\> >>
	;

    