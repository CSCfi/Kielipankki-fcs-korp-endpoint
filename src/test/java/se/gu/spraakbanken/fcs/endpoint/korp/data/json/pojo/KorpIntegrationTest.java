package se.gu.spraakbanken.fcs.endpoint.korp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;
import se.gu.spraakbanken.fcs.endpoint.korp.Config;
import java.net.URL;
import java.net.URLEncoder;

import se.gu.spraakbanken.fcs.endpoint.korp.data.json.pojo.query.Query;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class KorpIntegrationTest {

    @Test
    public void testQueryAgainstKorp() throws IOException {
        System.out.println("\n---testQueryAgainstKorp---");
        ObjectMapper mapper = new ObjectMapper();

        // Read parameters from config.properties
        String baseUrl = Config.get("web_service");
        String corpora = Config.get("corpus");
        String cqp = Config.get("query");
        String expectedWord = Config.get("expected_word");

        // Build request URL
        String queryString = String.format(
                "query?defaultcontext=1+sentence&show=msd,lemma&cqp=%s&start=0&end=9&corpus=%s",
                URLEncoder.encode(cqp, "UTF-8"),
                corpora
        );
        URL url = new URL(baseUrl + queryString);
        System.out.println("\nURL: " + url);

        Query result = mapper.readerFor(Query.class).readValue(url.openStream());

        // Assert response ok
        assertNotNull("Result returned null", result);
        assertNotNull("Result missing time field", result.getTime());
        assertTrue("Expected at least 1 hit", result.getHits() > 0);

        // Print POS tags from KWIC results: take the first KWIC row from the JSON,
        // loop over its 'tokens', print its word and 'msd'
        if (result.getKwic() != null) {
            System.out.println("\nFirst hit tokens with POS tags:");
            result.getKwic().stream()
                .limit(1) // just the first kwic hit
                .forEach(kwic -> {
                    kwic.getTokens().forEach(tok ->
                        System.out.printf("%s/%s ", tok.getWord(), tok.getMsd())
                    );
                    System.out.println();
                });
        }

        // Assert that the key word is present in the results 
        if (expectedWord != null) {
            String jsonOut = mapper.writeValueAsString(result);
            System.out.println("\nReceived JSON:\n" + jsonOut.substring(0, Math.min(1000, jsonOut.length())) + " ... }");
            assertTrue("Response should mention expected word: " + expectedWord,
                    jsonOut.contains(expectedWord));
        }
    }

    @Test // useless test?
    public void testInvalidCorpusReturnsError() throws IOException {
        System.out.println("\n---testInvalidCorpusReturnsError---");
        ObjectMapper mapper = new ObjectMapper();

        String baseUrl = Config.get("web_service");
        String badCorpus = "NON_EXISTENT_CORPUS";
        String cqp = "[word = \"och\"]";

        String queryString = String.format(
                "query?defaultcontext=1+sentence&show=msd,lemma&cqp=%s&start=0&end=1&corpus=%s",
                URLEncoder.encode(cqp, "UTF-8"),
                badCorpus
        );
        URL url = new URL(baseUrl + queryString);

        Query result = mapper.readerFor(Query.class).readValue(url.openStream());
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result));
        // Korp usually returns hits = -1 and an ERROR object
        assertTrue("Should signal error when corpus is invalid",
                result.getHits() < 0 || result.toString().contains("ERROR"));
    }

    @Test
    public void testZeroHitsQuery() throws IOException {
        System.out.println("\n---testZeroHitsQuery---");
        ObjectMapper mapper = new ObjectMapper();

        String baseUrl = Config.get("web_service");
        String corpus = Config.get("corpus");
        String cqp = "[word = \"awordthatdoesnotexist123\"]";

        String queryString = String.format(
            "query?defaultcontext=1+sentence&show=msd,lemma&cqp=%s&start=0&end=9&corpus=%s",
            URLEncoder.encode(cqp, "UTF-8"), corpus
        );
        URL url = new URL(baseUrl + queryString);

        Query result = mapper.readerFor(Query.class).readValue(url.openStream());
        if (result.getHits() == 0) {System.out.println("No hits, all good");}

        assertNotNull(result);
        assertTrue("Expected no hits", result.getHits() == 0);
    }
}