/**
 *
 * @license http://www.gnu.org/licenses/gpl-3.0.txt
 *  GNU General Public License v3
 */
package se.gu.spraakbanken.fcs.endpoint.korp.cqp;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.clarin.sru.server.SRUConstants;
import eu.clarin.sru.server.SRUException;

public class TDTTranslator implements PosTranslator {
    private static final Map<String, List<String>> TO_TDT = createToTDT();
    private static final Map<String, List<String>> TO_UD17 = createToUd17();

    private static Map<String, List<String>> createToTDT() {
        Map<String, List<String>> tdt = new HashMap<>();
        tdt.put("NOUN", Arrays.asList("N"));
        tdt.put("VERB", Arrays.asList("V"));
        tdt.put("ADJ", Arrays.asList("A"));
        tdt.put("ADV", Arrays.asList("ADV"));
        tdt.put("PRON", Arrays.asList("PRON"));
        tdt.put("NUM", Arrays.asList("NUM"));
        tdt.put("ADP", Arrays.asList("ADP"));
        tdt.put("CCONJ", Arrays.asList("C"));
        tdt.put("SCONJ", Arrays.asList("C"));
        tdt.put("INTJ", Arrays.asList("INTERJ"));
        tdt.put("PUNCT", Arrays.asList("PUNCT"));
        tdt.put("SYM", Arrays.asList("SYMB"));
        tdt.put("X", Arrays.asList("FOREIGN"));
        return Collections.unmodifiableMap(tdt);
    }

    private static Map<String, List<String>> createToUd17() {
        Map<String, List<String>> ud17 = new HashMap<>();
        ud17.put("N", Arrays.asList("NOUN", "PROPN"));
        ud17.put("V", Arrays.asList("VERB", "AUX"));
        ud17.put("A", Arrays.asList("ADJ"));
        ud17.put("ADV", Arrays.asList("ADV"));
        ud17.put("PRON", Arrays.asList("PRON")); // add "DET" ?
        ud17.put("NUM", Arrays.asList("NUM"));
        ud17.put("ADP", Arrays.asList("ADP"));
        ud17.put("C", Arrays.asList("CCONJ", "SCONJ"));
        ud17.put("INTERJ", Arrays.asList("INTJ"));
        ud17.put("PUNCT", Arrays.asList("PUNCT"));
        ud17.put("SYMB", Arrays.asList("SYM"));
        ud17.put("FOREIGN", Arrays.asList("X"));
        return Collections.unmodifiableMap(ud17);
    }

    @Override
    public List<String> toCorpus(final String ud17Pos) throws SRUException {
        String key = ud17Pos.toUpperCase();
        List<String> res = TO_TDT.get(key);
        if (res == null) {
            throw new SRUException(
                SRUConstants.SRU_QUERY_SYNTAX_ERROR,
                "Unknown UD-17 PoS code in query: " + ud17Pos);
        }
        return res;
    }

    /**
     * @param tdtPos The TDT PoS code
     * @return A list of translated codes in UD-17 PoS.
     */
    @Override
    public List<String> fromCorpus(final String tdtPos) throws SRUException {
        List<String> res = null;
        String key = tdtPos.toUpperCase();
        //System.out.println("TDT POS key used for lookup = = " + key); // debugging
        res = TO_UD17.get(key);
        if (res == null) {
            throw new SRUException(
                SRUConstants.SRU_CANNOT_PROCESS_QUERY_REASON_UNKNOWN,
                "Unknown PoS code from search engine: " + key);
        }
        return res;
    }
}
