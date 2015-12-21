package scouter.util;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by gunlee on 2015. 12. 11..
 */
public class ParamTextTest {

    @Test
    public void testGetText() throws Exception {

        Map<Object, Object> args = new HashMap<Object, Object>();
        args.putAll(System.getenv());
        args.putAll(System.getProperties());
        args.put("df","df-value");

        String text = new ParamText(StringUtil.trim("abc${df}df${df}xxx")).getText(args);
        System.out.println(text);

    }

    @Test
    public void testToString() throws Exception {

    }

    @Test
    public void testGetKeyList() throws Exception {

    }
}