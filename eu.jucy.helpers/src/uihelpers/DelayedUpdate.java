package uihelpers;



import helpers.StatusObject.ChangeType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


import org.eclipse.jface.viewers.TableViewer;



public class DelayedUpdate<V> {
	
	private final TableViewer viewer;
	
	private volatile boolean updateinProgress = false;
	private final Object synchUser = new Object();
	
	private final Set<V> add = new HashSet<V>();
	private final Set<V> update = new HashSet<V>();
	private final Set<V> remove = new HashSet<V>();
	
	private final int delay;
	
	private List<DUEntry<V>> delayed = new ArrayList<DUEntry<V>>();
	
	public DelayedUpdate(TableViewer viewer) {
		this(viewer,500);
	}
	
	public DelayedUpdate(TableViewer viewer,int millisecDelay) {
		this.viewer = viewer;
		this.delay = millisecDelay;
	}
	
	
	private boolean isPresent(V v) {
		return add.contains(v)|| update.contains(v)|| remove.contains(v);
	}
	
	public void add(V v) {
		synchronized(synchUser) {
			if (isPresent(v)) {
				delayed.add(new DUEntry<V>(v, ChangeType.ADDED));
			} else {
				add.add(v);
			}
		}
		ensureUpdate();
	}
	
	public void change(V v) {
		synchronized(synchUser) {
			if (add.contains(v) || remove.contains(v)) {
				delayed.add(new DUEntry<V>(v, ChangeType.CHANGED));
			} else {
				update.add(v);
			}
		}
		ensureUpdate();
	}
	
	public void remove(V v) {
		synchronized(synchUser) {
			if (isPresent(v)) {
				delayed.add(new DUEntry<V>(v, ChangeType.REMOVED));
			} else {
				remove.add(v);
			}
		}
		ensureUpdate();
	}
	
	private void ensureUpdate() {
		if (!updateinProgress) {
			updateinProgress = true;
			new SUIJob() { //"bulkupdate"
				public void run() {
					if (!viewer.getControl().isDisposed()) {
						synchronized(synchUser) {
							while (!add.isEmpty() || !update.isEmpty() || !remove.isEmpty()) {
								
								if (!add.isEmpty()) {
									viewer.add(add.toArray());
									add.clear();
								}
								if (!update.isEmpty()) {
									viewer.update(update.toArray(), null);
									update.clear();
								}
								if (!remove.isEmpty()) {
									viewer.remove(remove.toArray());
									remove.clear();
								}
								if (!delayed.isEmpty()) {
									List<DUEntry<V>> dlaLocal = delayed;
									delayed = new ArrayList<DUEntry<V>>();
									for (DUEntry<V> de: dlaLocal) {
										switch(de.ct) {
										case ADDED: 	add(de.v); 		break;
										case CHANGED:	change(de.v); 	break;
										case REMOVED:	remove(de.v); 	break;
										}
									}
								}
							}
							updateinProgress = false;
						}
						updateDone();
					}
				}
				
			}.schedule(delay);
		}
	}
	
	/**
	 * discards all running add/update/delete ops
	 */
	public void clear() {
		synchronized(synchUser) {
			add.clear();
			update.clear();
			remove.clear();
		}
	}
	
	/**
	 * method for overwriting.. called by UI thread..
	 */
	protected void updateDone() {}
		
		
	private static class DUEntry<V> {
		private final V v;
		private final ChangeType ct;
		
		public DUEntry(V v, ChangeType ct) {
			super();
			this.v = v;
			this.ct = ct;
		}
		
		
	}
	
	
}