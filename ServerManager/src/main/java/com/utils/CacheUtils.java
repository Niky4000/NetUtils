package com.utils;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;

public class CacheUtils {

	public static void destroyCacheIfItExists(Ignite ignite, String cacheName) {
		try {
			IgniteCache<Object, Object> cache = ignite.cache(cacheName);
			cache.destroy();
		} catch (Exception e) {
			// Ignore it!
		}
	}
}
