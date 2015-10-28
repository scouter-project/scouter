/*
 *  Copyright 2015 the original author or authors. 
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); 
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License. 
 *
 */
package scouter.client.threads;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ObjectSelectManager {
	
	private static ObjectSelectManager instance;
	private Set<Integer> unSelectedObjSet = new HashSet<Integer>();
	List<IObjectCheckListener> objectCheckStateListeners = new ArrayList<IObjectCheckListener>();
	
	public synchronized static ObjectSelectManager getInstance() {
		if (instance == null) {
			instance = new ObjectSelectManager();
		}
		return instance;
	}
	
	public boolean isUnselectedObject(int objHash) {
		return unSelectedObjSet.contains(objHash);
	}
	
	public void clear() {
		unSelectedObjSet.clear();
		notifyChangeCheckedState();
	}
	
	public int unselectedSize() {
		return unSelectedObjSet.size();
	}
	
	public void addAll(Set<Integer> unSelectedSet) {
		unSelectedObjSet.clear();
		unSelectedObjSet.addAll(unSelectedSet);
		notifyChangeCheckedState();
	}
	
	public void selectObj(int objHash) {
		if (unselectedSize() > 0) {
			if (isUnselectedObject(objHash)) {
				unSelectedObjSet.remove(objHash);
			} else {
				unSelectedObjSet.add(objHash);
			}
			notifyChangeCheckedState();
		}
	}
	
	public void addObjectCheckStateListener(IObjectCheckListener listener) {
		objectCheckStateListeners.add(listener);
	}
	
	public void removeObjectCheckStateListener(IObjectCheckListener listener) {
		objectCheckStateListeners.remove(listener);
	}
	
	private void notifyChangeCheckedState() {
		for (IObjectCheckListener listener : objectCheckStateListeners) {
			listener.notifyChangeState();
		}
	}
	
	public interface IObjectCheckListener {
		public void notifyChangeState();
	}

	public static void main(String[] args) {
		Set<Integer> objSet = new HashSet<Integer>();
		Set<Integer> aaaSet = new HashSet<Integer>();
		objSet.add(1);
		objSet.add(2);
		objSet.add(3);
		
		aaaSet.add(3);
		aaaSet.add(4);
		aaaSet.add(5);
		aaaSet.add(6);
		objSet.retainAll(aaaSet);
		System.out.println(objSet);
	}
}

