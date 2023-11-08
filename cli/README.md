# PhonTalk CLI

## Installation

Download current version of the command line interface here.

Extract contents of .zip to a folder of your choice, then optionally set up the following alias:

```
alias phontalk="java -jar /path/to/phontalk-cli-<version>.jar"
```

## Usage info (-h)

```

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

## Produce XML Fragments

PhonTalk may produce XML Fragments for utterances as an alternate mode to converting files.

### Example: Produce utterance fragment

```
phontalk -u "hello world ."
```

Output:
```
   
```

Adding ```-m``` will produce formatted output:

```
phontalk -u "[- eng] <hello world> [!!] goodbye sanity !" -m
```

Output:
```

```

Additional tiers may be supplied using ```-mod```, ```-pho```, ```-mor```, ```-gra```,
```-trn```, and ```-grt```.

### Example: Produce utterance fragment with ipa data

### Example: Produce utterance fragment with syllabified ipa data

### Example: Auto-transcribe ipa tiers

> This feature is not yet implemented.

```
phontalk -u "this is a test ." -dict eng -amod -apho
```

### Example: Produce utterance fragment with mor data

