package se.gu.spraakbanken.fcs.endpoint.korp.cqp;

public class TranslatorChooser {
    public static PosTranslator getTranslator(String corpusName) {
        if (corpusName.startsWith("KLK_SV")) {
            return new SUCTranslator();
            } else if (corpusName.startsWith("YLENEWS_FI")) {
            return new TDTTranslator();
        }
        throw new RuntimeException("Unknown corpus: " + corpusName);
    }
}