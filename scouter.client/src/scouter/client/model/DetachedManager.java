package scouter.client.model;

import java.util.HashSet;
import java.util.Set;

public class DetachedManager {
	
	private static DetachedManager instance;
	
	Set<String> detachedSet = new HashSet<String>();

	public synchronized static DetachedManager getInstance() {
		if (instance == null) {
			instance = new DetachedManager();
		}
		return instance;
	}
	
	public boolean isInitialView(String id, String secId) {
		return detachedSet.contains(id + secId) == false;
	}
	
	public void registerOpend(String id, String secId) {
		detachedSet.add(id + secId);
	}
}
