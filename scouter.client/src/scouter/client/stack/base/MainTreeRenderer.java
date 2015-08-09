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
package scouter.client.stack.base;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

import scouter.client.stack.data.StackFileInfo;
import scouter.client.stack.utils.ResourceUtils;

@SuppressWarnings("serial")
public class MainTreeRenderer extends DefaultTreeCellRenderer {

    public MainTreeRenderer() {
    }

    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
                        boolean expanded, boolean leaf, int row, boolean hasFocus) {

        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        if(((DefaultMutableTreeNode)value).getUserObject() instanceof StackFileInfo){
            setIcon(ResourceUtils.getImageResource("thread.gif"));
        }else{
            setIcon(ResourceUtils.getImageResource("list.gif"));        	
        }
        this.setBackgroundNonSelectionColor(new Color(0,0,0,0));       
        return this;
    }
}
