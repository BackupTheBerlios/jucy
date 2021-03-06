package uc.files.transfer;





import helpers.GH;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import logger.LoggerFactory;


import org.apache.log4j.Logger;
import org.eclipse.core.runtime.Platform;

import uc.DCClient;
import uc.ISlotManager;
import uc.IUser;
import uc.InfoChange;
import uc.PI;
import uc.files.filelist.FileListFile;
import uc.protocols.TransferType;

public class SlotManager implements ISlotManager {
	
	private static final Logger logger = LoggerFactory.make();
	
	private static final long RemoveAfterSeconds = 1000 * 60 *10 ; //10 Minutes
	private static final long OnlineAndInterestedTime = 1000 * 60 *3; // at least every 3 minutes the user must request a slot to be seen as interested 
	private static final long MinislotSize = 64 * 1024;
	private static final int MaxTransfers = 100;
	
	
	private final DCClient dcc;

	
	
	private final Object slotsSynch = new Object();
	private final Set<Slot> currentSlots = new HashSet<Slot>();
	private final Set<Slot> currentExtraSlots = new HashSet<Slot>();
	private final Map<IUser,WaitingUser> usersThatReceivedSlot = 
		new HashMap<IUser,WaitingUser>(); 
	
	
	
	private final CopyOnWriteArrayList<WaitingUser> waitingQueue = 
		new CopyOnWriteArrayList<WaitingUser>();
	
	public SlotManager(DCClient client) {
		this.dcc = client;
	}
	
	/* (non-Javadoc)
	 * @see uc.ISlotManager#init()
	 */
	public void init() {
		dcc.getSchedulerDir().schedule(new Runnable() {
			public void run() {
				removeUsersNotRecentlyOnlineAndInterested();
			}
			
		}, 60, TimeUnit.SECONDS);
	}
	
    /* (non-Javadoc)
	 * @see uc.ISlotManager#getSlot(uc.User, uc.protocols.TransferType, java.io.File)
	 */
	public Slot getSlot(IUser usr ,TransferType type,FileListFile f)  {
		if (usr == null || (type == TransferType.FILE && f == null )) {
			if (Platform.inDevelopmentMode()) {
				logger.error("user "+(usr==null? "null":usr)+" file:"+(f==null?"null":f));
			}
			return null;
		}
		synchronized(slotsSynch) {
			if (usersThatReceivedSlot.containsKey(usr)) {
				logger.debug(usr.getNick()+" tried getting upload even if he already had an upload running");
				return null; //no slot for that user
			}
			if (currentExtraSlots.size() >= MaxTransfers && !usr.isOp()) { //Too many uploads running.. no slots anymore. To prevent DoS 
				return null; 
			}
			updateUserOnlineAndInterested(usr);
			
			Slot s = getSlotPriv(usr,type,f);
			WaitingUser wu = getWU(usr);
			if (s == null) {
				waitingQueue.addIfAbsent(wu);
				//logger.debug("User in Queu: "+usr.getNick()+"  Position: "+getPositionInQueue(usr));

			} else {
				waitingQueue.remove(wu);
				usersThatReceivedSlot.put(usr, wu);
			}
			return s;
		}
	}
	
	private Slot getSlotPriv(IUser usr ,TransferType type,FileListFile f) {
		switch(type) {
		case TTHL:
		case FILELIST:
			Slot extra = new Slot(false);
			currentExtraSlots.add(extra);
			return extra;
		case FILE:
			int slotsAvailable =   getTotalSlots() - currentSlots.size();
			boolean eligibleForSlotByQueue = isOneofThFirstXUsers(slotsAvailable,usr);
			if (slotsAvailable > 0 && eligibleForSlotByQueue) {
			//	logger.info("User "+usr.getNick()+" got Slot over Queue: "+usr);
				Slot normal = new Slot(true);
				currentSlots.add(normal);
				dcc.notifyChangedInfo(InfoChange.CurrentSlots);
				return normal;		
			} else if (usr.hasCurrentlyAutogrant() || f.getSize() <= MinislotSize || f.automaticExtraSlot()) {
				Slot autogrant = new Slot(false);
				currentSlots.add(autogrant);
				currentExtraSlots.add(autogrant);
				dcc.notifyChangedInfo(InfoChange.CurrentSlots);
				return autogrant;
			} else {
			//	logger.info("No Slot for: "+usr.getNick());
				return null;
			}
		}

		throw new IllegalStateException("no known TransferType was set");   
	}

	/* (non-Javadoc)
	 * @see uc.ISlotManager#returnSlot(uc.files.transfer.Slot, uc.IUser)
	 */
	public void returnSlot(Slot slot,IUser usr) {
		if (usr == null && Platform.inDevelopmentMode()) {
			logger.error("user was null");
		}
		
		synchronized(slotsSynch) {
			currentSlots.remove(slot);
			currentExtraSlots.remove(slot);
			dcc.notifyChangedInfo(InfoChange.CurrentSlots);
			
			
			WaitingUser wu = usersThatReceivedSlot.remove(usr);
			if (wu != null) {
				waitingQueue.add(wu);
			//	logger.info("Readding user: "+wu.usr.getNick());
			}
			
			ArrayList<WaitingUser> wus = new ArrayList<WaitingUser>(waitingQueue);
			Collections.sort(wus,WaitingUserComp);
			waitingQueue.clear();
			waitingQueue.addAll(wus);
			
//			if (slot != null) {
//				logger.info("Return slot: "+usr.getNick()+" pos: "+waitingQueue.indexOf(wu));
//			}
			
		}
	}


	/* (non-Javadoc)
	 * @see uc.ISlotManager#getCurrentSlots()
	 */
	public int getCurrentSlots() {
		synchronized (slotsSynch) {
			return  Math.max(0,getTotalSlots() - currentSlots.size());
		}
	}


	/* (non-Javadoc)
	 * @see uc.ISlotManager#getTotalSlots()
	 */
	public int getTotalSlots() {
		return PI.getInt(PI.slots);
	}
	
	/* (non-Javadoc)
	 * @see uc.ISlotManager#getPositionInQueue(uc.IUser)
	 */
	public int getPositionInQueue(IUser usr) {
		int x = -1;
		synchronized (slotsSynch) {
			for (WaitingUser wu:waitingQueue) {
				if (wu.isOnlineAndInterested()) {
					x++;
				}
				if (wu.usr.equals(usr)) {
					return x;
				}
			}
		}
		return -1;
	}

	
	private boolean isOneofThFirstXUsers(int x,IUser usr) {
		for (WaitingUser wu:waitingQueue) {
			if (wu.isOnlineAndInterested()) {
				if (wu.usr.equals(usr)) {
					return true;
				} else if ( --x  <= 0) {
					return false;
				}
			}
		}
		return true; //not in Queue -> well give him a slot then..
	}
	
	private void updateUserOnlineAndInterested(IUser user) {
		for (WaitingUser wu:waitingQueue) {
			if (wu.usr.equals(user)) {
				wu.stillInterestedReceived();
				break;
			}
		}
	}
	
	/**
	 * 
	 * @param usr - for which usr...
	 * @return a waiting user object for the given username
	 */
	private WaitingUser getWU(IUser usr) {
		synchronized (slotsSynch) {
			for (WaitingUser wu:waitingQueue) {
				if (wu.usr.equals(usr)) {
					return wu;
				}
			}
		}
	//	logger.info("Creating waiting user: "+usr);
		return new WaitingUser(usr);
	}
	
	public void printQueue() {
		dcc.logEvent(waitingQueue.toString());
	}
	
	
	private void removeUsersNotRecentlyOnlineAndInterested() {
		for (WaitingUser wu:waitingQueue) {
			if (wu.checkLastTimeInterestedAndOnline()) {
				waitingQueue.remove(wu);
			}
		}
	}

	
	private static class WaitingUser implements Comparable<WaitingUser> {
		private final IUser usr;
		private final long waitingSince;
		
		private long lastInterestedReceived;
		
		public WaitingUser(IUser usr) {
			if (usr == null) {
				throw new IllegalArgumentException("User null");
			}
			this.usr = usr;
			waitingSince = System.currentTimeMillis();
			lastInterestedReceived = System.currentTimeMillis();
		}
		
		/**
		 * 
		 * @return if the users has been offline for such a long time that he should be removed..
		 * true if should be removed..
		 */
		private void stillInterestedReceived() {
		//	if (usr.isOnline()) {
				lastInterestedReceived = System.currentTimeMillis();
		
			//} else {
				
			//}
		}
		
		private boolean isOnlineAndInterested() {
			return usr.isOnline() && System.currentTimeMillis() - lastInterestedReceived < OnlineAndInterestedTime;
		}
		
		private boolean checkLastTimeInterestedAndOnline() {
			return System.currentTimeMillis() - lastInterestedReceived > RemoveAfterSeconds ;
		}
		
		@Override
		public int hashCode() {
			return usr.hashCode();
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			return usr.equals(((WaitingUser)obj).usr);
		}

		public int compareTo(WaitingUser o) {
			if (o == null) {
				return Long.valueOf(waitingSince).compareTo(Long.MAX_VALUE);
			}
			int comp = Long.valueOf(waitingSince).compareTo(o.waitingSince); 
			if (comp == 0) { 
				//if several users wait for the same duration this preserves from problems! 
				return GH.compareTo(hashCode(), o.hashCode());
			}
			return comp;
		}
		
		public String toString() {
			return usr.getNick()+" Wait: "+((System.currentTimeMillis()-waitingSince)/1000)
			+" LIR: "+((System.currentTimeMillis()-lastInterestedReceived)/1000);
		}
	}
	
	private static final Comparator<WaitingUser> WaitingUserComp = new  Comparator<WaitingUser>() {

		public int compare(WaitingUser o1, WaitingUser o2) {
			long a = /*o1 == null? Long.MAX_VALUE :  */o1.waitingSince;
			long b = /*o2 == null? Long.MAX_VALUE : */ o2.waitingSince;
			return Long.valueOf(a).compareTo(b);
		}
		
	};
/*
 * [
 * Hedayat Wait: 27656 LIR: 25028, 
 * nick-try-16-16 Wait: 27656 LIR: 9432, 
 * Ara Wait: 27656 LIR: 183, 
 * GrillBill2 Wait: 27656 LIR: 48, 
 * YODA-41 Wait: 27656 LIR: 47, 
 * Powolniak Wait: 27656 LIR: 39, 
 * Kaptain_Komfy Wait: 27656 LIR: 28, 
 * °^Kumba^° Wait: 27656 LIR: 18, 
 * GrillBill Wait: 27656 LIR: 5, 
 * buffy Wait: 27342 LIR: 0, 
 * °^hAliGalLiE@works^° Wait: 26678 LIR: 13829, 
 * stroncium12800 Wait: 23384 LIR: 17, 
 * [RO][BUC][ISP]eruho Wait: 20820 LIR: 20820, 
 * kolina Wait: 11502 LIR: 2, 
 * tempestuous Wait: 11426 LIR: 11
 * ]
 * 
 */
	
}
