

        
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
	|	tagmarker
	->	template( v={$tagmarker.st} )
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
	:	^(W_START (wattrlist+=wattr)* langs? (contentlist+=wele)* )
	->	template( content={$contentlist}, l={$langs.st}, wattrs={$wattrlist} )
		"\<w <if(wattrs)><wattrs; separator=\"\"><endif>\><if(l)><l><endif><content; separator=\"\">\</w\>"
	;
	
wele
	:	TEXT
	->	template( v={$TEXT.text} )
		"<v>"
	|	overlappoint
	->	template( v={$overlappoint.st} )
		"<v>"
	|	underline
	->	template( v={$underline.st} )
		"<v>"
	|	italic
	->	template( v={$italic.st} )
		"<v>"
	|	shortening
	->	template( v={$shortening.st} )
		"<v>"
	|	p
	->	template( v={$p.st} )
		"<v>"
	|	longfeature
	->	template( v={$longfeature.st} )
		"<v>"
	|	wk
	->	template( v={$wk.st} )
		"<v>"
	|	pos
	->	template( v={$pos.st} )
		"<v>"
	
	|	replacement
	->	template( v={$replacement.st} )
		"<v>"
	|	mor
	->	template( v={$mor.st} )
		"<v>"
	|	mk
	->	template( v={$mk.st} )
		"<v>"
	;
	
wattr
	:	W_ATTR_SEPARATEDPREFIX
	->	template( v={$W_ATTR_SEPARATEDPREFIX.text} )
	<<separated-prefix="<v>" >>
	|	W_ATTR_FORMTYPE
	->	template( v={$W_ATTR_FORMTYPE.text} )
	<<formType="<v>" >>
	|	W_ATTR_TYPE
	->	template( v={$W_ATTR_TYPE.text} )
	<<type="<v>" >>
	|	W_ATTR_USERSPECIALFORM
	->	template( v={$W_ATTR_USERSPECIALFORM.text} )
	<<user-special-form="<v>" >>
	|	W_ATTR_FORMSUFFIX
	->	template( v={$W_ATTR_FORMSUFFIX.text} )
	<<form-suffix="<v>" >>
	|	W_ATTR_UNTRANSCRIBED
	->	template( v={$W_ATTR_UNTRANSCRIBED.text} )
	<<untranscribed="<v>" >>
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

    

        
overlappoint
    :    ^(OVERLAPPOINT_START overlappointattrs+)
    ;
    
overlappointattrs
    :    OVERLAPPOINT_ATTR_INDEX
    |    OVERLAPPOINT_ATTR_STARTEND
    |    OVERLAPPOINT_ATTR_TOPBOTTOM
    ;

    

        
underline
	:	^(UNDERLINE_START type=UNDERLINE_ATTR_TYPE)
	->	template( type={$UNDERLINE_ATTR_TYPE} )
	"\<underline type=\"<type>\"/\>"
	;	

    

        
italic
	:	^(ITALIC_START type=ITALIC_ATTR_TYPE)
	->	template( type={$ITALIC_ATTR_TYPE} )
	"\<italic type=\"<type>\"/\>"
	;	

    

        
shortening
    :    ^(SHORTENING_START TEXT?)
    ->    template( v={$TEXT.text} )
    "\<shortening\><v>\</shortening\>"
    ;

    

		
p
	:	^(P_START P_ATTR_TYPE?)
	->	template( type={$P_ATTR_TYPE} )
	<<\<p type="<type>"/\> >>
	;

	

        
longfeature
    :    ^(LONGFEATURE_START LONGFEATURE_ATTR_TYPE TEXT)
    ;

    

        
wk
    :    ^(WK_START WK_ATTR_TYPE?)
    ->    template( type={$WK_ATTR_TYPE.text} )
    "\<wk type=\"<type>\"/\>"
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

    

        
mor
    :    ^(MOR_START MOR_ATTR_TYPE MOR_ATTR_OMITTED? morchoice* gra? morseq*)
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
    :    ^(MORPRE_START MOR_ATTR_TYPE MOR_ATTR_OMITTED? morchoice* gra? morseq*)
    {
        unsupportedWarning();
    }
    ;
    
morpost
    :    ^(MORPOST_START MOR_ATTR_TYPE MOR_ATTR_OMITTED? morchoice* gra? morseq*)
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
	|	tagmarker
	->	template( v={$tagmarker.st} )
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

    

        
tagmarker
	:	^(TAGMARKER_START TAGMARKER_ATTR_TYPE morlist+=mor*)
	->    template(  type={$TAGMARKER_ATTR_TYPE.text},
	                 morcontent={$morlist}
	              )
	<<
\<tagMarker type="<type>"><morcontent; separator="">\</tagMarker\>
>>
	;

    

		
e
	:	^(E_START req=echoice1 (contentlist+=echoice2)*)
	->	template( required={$req.st}, econtent={$contentlist} )
	<<\<e\><required><if(econtent)><econtent; separator=""><endif>\</e\> >>
	;

echoice1
	:	action
	->	template( v={$action.st} )
		"<v>"
	|	happening
	->	template( v={$happening.st} )
		"<v>"
	|	otherspokenevent
	->	template( v={$otherspokenevent.st} )
		"<v>"
	;

echoice2
	:	k
	->	template( v={$k.st} )
		"<v>"
	|	error
	->	template( v={$error.st} )
		"<v>"
	|	r
	->	template( v={$r.st} )
		"<v>"
	|	overlap
	->	template( v={$overlap.st} )
		"<v>"
	|	ga
	->	template( v={$ga.st} )
		"<v>"
	|	duration
	->	template( v={$duration.st} )
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

    

        

otherspokenevent
    :    ^(OTHERSPOKENEVENT_START OTHERSPOKENEVENT_ATTR_WHO w)
    ->    template( who={$OTHERSPOKENEVENT_ATTR_WHO.text}, word={$w.st} )
    <<\<otherSpokenEvent times="<who>"\><word>\</otherSpokenEvent\> >>
    ;


    

        
error
	:	^(ERROR_START et=TEXT?)
	->	template( errtext={$et.text} )
		"\<error\><errtext>\</error\>"
	;

    

        
duration
    :    ^(DURATION_START TEXT)
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

	

		
g	
	:	^(G_START (contentlist+=gele)+ (choicelist+=gchoice)*)
	->	template( content={$contentlist}, choices={$choicelist} )
	<<\<g\><content; separator=""><if(choices)><choices; separator=""><endif>\</g\> >>
	;
	
gele
	:	w 
	->	template( v={$w.st} )
		"<v>"
	|	g
	->	template( v={$g.st} )
		"<v>"
	|	pg
	->	template( v={$pg.st} )
		"<v>"
	|	sg
	|	quotation
	|	quotation2
	|	pause
	->	template( v={$pause.st} )
		"<v>"
	|	internalmedia
	|	freecode
	|	e
	->	template( v={$e.st} )
		"<v>"
	|	s
	->	template( v={$s.st} )
		"<v>"
	|	tagmarker
	->  template( v={$tagmarker.st} )
		"<v>"
	|	longfeature
	|	nonvocal
	|	overlappoint
	|	underline
	->	template( v={$underline.st} )
		"<v>"
	|	italic
	;
	
gchoice
	:	k
	->	template( v={$k.st} )
		"<v>"
	|	error
	->	template( v={$error.st} )
		"<v>"
	|	r 
	->	template( v={$r.st} )
		"<v>"
	|	duration
	|	ga
	->	template( v={$ga.st} )
		"<v>"
	|	overlap
	->	template( v={$overlap.st} )
		"<v>"
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
    :    ^(SW_START TEXT)
    ;

    

        
quotation
    :    ^(QUOTATION_START QUOTATION_ATTR_TYPE mor*)
    ;

    

        
quotation2
    :    ^(QUOTATION2_START QUOTATION2_ATTR_TYPE mor*)
    ;

    

        
internalmedia
    :   ^(INTERNALMEDIA_START internalmedia_attr*)
	;
	
internalmedia_attr
	:	INTERNALMEDIA_ATTR_START
	|	INTERNALMEDIA_ATTR_END
	|	INTERNALMEDIA_ATTR_UNIT
	;

    

        
freecode
    :    ^(FREECODE_START TEXT)
    ;

    

        
nonvocal
    :    ^(NONVOCAL_START NONVOCAL_ATTR_TYPE TEXT)
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

    