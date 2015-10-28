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
import java.io.FileNotFoundException;
import java.io.IOException;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class ImageRegistryUtil {

    public static ImageDescriptor registImageFile(AbstractUIPlugin plugin, File file) throws IOException{
        ImageRegistry registry = plugin.getImageRegistry();
        String rootpath = LocationUtil.getAbsoluteFile(plugin, "").getAbsolutePath();
        if (!file.getAbsolutePath().startsWith(rootpath)) return null;
        String rpath = file.getAbsolutePath().substring(rootpath.length() + 1).replaceAll("\\\\", "/");
        ImageDescriptor descriptor = ImageDescriptor.createFromURL(LocationUtil.getURL(plugin, rpath));
        registry.put(rpath, descriptor);
        return descriptor;
    }

    public static ImageDescriptor registImage(AbstractUIPlugin plugin, String path) throws FileNotFoundException{
        ImageRegistry registry = plugin.getImageRegistry();
        ImageDescriptor descriptor = ImageDescriptor.createFromURL(LocationUtil.getURL(plugin, path));
        registry.put(path, descriptor);
        return descriptor;
    }

    public static Image registImage(AbstractUIPlugin plugin, String id, Image image){
        ImageRegistry registry = plugin.getImageRegistry();
        registry.put(id, image);
        return image;
    }

    public static ImageDescriptor registImageDescriptor(AbstractUIPlugin plugin, String id, ImageDescriptor descriptor){
        ImageRegistry registry = plugin.getImageRegistry();
        registry.put(id, descriptor);
        return descriptor;
    }

    //Check
    //getImageDescriptor & getImage �ÿ� ImageDescriptor ����Ұ�
    public static Image getImage(AbstractUIPlugin plugin, String id){
        ImageRegistry imageRegistry = plugin.getImageRegistry();
        Image image = imageRegistry.get(id);
        if (image == null) {
            try {
                registImage(plugin, id);
                image = imageRegistry.get(id);
            } catch (FileNotFoundException e) {
                image = ImageUtil.UNKNOWN;
            }
        }
        return image;
    }

}
