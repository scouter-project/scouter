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
package scouter.client.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.dynamichelpers.IExtensionChangeHandler;
import org.eclipse.jface.preference.IPreferenceNode;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveRegistry;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.views.IViewDescriptor;
import org.eclipse.ui.views.IViewRegistry;

@SuppressWarnings("restriction")
public class RCPUtil {

    public static boolean isEclipseIdeRunning(){
        IProduct product = Platform.getProduct();
        if (product == null) return false;
        // ("Eclipse SDK".equals(product.getName()));
        return "org.eclipse.sdk.ide".equals(product.getId()) && "org.eclipse.ui.ide.workbench".equals(product.getApplication());
    }

    public static void preLoadingPerspective(String[] ids){
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        if (page != null) {
            IPerspectiveRegistry registry = PlatformUI.getWorkbench().getPerspectiveRegistry();
            IPerspectiveDescriptor active = page.getPerspective();
            for (int idx = ids.length - 1; idx >= 0; idx--) {
                if (active == null || !active.getId().equals(ids[idx])) {
                    IPerspectiveDescriptor perspective = registry.findPerspectiveWithId(ids[idx]);
                    page.setPerspective(perspective);
                }
            }
            page.setPerspective(active);
        }
    }

    public static void hideActions(String[] ids){
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        for (int idx = 0; idx < ids.length; idx++) {
            page.hideActionSet(ids[idx]);
        }
    }
    
    public static void resetPerspective() {
    	IWorkbenchWindow windows[] = PlatformUI.getWorkbench().getWorkbenchWindows();
    	for (int i = 0; i < windows.length; i++) {
    		IWorkbenchPage pages[] = windows[i].getPages();
    		if (pages == null || pages.length < 1) continue;
    		for (int j = 0; j < pages.length; j++) {
    			pages[j].resetPerspective();
    		}
    	}
    }

    public static void hidePreference(String[] ids){
//    	List<String> list = Arrays.asList(ids);
        PreferenceManager preferenceManager = PlatformUI.getWorkbench().getPreferenceManager();
//        @SuppressWarnings("unchecked")
//        List<IPreferenceNode> preferenceNodes = preferenceManager.getElements(PreferenceManager.PRE_ORDER);
//        for (Iterator<IPreferenceNode> it = preferenceNodes.iterator(); it.hasNext();) {
//            IPreferenceNode preferenceNode = (IPreferenceNode)it.next();
//            if (list.contains(preferenceNode.getId())) {
//                preferenceManager.remove(preferenceNode);
//            }
//        }
        
        for(String id : ids){
        	preferenceManager.remove(id);
        }
        
    }
    
    public static void hidePerspectives(String[] ids) {
    	IPerspectiveRegistry registry = PlatformUI.getWorkbench().getPerspectiveRegistry();
    	IPerspectiveDescriptor[] descriptors = registry.getPerspectives();
    	List ignoredPerspectives = Arrays.asList(ids);
    	List removePerspectiveDesc = new ArrayList();
    	for (IPerspectiveDescriptor desc : descriptors) {
    		if (ignoredPerspectives.contains(desc.getId())) {
    			removePerspectiveDesc.add(desc);
    		}
    	}
    	if (registry instanceof IExtensionChangeHandler && !removePerspectiveDesc.isEmpty()) {
    		IExtensionChangeHandler extChgHandler = (IExtensionChangeHandler) registry;
    		extChgHandler.removeExtension(null, removePerspectiveDesc.toArray());
    	}
    }

    public static void printPreferencePages(){
        System.out.println("=== PreferencePages ===");
        PreferenceManager preferenceManager = PlatformUI.getWorkbench().getPreferenceManager();
        @SuppressWarnings("unchecked")
        List<IPreferenceNode> preferenceNodes = preferenceManager.getElements(PreferenceManager.PRE_ORDER);
        for (Iterator<IPreferenceNode> it = preferenceNodes.iterator(); it.hasNext();) {
            IPreferenceNode preferenceNode = (IPreferenceNode)it.next();
            System.out.println(preferenceNode.getId());
        }
    }

    public static void printPerspectives(){
        System.out.println("=== Perspectives ===");
        IPerspectiveRegistry registry = PlatformUI.getWorkbench().getPerspectiveRegistry();
        IPerspectiveDescriptor[] descriptors = registry.getPerspectives();
        for (int idx = 0; idx < descriptors.length; idx++) {
            System.out.println(descriptors[idx].getId());
        }
    }

    public static void printViews(){
        System.out.println("=== Views ===");
        IViewRegistry registry = PlatformUI.getWorkbench().getViewRegistry();
        IViewDescriptor[] descriptors = registry.getViews();
        for (int idx = 0; idx < descriptors.length; idx++) {
            System.out.println(descriptors[idx].getId());
        }

    }

    public static void exit(){
        IWorkbench workbench = PlatformUI.getWorkbench();
        if (workbench != null && !workbench.isClosing()) {
            workbench.close();
        }
    }
    
    public static void restart(){
        IWorkbench workbench = PlatformUI.getWorkbench();
        if (workbench != null && !workbench.isClosing()) {
            workbench.restart();
        }
    }

    
    public static File getWorkingDirectory() {
    	IPath workingPath = Platform.getLocation();
		File workingDir = workingPath.toFile();
		return workingDir;
    }
    
    public static void openPerspective(String perspectiveID) {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		try {
			PlatformUI.getWorkbench().showPerspective(perspectiveID, window, null);
		} catch (WorkbenchException e) {
			e.printStackTrace();
		}
	}
}
