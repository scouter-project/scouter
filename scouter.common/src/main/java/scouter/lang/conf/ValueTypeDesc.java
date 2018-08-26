/*
 *  Copyright 2015 the original author or authors.
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package scouter.lang.conf;

import scouter.lang.pack.MapPack;
import scouter.lang.value.ListValue;
import scouter.lang.value.MapValue;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2018. 8. 25.
 */
public class ValueTypeDesc {
    public final static String STRINGS = "strings";
    public final static String STRINGS1 = "strings1";
    public final static String STRINGS2 = "strings2";
    public final static String STRINGS3 = "strings3";
    public final static String BOOLEANS = "booleans";
    public final static String BOOLEANS1 = "booleans1";
    public final static String INTS = "ints";
    public final static String INTS1 = "ints1";

    String[] strings;
    String[] strings1;
    String[] strings2;
    String[] strings3;
    boolean[] booleans;
    boolean[] booleans1;
    int[] ints;
    int[] ints1;

    public String[] getStrings() {
        return strings;
    }

    public void setStrings(String[] strings) {
        this.strings = strings;
    }

    public String[] getStrings1() {
        return strings1;
    }

    public void setStrings1(String[] strings1) {
        this.strings1 = strings1;
    }

    public String[] getStrings2() {
        return strings2;
    }

    public void setStrings2(String[] strings2) {
        this.strings2 = strings2;
    }

    public String[] getStrings3() {
        return strings3;
    }

    public void setStrings3(String[] strings3) {
        this.strings3 = strings3;
    }

    public boolean[] getBooleans() {
        return booleans;
    }

    public void setBooleans(boolean[] booleans) {
        this.booleans = booleans;
    }

    public boolean[] getBooleans1() {
        return booleans1;
    }

    public void setBooleans1(boolean[] booleans1) {
        this.booleans1 = booleans1;
    }

    public int[] getInts() {
        return ints;
    }

    public void setInts(int[] ints) {
        this.ints = ints;
    }

    public int[] getInts1() {
        return ints1;
    }

    public void setInts1(int[] ints1) {
        this.ints1 = ints1;
    }

    public MapValue toMapValue() {
        return toMapPack().toMapValue();
    }

    public MapPack toMapPack() {
        MapPack pack = new MapPack();

        ListValue stringsLv = new ListValue();
        stringsLv.add(this.strings);
        ListValue strings1Lv = new ListValue();
        strings1Lv.add(this.strings1);
        ListValue strings2Lv = new ListValue();
        strings2Lv.add(this.strings2);
        ListValue strings3Lv = new ListValue();
        strings3Lv.add(this.strings3);

        ListValue booleanLv = new ListValue();
        booleanLv.add(this.booleans);
        ListValue boolean1Lv = new ListValue();
        boolean1Lv.add(this.booleans1);

        ListValue intLv = new ListValue();
        intLv.add(this.ints);
        ListValue int1Lv = new ListValue();
        int1Lv.add(this.ints1);

        pack.put(STRINGS, stringsLv);
        pack.put(STRINGS1, strings1Lv);
        pack.put(STRINGS2, strings2Lv);
        pack.put(STRINGS3, strings3Lv);
        pack.put(BOOLEANS, booleanLv);
        pack.put(BOOLEANS1, boolean1Lv);
        pack.put(INTS, intLv);
        pack.put(INTS1, int1Lv);

        return pack;
    }

    public static ValueTypeDesc of(MapValue mapValue) {
        ValueTypeDesc vd = new ValueTypeDesc();

        ListValue stringLv = mapValue.getList(STRINGS);
        String[] strings = new String[stringLv.size()];
        for (int i = 0; i < stringLv.size(); i++) {
            strings[i] = stringLv.getString(i);
        }

        ListValue string1Lv = mapValue.getList(STRINGS1);
        String[] strings1 = new String[string1Lv.size()];
        for (int i = 0; i < string1Lv.size(); i++) {
            strings1[i] = string1Lv.getString(i);
        }

        ListValue string2Lv = mapValue.getList(STRINGS2);
        String[] strings2 = new String[string2Lv.size()];
        for (int i = 0; i < string2Lv.size(); i++) {
            strings2[i] = string2Lv.getString(i);
        }

        ListValue string3Lv = mapValue.getList(STRINGS3);
        String[] strings3 = new String[string3Lv.size()];
        for (int i = 0; i < string3Lv.size(); i++) {
            strings3[i] = string3Lv.getString(i);
        }

        ListValue booleanLv = mapValue.getList(BOOLEANS);
        boolean[] booleans = new boolean[booleanLv.size()];
        for (int i = 0; i < booleanLv.size(); i++) {
            booleans[i] = booleanLv.getBoolean(i);
        }

        ListValue boolean1Lv = mapValue.getList(BOOLEANS1);
        boolean[] booleans1 = new boolean[boolean1Lv.size()];
        for (int i = 0; i < boolean1Lv.size(); i++) {
            booleans1[i] = boolean1Lv.getBoolean(i);
        }

        ListValue intLv = mapValue.getList(INTS);
        int[] ints = new int[intLv.size()];
        for (int i = 0; i < intLv.size(); i++) {
            ints[i] = intLv.getInt(i);
        }

        ListValue int1Lv = mapValue.getList(INTS1);
        int[] ints1 = new int[int1Lv.size()];
        for (int i = 0; i < int1Lv.size(); i++) {
            ints1[i] = int1Lv.getInt(i);
        }

        vd.setStrings(strings);
        vd.setStrings1(strings1);
        vd.setStrings2(strings2);
        vd.setStrings3(strings3);
        vd.setBooleans(booleans);
        vd.setBooleans1(booleans1);
        vd.setInts(ints);
        vd.setInts1(ints1);

        return vd;
    }
}
