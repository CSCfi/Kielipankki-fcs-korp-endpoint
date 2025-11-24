# fcs-korp-endpoint
A CLARIN-FCS 2.0 endpoint that forwards SRU/FCS queries to Kielipankki's Korp service.  

## Quick start

Call `mvn clean package -D maven.test.skip=true war:war` to create a war file.  
Use `mvn clean package -Dconfig.file=test-config.properties war:war` to do a full build with tests, war, sources and javadoc.

## Kielipankki quick start

Steps to get a local instance running on an apt-based distribution:

```bash
# Install dependencies
sudo apt install maven openjdk-21-jdk tomcat10 tomcat-jakartaee-migration

# Open the project's directory
cd Kielipankki-fcs-korp-endpoint

# Build the WAR file with Maven
mvn clean package -D maven.test.skip=true war:war
# OR run tests with test config:
mvn clean package -Dconfig.file=test-config.properties war:war

# Migrate the war file from javax to jakarta (necessary for Tomcat11)
/usr/bin/javax2jakarta target/fcs-korp-endpoint-1.0-kp.war target/fcs-korp-endpoint-1.0-kp-MIGRATED.war

# Copy WAR into Tomcat webapps
sudo cp ./target/fcs-korp-endpoint-1.0-kp-MIGRATED.war /var/lib/tomcat10/webapps/fcs-korp.war

#Start Tomcat
sudo systemctl start tomcat10

# Test with eg.
curl -s "http://localhost:8080/fcs-korp/sru?queryType=fcs&query=%5Bword%20%3D%20%27bastun%27%20%26%20lemma%20%3D%20%27bastu%27%20%26%20pos%20%3D%20%27NOUN%27%5D&x-fcs-context=urn:nbn:fi:lb-2016050301_1866-1905"

# (Optional) Check logs, last 50 lines
sudo journalctl -u tomcat10 -n 50
```


## Key features
- **Bidirectional POS translation.** UD‑17 → corpus tagset is applied when POS is used in multi-layer queries; corpus tagset → UD‑17 happens for every result before it is returned;
- **Tagset aware lemma matching.** 'contains' for SUC, strict string equality for TDT;
- **Corpus metadata loader.** reading supported_corpora.json, which is the ultimate source of truth for PIDs, the list of corpora within them, and the tagsets;
- **Single-PID enforcement from FCS.** `x-fcs-context` must be present and contain one PID;
- **Separate environment configs.** `config.properties` (production) and `test-config.properties` (tests);
- **Supported layers.** `word`, `lemma`, and `pos` across all corpora described in the metadata file;
- **Error logging.**


## Extending
- **Add a corpus/PID:** edit `supported_corpora.json` with the PID, corpus IDs, and tagset; ensure the corpora are actually available in Korp.
- **New tagset:** implement a new `PosTranslator`, register it in `TranslatorChooser.java`, and reference the tagset in `supported_corpora.json`.
- If in the new tagset lemmas are annotated as a list (like in SUC), adjust `boolean useContains` in `FCSToCQPConverter.java`.


## Architecture overview
```
SRU/FCS client
      │  (query + x-fcs-context)
      ▼
┌────────────────────────────┐
│ KorpEndpointSearchEngine   │
│  • parse request           │
│  • enforce single PID      │
│  • select corpora/tagset   │
└──────────┬─────────────────┘
           │ uses metadata
           │
           │
           ▼
┌────────────────────────────┐
│ CorpusTagsetMapper         │
│  • supported_corpora.json  │
└──────────┬─────────────────┘
           │ tagset
           ▼
┌────────────────────────────┐
│ TranslatorChooser          │
│  → PosTranslator (SUC/TDT) │
└──────────┬─────────────────┘
           │ translator
           ▼
┌────────────────────────────┐
│ FCSToCQPConverter          │
│  • build CQP               │
│  • translate UD→corpus POS │
└──────────┬─────────────────┘
           │ cqp + corpus IDs
           ▼
      Korp REST API (JSON hits)
           │
           ▼
┌────────────────────────────┐
│ KorpSRUSearchResultSet     │
│  • iterate KWIC matches    │
│  • translate POS back to   │
│    UD-17                   │
│  • emit CLARIN-FCS XML     │
└──────────┬─────────────────┘
           │
           ▼
  SRU/FCS response to client
```

`endpoint-description.xml` tells FCS which corpora are available for search and describes them (title, langauge, layers)