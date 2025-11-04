/*
Loads the supported_corpora.json once and provides methods:
    getTagsetForCorpus(corpusId) - returns the tagset for a corpus, used for POS translation in KorpSRUSearchResultSet
    getTagsetForPid(String)} - same for Pid
    groupByPid(CorpusIds) - takes a list of corpora to search in and retruns a map pid:[list of corpusIDs in that PID]
    getCorporaForPid(String pid) – expand a PID to the list of corpus IDs it contains.

If any corpus metadata is missing, it throws an exception. 
*/

package se.gu.spraakbanken.fcs.endpoint.korp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import se.gu.spraakbanken.fcs.endpoint.korp.data.json.CorpusMetadataLoader;
import se.gu.spraakbanken.fcs.endpoint.korp.data.json.pojo.CorpusMetadata;
import eu.clarin.sru.server.SRUConstants;
import eu.clarin.sru.server.SRUException;

public final class CorpusTagsetMapper {

private static final Map<String, CorpusMetadata> PID_TO_METADATA = loadMetadata();
private static final Map<String, String> CORPUS_TO_PID = buildCorpusToPid(PID_TO_METADATA);
private static final Map<String, String> CORPUS_TO_TAGSET = buildCorpusToTagset(PID_TO_METADATA);

private CorpusTagsetMapper() {
        // prevent instantiation - we never need an object of this class, so other code can't call new CorpusTagsetMapper()
    }

    private static Map<String, CorpusMetadata> loadMetadata() {
        CorpusMetadataLoader loader = new CorpusMetadataLoader();
        try {
            Map<String, CorpusMetadata> loaded = loader.loadFromClasspath();
            if (loaded == null || loaded.isEmpty()) {
                throw new IllegalStateException("supported_corpora.json is empty or missing PID entries.");
            }
            return Collections.unmodifiableMap(new LinkedHashMap<>(loaded));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load supported_corpora.json", e);
        }

    }

    private static Map<String, String> buildCorpusToPid(Map<String, CorpusMetadata> metadata) {
        Map<String, String> map = new LinkedHashMap<>();
        for (Map.Entry<String, CorpusMetadata> entry : metadata.entrySet()) {
            String pid = entry.getKey();
            CorpusMetadata corpusMetadata = entry.getValue();
            if (corpusMetadata.getCorpora() == null || corpusMetadata.getCorpora().isEmpty()) {
                throw new IllegalStateException("PID '" + pid + "' has no corpora defined.");
            }
            for (String corpusId : corpusMetadata.getCorpora()) {
                if (map.containsKey(corpusId)) {
                    throw new IllegalStateException(
                            "Corpus '" + corpusId + "' is listed under multiple PIDs.");
                }
                map.put(corpusId, pid);
            }
        }
        return Collections.unmodifiableMap(map);
    }

    private static Map<String, String> buildCorpusToTagset(Map<String, CorpusMetadata> metadata) {
        Map<String, String> map = new LinkedHashMap<>();
        for (Map.Entry<String, CorpusMetadata> entry : metadata.entrySet()) {
            String pid = entry.getKey();
            CorpusMetadata corpusMetadata = entry.getValue();
            String tagset = corpusMetadata.getTagset();
            if (tagset == null || tagset.isEmpty()) {
                throw new IllegalStateException("PID '" + pid + "' does not define a tagset.");
            }
            for (String corpusId : corpusMetadata.getCorpora()) {
                map.put(corpusId, tagset);
            }
        }
        return Collections.unmodifiableMap(map);
    }

    public static Map<String, List<String>> groupByPid(Collection<String> corpusIds) {
        if (corpusIds == null) {
            throw new IllegalArgumentException("corpusIds must not be null when grouping by PID.");
        }
        Map<String, List<String>> groups = new LinkedHashMap<>();
        for (String corpusId : corpusIds) {
            String pid = CORPUS_TO_PID.get(corpusId);
            if (pid == null) {
                throw new IllegalStateException(
                        "Corpus '" + corpusId + "' is not present in supported_corpora.json.");
            }
            groups.computeIfAbsent(pid, key -> new ArrayList<>()).add(corpusId);
        }
        return groups;
    }

    public static String getTagsetForPid(String pid) {
        CorpusMetadata metadata = PID_TO_METADATA.get(pid);
        if (metadata == null) {
            throw new IllegalStateException("PID '" + pid + "' is not in supported_corpora.json.");
        }
        return metadata.getTagset();
    }

    public static String getTagsetForCorpus(String corpusId) {
        String tagset = CORPUS_TO_TAGSET.get(corpusId);
        if (tagset == null) {
            throw new IllegalStateException("Corpus '" + corpusId + "' is not in supported_corpora.json.");
        }
        return tagset;
    }

    public static List<String> getCorporaForPid(String pid) throws SRUException {
        CorpusMetadata metadata = PID_TO_METADATA.get(pid);
        if (metadata == null) {
            throw new SRUException(
                    SRUConstants.SRU_CANNOT_PROCESS_QUERY_REASON_UNKNOWN,
                    "PID '" + pid + "' from x-fcs-context is not in supported_corpora.json.");
        }
        return Collections.unmodifiableList(new ArrayList<>(metadata.getCorpora()));
    }

}
