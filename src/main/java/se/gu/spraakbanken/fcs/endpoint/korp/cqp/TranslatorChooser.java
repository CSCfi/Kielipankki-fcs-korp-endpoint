package se.gu.spraakbanken.fcs.endpoint.korp.cqp;

public class TranslatorChooser {

    public static PosTranslator getTranslatorForTagset(String tagset) {
        if (tagset.equalsIgnoreCase("SUC")) {
            return new SUCTranslator();
        } else if (tagset.equalsIgnoreCase("TDT")) {
            return new TDTTranslator();
        }
        throw new RuntimeException("Unknown tagset: " + tagset);
    }
}