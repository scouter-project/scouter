package scouter.agent;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 2. 5.
 * Common constants for scouter agent.
 *
 */
public class AgentCommonConstant {
    public static final String SPRING_REQUEST_MAPPING_POSTFIX_FLAG = " [:SRM]";
    public static final String REQUEST_ATTRIBUTE_INITIAL_TRACE_CONTEXT = "__scouter__itc__";
    public static final String REQUEST_ATTRIBUTE_TRACE_CONTEXT = "__scouter__tc__";
    public static final String REQUEST_ATTRIBUTE_ASYNC_DISPATCH = "__scouter__ad__";
    public static final String REQUEST_ATTRIBUTE_CALLER_TRANSFER_MAP = "__scouter__ctm__";
    public static final String REQUEST_ATTRIBUTE_ALL_DISPATCHED_TRACE_CONTEXT = "__scouter__adtc__";
    public static final String REQUEST_ATTRIBUTE_SELF_DISPATCHED = "__scouter__sd__";

    public static final String ASYNC_SERVLET_DISPATCHED_PREFIX = "f>";

    private static final char at = '@';

    /**
     * remove " [:SRM]" from service name
     */
    public static String removeSpringRequestMappingPostfixFlag(String org) {
        int pos = org.indexOf(SPRING_REQUEST_MAPPING_POSTFIX_FLAG);
        if(pos < 0) return org;
        String pre = org.substring(0, pos);
        if(org.length() > pre.length() + SPRING_REQUEST_MAPPING_POSTFIX_FLAG.length()) {
            return pre + org.substring(pos + SPRING_REQUEST_MAPPING_POSTFIX_FLAG.length());
        } else {
            return pre;
        }
    }

    public static String normalizeHashCode(String text) {
        if(text == null) return text;
        int atPos = text.lastIndexOf(at);
        if(atPos > 0 && text.length() >= atPos + 8 + 1) {
            String hexa = text.substring(atPos+1, atPos+1+8);
            try {
                Long.parseLong(hexa, 16);
            } catch (NumberFormatException e) {
                return text;
            }
            if (text.length() > atPos + 8 + 1) {
                return text.substring(0, atPos+1) + text.substring(atPos+1+8);
            } else {
                return text.substring(0, atPos+1);
            }
        }
        return text;
    }

    public static void main(String[] args) {
        String serviceName = "xxxiej.s@dfljoeif@0000000f";
        System.out.println(normalizeHashCode(serviceName));
    }
}
