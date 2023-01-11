package se.gu.spraakbanken.fcs.endpoint.korp.data.json.pojo.info;

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

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
	"corpora",
	"cqp-version",
	"protected_corpora",
	"time"
})

public class ServiceInfo {
    
    @JsonProperty("corpora")
    private List<String> corpora = new ArrayList<String>();
    @JsonProperty("cqp-version")
    private String cqpVersion;
    @JsonProperty("protected_corpora")
    private List<String> protectedCorpora = new ArrayList<String>();
    @JsonProperty("time")
    private Double time;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    private static final List<String> MODERN_CORPORA = Collections.unmodifiableList(Arrays.asList("KLK_SV_1910", "KLK_SV_1911"));

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
     * The cqpVersion
     */
    @JsonProperty("cqp-version")
    public String getCqpVersion() {
	return cqpVersion;
    }

    /**
     *
     * @param cqpVersion
     * The cqp-version
     */
    @JsonProperty("cqp-version")
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

    public static List<String> getOpenCorpora() {
        ObjectMapper mapper = new ObjectMapper();

	ServiceInfo si = null;
	final String wsString = "https://korp.csc.fi/korp/api8/?";
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

    public static List<String> getModernCorpora() {
	List<String> modernCorpora = new ArrayList<String>();
	List<String> openCorpora = ServiceInfo.getOpenCorpora();
	for (String corpus : openCorpora) {
		if (MODERN_CORPORA.contains(corpus)) {
		    modernCorpora.add(corpus);
		}
	}
	return modernCorpora;
    }

}
