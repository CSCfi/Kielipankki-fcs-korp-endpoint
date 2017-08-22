/**
 *
 * @license http://www.gnu.org/licenses/gpl-3.0.txt
 *  GNU General Public License v3
 */
package fi.kielipankki.fcs.endpoint.korp;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.xml.XMLConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.z3950.zing.cql.CQLAndNode;
import org.z3950.zing.cql.CQLBooleanNode;
import org.z3950.zing.cql.CQLNode;
import org.z3950.zing.cql.CQLNotNode;
import org.z3950.zing.cql.CQLOrNode;
import org.z3950.zing.cql.CQLTermNode;

import eu.clarin.sru.server.CQLQueryParser;
import eu.clarin.sru.server.SRUConfigException;
import eu.clarin.sru.server.SRUConstants;
import eu.clarin.sru.server.SRUDiagnosticList;
import eu.clarin.sru.server.SRUException;
import eu.clarin.sru.server.SRUQueryParserRegistry;
import eu.clarin.sru.server.SRURequest;
import eu.clarin.sru.server.SRUScanResultSet;
import eu.clarin.sru.server.SRUSearchResultSet;
import eu.clarin.sru.server.SRUServerConfig;
import eu.clarin.sru.server.fcs.Constants;
import eu.clarin.sru.server.fcs.DataView;
import eu.clarin.sru.server.fcs.EndpointDescription;
import eu.clarin.sru.server.fcs.FCSQueryParser;
import eu.clarin.sru.server.fcs.Layer;
import eu.clarin.sru.server.fcs.ResourceInfo;
import eu.clarin.sru.server.fcs.SimpleEndpointSearchEngineBase;
import eu.clarin.sru.server.fcs.parser.Expression;
import eu.clarin.sru.server.fcs.parser.Operator;
import eu.clarin.sru.server.fcs.parser.QueryNode;
import eu.clarin.sru.server.fcs.parser.QuerySegment;
import eu.clarin.sru.server.fcs.utils.SimpleEndpointDescriptionParser;

import org.w3c.dom.Document;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import se.gu.spraakbanken.fcs.endpoint.korp.cqp.FCSToCQPConverter;
import se.gu.spraakbanken.fcs.endpoint.korp.data.json.pojo.info.CorporaInfo;
import se.gu.spraakbanken.fcs.endpoint.korp.data.json.pojo.info.ServiceInfo;
import se.gu.spraakbanken.fcs.endpoint.korp.data.json.pojo.query.Query;

import se.gu.spraakbanken.fcs.endpoint.korp.KorpEndpointSearchEngine;

/**
 * A Korp CLARIN FCS 2.0 endpoint example search engine.
 *
 */
public class KorpEndpointSearchEngine
    extends se.gu.spraakbanken.fcs.endpoint.korp.KorpEndpointSearchEngine
{
    public static final String RESOURCE_INVENTORY_URL =
            "fi.kielipankki.fcs.korp.sru.resourceInventoryURL";

    /**
     * Initialize the search engine. This initialization should be tailored
     * towards your environment and needs.
     *
     * @param context
     *            the {@link ServletContext} for the Servlet
     * @param config
     *            the {@link SRUServerConfig} object for this search engine
     * @param queryParserBuilder
     *            the {@link SRUQueryParserRegistry.Builder} object to be used
     *            for this search engine. Use to register additional query
     *            parsers with the {@link SRUServer}.
     * @param params
     *            additional parameters gathered from the Servlet configuration
     *            and Servlet context.
     * @throws SRUConfigException
     *             if an error occurred
     */
    protected void doInit(ServletContext context,
            SRUServerConfig config,
            SRUQueryParserRegistry.Builder queryParserBuilder,
            Map<String, String> params) throws SRUConfigException {
	doInit(config, queryParserBuilder, params);
    }

    protected void doInit(SRUServerConfig config,
            SRUQueryParserRegistry.Builder queryParserBuilder,
            Map<String, String> params) throws SRUConfigException {
	LOG.info("KorpEndpointSearchEngine::doInit {}", config.getPort());
	List<String> openCorpora = ServiceInfo.getModernCorpora();
	openCorporaInfo = CorporaInfo.getCorporaInfo(openCorpora);
    }

    protected Query makeQuery(final String cqpQuery, CorporaInfo openCorporaInfo, final int startRecord, final int maximumRecords) {
	ObjectMapper mapper = new ObjectMapper();
	// Edit the URL below
	String wsString = "http://localhost/cgi-bin/korp.cgi?";
	String queryString = "command=query&defaultcontext=1+sentence&show=msd,lemma&cqp=";
	String startParam = "&start=" + (startRecord == 1 ? 0 : startRecord - 1);
	String endParam = "&end=" + (maximumRecords == 0 ? 250 : startRecord - 1 + maximumRecords - 1);
	String corpusParam = "&corpus=";
	    //"SUC2";
	String corpusParamValues = CorporaInfo.getCorpusParameterValues(openCorporaInfo.getCorpora().keySet());
        try {
	    URL korp = new URL(wsString + queryString + URLEncoder.encode(cqpQuery, "UTF-8") + startParam + endParam + corpusParam + corpusParamValues);
	    // mapper.reader(Query.class).readValue(korp.openStream());
	    // truncates the query string 
	    // using URLConnection.getInputStream() instead. /ljo
	    URLConnection connection = korp.openConnection();
	    return mapper.reader(Query.class).readValue(connection.getInputStream());
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
	return null;
    }

    protected CorporaInfo getCorporaInfo() {
	return openCorporaInfo;
    }

}
