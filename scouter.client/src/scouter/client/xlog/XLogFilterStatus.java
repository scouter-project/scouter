package scouter.client.xlog;

import scouter.util.HashUtil;

public class XLogFilterStatus {
	
	public String objName = "";
	public String service = "";
	public String ip = "";
	public String startHmsFrom = "";
	public String startHmsTo = "";
	public String responseTimeFrom = "";
	public String responseTimeTo = "";
	public String login = "";
	public String desc = "";
	public String hasDumpYn = "";
	public String text1 = "";
	public String text2 = "";
	public String text3 = "";
	public String text4 = "";
	public String text5 = "";
	public String userAgent = "";
	public boolean onlySql;
	public boolean onlyApicall;
	public boolean onlyError;
	public boolean onlySync;
	public boolean onlyAsync;
	public String profileSizeText = "";
	public String profileBytesText = "";

	@Override
	public int hashCode() {
		int filter_hash = HashUtil.hash(objName);
		filter_hash ^= HashUtil.hash(service);
		filter_hash ^= HashUtil.hash(ip);
		filter_hash ^= HashUtil.hash(startHmsFrom);
		filter_hash ^= HashUtil.hash(startHmsTo);
		filter_hash ^= HashUtil.hash(responseTimeFrom);
		filter_hash ^= HashUtil.hash(responseTimeTo);
		filter_hash ^= HashUtil.hash(login);
		filter_hash ^= HashUtil.hash(desc);
		filter_hash ^= HashUtil.hash(hasDumpYn);
		filter_hash ^= HashUtil.hash(text1);
		filter_hash ^= HashUtil.hash(text2);
		filter_hash ^= HashUtil.hash(text3);
		filter_hash ^= HashUtil.hash(text4);
		filter_hash ^= HashUtil.hash(text5);
		filter_hash ^= HashUtil.hash(userAgent);
		filter_hash ^= HashUtil.hash(onlyError ? "onlyError" : "");
		filter_hash ^= HashUtil.hash(onlySql ? "onlySql" : "");
		filter_hash ^= HashUtil.hash(onlyApicall ? "onlyApicall" : "");
		filter_hash ^= HashUtil.hash(onlySync ? "onlySync" : "");
		filter_hash ^= HashUtil.hash(onlyAsync ? "onlyAsync" : "");
		filter_hash ^= HashUtil.hash(profileSizeText);
		filter_hash ^= HashUtil.hash(profileBytesText);
		return filter_hash;
	}
	
	public XLogFilterStatus clone() {
		XLogFilterStatus status = new XLogFilterStatus();
		status.objName = objName;
		status.service = service;
		status.ip = ip;
		status.startHmsFrom = startHmsFrom;
		status.startHmsTo = startHmsTo;
		status.responseTimeFrom = responseTimeFrom;
		status.responseTimeTo = responseTimeTo;
		status.login = login;
		status.desc = desc;
		status.hasDumpYn = hasDumpYn;
		status.text1 = text1;
		status.text2 = text2;
		status.text3 = text3;
		status.text4 = text4;
		status.text5 = text5;
		status.userAgent = userAgent;
		status.onlySql = onlySql;
		status.onlyApicall = onlyApicall;
		status.onlyError = onlyError;
		status.onlySync = onlySync;
		status.onlyAsync = onlyAsync;
		return status;
	}
}
