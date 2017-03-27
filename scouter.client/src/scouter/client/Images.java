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
package scouter.client;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Bundle;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.util.ImageUtil;
import scouter.lang.ObjectType;
import scouter.lang.counters.CounterEngine;
import scouter.util.StringUtil;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;


public class Images {

	public static final Image unknown = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FILE);
	
    public static final Image folder = Activator.getImage("icons/folder.png");
    public static final Image folder_star = Activator.getImage("icons/folder_star.png");

    public static final Image refresh = Activator.getImage("icons/refresh.png");
    public static final Image refresh_auto = Activator.getImage("icons/refresh_auto.png");

    public static final Image expand = Activator.getImage("icons/expand.png");
    public static final Image collapse = Activator.getImage("icons/collapse.png");
    
    public static final Image download = Activator.getImage("icons/download.png");

    public static final Image table_delete = Activator.getImage("icons/table_delete.png");
    public static final Image minus = Activator.getImage("icons/minus.png");
    
    public static final Image add = Activator.getImage("icons/add.gif");

    public static final Image filter = Activator.getImage("icons/filter.png");

	public static final Image monitor  = Activator.getImage("icons/monitor.png");
	
	public static final Image drive  = Activator.getImage("icons/drive.png");

	public static final Image agent = Activator.getImage("icons/agent.png"); 

	public static final Image default_context = Activator.getImage("icons/context.png");
	public static final ImageDescriptor default_context_descriptor = Activator.getImageDescriptor("icons/context.png");

	public static final Image save = ImageUtil.getSharedImage(ISharedImages.IMG_ETOOL_SAVE_EDIT);
	public static final Image saveas = ImageUtil.getSharedImage(ISharedImages.IMG_ETOOL_SAVEAS_EDIT);
	
	public static final Image previous = Activator.getImage("icons/previous.png");
	
	public static final Image copy = Activator.getImage("icons/copy.png");
	
	public static final Image tostart = Activator.getImage("icons/tostart.png");
	public static final Image toend = Activator.getImage("icons/toend.png");
	public static final Image tonext = Activator.getImage("icons/tonext.png");
	public static final Image toprev = Activator.getImage("icons/toprev.png");
	
	public static final Image zoomin = Activator.getImage("icons/zoomin.png");
	public static final Image zoomout = Activator.getImage("icons/zoomout.png");
	public static final Image explorer = Activator.getImage("icons/explorer.png");
	public static final Image heap = Activator.getImage("icons/heap.png");
	
	public static final Image capture = Activator.getImage("icons/capture.png");
	
	public static final Image server_delete = Activator.getImage("icons/server_delete.png");
	
	public static final Image close_folder = Activator.getImage("icons/close_folder.png");
	
	public static final Image folder_48 = Activator.getImage("icons/folder48.png");
	public static final Image scouter_48 = Activator.getImage("icons/h48.png");
	
	public static final Image CONFIG_USER  = Activator.getImage("icons/user.png");
	
	// icons for actions
	public static final Image thread         = Activator.getImage("icons/thread.png");
	public static final Image active         = Activator.getImage("icons/active.png");
	public static final Image inactive       = Activator.getImage("icons/inactive.png");
	public static final Image dead           = Activator.getImage("icons/dead.png");
	public static final Image all            = Activator.getImage("icons/all.png");
	public static final Image total            = Activator.getImage("icons/total.png");
	public static final Image alert          = Activator.getImage("icons/alert.png");
	public static final Image transrealtime     = Activator.getImage("icons/transrealtime.png");
	public static final Image preference     = Activator.getImage("icons/preference.png");
	public static final Image config         = Activator.getImage("icons/config.png");
	public static final Image config_edit         = Activator.getImage("icons/config_edit.png");
	public static final Image table          = Activator.getImage("icons/table.png");
	public static final Image bar          = Activator.getImage("icons/chart_bar.png");
	
	// counters
	public static final Image TYPE_SERVICE_COUNT = Activator.getImage("icons/counter/service_count.png");
	public static final Image TYPE_ACTSPEED      = Activator.getImage("icons/counter/activespeed.png");
	
	public static final Image MENU_EXIT      = Activator.getImage("icons/exit.png");
	
	public static final Image CTXMENU_RTC     = Activator.getImage("icons/ctx_realtimecounter.png");
	public static final Image CTXMENU_RDC     = Activator.getImage("icons/ctx_realdatecounter.png");

	public static final Image selection = Activator.getImage("icons/selection.png");
	public static final Image csv = Activator.getImage("icons/csv.png");
	public static final Image calendar = Activator.getImage("icons/calendar.png");
	public static final Image application_put = Activator.getImage("icons/application_put.png");
	public static final Image application_split = Activator.getImage("icons/application_split.png");
	
	public static final Image SERVER_ACT = Activator.getImage("icons/object/serverobj.png");
	public static final Image SERVER_INACT = Activator.getImage("icons/object/serverobj_inact.png");

	public static final Image SERVER_DEFAULT_ACT = Activator.getImage("icons/object/serverobj_def.png");
	public static final Image SERVER_DEFAULT_INACT = Activator.getImage("icons/object/serverobj_def_inact.png");
	
	public static final ImageDescriptor XY_MAX = Activator.getImageDescriptor("icons/xymax.png");
	public static final ImageDescriptor XY_LEFT = Activator.getImageDescriptor("icons/xyleft.png");
	public static final ImageDescriptor XY_YMIN = Activator.getImageDescriptor("icons/ymin.png");
	public static final ImageDescriptor SHEET_VIEW = Activator.getImageDescriptor("icons/sheet_view.png");
	
	public static final ImageDescriptor CAPTURE = Activator.getImageDescriptor("icons/capture.png");
	
	public static final ImageDescriptor SELECTOR = Activator.getImageDescriptor("icons/selector.png");
	
	public static final ImageDescriptor DEBUG = Activator.getImageDescriptor("icons/bug.png");
	
	public static final Image WARN               = Activator.getImage("icons/warn.png");
	public static final Image PASSWORD_img       = Activator.getImage("icons/password.png");
	public static final Image GO_PAST            = Activator.getImage("icons/gopast.png");
	public static final Image SEARCH             = Activator.getImage("icons/zoom.png");
	public static final ImageDescriptor PASSWORD = Activator.getImageDescriptor("icons/password.png");
	
	public static final Image PROFILE_FULL       = Activator.getImage("icons/profilefull.png");
	public static final Image PROFILE_SUMMARY    = Activator.getImage("icons/profilesummary.png");
	
	public static final Image COMMENT                       = Activator.getImage("icons/comment.png");
	public static final ImageDescriptor COMMENT_DESCRIPTOR  = Activator.getImageDescriptor("icons/comment.png");
	
	public static final ImageDescriptor WRITE               = Activator.getImageDescriptor("icons/counter/packet_recv.png");
	
	public static final ImageDescriptor LIST                 = Activator.getImageDescriptor("icons/list.png");
	public static final ImageDescriptor DETAIL               = Activator.getImageDescriptor("icons/detail.png");
	
	private static HashMap<String, Image> objAliveMap = new HashMap<String, Image>();
	private static HashMap<String, ImageDescriptor> objAliveDescriptorMap = new HashMap<String, ImageDescriptor>();
	
	public static final Image MENU                       = Activator.getImage("icons/menu.png");
	
	public static final Image ALERT_BIG                  = Activator.getImage("icons/alert_big.png");
	public static final ImageDescriptor ALERT_FATAL                = Activator.getImageDescriptor("icons/alert_fatal.png");
	public static final ImageDescriptor ALERT_WARN                 = Activator.getImageDescriptor("icons/alert_warn.png");
	public static final ImageDescriptor ALERT_ERROR                = Activator.getImageDescriptor("icons/alert_error.png");
	public static final ImageDescriptor ALERT_INFO                = Activator.getImageDescriptor("icons/alert_info.png");
	
	public static ImageDescriptor getObjectImageDescriptor(String objType, boolean isActive, int serverId){
		Server server = null;
		if (serverId == 0) {
			server = ServerManager.getInstance().getDefaultServer();
		} else {
			server = ServerManager.getInstance().getServer(serverId);
		}
		CounterEngine counterEngine = server.getCounterEngine();
		ObjectType type = counterEngine.getObjectType(objType);
		if (type != null && StringUtil.isNotEmpty(type.getIcon())) {
			objType = type.getIcon();
		} 
		String imgName = "icons/object/"+objType+".png";
		if(!isActive){
			imgName = "icons/object/"+objType+"_inact.png";
		}
		ImageDescriptor image = objAliveDescriptorMap.get(imgName);
		if(image!= null)
			return image;
		image = Activator.getImageDescriptor(imgName);
		if(image== null)
			image = default_context_descriptor;
		objAliveDescriptorMap.put(imgName, image);
		return image;
	}
	
	public static Image getObjectIcon(String objType, boolean isActive, int serverId){
		Server server = null;
		if (serverId == 0) {
			server = ServerManager.getInstance().getDefaultServer();
		} else {
			server = ServerManager.getInstance().getServer(serverId);
		}
		CounterEngine counterEngine = server.getCounterEngine();
		ObjectType type = counterEngine.getObjectType(objType);
		if (type != null && StringUtil.isNotEmpty(type.getIcon())) {
			objType = type.getIcon();
		} 
		String imgName = "icons/object/"+objType+".png";
		if(!isActive){
			imgName = "icons/object/"+objType+"_inact.png";
		}
		Image image = objAliveMap.get(imgName);
		if(image!= null)
			return image;
		image = Activator.getImage(imgName);
		if(image== null)
			image = default_context;
		objAliveMap.put(imgName, image);
		return image;
	}
	
	public static Image getObjectAlert48Icon(String objType, int serverId){
		Server server = null;
		if (serverId == 0) {
			server = ServerManager.getInstance().getDefaultServer();
		} else {
			server = ServerManager.getInstance().getServer(serverId);
		}
		CounterEngine counterEngine = server.getCounterEngine();
		ObjectType type = counterEngine.getObjectType(objType);
		if (type != null && StringUtil.isNotEmpty(type.getIcon())) {
			objType = type.getIcon();
		} 
		String imgName = "icons/object/48/"+objType+"_alert.png";
		Image image = Activator.getImage(imgName);
		if(image== null || image == ImageUtil.UNKNOWN)
			image = folder_48;
		return image;
	}
	
	public static Image getObject48Icon(String objType, boolean isActive, int serverId){
		Server server = null;
		if (serverId == 0) {
			server = ServerManager.getInstance().getDefaultServer();
		} else {
			server = ServerManager.getInstance().getServer(serverId);
		}
		CounterEngine counterEngine = server.getCounterEngine();
		ObjectType type = counterEngine.getObjectType(objType);
		if (type != null && StringUtil.isNotEmpty(type.getIcon())) {
			objType = type.getIcon();
		} 
		String imgName = "icons/object/48/"+objType+".png";
		if(!isActive){
			imgName = "icons/object/48/"+objType+"_inact.png";
		}
		Image image = Activator.getImage(imgName);
		if(image== null || image == ImageUtil.UNKNOWN)
			image = folder_48;
		return image;
	}
	
	private static HashMap<String, Image> objCntImgMap = new HashMap<String, Image>();
	public static Image getCounterImage(String objType, String counter, int serverId) {
		
		Server server = ServerManager.getInstance().getServer(serverId);
		if(server == null)
			return Images.unknown;
		String imgName = server.getCounterEngine().getCounterIconFileName(objType, counter);
		if (StringUtil.isEmpty(imgName)) {
			return ImageUtil.UNKNOWN;
		}
		Image image = objCntImgMap.get(imgName);
		if(image!= null)
			return image;
		image = Activator.getImage("icons/counter/"+imgName);
		objCntImgMap.put(imgName, image);
		return image;
	}
	
	private static HashMap<String, ImageDescriptor> objCntImgDescMap = new HashMap<String, ImageDescriptor>();
	public static ImageDescriptor getCounterImageDescriptor(String objType, String counter, int serverId) {
		
		Server server = ServerManager.getInstance().getServer(serverId);
		if(server == null)
			return null;
		String imgName = server.getCounterEngine().getCounterIconFileName(objType, counter);
		
		if (StringUtil.isEmpty(imgName)) {
			return ImageUtil.getImageDescriptor(ImageUtil.UNKNOWN);
		}
		ImageDescriptor image = objCntImgDescMap.get(imgName);
		if(image!= null)
			return image;
		image = Activator.getImageDescriptor("icons/counter/"+imgName);
		objCntImgDescMap.put(imgName, image);
		return image;
	}
	
	public static Image getCounterIconByName(String imageName){
		return Activator.getImage("icons/counter/"+imageName);
	}
	
	public static ArrayList<String> getAllCounterImages(int serverId){
		String path = "";
		try {
			Bundle bundle = Platform.getBundle("scouter.client");
			URL url = FileLocator.find(bundle, new Path("icons/counter/"), null);
			path = FileLocator.toFileURL(url).getPath();
			File file = new File(path);
			ArrayList<String> arr = new ArrayList<String>(Arrays.asList(file.list()));
			Collections.sort(arr);
			return arr;
		} catch (MalformedURLException me) {
			System.out.println("Fehler bei URL " + me.getStackTrace());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}	
	
	public static final ImageDescriptor REMOVE_WARNING         = Activator.getImageDescriptor("icons/remove_warn.png");
	
	public static final Image error                            = Activator.getImage("icons/error.png");
	public static final Image circle                           = Activator.getImage("icons/circle.png");
	public static final Image arrow_right                      = Activator.getImage("icons/arrow_right.png");
	public static final Image arrow_left                       = Activator.getImage("icons/arrow_left.png");
	public static final Image box			                   = Activator.getImage("icons/box.png");
	public static final Image align_left			           = Activator.getImage("icons/align_left.png");
	public static final Image color_swatch		               = Activator.getImage("icons/color_swatch.png");
	public static final Image color_wheel			           = Activator.getImage("icons/color_wheel.png");
	public static final Image arrow_rotate			           = Activator.getImage("icons/arrow_rotate_anticlockwise.png");
	public static final Image timer			                   = Activator.getImage("icons/timer.gif");
	public static final ImageDescriptor MORE                   = Activator.getImageDescriptor("icons/moredot.png");
	
	public static final Image group                            = Activator.getImage("icons/group.png");
	public static final Image group_add                        = Activator.getImage("icons/group_add.png");
	public static final Image group_delete                     = Activator.getImage("icons/group_delete.png");
	public static final Image group_go                         = Activator.getImage("icons/group_go.png");
	public static final Image group_edit                       = Activator.getImage("icons/group_edit.png");
	
	public static final Image user_add                         = Activator.getImage("icons/user_add.png");
	public static final Image user_edit                        = Activator.getImage("icons/user_edit.png");
	
	public static final Image database_go                      = Activator.getImage("icons/database_go.png");
	public static final Image link                             = Activator.getImage("icons/link.png");
	public static final Image flat_layout                      = Activator.getImage("icons/flat_layout.gif");
	public static final Image tree_mode                        = Activator.getImage("icons/tree_mode.gif");
	public static final Image exclamation                      = Activator.getImage("icons/exclamation.png");
	public static final Image find                             = Activator.getImage("icons/find.png");
	public static final Image server_chart                     = Activator.getImage("icons/server_chart.png");
	public static final Image server                           = Activator.getImage("icons/server.png");
	public static final Image database                         = Activator.getImage("icons/database.png");
	public static final Image grid                             = Activator.getImage("icons/grid.png");
	public static final Image log                              = Activator.getImage("icons/log.png");
	public static final Image sum                              = Activator.getImage("icons/sum.png");
	public static final Image pin                              = Activator.getImage("icons/pin.gif");
	public static final Image lock                             = Activator.getImage("icons/lock.png");
	public static final Image star                             = Activator.getImage("icons/star.png");
	public static final Image page_white_stack                 = Activator.getImage("icons/page_white_stack.png");
	public static final Image page_white_text                  = Activator.getImage("icons/page_white_text.png");
}

