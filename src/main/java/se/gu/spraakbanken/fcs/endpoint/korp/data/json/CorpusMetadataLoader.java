// Loads the json with metadata about supported corpora
package se.gu.spraakbanken.fcs.endpoint.korp.data.json;
import se.gu.spraakbanken.fcs.endpoint.korp.data.json.pojo.CorpusMetadata;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class CorpusMetadataLoader {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public Map<String, CorpusMetadata> loadFromClasspath() throws IOException {
        try (InputStream in = getClass().getResourceAsStream(
                "/se/gu/spraakbanken/fcs/endpoint/korp/data/json/supported_corpora.json")) {
            if (in == null) {
                throw new IllegalStateException("supported_corpora.json not found on classpath");
            }
            return MAPPER.readValue(in, new TypeReference<Map<String, CorpusMetadata>>() {});
        }
    }

    public Map<String, CorpusMetadata> loadFromString(String json) throws IOException {
        return MAPPER.readValue(json, new TypeReference<Map<String, CorpusMetadata>>() {});
    }
}