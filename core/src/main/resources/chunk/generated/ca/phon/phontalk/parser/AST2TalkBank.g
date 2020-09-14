

        
tree grammar AST2TalkBank;

options
{
	ASTLabelType = CommonTree ;
	tokenVocab = TalkBank2AST ;
	output = template;
	superClass=PhonTalkTreeParser;
}

@header {
package ca.phon.phontalk.parser;

import ca.phon.phontalk.*;

import java.util.logging.*;

}

@members {
private final static Logger LOGGER = Logger.getLogger("ca.phon.phontalk.parser");

private int recordIndex = 0;

public int getRecordNumber() {
	return recordIndex+1;
}

public void reportError(RecognitionException e) {
	throw new TreeWalkerError(e);
}

}


    

		
chat
	:	^(CHAT_START (attrlist+=chat_attrs)+ (partlist=participants) (contentlist+=chat_content)*)
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
	<if(md)><md><endif>
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
	|	begin_gem
	->	template( v={$begin_gem.st} )
		"<v>"
	|	end_gem
	->	template( v={$end_gem.st} )
		"<v>"
	;
	
chat_attrs
	:	CHAT_ATTR_VERSION
	->	template(version={$CHAT_ATTR_VERSION.text})
	<<
Version="<version>" >>
	|	CHAT_ATTR_DATE
	->	template(date={$CHAT_ATTR_DATE.text})
	<<
Date="<date>" >>
	|	CHAT_ATTR_CORPUS
	->	template(corpus={$CHAT_ATTR_CORPUS.text})
	<<
Corpus="<corpus>" >>
	|	CHAT_ATTR_VIDEOS
	->	template(videos={$CHAT_ATTR_VIDEOS.text})
	<<
Videos="<videos>" >>
	|	CHAT_ATTR_MEDIA
	->	template(media={$CHAT_ATTR_MEDIA.text})
	<<
Media="<media>" >>
	|	CHAT_ATTR_MEDIATYPES
	->	template(types={$CHAT_ATTR_MEDIATYPES.text})
	<<
Mediatypes="<types>" >>
	|	CHAT_ATTR_LANG
	->	template(lang={$CHAT_ATTR_LANG.text})
	<<
Lang="<lang>" >>
	|	CHAT_ATTR_OPTIONS
	->	template(opts={$CHAT_ATTR_OPTIONS.text})
	<<
Options="<opts>" >>
	|	CHAT_ATTR_DESIGNTYPE
	->	template(type={$CHAT_ATTR_DESIGNTYPE.text})
	<<
DesignType="<type>" >>
	|	CHAT_ATTR_ACTIVITYTYPE
	->	template(type={$CHAT_ATTR_ACTIVITYTYPE.text})
	<<
ActivityType="<type>" >>
	|	CHAT_ATTR_GROUPTYPE
	->	template(type={$CHAT_ATTR_GROUPTYPE.text})
	<<
GroupType="<type>" >>
	|	CHAT_ATTR_COLORWORDS
	->	template(wrds={$CHAT_ATTR_COLORWORDS.text})
	<<
Colorwords="<wrds>" >>
	|	CHAT_ATTR_WINDOW
	->	template(window={$CHAT_ATTR_WINDOW.text})
	<<
Window="<window>" >>
	|	CHAT_ATTR_ID
	->	template(id={$CHAT_ATTR_ID.text})
	<<
Id="<id>" >>
	|	CHAT_ATTR_PID
	->	template(pid={$CHAT_ATTR_PID.text})
	<<
PID="<pid>" >>
	|	CHAT_ATTR_FONT
	->	template(font={$CHAT_ATTR_FONT.text})
	<<
Font="<font>" >>
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
	|	PARTICIPANT_ATTR_CUSTOM_FIELD
	->	template(custom={$PARTICIPANT_ATTR_CUSTOM_FIELD.text})
		"custom-field=\"<custom>\""	
	;

	

		
comment
	:	^(COMMENT_START ctype=COMMENT_ATTR_TYPE? vals+=commentele*)
	->	template(
			type={$ctype.text},
			vals={$vals}
		)
	<<\<comment type="<type>"\><vals>\</comment\> >>
	;
	
commentele
	:	media
	->	template(v={$media.st})
	"<v>"
	|	TEXT
	->	template(v={$TEXT.text})
	"<v>"
	;

	

        
begin_gem
    :    ^(BEGIN_GEM_START BEGIN_GEM_ATTR_LABEL)
    ->    template(lbl={$BEGIN_GEM_ATTR_LABEL.text})
    <<\<begin-gem label="<lbl>"/\> >>
    ;

    

        
end_gem
    :    ^(END_GEM_START END_GEM_ATTR_LABEL)
    ->    template(lbl={$END_GEM_ATTR_LABEL.text})
    <<\<end-gem label="<lbl>"/\> >>
    ;

    

        
lazy_gem
	:	^(LAZY_GEM_START LAZY_GEM_ATTR_LABEL?)
	->    template( label={$LAZY_GEM_ATTR_LABEL.text} )
	<<\<lazy-gem label="<label>"/\> >>
	;

    

		
u
	:	^(U_START (attrs+=u_attrs)* (links+=linker)* (contentlist+=uele)* t (pcs+=postcode)* media? (annotationlist+=uendele)*)
	->	template(
			attrlist={$attrs},
			ucontent={$contentlist},
			linkers={$links},
			terminator={$t.st},
			postcodes={$pcs},
			segment={$media.st},
			annotations={$annotationlist},
			uttid={recordIndex++}
		)
	<<
\<u <attrlist; separator=" "> uID="u<uttid>"\>
	<linkers; separator="">
	<ucontent; separator="">
	<terminator>
	<pcs; separator="">
	<segment>
	<annotations; separator="">
\</u\>
	>>
	;
	
u_attrs
	:	U_ATTR_WHO
	->	template(who={$U_ATTR_WHO.text})
	<<who="<who>" >>
	|	U_ATTR_LANG
	->	template(lang={$U_ATTR_LANG.text})
	<<xml:lang="<lang>" >>
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
	: 	blob
	->	template( v={$blob.st} )
		"<v>"
	|	quotation
	->	template( v={$quotation.st} )
		"<v>"
	|	quotation2
	->	template( v={$quotation2.st} )
		"<v>"
	|	pause
	->	template( v={$pause.st} )
		"<v>"
	|	internal_media
	->	template( v={$internal_media.st} )
		"<v>"
	|	freecode
	->	template( v={$freecode.st} )
		"<v>"
	|	e
	->	template( v={$e.st} )
		"<v>"
	|	s 
	->	template( v={$s.st} )
		"<v>"
	|	tagmarker
	->	template( v={$tagmarker.st} )
		"<v>"
	|	overlap_point
	->	template( v={$overlap_point.st} )
		"<v>"
	|	underline
	->	template( v={$underline.st} )
		"<v>"
	|	italic
	->	template( v={$italic.st} )
		"<v>"
	|	long_feature
	->	template( v={$long_feature.st} )
		"<v>"
	|	nonvocal
	->	template( v={$nonvocal.st} )
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
	|	sg
	->	template( v={$sg.st} )
		"<v>"
	;
	
uendele
	:	k
	->	template( v={$k.st} )
		"<v>"
	|	error
	->	template( v={$error.st} )
		"<v>"
	|	r
	->	template( v={$r.st} )
		"<v>"
	|	a
	->	template( v={$a.st} )
		"<v>"
	;

	

		
linker
	: 	^(LINKER_START LINKER_ATTR_TYPE)
	->	template( type={$LINKER_ATTR_TYPE.text} )
	<<\<linker type="<type>"/\> >>
	;

	

		
w
	:	^(W_START (wattrlist+=wattr)* langs? (contentlist+=wele)* )
	->	template( content={$contentlist}, l={$langs.st}, wattrs={$wattrlist} )
		"\<w<if(wattrs)> <wattrs; separator=\"\"><endif><if(content)>\><else>/\><endif><if(l)><l><endif><if(content)><content; separator=\"\">\</w\><endif>"
	;
	
wele
	:	TEXT
	->	template( v={$TEXT.text} )
		"<v>"
	|	overlap_point
	->	template( v={$overlap_point.st.toString()} )
		"<v>"
	|	underline
	->	template( v={$underline.st.toString()} )
		"<v>"
	|	italic
	->	template( v={$italic.st.toString()} )
		"<v>"
	|	shortening
	->	template( v={$shortening.st.toString()} )
		"<v>"
	|	p
	->	template( v={$p.st.toString()} )
		"<v>"
	|	long_feature
	->	template( v={$long_feature.st.toString()} )
		"<v>"
	|	wk
	->	template( v={$wk.st.toString()} )
		"<v>"
	|	pos
	->	template( v={$pos.st.toString()} )
		"<v>"
	|	replacement
	->	template( v={$replacement.st.toString()} )
		"<v>"
	|	mor
	->	template( v={$mor.st} )
		"<v>"
	|	mk
	->	template( v={$mk.st} )
		"<v>"
	;
	
wattr
	:	W_ATTR_SEPARATED_PREFIX
	->	template( v={$W_ATTR_SEPARATED_PREFIX.text} )
	<<separated-prefix="<v>" >>
	|	W_ATTR_FORMTYPE
	->	template( v={$W_ATTR_FORMTYPE.text} )
	<<formType="<v>" >>
	|	W_ATTR_TYPE
	->	template( v={$W_ATTR_TYPE.text} )
	<<type="<v>" >>
	|	W_ATTR_USER_SPECIAL_FORM
	->	template( v={$W_ATTR_USER_SPECIAL_FORM.text} )
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

    

        
overlap_point
    :    ^(OVERLAP_POINT_START (attrlist+=overlap_pointattrs)+)
    ->    template(
			attrs={$attrlist}
	)
    <<\<overlap-point <attrs; separator=" "> /\> >>
    ;
    
overlap_pointattrs
    :    OVERLAP_POINT_ATTR_INDEX
    ->    template(idx={$OVERLAP_POINT_ATTR_INDEX})
    "index=\"<idx>\""
    |    OVERLAP_POINT_ATTR_START_END
    ->    template(startEnd={$OVERLAP_POINT_ATTR_START_END})
    "start-end=\"<startEnd>\""
    |    OVERLAP_POINT_ATTR_TOP_BOTTOM
    ->    template(topBottom={$OVERLAP_POINT_ATTR_TOP_BOTTOM})
    "top-bottom=\"<topBottom>\""
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

	

        
long_feature
    :    ^(LONG_FEATURE_START LONG_FEATURE_ATTR_TYPE TEXT)
    ->    template( type={$LONG_FEATURE_ATTR_TYPE.text},
                    val={$TEXT.text} )
    <<\<long-feature type="<type>"\><val>\</long-feature\> >>
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
    :    ^(MOR_START (attrlist+=morattr)+ morchoice (list1+=menx)* (list2+=gra)* (list3+=morseq)*)
    ->    template( attrs={$attrlist}, choice={$morchoice.st}, enxlist={$list1},
            gralist={$list2}, seqlist={$list3} )
    "\<mor <attrs>\><choice><if(enxlist)><enxlist><endif><if(gralist)><gralist><endif><if(seqlist)><seqlist><endif>\</mor\>"
    ;
    
morattr
    :    MOR_ATTR_TYPE
    ->    template ( type={$MOR_ATTR_TYPE.text} )
    <<type="<type>" >>
    |    MOR_ATTR_OMITTED
    ->    template ( om={$MOR_ATTR_OMITTED.text} )
    <<omitted="<om>" >>
    ;
    
morchoice
    :    mw
    ->   template( v={$mw.st} )
         "<v>"
    |    mwc
    ->   template( v={$mwc.st} )
         "<v>"
    |    mt
    ->   template( v={$mt.st} )
         "<v>"
    ;
    
morseq
    :    mor_pre
    ->   template( v={$mor_pre.st} )
         "<v>"
    |    mor_post
    ->   template( v={$mor_post.st} )
         "<v>"
    ;
    
mor_pre
    :    ^(MOR_PRE_START morchoice (list1+=menx)* (list2+=gra)*)
    ->   template( choice={$morchoice.st}, enxlist={$list1}, gralist={$list2} )
    "\<mor-pre\><choice><if(enxlist)><enxlist><endif><if(gralist)><gralist><endif>\</mor-pre\>"
    ;
    
mor_post 
    :    ^(MOR_POST_START morchoice (list1+=menx)* (list2+=gra)*)
    ->   template( choice={$morchoice.st}, enxlist={$list1}, gralist={$list2} )
    "\<mor-post\><choice><if(enxlist)><enxlist><endif><if(gralist)><gralist><endif>\</mor-post\>"
    ;
    
menx
    :    ^(MENX_START txt=TEXT)
    ->    template( v={$txt.text} )
    "\<menx\><v>\</menx\>"
    ;

gra
    :    ^(GRA_START (attrlist+=graattrs)+)
    ->    template( attrs={$attrlist} )
    "\<gra <attrs>/\>"
    ;
    
graattrs
    :    GRA_ATTR_TYPE
    ->    template( type={$GRA_ATTR_TYPE.text} )
    <<type="<type>" >>   
    |    GRA_ATTR_INDEX
    ->    template( index={$GRA_ATTR_INDEX.text} )
    <<index="<index>" >>
    |    GRA_ATTR_HEAD
    ->    template( head={$GRA_ATTR_HEAD.text} )
    <<head="<head>" >>
    |    GRA_ATTR_RELATION
    ->    template( rel={$GRA_ATTR_RELATION.text} )
    <<relation="<rel>" >>
    ;
    
mw
    :   ^(MW_START (list1+=mpfx)* pos stem (list2+=mk)*)
    ->    template( mpfxlist={$list1}, mwp={$pos.st}, mws={$stem.st}, mklist={$list2} )
    "\<mw\><if(mpfxlist)><mpfxlist><endif><mwp><mws><if(mklist)><mklist><endif>\</mw\>"
    ;
    
mwc
    :    ^(MWC_START (list1+=mpfx)* pos (list2+=mw)+)
    ->    template( mpfxlist={$list1}, mwcp={$pos.st}, mwlist={$list2} )
    "\<mwc\><if(mpfxlist)><mpfxlist><endif><mwcp><mwlist>\</mwc\>"
    ;
    
mt
    :    ^(MT_START MT_ATTR_TYPE)
    ->    template( type={$MT_ATTR_TYPE.text} )
    "\<mt type=\"<type>\"/\>"
    ;
    
mpfx
    :    ^(MPFX_START TEXT)
    ->    template( v={$TEXT.text} )
    "\<mpfx\><v>\</mpfx\>"
    ;
   
pos 
    :    ^(POS_START morposc (list1+=morposs)*)
    ->    template( posc={$morposc.st}, slist={$list1} )
    "\<pos\><posc><if(slist)><slist><endif>\</pos\>"
    ;
    
morposc
    :    ^(C_START TEXT)
    ->    template( v={$TEXT.text} )
    "\<c\><v>\</c\>"
    ;
    
morposs
    :    ^(S_START TEXT)
    ->    template( v={$TEXT.text} )
    "\<s\><v>\</s\>"
    ;
    
stem
    :    ^(STEM_START TEXT)
    ->    template( v={$TEXT.text} )
    "\<stem\><v>\</stem\>"
    ;
    
mk 
    :    ^(MK_START MK_ATTR_TYPE TEXT)
    ->    template( type={$MK_ATTR_TYPE.text}, v={$TEXT.text} )
    "\<mk type=\"<type>\"\><v>\</mk\>"
    ;

    

        
blob
    :    ^(BLOB_START content+=TEXT*)
    ->    template( val={$content} )
    <<\<blob\><val>\</blob\> >>
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
	->	template( v={$sg.st} )
		"<v>"
	|	quotation
	->	template( v={$quotation.st} )
		"<v>"
	|	quotation2
	->	template( v={$quotation2.st} )
		"<v>"
	|	pause
	->	template( v={$pause.st} )
		"<v>"
	|	internal_media
	->	template( v={$internal_media.st} )
		"<v>"
	|	freecode
	->	template( v={$freecode.st} )
		"<v>"
	|	e
	->	template( v={$e.st} )
		"<v>"
	|	s
	->	template( v={$s.st} )
		"<v>"
	|	tagmarker
	->  template( v={$tagmarker.st} )
		"<v>"
	|	long_feature
	->	template( v={$long_feature.st} )
		"<v>"
	|	nonvocal
	->	template( v={$nonvocal.st} )
		"<v>"
	|	overlap_point
	->	template( v={$overlap_point.st} )
		"<v>"
	|	underline
	->	template( v={$underline.st} )
		"<v>"
	|	italic
	->	template( v={$italic.st} )
		"<v>"
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
	|	quotation
	->	template( v={$quotation.st} )
		"<v>"
	|	quotation2
	->	template( v={$quotation2.st} )
		"<v>"
	|	pause
	->	template( v={$pause.st} )
		"<v>"
	|	internal_media
	->	template( v={$internal_media.st} )
		"<v>"
	|	freecode
	->	template( v={$freecode.st} )
		"<v>"
	|  	e
	->	template( v={$e.st} )
		"<v>"
	|	s
	->	template( v={$s.st} )
		"<v>"
	|	tagmarker
	->	template( v={$tagmarker.st} )
		"<v>"
	|	long_feature
	->	template( v={$long_feature.st} )
		"<v>"
	|	nonvocal
	->	template( v={$nonvocal.st} )
		"<v>"
	|	overlap_point
	->	template( v={$overlap_point.st} )
		"<v>"
	|	underline
	->	template( v={$underline.st} )
		"<v>"
	|	italic
	->	template( v={$italic.st} )
		"<v>"
	;

    

        
quotation
    :    ^(QUOTATION_START QUOTATION_ATTR_TYPE mor*)
    ;

    

        
quotation2
    :    ^(QUOTATION2_START QUOTATION2_ATTR_TYPE mor*)
    ;

    

        
pause
	:	^(PAUSE_START PAUSE_ATTR_SYMBOLIC_LENGTH?)
	->    template( len={$PAUSE_ATTR_SYMBOLIC_LENGTH} )
	"\<pause symbolic-length=\"<len>\"/\>"
	;

    

        
internal_media
    :   ^(INTERNAL_MEDIA_START (attrlist+=internal_media_attr)*)
	->	template(
			attrs={$attrlist}
	)
	<<\<internal-media <attrs; separator=" "> /\> >>
	;
	
internal_media_attr
	:	INTERNAL_MEDIA_ATTR_START
	->	template(start={$INTERNAL_MEDIA_ATTR_START})
		"start=\"<start>\""
	|	INTERNAL_MEDIA_ATTR_END
	->	template(end={$INTERNAL_MEDIA_ATTR_END})
		"end=\"<end>\""
	|	INTERNAL_MEDIA_ATTR_UNIT
	->	template(unit={$INTERNAL_MEDIA_ATTR_UNIT})
		"unit=\"<unit>\""
	;

    

        
freecode
    :    ^(FREECODE_START TEXT)
    ->    template( txt={$TEXT.text} )
    <<\<freecode\><txt>\</freecode\> >>
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
    <<\<action<if(v)>\><v>\</action\><else>/\><endif> >>
    ;

    

        
happening
	:	^(HAPPENING_START TEXT?)
	->    template( v={$TEXT.text} )
	<<\<happening\><v>\</happening\> >>
	;

    

        
ga
	:	^(GA_START GA_ATTR_TYPE? TEXT?)
	->    template(
	        type={$GA_ATTR_TYPE.text},
	        val={EscapeUtils.unescapeParenthesis($TEXT.text)}
	      )
	<<\<ga type="<type>"\><val>\</ga\> >>
	;


    

        
overlap
	:	^(OVERLAP_START (attrlist+=overlap_attr)*)
	->    template( attrs={$attrlist} )
	"\<overlap <attrs; separator=\" \">/\>"
	;
	
overlap_attr
	:	OVERLAP_ATTR_TYPE
	->    template( type={$OVERLAP_ATTR_TYPE.text})
	"type=\"<type>\""
	|	OVERLAP_ATTR_INDEX
	->    template( idx={$OVERLAP_ATTR_INDEX.text})
	"index=\"<idx>\""
	;

    

        

otherspokenevent
    :    ^(OTHERSPOKENEVENT_START (attrlist+=otherspokenevent_attr)*)
    ->    template( attrs={$attrlist} )
    "\<otherSpokenEvent <attrs; separator=\"\">/\>"
    ;
    
otherspokenevent_attr
	:	OTHERSPOKENEVENT_ATTR_WHO
	-> template(who={$OTHERSPOKENEVENT_ATTR_WHO.text})
	<<who="<who>" >>
	|	OTHERSPOKENEVENT_ATTR_SAID
	-> template(said={$OTHERSPOKENEVENT_ATTR_SAID.text})
	<<said="<said>" >>
	;


    

        
k
	:    ^(K_START K_ATTR_TYPE?)
	->    template( type={$K_ATTR_TYPE} )
	<<\<k type="<type>"/\> >>
	;

    

        
error
	:	^(ERROR_START et=TEXT?)
	->	template( errtext={$et.text} )
		"\<error\><errtext>\</error\>"
	;

    

        
duration
    :    ^(DURATION_START TEXT)
    ;

    

		
s
	:	^(S_START S_ATTR_TYPE? TEXT?)
	->	template(
			type={$S_ATTR_TYPE.text},
			val={$TEXT.text}
		)
	"\<s type=\"<type>\"\><val>\</s\>"
	;

	

        
tagmarker
	:	^(TAGMARKER_START TAGMARKER_ATTR_TYPE (morlist+=mor)*)
	->    template(  type={$TAGMARKER_ATTR_TYPE.text},
	                 morcontent={$morlist}
	              )
	"\<tagMarker type=\"<type>\"\><if(morcontent)><morcontent><endif>\</tagMarker\>"
	;

    

        
nonvocal
    :    ^(NONVOCAL_START NONVOCAL_ATTR_TYPE TEXT)
    ->    template( type={$NONVOCAL_ATTR_TYPE.text},
                    val={$TEXT.text} )
    <<\<nonvocal type="<type>"\><val>\</nonvocal\> >>
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
	|	PH_ATTR_HIATUS
	->	template( v = {$PH_ATTR_HIATUS.text} )
	<<hiatus="<v>">>
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

    

        
r
	:	^(R_START R_ATTR_TIMES?)
	->    template( times={$R_ATTR_TIMES.text} )
	<<\<r times="<times>"/\> >>
	;

    

        
t
	:	^(T_START T_ATTR_TYPE? m=mor?)
	->    template( type={$T_ATTR_TYPE}, morval={$m.st} )
	<<\<t type="<type>"<if(morval)>\><morval>\</t\><else>/\><endif> >>
	;

    

		
postcode
	:	^(POSTCODE_START TEXT?)
	->	template( v={$TEXT.text} )
	<<\<postcode\><v>\</postcode\> >>
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

	