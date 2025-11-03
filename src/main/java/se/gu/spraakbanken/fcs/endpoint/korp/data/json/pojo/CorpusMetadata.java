// defines the structure of the corpus metadata POJO (which isread from supported_corpora.json)
package se.gu.spraakbanken.fcs.endpoint.korp.data.json.pojo;
import java.util.List;

public class CorpusMetadata {

        private String tagset;
        private List<String> corpora;

        // Getters and setters necessary to allow Jackson to read and assign values to the fields tagset, lang, corpora
        public String getTagset() { return tagset; }
        public void setTagset(String tagset) { this.tagset = tagset; }
        public List<String> getCorpora() { return corpora; }
        public void setCorpora(List<String> corpora) { this.corpora = corpora; }

}