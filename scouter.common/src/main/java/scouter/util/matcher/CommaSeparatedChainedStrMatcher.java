package scouter.util.matcher;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 7. 8.
 */
public class CommaSeparatedChainedStrMatcher extends ChainedStrMatcher {
    public CommaSeparatedChainedStrMatcher(String patterns) {
        super(patterns, ",");
    }
}
