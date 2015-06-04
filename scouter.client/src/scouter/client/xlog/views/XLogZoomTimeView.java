/*
 *  Copyright 2015 LG CNS.
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
package scouter.client.xlog.views;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import scouter.client.Images;
import scouter.client.model.XLogData;
import scouter.client.util.ExUtil;
import scouter.client.util.ImageUtil;
import scouter.util.DateUtil;
import scouter.util.LongEnumer;
import scouter.util.LongKeyLinkedMap;


public class XLogZoomTimeView extends XLogViewCommon {

	public static final String ID = XLogZoomTimeView.class.getName();
	
	public void createPartControl(Composite parent) {
		display = Display.getCurrent();
		shell = new Shell(display);
		IToolBarManager man = getViewSite().getActionBars().getToolBarManager();
		
		create(parent, man);
		
		showFilters = new Action("Show Filters", IAction.AS_CHECK_BOX){ 
	        public void run(){    
	        	if(showFilters.isChecked()){
	        		sashForm.setMaximizedControl(null);
	        		settingFilterInputs();
	        	}else{
	        		sashForm.setMaximizedControl(canvas);
	        		clearFilters();
	        		viewPainter.build();
	        		canvas.redraw();
	        	}
	        }
	    };  
	    showFilters.setImageDescriptor(ImageUtil.getImageDescriptor(Images.filter));
	    man.add(showFilters);
		
		sashForm.setWeights(new int[]{2, 1});
		sashForm.setMaximizedControl(canvas);
		
		canvas.addControlListener(new ControlListener() {
			public void controlResized(ControlEvent e) {
				viewPainter.set(canvas.getClientArea());
				viewPainter.build();
			}
			public void controlMoved(ControlEvent e) {
			}
		});
	}
	
	public void refresh(){
		viewPainter.build();
		ExUtil.exec(canvas, new Runnable() {
			public void run() {
				canvas.redraw();
			}
		});
	}

	public void setInput(long stime, long etime, double max, double min, LongKeyLinkedMap<XLogData> data, String objType) {
		twdata.clear();
		this.objType = objType;
		LongEnumer enumer = data.keys();
		while (enumer.hasMoreElements()) {
			long key = enumer.nextLong();
			twdata.put(key, data.get(key));
		}
		setObjType(objType);
		viewPainter.setEndTime(etime);
		viewPainter.setTimeRange(etime - stime);
		viewPainter.setValueRange(min, max);

		this.setPartName("Zoom-in Area");
		setContentDescription(DateUtil.format(stime, "yyyy-MM-dd") + "(" + DateUtil.format(stime, "HH:mm:ss")
						+ "~" + DateUtil.format(etime, "HH:mm:ss") + ")");
		setDate(DateUtil.yyyymmdd(stime));
		refresh();
	}
	
	public void setFocus() {
		super.setFocus();
		String statusMessage = "setInput(objType:"+objType + ", twdata size(): " + twdata.size() +")";
		IStatusLineManager slManager= getViewSite().getActionBars().getStatusLineManager();
		slManager.setMessage(statusMessage);
	}
	
	public void dispose() {
		super.dispose();
	}

	public void loadAdditinalData(long stime, long etime, boolean reverse) {
		
	}
}
