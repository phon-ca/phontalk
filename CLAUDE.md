# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

PhonTalk converts between [Phon](https://www.phon.ca) session XML and [TalkBank](http://www.talkbank.org) CHAT XML formats. It is distributed with Phon and also provides a standalone CLI. Licensed under GPLv3.

## Build Commands

```bash
mvn package                          # Build all modules
mvn test                             # Run all tests
mvn test -pl core                    # Run core module tests only
mvn test -pl core -Dtest=RoundTripTests  # Run a specific test class
```

Requires Java 21 and Maven. Dependencies from GitHub Packages (TalkBank schema, Phon libraries) require a configured `~/.m2/settings.xml` with GitHub token access.

## Architecture

### Module Structure

- **core** (`phontalk-core`) — Conversion engine. All format reading/writing lives here.
- **cli** (`phontalk-cli`) — Command-line interface. Entry point: `ca.phon.phontalk.cli.Main`.
- **plugin** (`phontalk-plugin`) — Phon application plug-in. Registers `SessionReader`/`SessionWriter` implementations via `@SessionIO` annotations and Phon's `IPluginExtensionPoint` SPI.

### Core Conversion Pipeline

All conversions flow through Phon's `Session` object as the intermediate representation:

- **TalkBank XML → Phon Session**: `TalkbankReader` uses StAX (`XMLStreamReader`) to parse TalkBank XML into a `Session`. Key method: `readFile()`/`readStream()`.
- **Phon Session → TalkBank XML**: `TalkbankWriter` uses StAX (`XMLStreamWriter`) to serialize a `Session` to TalkBank XML. Uses `RecordXmlStreamWriter` (inner class) to inject utterance attributes (`who`, `uID`), media segments, dependent tiers, IPA syllabification, and alignment data during XML stream transformation.
- **TalkBank XML → Phon XML**: `Xml2PhonConverter` reads via `TalkbankReader`, validates with `TalkbankValidator`, then writes using Phon's `SessionWriter`.
- **Phon XML → TalkBank XML**: `Phon2XmlConverter` reads via Phon's `SessionReader`, then writes using `TalkbankWriter`.
- **CHAT (.cha) ↔ TalkBank XML**: `CHATRunner` shells out to `bin/chatter.jar` (TalkBank's Chatter tool) as an external process.

### XML Processing Pattern

The codebase heavily uses **StAX streaming with XML transformers**. A common pattern is:
1. Generate XML fragments from Phon data models using `XMLFragments.toXml()`
2. Pipe through `StAXSource` → `Transformer` → `StAXResult` with a custom `DelegatingXMLStreamWriter` to rewrite elements on the fly
3. `MorRewriter` (inner class of `TalkbankReader`) rewrites TalkBank `pos/s` elements to Phon `subc` elements and vice versa

### Key Data Types

- **Orthography** — Structured utterance text with terminators, markers, groups, and postcodes
- **MorTierData** / **GraspTierData** — Morphological analysis and grammatical relation tiers
- **IPATranscript** — Phonetic transcription with syllable structure
- **TierData** — Generic tier content (comments, dependent tiers)
- **UserTierType** — Enum mapping between Phon tier names and CHAT tier names (e.g., `%mor`, `%gra`, `%trn`, `%grt`, `%wor`)

### Namespaces

- Phon: `https://phon.ca/ns/session`
- TalkBank: `http://www.talkbank.org/ns/talkbank`
- Legacy Phon: `http://phon.ling.mun.ca/ns/phonbank`

### Tests

Round-trip tests in `core/src/test/java/.../tests/RoundTripTests.java` read TalkBank XML, convert to `Session`, write back to TalkBank XML, and verify XML equivalence using xmlunit. Test input files are in `core/src/test/resources/.../RoundTripTests/good-xml/`. There are also corpus-specific test classes (`RoundTripTestsAphasiaBank`, `RoundTripTestsFluencyBank`).

### Version Coupling

The project version (`4.0.0-3.2.4-SNAPSHOT`) encodes both the Phon version (`4.0.0`) and the Chatter/TalkBank schema version (`3.2.4`). The TalkBank XML output version is set in `TalkbankWriter.TB_VERSION`.
