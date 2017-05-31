/*
 *  Copyright 2015 the original author or authors. 
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); 
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License. 
 */

package scouter.util.logo;

import scouter.Version;
import scouter.util.*;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

public class Logo {
    public static void print() {
        print(false);
    }

    public static void print(boolean server) {
        if (server) {
            printDLogo();
        }

        InputStream in = null;
        try {
            String scouter_logo = System.getProperty("scouter.logo", "scouter.logo");
            in = Logo.class.getResourceAsStream(scouter_logo);
            if (in == null)
                return;
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line = reader.readLine();
            while (line != null) {
                if (server) {
                    System.out.println(new ParamText(line).getText(Version.getServerFullVersion()));
                } else {
                    System.out.println(new ParamText(line).getText(Version.getAgentFullVersion()));
                }
                line = reader.readLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            FileUtil.close(in);
        }
    }

    public static void print(PrintWriter w, boolean server) {
        InputStream in = null;
        try {
            String scouter_logo = System.getProperty("scouter.logo", "scouter.logo");
            in = Logo.class.getResourceAsStream(scouter_logo);
            if (in == null)
                return;
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line = reader.readLine();
            while (line != null) {
                if (server) {
                    w.println(new ParamText(line).getText(Version.getServerFullVersion()));
                } else {
                    w.println(new ParamText(line).getText(Version.getAgentFullVersion()));
                }
                line = reader.readLine();
            }
            w.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            FileUtil.close(in);
        }
    }

    private static void printDLogo() {
        final String keyFlag = "!@#$logo!@#$";
        final int flagLength = keyFlag.length();
        final String delim = ",";

        class LogoData {
            String key;
            String contents;

            @Override
            public String toString() {
                return "LogoData{" +
                        "key='" + key + '\'' +
                        ", contents='" + contents + '\'' +
                        '}';
            }
        }

        List<LogoData> arr = new ArrayList<LogoData>();

        InputStream in = null;
        try {
            in = Logo.class.getResourceAsStream("/scouter/util/logo/scouter-day.logo");
            if (in == null)
                return;

            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;
            boolean init = false;
            StringBuilder sb = null;
            LogoData logoData = null;
            while ((line = reader.readLine()) != null) {
                int flagPos = line.indexOf(keyFlag);
                if (flagPos >= 0) {
                    String key = line.substring(flagPos + flagLength);
                    if (init) {
                        logoData.contents = sb.toString();
                    }

                    logoData = new LogoData();
                    logoData.key = key;
                    arr.add(logoData);
                    sb = new StringBuilder(200);
                    init = true;
                } else {
                    if (init) {
                        sb.append(line).append(System.getProperty("line.separator"));
                    }
                }
                //System.out.println(line);
            }
            if (logoData != null) {
                logoData.contents = sb.toString();
            }

            List<LogoData> arrTodayLogo = new ArrayList();
            for (int i = arr.size(); i > 0; i--) {
                String[] dateFlags = StringUtil.tokenizer(arr.get(i - 1).key, delim);
                int len = dateFlags.length;
                if (len != 3) {
                    continue;
                }
                String yymmdd = DateUtil.yyyymmdd();
                //yymmdd="20180215";
                String yyyy = yymmdd.substring(0, 4);
                String mm = yymmdd.substring(4, 6);
                String dd = yymmdd.substring(6);

                if (match(yyyy, dateFlags[0]) && match(mm, dateFlags[1]) && match(dd, dateFlags[2])) {
                    arrTodayLogo.add(arr.get(i - 1));
                }
            }

            int todayLogoCount = arrTodayLogo.size();
            if (todayLogoCount > 0) {
                int pos;
                if (todayLogoCount == 1) {
                    pos = 0;
                } else {
                    Random r = new Random(System.currentTimeMillis());
                    pos = r.nextInt(todayLogoCount);
                    //pos = r.nextDouble()
                }

                String todayLogo = arrTodayLogo.get(pos).contents;
                System.out.println(todayLogo);
                System.out.println();
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            FileUtil.close(in);
        }
    }

    private static boolean match(String dateString, String input) {
        if(!Pattern.matches("^[\\*0-9*-]*$", input)) {
            return false;
        }
        if ("*".equals(input)) {
            return true;
        }
        if (dateString.equals(input)) {
            return true;
        }
        if (input.indexOf('-') >= 0) {
            String[] digit = StringUtil.tokenizer(input, "-");
            if (digit.length != 2) {
                return false;
            }
            int idata = CastUtil.cint(dateString);
            if (idata >= CastUtil.cint(digit[0]) && idata <= CastUtil.cint(digit[1])) {
                return true;
            }
        } else {
            if(CastUtil.cint(dateString) == CastUtil.cint(input)) {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) {
        printDLogo();

    }
}