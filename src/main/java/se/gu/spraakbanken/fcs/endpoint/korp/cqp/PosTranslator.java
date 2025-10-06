// This is the contract for all translators.
// Every corpus-specific translator must implement these two methods.
package se.gu.spraakbanken.fcs.endpoint.korp.cqp;

import java.util.List;
import eu.clarin.sru.server.SRUException;

public interface PosTranslator {
    List<String> toCorpus(String ud17Pos) throws SRUException;
    List<String> fromCorpus(String corpusPos) throws SRUException;
}
