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
 *
 */
package scouter.client.preferences;

import scouter.util.StringUtil;

public class ServerPrefUtil {
	
	public static void storeDefaultServer(String addr) {
		PManager.getInstance().setValue(PreferenceConstants.P_SVR_DEFAULT, addr);
	}
	
	public static void addServerAddr(String addr) {
		String addrs = PManager.getInstance().getString(PreferenceConstants.P_SVR_LIST);
		if (StringUtil.isEmpty(addrs)) {
			 PManager.getInstance().setValue(PreferenceConstants.P_SVR_LIST, addr);
		} else {
			if (addrs.contains(addr) == false) {
				addrs += (PreferenceConstants.P_SVR_DIVIDER + addr);
				PManager.getInstance().setValue(PreferenceConstants.P_SVR_LIST, addrs);
			}
		}
	}
	
	public static void addAutoLoginServer(String addr, String id, String encryptedPass, String socksAddr) {
		String addrs = PManager.getInstance().getString(PreferenceConstants.P_SVR_AUTOLOGIN_LIST);
		if (StringUtil.isEmpty(addrs)) {
			 PManager.getInstance().setValue(PreferenceConstants.P_SVR_AUTOLOGIN_LIST, addr);
		} else {
			if (addrs.contains(addr) == false) {
				addrs += (PreferenceConstants.P_SVR_DIVIDER + addr);
				PManager.getInstance().setValue(PreferenceConstants.P_SVR_AUTOLOGIN_LIST, addrs);
			}
		}
		
		
		String socksAddrs = PManager.getInstance().getString(PreferenceConstants.P_SOCKS_SVR_AUTOLOGIN_LIST);
		if (StringUtil.isEmpty(socksAddrs)) {
			 PManager.getInstance().setValue(PreferenceConstants.P_SOCKS_SVR_AUTOLOGIN_LIST, socksAddr);
		} else {
			if (socksAddrs.contains(socksAddr) == false) {
				socksAddrs += (PreferenceConstants.P_SVR_DIVIDER + socksAddr);
				PManager.getInstance().setValue(PreferenceConstants.P_SOCKS_SVR_AUTOLOGIN_LIST, socksAddrs);
			}
		}
		
		PManager.getInstance().setValue(PreferenceConstants.P_SVR_ACCOUNT_PREFIX + addr, id + PreferenceConstants.P_SVR_DIVIDER + encryptedPass);
		PManager.getInstance().setValue(PreferenceConstants.P_SOCKS_SVR_ADDR_PREFIX + addr, socksAddr);
	}
	
	public static void removeServerAddr(String addr) {
		String addrs = PManager.getInstance().getString(PreferenceConstants.P_SVR_LIST);
		if (addrs != null && addrs.contains(addr)) {
			String str = pickOutString(addrs, addr, PreferenceConstants.P_SVR_DIVIDER);
			PManager.getInstance().setValue(PreferenceConstants.P_SVR_LIST, str);
		}
		removeAutoLoginServer(addr);
	}
	
	public static void removeAutoLoginServer(String addr) {
		String addrs = PManager.getInstance().getString(PreferenceConstants.P_SVR_AUTOLOGIN_LIST);
		if (addrs != null && addrs.contains(addr)) {
			String str = pickOutString(addrs, addr, PreferenceConstants.P_SVR_DIVIDER);
			PManager.getInstance().setValue(PreferenceConstants.P_SVR_AUTOLOGIN_LIST, str);
		}
		PManager.getInstance().setValue(PreferenceConstants.P_SVR_ACCOUNT_PREFIX + addr, "");
	}
	
	public static String getStoredDefaultServer() {
		return PManager.getInstance().getString(PreferenceConstants.P_SVR_DEFAULT);
	}
	
	public static String[] getStoredServerList() {
		String addrs = PManager.getInstance().getString(PreferenceConstants.P_SVR_LIST);
		return StringUtil.tokenizer(addrs, PreferenceConstants.P_SVR_DIVIDER);
	}
	
	public static String[] getStoredSocks5ServerList() {
		String addrs = PManager.getInstance().getString(PreferenceConstants.P_SOCKS_SVR_AUTOLOGIN_LIST);
		return StringUtil.tokenizer(addrs, PreferenceConstants.P_SVR_DIVIDER);
	}
	
	public static String[] getStoredAutoLoginServerList() {
		String addrs = PManager.getInstance().getString(PreferenceConstants.P_SVR_AUTOLOGIN_LIST);
		return StringUtil.tokenizer(addrs, PreferenceConstants.P_SVR_DIVIDER);
	}
	
	public static String getStoredAccountInfo(String addr) {
		return PManager.getInstance().getString(PreferenceConstants.P_SVR_ACCOUNT_PREFIX + addr);
	}
	
	public static boolean isSocksLogin(String addr) {
		return StringUtil.isNotEmpty(PManager.getInstance().getString(PreferenceConstants.P_SOCKS_SVR_ADDR_PREFIX + addr));
	}
	
	public static String getStoredSocksServer(String addr) {
		return PManager.getInstance().getString(PreferenceConstants.P_SOCKS_SVR_ADDR_PREFIX + addr);
	}
	
	public static boolean isAutoLoginAddress(String addr) {
		String addrs = PManager.getInstance().getString(PreferenceConstants.P_SVR_AUTOLOGIN_LIST);
		return addrs.contains(addr);
	}
	
	public static String pickOutString(String fullStr, String pickStr, String delimeter) {
		if (fullStr.equals(pickStr)) {
			return "";
		}
		int index = fullStr.indexOf(pickStr);
		if (index < 0) {
			return fullStr;
		}
		if (index == 0) {
			return fullStr.substring(pickStr.length() + delimeter.length(), fullStr.length());
		} else {
			return fullStr.substring(0, index - delimeter.length()) + fullStr.substring(index + pickStr.length(), fullStr.length());
		}
	}
	
	public static void main(String[] args) {
		String fullStr = "gggg,tytytyty,abc,ggg,kdkdkd";
		String pickStr = "abc";
		System.out.println(pickOutString(fullStr, pickStr, PreferenceConstants.P_SVR_DIVIDER));
	}
}
