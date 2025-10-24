/**
 *
 * @license http://www.gnu.org/licenses/gpl-3.0.txt
 *  GNU General Public License v3
 */
package se.gu.spraakbanken.fcs.endpoint.korp.cqp;

import java.util.ArrayList;
import java.util.List;
//import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.z3950.zing.cql.CQLAndNode;
import org.z3950.zing.cql.CQLBooleanNode;
import org.z3950.zing.cql.CQLNode;
import org.z3950.zing.cql.CQLNotNode;
import org.z3950.zing.cql.CQLOrNode;
import org.z3950.zing.cql.CQLTermNode;

import eu.clarin.sru.server.CQLQueryParser;
import eu.clarin.sru.server.SRUConstants;
import eu.clarin.sru.server.SRUDiagnosticList;
import eu.clarin.sru.server.SRUException;
import eu.clarin.sru.server.SRUQuery;
import eu.clarin.sru.server.SRUQueryParserRegistry;
import eu.clarin.sru.server.fcs.Constants;
import eu.clarin.sru.server.fcs.FCSQueryParser;
import eu.clarin.sru.server.fcs.parser.Expression;
import eu.clarin.sru.server.fcs.parser.ExpressionAnd;
import eu.clarin.sru.server.fcs.parser.ExpressionNot;
import eu.clarin.sru.server.fcs.parser.ExpressionOr;
import eu.clarin.sru.server.fcs.parser.ExpressionWildcard;
import eu.clarin.sru.server.fcs.parser.Operator;
import eu.clarin.sru.server.fcs.parser.QueryNode;
import eu.clarin.sru.server.fcs.parser.QuerySegment;
import eu.clarin.sru.server.fcs.parser.QuerySequence;
import eu.clarin.sru.server.fcs.parser.RegexFlag;
import se.gu.spraakbanken.fcs.endpoint.korp.cqp.TranslatorChooser;


/**
 * A Korp CLARIN FCS 2.0 endpoint example converter of FCS to CQP.
 *
 */
public class FCSToCQPConverter {
    private static final Logger LOG =
            LoggerFactory.getLogger(FCSToCQPConverter.class);

    /**
     *
     * @param query The CQL query
     * @return The CQP query
     * @throws eu.clarin.sru.server.SRUException If the query is too complex or it 
     * for any other reason cannot be performed
     */
    public static String makeCQPFromCQL(final SRUQuery<CQLNode> query)
	throws SRUException {
        final CQLNode node = query.getParsedQuery();
        /*
         * Translate the CQL query to a Lucene query. If a CQL feature was used,
         * that is not supported by us, throw a SRU error (with a detailed error
         * message)
         *
         * Right now, we're pretty stupid and only support terms
         */
        if (node instanceof CQLBooleanNode) {
            String operator;
            //if (node instanceof CQLAndNode) {
            //    operator = "AND";
            //} else if (node instanceof CQLOrNode) {
            //    operator = "OR";
            //} else
	    if (node instanceof CQLNotNode) {
                operator = "NOT";
            } else {
                operator = node.getClass().getSimpleName();
	    }
            throw new SRUException(
				   SRUConstants.SRU_UNSUPPORTED_BOOLEAN_OPERATOR,
				   operator,
				   "Unsupported Boolean operator: " + operator);
        } else if (node instanceof CQLTermNode) {
            CQLTermNode ctn = (CQLTermNode) node;
	    
            String[] terms = ctn.getTerm().toLowerCase().split("\\s+");
           if (terms.length > 1) {
	       String phrase = "";
                for (int i = 0; i < terms.length; i++) {
		    if (terms[i].startsWith("\"") || terms[i].startsWith("'") || terms[i].endsWith("\"") || terms[i].endsWith("'")) {
			String tmp = terms[i];
			if (terms[i].startsWith("\"") || terms[i].startsWith("'")) {
			    tmp = tmp.substring(1);
			}
			if (terms[i].endsWith("\"") || terms[i].endsWith("'")) {
			    tmp = tmp.substring(0, tmp.length()-1);
			}
			phrase += "[word = '" + tmp + "']";
		    } else { 
			phrase += "[word = '" + terms[i] + "']";
		    }
                }
                return phrase;
            } else {
                return "[word = '" + terms[0] + "']";
            }
        } else {
            throw new SRUException(
				   SRUConstants.SRU_CANNOT_PROCESS_QUERY_REASON_UNKNOWN,
				   "unknown cql node: " + node);
        }
    }
    
    /**
     *
     * @param query The FCS 2.0 query
     * @return The CQP query
     * @throws eu.clarin.sru.server.SRUException If the query is too complex or it 
     * for any other reason cannot be performed
     */
    public static String makeCQPFromFCS(final SRUQuery<QueryNode> query, final String tagset)
	throws SRUException {
		
		if (tagset == null || tagset.isEmpty()) {
        	throw new SRUException(Constants.FCS_DIAGNOSTIC_GENERAL_QUERY_TOO_COMPLEX_CANNOT_PERFORM_QUERY, 
			"No tagset specified for POS translation"); } // avoids cryptic errors if for some reason tagset wasn't passed

        QueryNode tree = query.getParsedQuery();
        LOG.debug("FCS-Query: {}", tree.toString());
	//System.out.println("tree=" + tree.toString());	
        // A somewhat crude query translator
        if (tree instanceof QuerySequence) {
	    return getQuerySequence(tree, tagset);
        } else if (tree instanceof QuerySegment) {
	    return getQuerySegment(tree, tagset);
        } else {
            throw new SRUException(
				   Constants.FCS_DIAGNOSTIC_GENERAL_QUERY_TOO_COMPLEX_CANNOT_PERFORM_QUERY,
				   "Endpoint only supports sequences or single segment queries");
        }
    }

    private static String getQuerySequence(final QueryNode tree, final String tagset) throws SRUException {
	List<String> children = new ArrayList<String>();
	QuerySequence sequence = (QuerySequence) tree;

	for (int i = 0; i < sequence.getChildCount(); i++) {
	    QueryNode child = sequence.getChild(i);
	    if (child instanceof QuerySegment) {
		children.add(getQuerySegment(child, tagset));
	    }
	}
	StringBuffer sb = new StringBuffer();
	for (String child : children) {
	    sb.append(child);
	}
	return sb.toString();
    }

    private static String getQuerySegment(final QueryNode tree, final String tagset) throws SRUException {
	QuerySegment segment = (QuerySegment) tree;
	QueryNode op = segment.getExpression();
	if (op instanceof ExpressionAnd) {
	    return "[" + getExpressionBoolOp(op, " & ", tagset) + "]";
	} else if (op instanceof ExpressionOr) {
	    return "[" + getExpressionBoolOp(op, " | ", tagset ) + "]";
	} else {
	    String occurrences = getOccurrences(segment.getMinOccurs(), segment.getMaxOccurs());
	    QueryNode child = segment.getExpression();
	    if (child instanceof Expression) {
		return "[" + getExpression((Expression) child, tagset) + "]" + occurrences;
	    } else if (child instanceof ExpressionWildcard) {
		return " []" + occurrences;
	    } else {
		throw new SRUException(
				       Constants.FCS_DIAGNOSTIC_GENERAL_QUERY_TOO_COMPLEX_CANNOT_PERFORM_QUERY,
				       "Endpoint only supports sequences or single segment expressions");
	    }
	}
    }

    private static String getOccurrences(final int min, final int max) {
	if (min == 1 && max == 1) {
	    return " ";
	} else 	if (min == max) {
	    return "{" + min + "} ";
	} else {
	    return "{" + min + "," + max + "} ";
	}
    }

    private static String getExpressionBoolOp(final QueryNode op, final String opString,final String tagset) throws SRUException {
	List<String> children = new ArrayList<String>();
	for (int i = 0; i < op.getChildCount(); i++) {
	    QueryNode child = op.getChild(i);
	    if (child instanceof Expression) {
		children.add(getExpression((Expression) child, tagset));
	    }
	}
	return 	children.get(0) + opString + children.get(1);
    }

    private static String getExpression(final Expression child, final String tagset) throws SRUException {
	Expression expression = (Expression) child;
	if ((expression.getLayerIdentifier().equals("text") || expression.getLayerIdentifier().equals("token") || expression.getLayerIdentifier().equals("word") || expression.getLayerIdentifier().equals("lemma") || expression.getLayerIdentifier().equals("pos")) &&
	    (expression.getLayerQualifier() == null) &&
	    (expression.getOperator() == Operator.EQUALS || expression.getOperator() == Operator.NOT_EQUALS) //&&
	    //(expression.getRegexFlags() == null)
	    ) {
	    // Not really worth it using regexFlags. 
	    // Still handled in getWordLayerFilter(). /ljo 

	    // Translate PoS value or just get the text/word layer as is.
	    if (expression.getLayerIdentifier().equals("pos")) {
		return translatePos(tagset, expression.getLayerIdentifier(), getOperator(expression.getOperator()), expression.getRegexValue()); // TO DO: make a for loop that goes through every selected corpus
	    } else if (expression.getLayerIdentifier().equals("lemma")) {
		return getLemmaLayerFilter(expression);
	    }
	    return getWordLayerFilter(expression);

	} else {
	    throw new SRUException(
				   Constants.FCS_DIAGNOSTIC_GENERAL_QUERY_TOO_COMPLEX_CANNOT_PERFORM_QUERY,
				   "Endpoint only supports 'text', 'word', 'lemma', and 'pos' layers, the '=' and '!=' operators and no regex flags");
	}
    }

    // Translate a UD17 PoS (from FCS) into the corpus-specific tagset using TranslatorChooser.java
	private static String translatePos(final String tagset, final String layerIdentifier, final String operator, final String pos) throws SRUException {
		PosTranslator translator = TranslatorChooser.getTranslatorForTagset(tagset);

		// Translate the UD tag into the target corpus tags
		List<String> tags = translator.toCorpus(pos);

		StringBuffer buf = new StringBuffer();
		buf.append(layerIdentifier);
		buf.append(" ");
		buf.append(operator);
		buf.append(" '");

		if (tags.size() == 1) {
			buf.append(tags.get(0));
		} else {
			buf.append("("); // if returned POS are more than 1, make OR's like: (NN|PM|NNP)
			for (int i = 0; i < tags.size(); i++) {
				if (i > 0) buf.append("|");
				buf.append(tags.get(i));
			}
			buf.append(")");
		}
		return buf.append("'").toString();
	}


    private static String getOperator(Operator op) {
	if (op == Operator.NOT_EQUALS) {
	    return "!=";
	}
	return "=";
    }

    private static String getWordLayerFilter(Expression expression) {
	boolean contRegexFlag = false;
	StringBuffer buf = new StringBuffer();
	buf.append((expression.getLayerIdentifier().equals("text") || expression.getLayerIdentifier().equals("token")) ? "word" : expression.getLayerIdentifier());
	buf.append(" ");
	buf.append(getOperator(expression.getOperator()));
	buf.append(" '");
	buf.append(expression.getRegexValue());
	buf.append("'");
	if (expression.getRegexFlags() != null) {
	    if (expression.getRegexFlags().contains(RegexFlag.CASE_INSENSITIVE)) {
		buf.append(" %c");
		contRegexFlag = true;
	    }
	    if (expression.getRegexFlags().contains(RegexFlag.CASE_SENSITIVE)) {
	    }
	    if (expression.getRegexFlags().contains(RegexFlag.LITERAL_MATCHING)) {
		if (!contRegexFlag) {
		    buf.append(" %");
		}
		buf.append("l");
		contRegexFlag = true;
	    }
	    if (expression.getRegexFlags().contains(RegexFlag.IGNORE_DIACRITICS)) {
		if (!contRegexFlag) {
		    buf.append(" %");
			}
		buf.append("d");
		contRegexFlag = true;
	    }
	    if (contRegexFlag) {
		//buf.append(" ");
	    }
	}
	return buf.toString();
    }

    private static String getLemmaLayerFilter(Expression expression) {
	boolean contRegexFlag = false;
	StringBuffer buf = new StringBuffer();
	buf.append(expression.getLayerIdentifier());
	buf.append(" ");
	if (expression.getOperator() == Operator.NOT_EQUALS) {
	    buf.append("not contains");
	} else if (expression.getOperator() == Operator.EQUALS) {
	    buf.append("contains");
	}
	buf.append(" '");
	buf.append(expression.getRegexValue());
	buf.append("'");
	if (expression.getRegexFlags() != null) {
	    if (expression.getRegexFlags().contains(RegexFlag.CASE_INSENSITIVE)) {
		buf.append(" %c");
		contRegexFlag = true;
	    }
	    if (expression.getRegexFlags().contains(RegexFlag.CASE_SENSITIVE)) {
	    }
	    if (expression.getRegexFlags().contains(RegexFlag.LITERAL_MATCHING)) {
		if (!contRegexFlag) {
		    buf.append(" %");
		}
		buf.append("l");
		contRegexFlag = true;
	    }
	    if (expression.getRegexFlags().contains(RegexFlag.IGNORE_DIACRITICS)) {
		if (!contRegexFlag) {
		    buf.append(" %");
			}
		buf.append("d");
		contRegexFlag = true;
	    }
	    if (contRegexFlag) {
		//buf.append(" ");
	    }
	}
	return buf.toString();
    }

}
