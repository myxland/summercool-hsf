package org.summercool.hsf.test.normal;

import java.net.URL;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

public class EhcacheServiceImpl implements EhcacheService {

	private URL url;
	private CacheManager manager;
	private final Cache cache;

	public EhcacheServiceImpl() {
		url = EhcacheServiceImpl.class.getResource("/ehcache.xml");
		manager = new CacheManager(url);
		cache = manager.getCache("sample-offheap-cache");
	}

	public void setState(String key, State state) {
		cache.put(new Element(key, state));
	}

	public State getState(String key) {
		// Element element = cache.get(key);
		// Element element = null;
		// if (element == null) {
		// return null;
		// }
		// return (State) element.getValue();
		return null;
	}

}
