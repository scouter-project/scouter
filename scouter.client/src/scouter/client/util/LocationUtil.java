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
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class LocationUtil {

    public static URL getURL(AbstractUIPlugin plugin, String path) throws FileNotFoundException{
        URL url = FileLocator.find(plugin.getBundle(), new Path(path), null);
        if (url == null) throw new FileNotFoundException(path + " Not found");
        return url;
    }

    public static File getAbsoluteFile(AbstractUIPlugin plugin, String path) throws IOException{
        URL url = FileLocator.resolve(getURL(plugin, path));
        return new File(url.getFile());
    }

    public static File getBundleLocation(AbstractUIPlugin plugin){
        return new File(plugin.getBundle().getLocation());
    }

    // eclipse/configuration
    public static File getConfigurationLocation(){
        Location loc = Platform.getConfigurationLocation();
        return new File(loc.getURL().getFile());
    }

    // eclipse
    public static File getInstallLocation(){
        Location loc = Platform.getInstallLocation();
        return new File(loc.getURL().getFile());
    }

    // eclipse/workspace
    public static File getInstanceLocation(){
        Location loc = Platform.getInstanceLocation();
        Platform.getUserLocation();
        return new File(loc.getURL().getFile());
    }

    public static File getStateLocation(AbstractUIPlugin plugin){
        IPath path = Platform.getStateLocation(plugin.getBundle());
        return path.toFile();
    }

}
