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
 *  @Kwon
 *
 */
package scouter.client;


import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.PropertyResourceBundle;
import java.util.Set;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import scouter.client.util.ImageRegistryUtil;
import scouter.client.util.ImageUtil;


/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	public static final String ID = Activator.class.getPackage().getName();

	public static final String APPLICATION_ID = ID + ".application"; //$NON-NLS-1$

	public static final String PRODUCT_ID = ID + ".product"; //$NON-NLS-1$

	private static Activator plugin;
	
	protected PropertyResourceBundle localProperties;
	
	private static Set<String> prePerspectiveSet = new HashSet<String>();

	public Activator() {
		plugin = this;
	}

	public void start(BundleContext context) throws Exception {
		super.start(context);
		if (plugin == null) {
			plugin = this;
		}

	}

	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	public static Activator getDefault() {
		return plugin;
	}

	public static Image getImage(String id) {
		return ImageRegistryUtil.getImage(plugin, id);
	}

	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(ID, path);
	}

	public static Image getExtensionImage(File file) {
		return ImageUtil.getExtensionIcon((AbstractUIPlugin) getDefault(), file.getName());
	}

	public static Image getExtensionImage(String filename) {
		return ImageUtil.getExtensionIcon((AbstractUIPlugin) getDefault(), filename);
	}
	
	public  PropertyResourceBundle getLocalProperties() {
		 if (localProperties == null){
			 try {
				 localProperties = new PropertyResourceBundle(
						 FileLocator.openStream(plugin.getBundle(), new Path("localization.properties"),false));
			 } catch (IOException e) {
			 }
		 }
		 return localProperties;
	 }
	 
	public void addPrePerspective(String id) {
		prePerspectiveSet.add(id);
	}
		
	public boolean isPrePerspective(String id) {
		return prePerspectiveSet.contains(id);
	}
}
