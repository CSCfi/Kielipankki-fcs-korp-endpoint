/**
 *
 * @license http://www.gnu.org/licenses/gpl-3.0.txt
 *  GNU General Public License v3
 */
package se.gu.spraakbanken.fcs.endpoint.korp;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collection;

import javax.servlet.ServletContext;
import javax.xml.XMLConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.clarin.sru.server.CQLQueryParser;
import eu.clarin.sru.server.SRUConfigException;
import eu.clarin.sru.server.SRUConstants;
import eu.clarin.sru.server.SRUDiagnosticList;
import eu.clarin.sru.server.SRUException;
import eu.clarin.sru.server.SRUQueryParserRegistry;
import eu.clarin.sru.server.SRURequest;
import eu.clarin.sru.server.SRUScanResultSet;
import eu.clarin.sru.server.SRUSearchResultSet;
import eu.clarin.sru.server.SRUServer;
import eu.clarin.sru.server.SRUServerConfig;
import eu.clarin.sru.server.fcs.Constants;
import eu.clarin.sru.server.fcs.DataView;
import eu.clarin.sru.server.fcs.EndpointDescription;
import eu.clarin.sru.server.fcs.FCSQueryParser;
import eu.clarin.sru.server.fcs.Layer;
import eu.clarin.sru.server.fcs.ResourceInfo;
import eu.clarin.sru.server.fcs.SimpleEndpointSearchEngineBase;
import eu.clarin.sru.server.fcs.utils.SimpleEndpointDescriptionParser;
import eu.clarin.sru.server.fcs.parser.QueryNode;
import eu.clarin.sru.server.fcs.parser.Expression;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import se.gu.spraakbanken.fcs.endpoint.korp.cqp.FCSToCQPConverter;
import se.gu.spraakbanken.fcs.endpoint.korp.data.json.pojo.info.CorporaInfo;
import se.gu.spraakbanken.fcs.endpoint.korp.data.json.pojo.info.Corpus;
import se.gu.spraakbanken.fcs.endpoint.korp.data.json.pojo.info.ServiceInfo;
import se.gu.spraakbanken.fcs.endpoint.korp.data.json.pojo.query.Query;

/**
 * A Korp CLARIN FCS 2.0 endpoint example search engine.
 *
 */
public class KorpEndpointSearchEngine extends SimpleEndpointSearchEngineBase {
    private static final Logger LOG = LoggerFactory.getLogger(KorpEndpointSearchEngine.class);

    private static final String ED_NS = "http://clarin.eu/fcs/endpoint-description";
    private static final String ED_PREFIX = "ed";
    private static final int ED_VERSION = 2;

    public static final String RESOURCE_INVENTORY_URL = "se.gu.spraakbanken.fcs.korp.sru.resourceInventoryURL";

    private static CorporaInfo openCorporaInfo;
    protected EndpointDescription endpointDescription;

    protected EndpointDescription createEndpointDescription(
            ServletContext context, SRUServerConfig config,
            Map<String, String> params) throws SRUConfigException {
        try {
            URL url = null;
            String riu = params.get(RESOURCE_INVENTORY_URL);
            if ((riu == null) || riu.isEmpty()) {
                url = context.getResource("/WEB-INF/endpoint-description.xml");
                LOG.debug("using bundled 'endpoint-description.xml' file");
            } else {
                url = new File(riu).toURI().toURL();
                LOG.debug("using external file '{}'", riu);
            }

            return SimpleEndpointDescriptionParser.parse(url);
        } catch (MalformedURLException mue) {
            throw new SRUConfigException("Malformed URL for initializing resource info inventory", mue);
        }
    }

    /**
     * Initialize the search engine. This initialization should be tailored
     * towards your environment and needs.
     *
     * @param context            the {@link ServletContext} for the Servlet
     * @param config             the {@link SRUServerConfig} object for this search
     *                           engine
     * @param queryParserBuilder the {@link SRUQueryParserRegistry.Builder} object
     *                           to be used for this search engine. Use to register
     *                           additional query parsers with the
     *                           {@link SRUServer}.
     * @param params             additional parameters gathered from the Servlet
     *                           configuration and Servlet context.
     * @throws SRUConfigException if an error occurred
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

        // The master list of Korp corpora the endpoint is allowed to use, from ServiceInfo.java. Includes Korp metadata,
        List<String> openCorpora = ServiceInfo.getKorpCorpora(); 
        openCorporaInfo = CorporaInfo.getCorporaInfo(openCorpora);
    }

    /**
     * Destroy the search engine. Override this method for any cleanup the
     * search engine needs to perform upon termination.
     */
    protected void doDestroy() {

    }

    /**
     * Handle a <em>scan</em> operation. The default implementation is a no-op.
     * Override this method, if you want to provide a custom behavior.
     *
     * @param config      the <code>SRUEndpointConfig</code> object that contains
     *                    the endpoint configuration
     * @param request     the <code>SRURequest</code> object that contains the
     *                    request made to the endpoint
     * @param diagnostics the <code>SRUDiagnosticList</code> object for storing
     *                    non-fatal diagnostics
     * @return a <code>SRUScanResultSet</code> object or <code>null</code> if
     *         this operation is not supported by this search engine
     * @throws SRUException if an fatal error occurred
     */
    @Override
    protected SRUScanResultSet doScan(SRUServerConfig config,
            SRURequest request, SRUDiagnosticList diagnostics)
            throws SRUException {
        // final CQLNode scanClause = request.getScanClause();
        // if (scanClause instanceof CQLTermNode) {
        // final CQLTermNode root = (CQLTermNode) scanClause;
        // final String index = root.getIndex();
        // throw new SRUException(SRUConstants.SRU_UNSUPPORTED_INDEX, index,
        // "scan operation on index '" + index + "' is not supported");
        // } else {
        // throw new SRUException(SRUConstants.SRU_QUERY_FEATURE_UNSUPPORTED,
        // "Scan clause too complex.");
        // }
        return null;
    }

    /**
     * Unused
     * Convenience method for parsing a string to boolean. Values <code>1</code>,
     * <code>true</code>, <code>yes</code> yield a <em>true</em> boolean value
     * as a result, all others (including <code>null</code>) a <em>false</em>
     * boolean value.
     *
     * @param value the string to parse
     * @return <code>true</code> if the supplied string was considered something
     *         representing a <em>true</em> boolean value, <code>false</code>
     *         otherwise
     */
    protected static boolean parseBoolean(String value) {
        if (value != null) {
            return value.equals("1") || Boolean.parseBoolean(value);
        }
        return false;
    }

    private void writeEndpointDescription(XMLStreamWriter writer)
            throws XMLStreamException {
        writer.setPrefix(ED_PREFIX, ED_NS);
        writer.writeStartElement(ED_NS, "EndpointDescription");
        writer.writeNamespace(ED_PREFIX, ED_NS);
        writer.writeAttribute("version", Integer.toString(ED_VERSION));

        // Capabilities
        writer.writeStartElement(ED_NS, "Capabilities");
        for (URI capability : endpointDescription.getCapabilities()) {
            writer.writeStartElement(ED_NS, "Capability");
            writer.writeCharacters(capability.toString());
            writer.writeEndElement(); // "Capability" element
        }
        writer.writeEndElement(); // "Capabilities" element

        // SupportedDataViews
        writer.writeStartElement(ED_NS, "SupportedDataViews");
        for (DataView dataView : endpointDescription.getSupportedDataViews()) {
            writer.writeStartElement(ED_NS, "SupportedDataView");
            writer.writeAttribute("id", dataView.getIdentifier());
            String s;
            switch (dataView.getDeliveryPolicy()) {
                case SEND_BY_DEFAULT:
                    s = "send-by-default";
                    break;
                case NEED_TO_REQUEST:
                    s = "need-to-request";
                    break;
                default:
                    throw new XMLStreamException(
                            "invalid value for payload delivery policy: " +
                                    dataView.getDeliveryPolicy());
            } // switch
            writer.writeAttribute("delivery-policy", s);
            writer.writeCharacters(dataView.getMimeType());
            writer.writeEndElement(); // "SupportedDataView" element
        }
        writer.writeEndElement(); // "SupportedDataViews" element

        // SupportedLayers
        final List<Layer> layers = endpointDescription.getSupportedLayers();
        if (layers != null) {
            writer.writeStartElement(ED_NS, "SupportedLayers");
            for (Layer layer : layers) {
                writer.writeStartElement(ED_NS, "SupportedLayer");
                writer.writeAttribute("id", layer.getId());
                writer.writeAttribute("result-id",
                        layer.getResultId().toString());
                if (layer.getContentEncoding() == Layer.ContentEncoding.EMPTY) {
                    writer.writeAttribute("type", "empty");
                }
                if (layer.getQualifier() != null) {
                    writer.writeAttribute("qualifier", layer.getQualifier());
                }
                if (layer.getAltValueInfo() != null) {
                    writer.writeAttribute("alt-value-info",
                            layer.getAltValueInfo());
                    if (layer.getAltValueInfoURI() != null) {
                        writer.writeAttribute("alt-value-info-uri",
                                layer.getAltValueInfoURI().toString());
                    }
                }
                writer.writeCharacters(layer.getType());
                writer.writeEndElement(); // "SupportedLayer" element
            }
            writer.writeEndElement(); // "SupportedLayers" element

        }

        // Resources
        try {
            List<ResourceInfo> resources = endpointDescription.getResourceList(
                    EndpointDescription.PID_ROOT);
            writeResourceInfos(writer, resources);
        } catch (SRUException e) {
            throw new XMLStreamException("error retriving top-level resources",
                    e);
        }
        writer.writeEndElement(); // "EndpointDescription" element
    }

    private void writeResourceInfos(XMLStreamWriter writer,
            List<ResourceInfo> resources) throws XMLStreamException {
        if (resources == null) {
            throw new NullPointerException("resources == null");
        }
        if (!resources.isEmpty()) {
            writer.writeStartElement(ED_NS, "Resources");

            for (ResourceInfo resource : resources) {
                writer.writeStartElement(ED_NS, "Resource");
                writer.writeAttribute("pid", resource.getPid());

                // title
                final Map<String, String> title = resource.getTitle();
                for (Map.Entry<String, String> i : title.entrySet()) {
                    writer.setPrefix(XMLConstants.XML_NS_PREFIX,
                            XMLConstants.XML_NS_URI);
                    writer.writeStartElement(ED_NS, "Title");
                    writer.writeAttribute(XMLConstants.XML_NS_URI, "lang", i.getKey());
                    writer.writeCharacters(i.getValue());
                    writer.writeEndElement(); // "title" element
                }

                // description
                final Map<String, String> description = resource.getDescription();
                if (description != null) {
                    for (Map.Entry<String, String> i : description.entrySet()) {
                        writer.writeStartElement(ED_NS, "Description");
                        writer.writeAttribute(XMLConstants.XML_NS_URI, "lang",
                                i.getKey());
                        writer.writeCharacters(i.getValue());
                        writer.writeEndElement(); // "Description" element
                    }
                }

                // landing page
                final String landingPageURI = resource.getLandingPageURI();
                if (landingPageURI != null) {
                    writer.writeStartElement(ED_NS, "LandingPageURI");
                    writer.writeCharacters(landingPageURI);
                    writer.writeEndElement(); // "LandingPageURI" element
                }

                // languages
                final List<String> languages = resource.getLanguages();
                writer.writeStartElement(ED_NS, "Languages");
                for (String i : languages) {
                    writer.writeStartElement(ED_NS, "Language");
                    writer.writeCharacters(i);
                    writer.writeEndElement(); // "Language" element

                }
                writer.writeEndElement(); // "Languages" element

                // available data views
                StringBuilder sb = new StringBuilder();
                for (DataView dataview : resource.getAvailableDataViews()) {
                    if (sb.length() > 0) {
                        sb.append(" ");
                    }
                    sb.append(dataview.getIdentifier());
                }
                writer.writeEmptyElement(ED_NS, "AvailableDataViews");
                writer.writeAttribute("ref", sb.toString());

                final List<Layer> layers = resource.getAvailableLayers();
                if (layers != null) {
                    sb = new StringBuilder();
                    for (Layer layer : resource.getAvailableLayers()) {
                        if (sb.length() > 0) {
                            sb.append(" ");
                        }
                        sb.append(layer.getId());
                    }
                    writer.writeEmptyElement(ED_NS, "AvailableLayers");
                    writer.writeAttribute("ref", sb.toString());
                }

                // child resources
                List<ResourceInfo> subs = resource.getSubResources();
                if ((subs != null) && !subs.isEmpty()) {
                    writeResourceInfos(writer, subs);
                }

                writer.writeEndElement(); // "Resource" element
            }
            writer.writeEndElement(); // "Resources" element
        }
    }

    public SRUSearchResultSet search(SRUServerConfig config,
            SRURequest request, SRUDiagnosticList diagnostics)
            throws SRUException {
        String query = null;
        Query queryRes;

        // list of corporaIDs (from one PID) to run the query on if POS is not in the query:
        List<String> selectedCorpora = resolveCorporaSelection(request);
        
        if (request.isQueryType(Constants.FCS_QUERY_TYPE_CQL)) {
            /*
             * Got a CQL query (either SRU 1.1 or higher).
             * Translate to a proper CQP query ...
             */
            final CQLQueryParser.CQLQuery q = request.getQuery(CQLQueryParser.CQLQuery.class);
            query = FCSToCQPConverter.makeCQPFromCQL(q);
            queryRes = makeQuery(query, selectedCorpora, request.getStartRecord(), request.getMaximumRecords());
        } else if (request.isQueryType(Constants.FCS_QUERY_TYPE_FCS)) {
            /*
             * Got a FCS query (SRU 2.0).
             * Translate to a proper CQP query
             */
            final FCSQueryParser.FCSQuery q = request.getQuery(FCSQueryParser.FCSQuery.class);

            // if query uses POS, get the map of tagset:corpora, use correct POS translator for each and send a Korp query
            if (queryUsesPos(q.getParsedQuery())) { // q is the FCS query tree
                Map<String, List<String>> corporaByPid = groupCorporaByPid(selectedCorpora);
                
                // If multiple PIDs are in the query, throw an error 
                if (corporaByPid.size() > 1) {
                    throw new SRUException(
                        SRUConstants.SRU_CANNOT_PROCESS_QUERY_REASON_UNKNOWN,
                        "POS queries spanning multiple PIDs are not supported by this endpoint.");
                }

                List<Query> perPidResults = new ArrayList<>();

                for (Map.Entry<String, List<String>> entry : corporaByPid.entrySet()) {
                    String pid = entry.getKey();
                    List<String> corpora = entry.getValue();
                    String tagset;
                    try {
                        tagset = CorpusTagsetMapper.getTagsetForPid(pid);
                    } catch (IllegalStateException e) {
                        throw new SRUException(
                                SRUConstants.SRU_CANNOT_PROCESS_QUERY_REASON_UNKNOWN,
                                "Metadata error", e.getMessage());
                    }

                    String cqpForPid = FCSToCQPConverter.makeCQPFromFCS(q, tagset);
                    query = cqpForPid;
                    Query pidResult = makeQuery(cqpForPid, corpora,
                            request.getStartRecord(), request.getMaximumRecords());
                    if (pidResult != null) {
                        perPidResults.add(pidResult);
                    } else {
                        LOG.warn("Korp returned null result for PID '{}' (corpora {}).", pid, corpora);
                    }
                }
                if (perPidResults.isEmpty()) {
                    throw new SRUException(
                            SRUConstants.SRU_CANNOT_PROCESS_QUERY_REASON_UNKNOWN,
                            "No results object returned for any PID.");
                }
                queryRes = perPidResults.get(0); // returns the result of the first Korp query (which can only be 1 now)
            } else {
                // POS-free path. Can even be run on all the corpora on many PIDs because no POS translation
                String defaultTagset = "SUC";
                query = FCSToCQPConverter.makeCQPFromFCS(q, defaultTagset);
                queryRes = makeQuery(query, selectedCorpora, request.getStartRecord(), request.getMaximumRecords());
            }
        } else {
            /*
             * Got something else we don't support. Send error ...
             */
            throw new SRUException(
                    SRUConstants.SRU_CANNOT_PROCESS_QUERY_REASON_UNKNOWN,
                    "Queries with queryType '" +
                            request.getQueryType() +
                            "' are not supported by this CLARIN-FCS Endpoint.");
        }

        if (queryRes == null) {
            throw new SRUException(
                    SRUConstants.SRU_CANNOT_PROCESS_QUERY_REASON_UNKNOWN,
                    "The query execution failed by this CLARIN-FCS Endpoint.");
        }
        return new KorpSRUSearchResultSet(config, request, diagnostics, queryRes, query, openCorporaInfo);
    }

    protected Query makeQuery(final String cqpQuery, final List<String> corpora, final int startRecord, final int maximumRecords) {
        ObjectMapper mapper = new ObjectMapper();
        String wsString = Config.get("web_service");
        String queryString = "query?defaultcontext=1+sentence&show=msd,lemma,pos&cqp=";
        String startParam = "&start=" + (startRecord == 1 ? 0 : startRecord - 1);
        String endParam = "&end=" + (maximumRecords == 0 ? 250 : startRecord - 1 + maximumRecords - 1);
        String corpusParam = "&corpus=";
        String corpusParamValues = CorporaInfo.getCorpusParameterValues(corpora);
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
/**
 * Group the selected corpora by PID.
 * Return a map where the key is the PID
 * and the value is the list of corpora that use that tagset.
 */
    private Map<String, List<String>> groupCorporaByPid(Collection<String> corpora) {
        return CorpusTagsetMapper.groupByPid(corpora);
    }

/**
 * Walk and inspect the FCS query tree and detect if "pos" layer is used
 */    
    private boolean queryUsesPos(QueryNode node) {
        if (node instanceof Expression) { // expression is like [pos="NOUN"] or like [word="milk"]
            Expression expr = (Expression) node;
            return "pos".equalsIgnoreCase(expr.getLayerIdentifier());
    }
        
        // if not an Expression (e.g., sequence), inspect children 
        for (int i = 0; i < node.getChildCount(); i++) {
            QueryNode child = node.getChild(i);
            if (queryUsesPos(child)) {
                return true;
            }
        }
        return false;
    }

/*
 * Checks whether x-fcs-context is present in the query. If not present or contains a comma (multiple PIDs),
 * throws an exception. Otherwise takes one PID and returns the list of corpora IDs within that PID.
 */
    private List<String> resolveCorporaSelection(SRURequest request) throws SRUException {
        String context = request.getExtraRequestData("x-fcs-context");

        if (context == null || context.isBlank()) {
            throw new SRUException(
                    SRUConstants.SRU_CANNOT_PROCESS_QUERY_REASON_UNKNOWN,
                    "This endpoint requires x-fcs-context to be present and contain one PID.");
        }

        if (context.contains(",")) { // Will fail if we ever have a comma in a corpus name
            throw new SRUException(
                    SRUConstants.SRU_CANNOT_PROCESS_QUERY_REASON_UNKNOWN,
                    "Multiple PIDs in x-fcs-context are not supported by this endpoint.");
        }

        // get the corpora IDs for PID
        String pid = context.trim();
        List<String> corporaForPid;
        try {
            corporaForPid = CorpusTagsetMapper.getCorporaForPid(pid);
        } catch (IllegalStateException e) {
            throw new SRUException(
                    SRUConstants.SRU_CANNOT_PROCESS_QUERY_REASON_UNKNOWN,
                    "PID '" + pid + "' from x-fcs-context is not in supported_corpora.json.",
                    e);
        }
        
        // check if all the corporaIDs from the PID got a metadata object from Korp
        Map<String, Corpus> availableCorpora = openCorporaInfo.getCorpora();
        for (String corpusId : corporaForPid) {
            if (!availableCorpora.containsKey(corpusId)) {
                throw new SRUException(
                        SRUConstants.SRU_CANNOT_PROCESS_QUERY_REASON_UNKNOWN,
                        "PID '" + pid + "' references corpus '" + corpusId +
                        "', which is not available in Korp.");
            }
        }

        return new ArrayList<>(corporaForPid);
    }


}
