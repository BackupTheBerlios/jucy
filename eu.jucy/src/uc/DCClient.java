/*
 * DCClient.java
 *
 * Created on 11. November 2005, 22:00
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */
package uc;


import helpers.GH;
import helpers.Version;

import java.io.File;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.ref.WeakReference;




import java.security.Security;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.TimeUnit;



import logger.LoggerFactory;


import org.apache.log4j.Logger;





import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;



import uc.InfoChange.IInfoChanged;
import uc.User.Mode;
import uc.crypto.HashValue;
import uc.crypto.IHashEngine;
import uc.crypto.Tiger;
import uc.crypto.TigerHashValue;


import uc.database.IDatabase;
import uc.files.IUploadQueue;
import uc.files.UploadQueue;
import uc.files.downloadqueue.DownloadQueue;

import uc.files.filelist.FileList;
import uc.files.filelist.HashedFile;
import uc.files.filelist.IFilelistProcessor;
import uc.files.filelist.IOwnFileList;
import uc.files.filelist.OwnFileList;
import uc.files.search.AutomaticSearchForAlternatives;
import uc.files.search.FileSearch;
import uc.files.search.ISearchResult;
import uc.files.search.SearchResult.ISearchResultListener;
import uc.files.transfer.SlotManager;


import uc.listener.IUserChangedListener;
import uc.protocols.ConnectionState;
import uc.protocols.Socks;
import uc.protocols.hub.Hub;



/** 
 *
 *
 *  
 *
 * Presentation of magnet -> save as button .. or play button could be added / may be a view button also..
 * 
 * TODO functionality to store send messages until both users are online if other is offline..
 * 
 * My Personal Bugtracker:
 * 
 *  
 *  
 *  TODO full text indexing  ... adding PDF txt html.. mp3 tags.. avi tags.. wmv tags..
 *  for pdfs http://incubator.apache.org/pdfbox/
 *   --> for all apache tika
 *  
 *  TODO may be think about torrent support in dc client -> starting torrents downloaded..
 *  
 *  
 *  TODO spiel einbauen... Schiffe versenken
 *  
 * 
 * 
 * /me in PM TODO not really done I think..
 * 
 * 
 * TODO after finished downloading file .. may be check if should be hashed and immediately shared..
 *
 *
 * TODO ... new special user categorie for tons of settings..
 * i.e. special treatment for PMs ... and and and..
 * 
 * TODO transferred size boxes in statusline could on click change between showing total
 * vs showing only current session upload.. -> store uploaded/downloaded at end of session
 * or current upload vs average upload.. 
 * -> or may be rather add possibility for a total transferred box..
 * 
 *   
 * TODO store tested segments in DB... -> no rehashing for everything on start... 
 * could save lots of computing power..
 * 
 * 
 * 
 * TODO LuaJava plugin... -> wait till java 6 mandatory
 * 
 * TODO hideshare functionality.. -> better feature would be different filelists
 * on a per hub basis.. hideshare = no filelist..
 * 
 * TODO .. adding massive ammount of DQ entries makes jucy unstable...
 * -> especially closing will result on failed restore operations...
 * 
 * GET on partial FileLists not implemented.. -> ADC compatibility.. DONE --> needs testing ->> TODO unit test
 * 
 * TODO always reconnecting peer requesting the same interval over and over and over have to be stopped..
 * --> slots stay open  bandwidth is wasted..
 * 
 * TODO  may be some statistics window?? drawing what is currently transferred...
 *
 * TODO translation function : http://code.google.com/p/google-api-translate-java/ might be good
 *
 * 
 * TODO preview of files using some mediaplayer...
 * 
 * TODO test maximise on jucy icon  under kde  3 doubleclicks or 
 * 3 times clicking on Maximise.. needed according to hali -> might be problem with no active workbench window if minimized..
 * 
 * Low Priority:
 *  - integration of winamp/amarok(Linux) plugin; .. really? for learning jni would be nice..
 *  - write unit test for hashengine.. 
 *  - some SWT stuff.. need Documentation for jucy
 *  
 * TODO warning when running out of space.. --> not possible due to Missing api in 1.5
 *  
 * @author Quicksilver
 */
public final class DCClient {
 
	public  static final Logger logger = LoggerFactory.make();
	
	private static Initializer init = new Initializer();
	
	public static void setInitializer(Initializer initializer) {
		synchronized(synchSingleton) {
			init = initializer;
		}
	}
	

	
	private static volatile long debugcount = 0;
	
	
	public static void execute(final Runnable r) {
		if (++debugcount % 5000 == 0) {
			logger.debug("Totally submitted tasks "+debugcount);
		}
		exec.execute(
			new Runnable() {
				public void run() {
					try {
						r.run();
					} catch(Throwable e) {
						logger.error("execution of runnable failed.."+e+" isShutdown:"+exec.isShutdown(), e);
					}
				}
			}); 
	}
	
	/**
	 * timer replacement..
	 */
	private final ScheduledExecutorService scheduler;
		
	public static ScheduledExecutorService getScheduler() {
		return DCClient.get().scheduler;
	}
	
	public ScheduledExecutorService getSchedulerDir() {
		return scheduler;
	}
	
	{
		ScheduledThreadPoolExecutor ste = new ScheduledThreadPoolExecutor(5);
		ste.setRejectedExecutionHandler(new CallerRunsPolicy());
		scheduler = Executors.unconfigurableScheduledExecutorService(ste);
	}
	
	/**
	 * cached Thread pool executor with  CallerRunsPolicy to better find problems..
	 * at least better than discard policy..
	 */
	private static final ExecutorService exec = new ThreadPoolExecutor(0, 250,
            60L, TimeUnit.SECONDS,
            new SynchronousQueue<Runnable>(),
            new CallerRunsPolicy() 
            );
	


	private static DCClient singleton;
	private static final Object synchSingleton = new Object();
	
	public static DCClient get() {
		synchronized(synchSingleton) {
			return singleton;
		}
	}
	
	private final User fileListSelf; //the self that holds our own FileList

	private HashValue pid; 
	
	/**
	 * check if FileList was initialised ..
	 *  fall back mechanism if FileList initialiser in the beginning wasn't called
	 *  so client still has a legal startup
	 */
	private Future<?> fileListInitialised;
	
	
	
	private final Object synchAway = new Object();
	private  boolean away = false;
	private String awayMessage;
	
	/**
	 * running hubs..
	 */
	private final Map<FavHub,Hub> hubs	= 
		Collections.synchronizedMap(new HashMap<FavHub,Hub>());
	


	/**
	 * maps an IP-string  to an hub .. this is needed so SearchResults can resolve users
	 * ex.  87.35.157.86
	 * no DNS mapping!
	 */
	private final Map<String,WeakReference<Hub>> ipToHub = 
		Collections.synchronizedMap( new HashMap<String,WeakReference<Hub>>());


	private final CopyOnWriteArrayList<ISearchResultListener> searchListeners = 
			new CopyOnWriteArrayList<ISearchResultListener>();
	
	private final CopyOnWriteArrayList<IHubCreationListener> hublisteners = 
			new CopyOnWriteArrayList<IHubCreationListener>();
	
	private final CopyOnWriteArrayList<ISearchReceivedListener> srl =
			new CopyOnWriteArrayList<ISearchReceivedListener>();
	
	private final CopyOnWriteArrayList<IInfoChanged> changedInfo =
		new CopyOnWriteArrayList<IInfoChanged>();
	
	private final Set<InfoChange> changes = 
		Collections.synchronizedSet(new HashSet<InfoChange>());
	

	private final ConnectionHandler ch;  

	private final IUploadQueue upQueue ;
	private final IUploadQueue downQueue ;
	
	
	
	private final IConnectionDeterminator connectionDeterminator;

	private final IFavHubs favHubs;
	private final FavFolders favFolders;

	private final ISlotManager slotManager;
	
	private final Population population;
	private final DownloadQueue downloadQueue;



	private volatile ScheduledFuture<?> myInfoUpdater = null;
	
	/**
	 * current version as it is used in 
	 * Description
	 */
	public static final String VERSION; 
	
	/**
	 * longer version string for human readability..
	 */
	public static final String LONGVERSION;
	
	static {
		String v = " V:" + Version.getVersion();
		VERSION = "UC"+v;  
		LONGVERSION = "jucy"+v;
		
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			public void uncaughtException(Thread t, Throwable e) {
				logger.warn(e,e);
			}
		});
		
	//	logger.debug("force loading BC: "+loader.Activator.class.getName()); //forces loading of Bouncycastle..
	/*	Bundle b = Platform.getBundle("lib.org.bouncycastle.bcprov");
		logger.debug("got bundle "+ (b != null));
		try {
			b.start();
		} catch (Exception e) {
			logger.error(e,e);
		} */
		//int pos = //Security.insertProviderAt(new BouncyCastleProvider(),4);
	//	logger.debug(Loader2.class.getCanonicalName());
	//	Security.addProvider(new BouncyCastleProvider());
	//	logger.debug("position installed: "+pos);
	//	Security.removeProvider("SunJCE");
		//logger.debug("removeing Provider SunJCE");
		
	/*	for (Provider p: Security.getProviders()) {
			logger.debug("Name: "+p.getName());
			logger.debug("Info: "+p.getInfo());
			for (Service s: p.getServices()) {
				logger.debug("Alg: "+s.getAlgorithm());
			}
		} */
    //	logger.debug("installe dProvider: "+pos);
	}

	private final OwnFileList filelist;
	private final IHashEngine hashEngine; 
	private final IDatabase database;
	private final List<IFilelistProcessor> filelistProcessors;

	private final List<IOperatorPlugin> operatorPlugins;
	
	private final IUDPHandler udphandler;

	private final AutomaticSearchForAlternatives altSearch;
	

	private final IUserChangedListener databaseUserChanges = new IUserChangedListener() {
		public void changed(UserChangeEvent uce) {
			switch(uce.getDetail()) {
			case UserChangeEvent.DOWNLOADQUEUE_ENTRY_PRE_ADD_FIRST:
			case UserChangeEvent.DOWNLOADQUEUE_ENTRY_POST_REMOVE_LAST:
			case UserChangeEvent.FAVUSER_ADDED:
			case UserChangeEvent.FAVUSER_REMOVED:
			case UserChangeEvent.SLOT_GRANTED:
			case UserChangeEvent.SLOTGRANT_REVOKED:
			case UserChangeEvent.SLOTGRANT_CHANGED:
				logger.debug("Persistance change for user: "+uce.getChanged());
				database.addUpdateOrDeleteUser(uce.getChanged());
			}
		}
	};

    /**
     * Creates a new instance of DCClient 
     * and all its objects
     */
    public DCClient() {
    	logger.debug("creating client");
    	favFolders = new FavFolders();
    	population = new Population(this);
    	altSearch =  new AutomaticSearchForAlternatives(this);
    	downloadQueue = new DownloadQueue(this);
    	
    	database = init.loadDB(this);
    	
    	hashEngine = init.loadHashEngine();
    	filelistProcessors = init.loadFilelistProcessors();
    	operatorPlugins = init.loadOperatorPlugins();
    	connectionDeterminator = init.createConnectionDeterminator(this);
    	favHubs = init.getFavHubs(this);
    	slotManager = init.createSlotManager(this);
    	upQueue = init.createUploadQueue(this, true);
    	downQueue = init.createUploadQueue(this, false);
    	
    	fileListSelf = new User(this,PI.get(PI.nick),TigerHashValue.SELFHASH ) { 
			@Override
			public String getNick() { 
				return PI.get(PI.nick);
			}
    	};

    	filelist = new OwnFileList(fileListSelf,this); 
        
        ch = new ConnectionHandler(this); 
     
        udphandler = init.createUDPHandler(this); 
        
        PI.get().addPreferenceChangeListener(new IPreferenceChangeListener() {
			public void preferenceChange(PreferenceChangeEvent event) {
				String pref = event.getKey();
				logger.debug("pref changed: "+pref);
				if (pref.equals(PI.eMail) || 
						pref.equals(PI.description) || 
						pref.equals(PI.connection)||
						pref.equals(PI.passive) ||
						pref.equals(PI.uploadLimit) ||
						pref.startsWith("socksProxy")) {
					
					notifyChangedInfo(InfoChange.Misc);
					
				} else if(pref.equals(PI.slots) ) {
					notifyChangedInfo(InfoChange.Slots);
				}
				
			}
        	
        });
        logger.debug("created client");
        
    }
    


    
    /**
     * sends a search to all provided hubs ..
     * @param search  the search what should be searched.. and where the results should go to..
     * @param hubsToSearch which hubs should be searched..
     */
    public void search(FileSearch search, Set<IHub> hubsToSearch) {
    	if (!hubsToSearch.isEmpty()) {
	    	connectionDeterminator.searchStarted(search); 
    		for (IHub hub:hubsToSearch) {
	    		hub.search(search);
	    	}
    	}
    }
    
    /**
     * search all hubs..
     * @param search  the search what should be searched.. and where the results should go to.
     */
    public void search(FileSearch search) {
    	search(search,new HashSet<IHub>(hubs.values()));
    }
    
    /**
     * 
     * @return a user holding our filelist
     */
    public IUser getFilelistself() {
    	return fileListSelf;
    }
    
    /**
     * 
     * @return our own filelist..
     */
    public FileList getOwnFileList() {
    	return fileListSelf.getFilelistDescriptor().getFilelist();
    }
    
    /**
     * starts a FileList refresh in a separate Thread
     */
    public void refreshFilelist() {
    	filelist.refresh(false);
    }
    
    /**
     * clears all HashData from the DownloadQueue
     * forcing rehashing all Files on the disc
     */
    public void rebuildFilelist() {
    	database.deleteAllHashedFiles();
    	refreshFilelist();
    }
    
    /**
     * deletes all File hashes of the DB 
     * that are currently not in use.
     * 
     * @return the number of files pruned from the database.. 
     */
    public int pruneHashes() {
    	Map<File,HashedFile> pruned =  database.pruneUnusedHashedFiles();
    	if (logger.isDebugEnabled()) {
    		for (File f:pruned.keySet()) {
    			logger.info("Deleted hash for: "+f.getPath());
    		}
    	}
    	return pruned.size();
    }

    /**
     * initialises the own FileList, as this takes the most
     * time here separate method that can be called from the 
     * splashScreen is provided..
     */
    synchronized Future<?> initialiseFilelist() {
    	if (null == fileListInitialised) {
    		fileListInitialised = exec.submit(new Runnable() {
				public void run() {
		    		filelist.initialise(); 
				}
    		});
    	}
    	return fileListInitialised;
    } 

    /**
     * starts the DCClient
     * 
     * called after the GUI is ready...
     *
     */
    public synchronized void start(IProgressMonitor monitor) {
    	if (get() == this) {
    		throw new IllegalStateException("DCClient already started");
    	}
    	synchronized (synchSingleton) {
    		singleton = this;
    	}
    	monitor.beginTask("starting DCClient", 12);
    	
    	// favs are loaded.. time to register Database to be notified on userchanges..
    	population.registerUserChangedListener(databaseUserChanges); 
    	
    	Future<?> connectDetInit = connectionDeterminator.start();
    	
    	if (GH.isEmpty(PI.get(PI.uUID))) {
    		try {
        		connectDetInit.get(); //on first start we need the ConnectionDeterminator before UID creation
        	} catch(Exception e) {
        		logger.warn(e,e);
        	}
    		PI.put(PI.uUID, createUUID().toString());
    	}
    	pid = HashValue.createHash(PI.get(PI.uUID));
    	
    	monitor.worked(1);
    	
    	Future<?> filelistInit = initialiseFilelist(); 

 
    	upQueue.start();
    	downQueue.start();
    	
    	
    	udphandler.start();
    	monitor.worked(1);
    	ch.start();
    	monitor.worked(1);
    	
    	logger.info("loading DownloadQueue");
    	downloadQueue.loadDQ(); //loads the download queue 
    	monitor.worked(2);
    	

    	
    	try { //wait for deferred tasks...
    		connectDetInit.get();
    		filelistInit.get();
    		monitor.worked(3);
    	} catch(Exception e) {
    		logger.warn(e,e);
    	}
    	
    	logger.info("starting hubs");
    	//downloads can be started..
    	favHubs.openAutoStartHubs();
    	monitor.worked(3);
    	//can take some time.. we usually won't need this from start..-> done in separate thread..
    	OwnFileList.loadSharedDirsForIconManager(this); 
    	monitor.worked(2);

    	altSearch.start();
    	slotManager.init();
    	
    //	started = true;
    	monitor.done();
    	
    	while (true) { //Its a DC Client after all .. wouldn't work without this , kudos to NEV!
    		break;
    	}
    	
    	Security.setProperty("networkaddress.cache.ttl", ""+60); //cache positive results only for 1 minute... because of dyndns..

    	
    }
    
    /**
     * creates some UUID for this client 
     */
    private  HashValue createUUID() {
    	String id = "";
    	id += UUID.randomUUID().toString();
    	
		id += connectionDeterminator.getPublicIP().toString();
		id += " "+System.currentTimeMillis()+" "+System.nanoTime();
		logger.debug("id: "+id);
		return Tiger.tigerOfString(id);
	}
    
   
    /**
     * 
     * @return the CID of the client
     */
    public synchronized HashValue getPID() {
    	return pid;
    }
    
    /**
     * called to on closing by the GUI....hopefully..
     * @param shutDownExec .. if set to false
     * executor is let alive.. makign it possible to create
     * a new DCClient object..
     */
    public void stop(boolean shutDownExec) {
    	logger.info(LONGVERSION+" is stopping");
    	if (PI.getBoolean(PI.deleteFilelistsOnExit)) {
    		FileList.deleteFilelists();
    	}
    	ch.stop();
    	downloadQueue.stop();
    	altSearch.stop();
    	filelist.stop();
    	
    	if (shutDownExec) {
    		exec.shutdown();
    	}
    	scheduler.shutdown();
    	
    	
    	udphandler.stop();
    	connectionDeterminator.stop();
    	
    	population.unregisterUserChangedListener(databaseUserChanges);
    	logger.debug("shut down executors");
    	database.shutdown();
    	logger.debug("shut down database");
    	logger.info(LONGVERSION+" stopped");
    	
    }
    
    /**
     * creates tag  
     * (i.e. something like <UC 0.03,M:A,H:0/0/1,S:2> )
     * 
     */
    public String getTag() {
    	int[] hubs = getNumberOfHubs(false);
    	return String.format("<%s,M:%c,H:%d/%d/%d,S:%d%s>", VERSION ,
    			getMode().getModeChar(),hubs[0],hubs[1],hubs[2],getTotalSlots(),getAdditionalTag());
    }
    
    private String getAdditionalTag() {
    	String s = "";
    	int ul= PI.getInt(PI.uploadLimit);
    	if (ul > 0) {
    		s+=",L:"+ul;
    	}
    	
    	return s;
    }
    
    /**
     * 
     * @return a 3 dimensional array containing <p>
     * on index 0 - normal hubs <br>
     * on index 1 - registered hubs <br>
     * on index 2 - OP hubs 
     */
    public int[] getNumberOfHubs(boolean countChatOnly) {
    	int normal = 0,registered =0, ophub = 0;
    
    	for (Hub hub:hubs.values()) {
    		if (hub.getState() == ConnectionState.LOGGEDIN && 
    				(countChatOnly || !hub.getFavHub().isChatOnly()) ) {
    			
	    		if (hub.isOpHub()) {
	    			ophub++;
	    		} else if (hub.isRegistered()) {
	    			registered++;
	    		} else {
	    			normal++;
	    		}
    		}
    	}
    	return new int[] {normal,registered,ophub};
    }
    
    /**
     * 
     * @return the connection set by the user
     * i.e. 0.02 or 50  
     */
    public String getConnection() {
    	return PI.get(PI.connection); 
    }
    

    
    public Mode getMode() {
    	return isActive() ? Mode.ACTIVE : 
    					(Socks.isEnabled()? Mode.SOCKS:Mode.PASSIVE) ; 
    
    }
    
	/**
	 * check if TLS was enabled at start of Program .. and is now in a usable state.
	 * @return true if TLS is fully initialized and running..
	 */
	public boolean currentlyTLSSupport() {
		return  ch.isTLSRunning() &&  PI.getBoolean(PI.allowTLS);
	}
    
    
   /**
    * removes a mapping for a hub
    * from the list of all open hubs..
    * !! this is only called by the hub's destroy method..
    */
    public void internal_unregisterHub(FavHub favHub) {
    	
    	//first remove mapping from hubId to hub
    	hubs.remove(favHub);
    	//the inform hubs slowly of change..
    	notifyChangedInfo(InfoChange.Hubs);
    }
    
    
    public Hub getHub(FavHub favHub, boolean showInUI) {

    	Hub hub = hubs.get(favHub);
    	if (hub != null) {
    		return hub;
    	}
    	synchronized (this) {
    		hub = hubs.get(favHub);
    		if (hub == null) {


    			hub = new Hub(favHub,this);
    			synchronized(hub) {
    				hubs.put(favHub, hub);
    				if (!favHub.isChatOnly()) {
    					hub.registerCTMListener(ch);
    				} 
    				
    				for (IHubCreationListener hubl:hublisteners) {
    					hubl.hubCreated(favHub,showInUI);
    				}
    				
    				final Hub hubf = hub; 
    				//wait with starting some time (race condition though helps registering listeners)
    				scheduler.schedule(new Runnable() {
    					public void run() {
    						synchronized(hubf) {
    							hubf.start();
    						}
    						notifyChangedInfo(InfoChange.Hubs);
    					}

    				}, 100, TimeUnit.MILLISECONDS);
    			}
    		}
    	}

    	return hub;
    }
    
    public boolean isRunningHub(FavHub favHub) {
    	return hubs.containsKey(favHub);
    }
    
    /**
     * @return an unmodifiable view of the currently open hubs..
     */
    public Map<FavHub,Hub> getHubs() {
    	return Collections.unmodifiableMap(hubs);
    }
    
    
    /**
     *  registers a ISearchresult listener with the client
     *  
     * @param sirl - a search result listener .. for example a SearchEditor
     */
    public void register(ISearchResultListener sirl) {
    	searchListeners.addIfAbsent(sirl);
    }
    
    /**
     *  unregisters a ISearchresult listener with the client so he will no 
     *  longer receive notification for incoming searches
     *  
     * @param sirl - a search result listener .. for example a SearchEditor
     */
    public void unregister(ISearchResultListener sirl) {
    	searchListeners.remove(sirl);	
    }
    

    public void register(IInfoChanged iic) {
    	changedInfo.addIfAbsent(iic);
    }
    
 
    public void unregister(IInfoChanged iic) {
    	changedInfo.remove(iic);	
    }
    
    /**
     * this method is called by Hubs 
     * it delegates the searchResults to anyone interested..
     * 
     * @param sr - the sr received
     */
    public void srReceived(ISearchResult sr) {
	    for (ISearchResultListener srl: searchListeners) {
	    	srl.received(sr);
	   	}
    }
    
    public boolean isActive() {
    	return !PI.getBoolean(PI.passive) ;
    }
   
    
    /**
     * resolves a user only by his nick ... may result in the wrong user
     * @param nick - the nick searched
     * @return the user found  null if none
     */
    public User getUserByNick(String nick) {
    	synchronized(hubs){
    		for (Hub hub : hubs.values()) {
    			if (hub.isNMDC()) {
    				User usr= hub.getUserByNick(nick);
    				if (usr != null) {
    					return usr;
    				}
    			}
    		}
    	}
    	return null;
    }
    
    public User getUserForCID(HashValue cid) {
    	synchronized(hubs){
    		for (Hub hub : hubs.values()) {
    			if (!hub.isNMDC()) {
	    			User usr = hub.getUserByCID(cid);
	    			if (usr != null) {
	    				return usr;
	    			}
    			}
    		}
    	}
    	
    	return null;
    }

    
    /**
     * sends MyInfo to every hub about a changed Info
     *  (message may be delayed based on the type)
     */
    public void notifyChangedInfo(InfoChange type) {
    	if (type.isSeparateRefresh()) {
    		for (IInfoChanged iic: changedInfo) {
				iic.infoChanged(Collections.singleton(type));
			}
    	} else {
    	
	    	changes.add(type);
	    	if (myInfoUpdater != null && myInfoUpdater.getDelay(TimeUnit.MILLISECONDS) > type.getDelay()) {
	    		myInfoUpdater.cancel(false);
	    		myInfoUpdater = null;
	    	}
	    	
	    	if (myInfoUpdater == null) {
	    		myInfoUpdater = scheduler.schedule(
	    		    	new Runnable() {
	    					public void run() {
	    						myInfoUpdater = null;
	    						for (Hub hub : hubs.values()) {
	    							synchronized(hub) {
		    							if (hub.getState() == ConnectionState.LOGGEDIN) {
		    								hub.sendMyInfo(false);
		    							}
	    							}
	    							Set<InfoChange> copy;
	    							synchronized(changes) {
	    								copy = new HashSet<InfoChange>(changes);
	    								changes.clear();
	    							}
	    							for (IInfoChanged iic: changedInfo) {
	    								iic.infoChanged(copy);
	    							}
	    							
	    						}
	    						
	    					}
	    				}, type.getDelay(), TimeUnit.MILLISECONDS);
	    	}
    	}
    }
    
    /**
     * determines hub by nick of sender and hub-ip
     * @param nick - nick of SR sender
     * @param hubip - the hubIP in the SR string
     * @return  the hub that matches the provided values
     * null if none can be found
     */
    public Hub hubForNickAndIP(String nick, String hubip) {
    	WeakReference<Hub> refHub = ipToHub.get(hubip);
    	if (refHub != null && refHub.get()!= null) {
    		return refHub.get();
    	}
    	User usr = getUserByNick(nick);
    	if (usr != null) {
    		ipToHub.put(hubip, new WeakReference<Hub>(usr.getHub()));
    		return usr.getHub();
    	}
    	
    	return null;
    }
    
    
    
    
    /*
     * with this function an upload can request a slot
     * 
     * @param usr - the user that wants something from us
     * @param type - what we want to upload..
     * @param f if its a file the file is provided
     * @return a slot if available  null if none
     * 
     *
    public Slot getSlot(User usr ,TransferType type,File f)  {
    	
    	synchronized(slotsSynch) {
	    	if (usr.getUpload() != null) {
	    		logger.debug(usr.getNick()+" tried getting upload even if he already had an upload running");
	    		return null; //no slot for that user
	    	}
	    	if (currentExtraSlots.size() >= MaxTransfers && !usr.isOp()) { //TOO many uploads running.. no slots anymore. To prevent DoS 
	    		return null; 
	    	}
	    		
	    	try {
		    	switch(type) {
		    	case TTHL:
		    	case FILELIST:
					Slot extra = new Slot(false);
					currentExtraSlots.add(extra);
					return extra;
		    	case FILE:
		    		if (currentSlots.size() < getTotalSlots()) {
		    			Slot normal = new Slot(true);
		    			currentSlots.add(normal);
		    			return normal;
		    		} else if (usr.hasCurrentlyAutogrant() || f.length() <= MinislotSize) {
		    			Slot autogrant = new Slot(false);
		    			currentSlots.add(autogrant);
		    			currentExtraSlots.add(autogrant);
		    			return autogrant;
		    		} else {
		    			return null;
		    		}
		    	}
	    	} finally {
	    		notifyChangedInfo(InfoChange.CurrentSlots);
	    	}
    	}
    	throw new IllegalStateException("no known TransferType was set");    		
    } */
    
    /*
     * returns a slot after use to the client so someone else can reuse it..
     * 
     * @param slot - the slot that is returned..
     *
    public void returnSlot(Slot slot) {
    	synchronized(slotsSynch) {
    		currentSlots.remove(slot);
    		currentExtraSlots.remove(slot);
    		notifyChangedInfo(InfoChange.CurrentSlots);
    	}
    } */
    
    
    public void register(IHubCreationListener hubl) {
    	hublisteners.addIfAbsent(hubl);
    }
    
    public void unregister(IHubCreationListener hubl) {
    	hublisteners.remove(hubl);
    }
    

	

	public void registerSRL(ISearchReceivedListener listener) {
		srl.addIfAbsent(listener);
	}
	
	public void unregisterSRL(ISearchReceivedListener listener) {
		srl.remove(listener);
	}
	
	/**
	 * (only used for SearchSpy)
	 * 
	 * notifies the client that a search was received
	 * @param searchStrings what search strings are searched for.. size will be one if its a tth
	 * @param source - if active this will eb an inetSocketAddress if passive a User
	 */
	public void searchReceived(Set<String> searchStrings,Object source, int nrOfResults) {
		for (ISearchReceivedListener listener:srl) {
			listener.searchReceived(searchStrings, source, nrOfResults);
		}
	}
	

 


	/**
	 * @return the hashEngine
	 */
	public IHashEngine getHashEngine() {
		return hashEngine;
	}

	/**
	 * @return the ch
	 */
	public ConnectionHandler getCh() {
		return ch;
	}



	public IDatabase getDatabase() {
		return database;
	}

	public IOwnFileList getFilelist() {
		return filelist;
	}

	public IUDPHandler getUdphandler() {
		return udphandler;
	}


	
	/**
	 * 
	 * @return value= totalslots - currently in use slots.
	 * so it returns the value usually sent in search messages.  
	 */
	public int getCurrentSlots() {
		return slotManager.getCurrentSlots();
	}


	/**
	 * 
	 * @return totalSlots  - the current maximum of slots
	 */
	public int getTotalSlots() {
		return slotManager.getTotalSlots();
	}

	


	public ISlotManager getSlotManager() {
		return slotManager;
	}

	public List<IFilelistProcessor> getFilelistProcessors() {
		return filelistProcessors;
	}




	public boolean isAway() {
		synchronized(synchAway) {
			return away;
		}
	}


	/**
	 * 
	 * @param away - true for away - false for back
	 * away message is default away message if true
	 */
	public void setAway(boolean away) {
		if (away) {
			setAway(PI.get(PI.defaultAFKMessage));
		} else {
			synchronized(synchAway) {
				this.away = false;
			}
		}
	}
	
	/**
	 * sets a way to true
	 * @param awayMessage - with provided away message
	 */
	public void setAway(String awayMessage) {
		synchronized(synchAway) {
			this.awayMessage = awayMessage;
			this.away = true;
		}
	}




	public String getAwayMessage() {
		synchronized(synchAway) {
			return awayMessage+" <"+LONGVERSION+">";
		}
	}

	/**
	 * place holder  should return if
	 * IPv4 is used and not IPv6
	 * @return true
	 */
	public boolean isIPv4Used() {
		return true;
	}


	public List<IOperatorPlugin> getOperatorPlugins() {
		return Collections.unmodifiableList(operatorPlugins);
	}


	public IUploadQueue getUpQueue() {
		return upQueue;
	}

	public IUploadQueue getDownQueue() {
		return downQueue;
	}
	
	public IUploadQueue getUpDownQueue(boolean up) {
		return up? upQueue: downQueue;
	}

	
	public IConnectionDeterminator getConnectionDeterminator() {
		return connectionDeterminator;
	}

	public IFavHubs getFavHubs() {
		return favHubs;
	}


	public FavFolders getFavFolders() {
		return favFolders;
	}


	public Population getPopulation() {
		return population;
	}

	public DownloadQueue getDownloadQueue() {
		return downloadQueue;
	}




	public static class Initializer {
		
		protected IFavHubs getFavHubs(DCClient dcc) {
			return new FavHubs(dcc);
		}
		
		protected IConnectionDeterminator createConnectionDeterminator(DCClient dcc) {
			return new ConnectionDeterminator(dcc);
		}
		
		
		protected IUploadQueue createUploadQueue(DCClient dcc,boolean upload) {
			return new UploadQueue(dcc);
		}
		
		protected IUDPHandler createUDPHandler(DCClient dcc) {
			return new UDPhandler(dcc);
		}
		
	    
	    /**
	     * loads the database plug-in
	     *
	     */
	    protected IDatabase loadDB(DCClient dcc) {
	    	String idToLoad = PI.get(IDatabase.ExtensionpointID);
	    	IExtensionRegistry reg = Platform.getExtensionRegistry();
	    	
			IConfigurationElement[] configElements = reg
			.getConfigurationElementsFor(IDatabase.ExtensionpointID);
			
			IDatabase db = null;
			
			for (IConfigurationElement element : configElements) {
				logger.debug(element.getAttribute("id"));
				try {
					if(idToLoad.equals(element.getAttribute("id")) ) {
						
						db = (IDatabase) element.createExecutableExtension("class");
						
						db.init(PI.getStoragePath(),dcc);
					}
				} catch (CoreException e) {
					logger.error("Can't load the Database!",e);
					
				} catch (Exception e) {
					logger.error("Error initializing database: "+e.getMessage(),e);
					System.exit(-1);
				}
			}
			if (db == null) {
				logger.error("no database found .. UC can't work without a database");
				System.exit(-1);
			} 
			return db;
	    }
	    
	    
	    /**
	     * loads the hash-engine-plug-in
	     */
	    protected IHashEngine loadHashEngine() {
	    	String idToLoad= PI.get(IHashEngine.ExtensionpointID);
	    	IExtensionRegistry reg = Platform.getExtensionRegistry();
	    	
			IConfigurationElement[] configElements = reg
			.getConfigurationElementsFor(IHashEngine.ExtensionpointID);

			IHashEngine he = null;
			
			for (IConfigurationElement element : configElements) {
				try {
					if (idToLoad.equals(element.getAttribute("id")) ) {
						he = (IHashEngine) element.createExecutableExtension("class");
					}
				} catch (CoreException e) {
					logger.error("Can't load the HashEngine!",e);
				} 
			}
			if (he == null ) {
				logger.error("no hashEngine found");
				System.exit(-1);
			} 
			he.init();
			
			return he;
	    }
	    
	    /**
	     * loads all FileList processor plug-ins
	     * @return All plug-ins available..
	     */
	    protected List<IFilelistProcessor> loadFilelistProcessors() {
	    
	    	IExtensionRegistry reg = Platform.getExtensionRegistry();
	    	
			IConfigurationElement[] configElements = reg
			.getConfigurationElementsFor(IFilelistProcessor.ExtensionpointID);

			List<IFilelistProcessor> processor = new ArrayList<IFilelistProcessor>(configElements.length);
			
			for (IConfigurationElement element : configElements) {
				try {
					
					IFilelistProcessor fp = (IFilelistProcessor) element.createExecutableExtension("class");
					processor.add(fp);
				} catch (CoreException e) {
					logger.error("Can't load the FilelistProcessor: "+element.getAttribute("id"),e);
				} 
			}
		
			return processor;
	    }
	    
	    protected  List<IOperatorPlugin> loadOperatorPlugins() {
	    	IExtensionRegistry reg = Platform.getExtensionRegistry();
	    	
			IConfigurationElement[] configElements = reg
			.getConfigurationElementsFor(IOperatorPlugin.PointID);

			List<IOperatorPlugin> opPlugins = new ArrayList<IOperatorPlugin>(configElements.length);
			
			for (IConfigurationElement element : configElements) {
				try {
					
					IOperatorPlugin fp = (IOperatorPlugin) element.createExecutableExtension("class");
					opPlugins.add(fp);
				} catch (CoreException e) {
					logger.error("Can't load the Op Plugin: "+element.getAttribute("name")+"  "+element.getAttribute("id"),e);
				} 
			}
		
			return opPlugins;
	    }
	    
	    protected ISlotManager createSlotManager(DCClient dcc) {
	    	return new SlotManager(dcc);
	    }
		
	}
	
 
}