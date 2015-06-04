package scouter.agent.proxy;


public interface IHttpClient {
	public String getHost(Object o);
	public void addHeader(Object o, String key, String value);
	public String getURI(Object o);
	public String getHeader(Object o, String key);
}