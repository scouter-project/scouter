package scouter.util.matcher;

import scouter.util.StrMatch;
import scouter.util.StringUtil;

import java.util.ArrayList;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 7. 8.
 */
public class ChainedStrMatcher {
    ArrayList<StrMatch> strMatches = new ArrayList<StrMatch>();

    public ChainedStrMatcher(String patterns, String separator) {
        this(patterns, separator, '*');
    }

    public ChainedStrMatcher(String patterns, String separator, char c) {
        String[] arrPatterns = StringUtil.split(patterns, separator);
        for (String pattern : arrPatterns) {
            strMatches.add(new StrMatch(pattern));
        }
    }

    public boolean isMatch(String target) {
        for (StrMatch match : strMatches) {
            if (match.include(target)) {
                return true;
            }
        }
        return false;
    }
}
