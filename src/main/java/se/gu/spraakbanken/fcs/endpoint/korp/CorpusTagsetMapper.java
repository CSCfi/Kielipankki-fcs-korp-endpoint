/*
 * Loads the supported_corpora.json once and provides two methods:
 * getTagset(corpusId) - returns the tagset for a corpus
 * groupByTagset(CorpusIds) - takes a list of corpora to search in and retruns a map:
 *  tagset : [list of corpora in this tagset]
 *  tagset : [list of corpora in this tagset]
 */

package se.gu.spraakbanken.fcs.endpoint.korp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.gu.spraakbanken.fcs.endpoint.korp.data.json.CorpusMetadataLoader;
import se.gu.spraakbanken.fcs.endpoint.korp.data.json.pojo.CorpusMetadata;

public final class CorpusTagsetMapper {

    private static final Logger LOG = LoggerFactory.getLogger(CorpusTagsetMapper.class);

    // the corpusID->tagset mapping
    private static final Map<String, String> CORPUS_TO_TAGSET = loadMapping();

    private CorpusTagsetMapper() {
        // prevent instantiation - we never need an object of this class, so other code can't call new CorpusTagsetMapper()
    }

    private static Map<String, String> loadMapping() {
        Map<String, String> corpusToTagset = new LinkedHashMap<>();
        CorpusMetadataLoader loader = new CorpusMetadataLoader();
        try {
            Map<String, CorpusMetadata> metadata = loader.loadFromClasspath();
            if (metadata != null) {
                for (CorpusMetadata corpusMetadata : metadata.values()) {
                    if (corpusMetadata.getCorpora() == null) {
                        continue;
                    }
                    for (String corpusId : corpusMetadata.getCorpora()) {
                        corpusToTagset.put(corpusId, corpusMetadata.getTagset());
                    }
                }
            }
        } catch (IOException e) {
            LOG.error("Failed to load supported_corpora.json", e);
        }
        return Collections.unmodifiableMap(corpusToTagset);
    }

    public static String getTagset(String corpusId) {
        if (corpusId == null) {
            return null;
        }
        return CORPUS_TO_TAGSET.get(corpusId);
    }

    public static Map<String, List<String>> groupByTagset(Collection<String> corpusIds) {
        Map<String, List<String>> groups = new LinkedHashMap<>();
        if (corpusIds == null) {
            return groups;
        }

        for (String corpusId : corpusIds) {
            if (corpusId == null) {
                continue;
            }
            String tagset = getTagset(corpusId);
            if (tagset == null) {
                LOG.warn("No tagset metadata found for corpus '{}'; ignoring it for grouping.", corpusId);
                continue;
            }
            groups.computeIfAbsent(tagset, key -> new ArrayList<>()).add(corpusId);
        }
        return groups;
    }
}