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

import java.io.FileNotFoundException;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class ImageUtil {

    public static final Image UNKNOWN = getSharedImage(ISharedImages.IMG_OBJ_FILE);

    public static Image getSharedImage(String key){
        return PlatformUI.getWorkbench().getSharedImages().getImage(key);
    }

    public static ImageDescriptor getImageDescriptor(String pluginId, String path){
        return AbstractUIPlugin.imageDescriptorFromPlugin(pluginId, path);
    }

    public static ImageDescriptor getImageDescriptor(AbstractUIPlugin plugin, String path) throws FileNotFoundException{
        return ImageDescriptor.createFromURL(LocationUtil.getURL(plugin, path));
    }

    public static ImageDescriptor getImageDescriptor(Image image){
        return ImageDescriptor.createFromImage(image);
    }

    public static Image getImage(String pluginId, String path){
        return getImageDescriptor(pluginId, path).createImage();
    }

    public static Image getImage(AbstractUIPlugin plugin, String path) throws FileNotFoundException{
        return getImageDescriptor(plugin, path).createImage();
    }

    public static Image getExtensionIcon(AbstractUIPlugin plugin, String filename){
        int index = filename.lastIndexOf(".");
        String key = "." + (index == -1 ? "" : filename.substring(index + 1));
        ImageDescriptor ds = PlatformUI.getWorkbench().getEditorRegistry().getSystemExternalEditorImageDescriptor(key);
        return ds == null ? UNKNOWN : ds.createImage();
    }

}
