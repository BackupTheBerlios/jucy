package eu.jucy.ui.searchspy;

import uc.DCClient;
import helpers.NLS;


/**
* This is a automatically generated file! DO NOT CHANGE!!
* see eu.jucy.releng.languages -> CreateLangFile for more details
*/
public class Lang {

	public static String
		 SPYAverage
		,SPYCount
		,SPYDescription
		,SPYHideTTHSearches
		,SPYHitRatio
		,SPYHits
		,SPYHitsFormatted
		,SPYSearchSpy
		,SPYSearchString
		,SPYTime
		,SPYTotal ;
	
	static {
		try {
			NLS.load("nl.searchspy", Lang.class);
		} catch(RuntimeException re) {
			DCClient.logger.warn(re,re);
		}
	}
}