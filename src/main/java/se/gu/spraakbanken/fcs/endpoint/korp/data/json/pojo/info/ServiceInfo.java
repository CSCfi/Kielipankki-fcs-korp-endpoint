package se.gu.spraakbanken.fcs.endpoint.korp.data.json.pojo.info;

import java.util.Set;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.net.MalformedURLException;
import java.net.URL;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import se.gu.spraakbanken.fcs.endpoint.korp.Config;
import se.gu.spraakbanken.fcs.endpoint.korp.data.json.CorpusMetadataLoader;
import se.gu.spraakbanken.fcs.endpoint.korp.data.json.pojo.CorpusMetadata;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
	"version",
	"cqp_version",
	"corpora",
	"protected_corpora",
	"time"
})

public class ServiceInfo {
    
    @JsonProperty("corpora")
    private List<String> corpora = new ArrayList<String>();
    @JsonProperty("version")
    private String korpAPIVersion;
    @JsonProperty("cqp_version")
    private String cqpVersion;
    @JsonProperty("protected_corpora")
    private List<String> protectedCorpora = new ArrayList<String>();
    @JsonProperty("time")
    private Double time;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();
    
    private static final List<String> KORP_CORPORA;
    static {
        try {
            CorpusMetadataLoader loader = new CorpusMetadataLoader();
            List<String> list = new ArrayList<>();
            // Flatten all corpora from supported_corpora.json
            for (CorpusMetadata m : loader.loadFromClasspath().values()) {
            if (m.getCorpora() != null) {list.addAll(m.getCorpora());}
            }
            if (list.isEmpty()) {
            throw new IllegalStateException("supported_corpora.json contains no corpora");
            }
            KORP_CORPORA = Collections.unmodifiableList(list);
            // Logging print:
            System.out.println("KORP_CORPORA loaded: " + KORP_CORPORA.size());
        } catch (IOException e) {
        throw new IllegalStateException("Cannot read supported_corpora.json", e);
        }
    }

    private static final List<String> KORP_PROTECTED_CORPORA = Collections.unmodifiableList(Arrays.asList());


    /**
     *
     * @return
     * The corpora
     */
    @JsonProperty("corpora")
    public List<String> getCorpora() {
	return corpora;
    }

    /**
     *
     * @param corpora
     * The corpora
     */
    @JsonProperty("corpora")
    public void setCorpora(List<String> corpora) {
	this.corpora = corpora;
    }

    /**
     *
     * @return
     * The korpAPIVersion
     */
    @JsonProperty("version")
    public String getKorpAPIVersion() {
	return korpAPIVersion;
    }

    /**
     *
     * @param korpAPIVersion
     * The korpAPIversion
     */
    @JsonProperty("version")
    public void setKorpAPIVersion(String korpAPIVersion) {
	this.korpAPIVersion = korpAPIVersion;
    }

    /**
     *
     * @return
     * The cqpVersion
     */
    @JsonProperty("cqp_version")
    public String getCqpVersion() {
	return cqpVersion;
    }

    /**
     *
     * @param cqpVersion
     * The cqp-version
     */
    @JsonProperty("cqp_version")
    public void setCqpVersion(String cqpVersion) {
	this.cqpVersion = cqpVersion;
    }

    /**
     *
     * @return
     * The protectedCorpora
     */
    @JsonProperty("protected_corpora")
    public List<String> getProtectedCorpora() {
	return protectedCorpora;
    }

    /**
     *
     * @param protectedCorpora
     * The protected_corpora
     */
    @JsonProperty("protected_corpora")
    public void setProtectedCorpora(List<String> protectedCorpora) {
	this.protectedCorpora = protectedCorpora;
    }

    /**
     *
     * @return
     * The time
     */
    @JsonProperty("time")
    public Double getTime() {
	return time;
    }

    /**
     *
     * @param time
     * The time
     */
    @JsonProperty("time")
    public void setTime(Double time) {
	this.time = time;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
	return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
	this.additionalProperties.put(name, value);
    }

    public static List<String> getOpenCorporaLive() {
        ObjectMapper mapper = new ObjectMapper();

	ServiceInfo si = null;

	//final String wsString = "https://ws.spraakbanken.gu.se/ws/korp/v8/info";
	//final String queryString = "";

	// is this v8?
	final String wsString = "https://www.kielipankki.fi/korp/api8/?";
	final String queryString = "command=info";


        try {
	    URL korp = new URL(wsString + queryString);

            si = mapper.reader(ServiceInfo.class).readValue(korp.openStream());
        } catch (JsonParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JsonMappingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        List<String> openCorpora = new ArrayList<String>();
	boolean isPC = false;
	for (String corpus : si.getCorpora()) {
	    for (String pCorpus : si.getProtectedCorpora()) {
	        if (corpus.equals(pCorpus)) {
		    isPC = true;
		}
	    }
	    if (!isPC) {
		openCorpora.add(corpus);
	    }
	    isPC = false;
	}
	return openCorpora;
    }

    public static List<String> getOpenCorporaNonLive() {
        List<String> openCorpora = new ArrayList<String>();
        boolean isPC = false;
        for (String corpus : KORP_CORPORA) {
            for (String pCorpus : KORP_PROTECTED_CORPORA) {
                if (corpus.equals(pCorpus)) {
                    isPC = true;
                }
            }
            if (!isPC) {
                openCorpora.add(corpus);
            }
            isPC = false;
        }
        return openCorpora;
    }

    public static List<String> getKorpCorpora() {
        List<String> korpCorpora = new ArrayList<String>();
        List<String> openCorpora = ServiceInfo.getOpenCorporaLive();
        for (String corpus : openCorpora) {
            if (KORP_CORPORA.contains(corpus)) {
                korpCorpora.add(corpus);
            }
        }
        return korpCorpora;
    }
}
