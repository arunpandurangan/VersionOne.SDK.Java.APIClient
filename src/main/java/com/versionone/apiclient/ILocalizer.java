package com.versionone.apiclient;

/**
 * Interface required for localizers
 */
public interface ILocalizer {

	/**
	 * Resolve the key to it's localized value
	 * 
	 * @param key - String
	 * @return Localized String value
	 */
	String resolve(String key);

}
