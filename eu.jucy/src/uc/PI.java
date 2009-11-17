package uc;

import java.io.File;

import logger.LoggerFactory;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.BackingStoreException;


import uc.crypto.IHashEngine;
import uc.database.IDatabase;

/**
 * the preference initialiser ...
 * initialised our default values ...
 * and holds the keys to access the values
 * 
 * @author Quicksilver
 *
 */
public class PI extends AbstractPreferenceInitializer {
	
	private static final Logger logger = LoggerFactory.make();





	public static final String PLUGIN_ID= "eu.jucy";

	public PI() {
	}

	//config names
	public static final String 	
	downloadDirectory	=	"DownloadDirectory",
	tempDownloadDirectory=	"TempDownloadDirectory",
	changedTempDownloadDirectory=	"changedTempDownloadDirectory",
	
	storagePath			=	"StoragePath",
	inPort				=	"InPort",
	tlsPort				=	"tlsPort",
	bindAddress			=	"bindAddress",
	
	allowTLS			=	"allowTLS",
	description			=	"Description", //default userdescription
	connection			=	"Connection", //what connection IE 0.2 or 20 MBit
	udpPort				=	"UDPPort",
	slots				=	"Slots",
	nick				=	"Nick",
	defaultAFKMessage	=	"defaultAFKMessage",
	
	
	externalIp			=	"ExternalIp",
	eMail 				=	"EMail" ,
	passive				=	"passive",
	allowUPnP			=	"allowUPnP",
	maxSimDownloads		=	"MaxSimDownloads",
	downloadLimit		=	"DownloadLimit",
	uploadLimit			=	"UploadLimit",
	autoFollowRedirect	= 	"autoFollowRedirect",
	autoSearchForAlternates = "autoSearchForAlternates",
	
	//ProxyStuff
	socksProxyEnabled	=	"socksProxyEnabled",
	socksProxyHost		=	"socksProxyHost", 
	socksProxyPort		=	"socksProxyPort",
	socksProxyUsername 	=	"socksProxyUsername",//"java.net.socks.username",
	socksProxyPassword	=	"socksProxyPassword",//"java.net.socks.password", 
	
		
	shareHiddenFiles	=	"shareHiddenFiles",
	includeFiles		=	"includeFiles",
	excludedFiles		=	"excludedFiles",
	deleteFilelistsOnExit=	"deleteFilelistsOnExit", 
	filelistRefreshInterval= "filelistRefreshInterval",
	
	maxHashSpeed		= "maxHashSpeed",

	uUID				=	"PrivateID",
	
	logTimeStamps		=	"logTimeStamps",
	//loggingPath			=	"loggingPath",
	logMainChat 		= 	"logMainChat",
	logPM				=	"logPM",
	logFeed				=	"logFeed",
	
	
	
	
	
	checkForUpdates		=	"checkForUpdates",
	lastCheckForUpdates =	"lastCheckForUpdates";
	
	


	
	/**
	 * subnode  shareddirs
	 * vname is used for node name
	 */
	public static final String
	sharedDirs2			=	"sharedDirs2", //new Node.... containing everything..
	
	//old legacy stuff.. only used for loading in legacy mode.. replaced by sharedDirs2
	sharedDirs			=	"sharedDirs", //the node
	sharedDirsName		=	"vName", //the top name/ID for the directory
	sharedDirsdirectory	=	"directory",
	SharedDirslastShared=	"lastShared";
	//lastChanged	=	"lastChanged";
	
	
	/**
	 * subnode  FavDirs
	 *  names are used from Shared dirs
	 */
	public static final String
	favDirs		=	"favDirs";
	
	/** 
	 * subnode FavHubs
	 * int order is used for node name
	 * 
	 * unused.. only for legacy stuff used ->
	 * replaced by favHubs2
	 */
	public static final String 
	favHubs		=	"favHubs",  //the node itself
	favHubAutoconnect =	"autoconnect",
	favHubhubname		=	"hubname",	
	favHubhubaddy		=	"hubaddy",
	//description ="description", taken from above
	//nick taken from above
	favHubpassword	=	"password",
	favHubuserDescription="userDescription",
	//eMail  used from above
	favHubchatOnly	=	"ChatOnly",
	favHubweights		=	"weights",
	favCharset			=	"charset",
	favInfo				=	"info";	 //other stuff stored on the hub..
	
	/**
	 * modern favHubs... one string containing all hub info...
	 */
	public static final String favHubs2 = "favHubs2";
	
	public static final String LegacyMode = "LEGACY";


	
	@Override
	public void initializeDefaultPreferences() {
		IEclipsePreferences defaults = new DefaultScope().getNode(PI.PLUGIN_ID);
		
		defaults.put(description, "");
		defaults.put(connection, "0.02");

		String homedir = new File(Platform.getInstanceLocation().getURL().getFile()).getPath();
		defaults.put(storagePath, homedir);
		
		File systemHome = new File(System.getProperty("user.home"));
		
		defaults.put(downloadDirectory,new File(new File(systemHome,"Downloads"),"jucy").getPath() ); // homedir+File.separator+"FinishedDownloads"
		
		defaults.put(tempDownloadDirectory, homedir+File.separator+"Downloads"  );
		defaults.put(changedTempDownloadDirectory, "");

		
		defaults.put(eMail, "");
		

		defaults.put(externalIp, "" );
		

		defaults.put(defaultAFKMessage, "I am currently away. Feel free to leave a message.");
		
		
		defaults.putInt(inPort,  9086);
		defaults.putInt(tlsPort, 9087);
		defaults.putBoolean(allowTLS, true );

		defaults.put(bindAddress, "");
		
		defaults.put( nick ,System.getProperty("user.name") );
		
		
		defaults.putBoolean(passive, false); 
		defaults.putBoolean(allowUPnP, true);
		
		defaults.putInt(slots, 2);
		
		
		defaults.putInt(udpPort, 9086 );
		
		defaults.put(uUID, ""); //
		
		
		defaults.putInt(maxSimDownloads, 0);
		defaults.putInt(downloadLimit, 0);
		defaults.putInt(uploadLimit, 0);

		defaults.putBoolean(shareHiddenFiles, false);
		defaults.put(includeFiles, ".*");
		defaults.put( excludedFiles, "\\.antifrag$|^__INCOMPLETE___|^download[0-9]{16,18}\\.dat$|" +
				"^INCOMPLETE~|\\.dctmp$|part\\.met|\\.bc!$|\\.!ut$|\\.bt!$|" + //newer and older p2p stuff..
				"\\.mp3\\.exe$|\\.avi\\.exe$"); //Viruses
		
		defaults.putBoolean(deleteFilelistsOnExit, true);
		defaults.putInt(filelistRefreshInterval, 60);
		
		defaults.putInt(maxHashSpeed, 0);
		
		defaults.putBoolean(autoFollowRedirect, true);
		defaults.putBoolean(autoSearchForAlternates, true);
		
		defaults.putBoolean(socksProxyEnabled, false);
		defaults.put(socksProxyHost, "");
		defaults.putInt(socksProxyPort, 1080);
		defaults.put(socksProxyUsername, System.getProperty("user.name"));
		defaults.put(socksProxyPassword,""); 
		
	
		defaults.putBoolean(logMainChat, false);
		defaults.putBoolean(logPM, true);
		defaults.putBoolean(logFeed, true);
		defaults.put(logTimeStamps, "[dd.MM. HH:mm]");
		
		defaults.putBoolean(checkForUpdates	, true);
		defaults.putLong(lastCheckForUpdates, 0);
		
		defaults.put(favHubs2, LegacyMode);
		defaults.put(sharedDirs2, LegacyMode);
		/*
		 * initialising values for extension point loading
		 */
		defaults.put(IHashEngine.ExtensionpointID, IHashEngine.defaultID);
		defaults.put(IDatabase.ExtensionpointID, IDatabase.defaultID);
	}





	public static boolean put(String key,int value) {
		return put(key,""+value);
	}





	public static boolean put(String key,long value) {
		return put(key,""+value);
	}





	public static boolean put(String key,boolean value) {
		return put(key,""+value);
	}





	/**
	 * tries setting a value in the InstanceScope  
	 * @param key - 
	 * @param value - 
	 * @return true if successful set - false otherwise
	 */
	public static boolean put(String key,String value) {
		IEclipsePreferences is = new InstanceScope().getNode(PI.PLUGIN_ID);
		is.put(key, value);
		try {
			is.flush();
		} catch(BackingStoreException bse) {
			logger.warn(bse,bse);
			return false;
		}
		return true;
	}





	public static File getFileListPath() {
		return new File(getStoragePath(),"FileLists");
	}





	public static File getTempDownloadDirectory() {
		File f = new File(get(tempDownloadDirectory));
		if (!f.isDirectory()) {
			if (!f.mkdirs()) {
				logger.error("could not create Tempdir");
			}
		}
		return f;
	}





	/**
	 * @return the storagePath
	 */
	public static File getStoragePath() {
		return new File(Platform.getInstanceLocation().getURL().getFile());
	}





	public static IEclipsePreferences get() {
		return new InstanceScope().getNode(PI.PLUGIN_ID);
	}





	public static boolean getBoolean(String what){
		return Boolean.parseBoolean(get(what));
	}





	public static long getLong(String what) {
		return Long.parseLong(get(what));
	}





	public static int getInt(String what) {
		return Integer.parseInt(get(what));
	}





	public static String get(String what ) {
		String s = new InstanceScope().getNode(PI.PLUGIN_ID).get(what, null);
		if (s != null) {
			return s;
		}
		s = new ConfigurationScope().getNode(PI.PLUGIN_ID).get(what,null);
		if (s != null) {
			return s;
		}
		
		return new DefaultScope().getNode(PI.PLUGIN_ID).get(what, null);
	}


}
