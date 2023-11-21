# PhonTalk CLI

## Installation

Download current version of the command line interface here.

Extract contents of .zip to a folder of your choice, then optionally set up the following alias:

```
alias phontalk="java -jar /path/to/phontalk-cli-<version>.jar"
```

## Usage info (-h)

```
usage: java -jar phontalk-cli.jar [options]
 -f,--format <arg>          Output format.  Options are 'phon' or
                            'talkbank'. When converting files this option
                            may override default behaviour.  Default output
                            format for xml fragments is talkbank.
 -gra <arg>                 Add %gra tier to utterance. Requires -mor
 -grt <arg>                 Add %grt tier to utterance. Requires -trn
 -h,--help                  Show usage info
 -i,--inputFile <arg>       Input xml file, start element must be one of:
                            * {https://phon.ca/ns/session}session - output
                            will be an xml file with start element
                            {http://www.talkbank.org/ns/talkbank}CHAT
                            * {http://www.talkbank.org/ns/talkbank}CHAT -
                            output will be an xml file with start element
                            {https://phon.ca/ns/session}session
                            Output format may be overridden using -f.
 -lsb,--list-syllabifiers   List available syllabifier languages and exit.
                            This supersedes all other options.
 -m,--formatted             Output formatted xml.
 -mod <arg>                 Add ipa transcript for %mod tier. Requires -u
 -mor <arg>                 Add %mor tier to utterance. Requires -u
 -n,--namespace             Include namespace in xml fragments. Requires
                            -u.
 -o,--outputFile <arg>      Output file, required when using -f
 -pho <arg>                 Add ipa transcript for %. Requires -u
 -sb,--syllabifier <arg>    Syllabifier language, if provided IPA data
                            will be 'syllabified' before
                            xml fragment is produced
 -trn <arg>                 Add %trn tier to utterance. Requires -u
 -u <arg>                   Produce xml fragment for main line utterance.
                            May be combined with -mod -pho -mor -gra -trn
                            -grt
```

## Convert Between TalkBank and Phon xml formats

In the following examples ```tb.xml``` is a file in TalkBank xml format (CHAT version 2.20.0/2.21.0.)

### Example: Convert TalkBank -> Phon (session version 2.0)
```
phontalk -i tb.xml -o tb-phon.xml
```

### #Example: Convert  Phon -> TalkBank (CHAT version 2.21.0)
```
phontalk -i tb-phon.xml -o tb-phon-tb.xml
```

### Example: Force output format, Talkbank (2.20.0) -> TalkBank (2.21.0)
```
phontalk -i tb.xml -o tb-tb.xml -f talkbank
```

### Example: Force output format, Phon (1.2) -> Phon (2.0)
```
phontalk -i phon.xml -o phon-phon.xml -f phon
```

## XML Fragments

PhonTalk may produce XML Fragments for utterances as an alternate mode to converting files.

### Example: Produce utterance fragment

```
phontalk -u "[- eng] <hello world> [/] goodbye sanity ."
```

Output:
```
<u xml:lang="eng"><g><w>hello</w><w>world</w><k type="retracing"/></g><w>goodbye</w><w>sanity</w><t type="p"/></u>
```

Adding ```-m``` will produce formatted output:

```
phontalk -m -u "[- eng] <hello world> [/] goodbye sanity ."
```

Output:
```
<u xml:lang="eng">
  <g><w>hello</w><w>world</w><k type="retracing"/></g>
  <w>goodbye</w>
  <w>sanity</w>
  <t type="p"/>
</u>
```

Additional tiers may be supplied using ```-mod```, ```-pho```, ```-mor```, ```-gra```,
```-trn```, and ```-grt```.

### Example: Produce utterance fragment with mor and ipa data

```
phontalk -m -u "‹Mommy [?]› ." \
            -mor "n:prop|Mommy ." \
            -gra "1|0|INCROOT 2|1|PUNCT" \
            -pho "ˈɑmɪ" \
            -mod "ˈmɑmiː"
```

Output:
```
<u>
  <pg><g><w>Mommy<mor type="mor"><mw><pos><c>n</c><subc>prop</subc></pos><stem>Mommy</stem></mw><gra type="gra" index="1" head="0" relation="INCROOT"></gra></mor></w><k type="best guess"></k></g><mod><pw><stress type="primary"></stress><ph><base>m</base></ph><ph><base>ɑ</base></ph><ph><base>m</base></ph><ph><base>i</base><phlen>ː</phlen></ph></pw></mod><pho><pw><stress type="primary"></stress><ph><base>ɑ</base></ph><ph><base>m</base></ph><ph><base>ɪ</base></ph></pw></pho></pg>
  <t type="p"><mor type="mor"><mt type="p"></mt><gra type="gra" index="2" head="1" relation="PUNCT"></gra></mor></t>
</u>
```

### Example: Produce utterance fragment with syllabified ipa data

```
phontalk -m -u "‹Mommy [?]› ." \
            -pho "ˈɑmɪ" \
            -mod "ˈmɑmiː" \
            -sb eng
```

Output: (scType annotations have been specified)
```
<u>
  <pg><g><w>Mommy</w><k type="best guess"></k></g><mod><pw><stress type="primary"></stress><ph scType="onset"><base>m</base></ph><ph scType="nucleus"><base>ɑ</base></ph><ph scType="onset"><base>m</base></ph><ph scType="nucleus"><base>i</base><phlen>ː</phlen></ph></pw></mod><pho><pw><stress type="primary"></stress><ph scType="nucleus"><base>ɑ</base></ph><ph scType="onset"><base>m</base></ph><ph scType="nucleus"><base>ɪ</base></ph></pw></pho></pg>
  <t type="p"></t>
</u>
```

### Example: List available syllabifier languages

```
phontalk -lsb
```

Output:
```
Available syllabifiers:  ara arn cat-simple cat cmn cre-simple cre deu-ambi deu-simple deu ell eng-ambi eng-simple eng-smith eng-smith-simple eng fas fas fra-simple fra gue isl-simple isl ita jbe nld-ambi nld-clpf-ambi nld-clpf nld-simple nld pol por slk slv swe tsn urd yue vie zxx
```