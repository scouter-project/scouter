package scouter.agent;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 2. 5.
 * Common constants for scouter agent.
 *
 */
public class AgentCommonConstant {
    public static final String REQUEST_ATTRIBUTE_INITIAL_TRACE_CONTEXT = "__scouter__itc__";
    public static final String REQUEST_ATTRIBUTE_TRACE_CONTEXT = "__scouter__tc__";
    public static final String REQUEST_ATTRIBUTE_ASYNC_DISPATCH = "__scouter__ad__";
    public static final String REQUEST_ATTRIBUTE_CALLER_TRANSFER_MAP = "__scouter__ctm__";
    public static final String REQUEST_ATTRIBUTE_ALL_DISPATCHED_TRACE_CONTEXT = "__scouter__adtc__";
    public static final String REQUEST_ATTRIBUTE_SELF_DISPATCHED = "__scouter__sd__";
    public static final String TRACE_ID = "__scouter__txid__";
    public static final String TRACE_CONTEXT = "__scouter__tctx__";
    public static final String SUBS_DEPTH = "__scouter__subdepth__";
    public static final String SCOUTER_ADDED_FIELD = "__scouter__added__";

    public static final String ASYNC_SERVLET_DISPATCHED_PREFIX = "f>";

    private static final char at = '@';

    public static String normalizeHashCode(String text) {
        if(text == null) return text;
        int atPos = text.lastIndexOf(at);
        if(atPos<=0) {
            return text;
        }

        if(text.length() >= atPos + 8 + 1) {
            try {
                text = normalizeHashCode(text, atPos, 8);
            } catch (NumberFormatException e) {
                try {
                    text = normalizeHashCode(text, atPos, 7);
                } catch (NumberFormatException e1) {
                    return text;
                }
            }
        } else if(text.length() >= atPos + 7 + 1) {
            try {
                text = normalizeHashCode(text, atPos, 7);
            } catch (NumberFormatException e) {
                return text;
            }
        }
        return text;
    }

    private static String normalizeHashCode(String text, int atPos, int length) {
        String hexa = text.substring(atPos+1, atPos+1+length);
        Long.parseLong(hexa, 16);
        if (text.length() > atPos + length + 1) {
            return text.substring(0, atPos+1) + text.substring(atPos+1+length);
        } else {
            return text.substring(0, atPos+1);
        }
    }

    public static void main(String[] args) {
        String serviceName = "xxxiej.s@dfljoeif@1c2ba103";
        System.out.println(normalizeHashCode(serviceName));
    }
}
