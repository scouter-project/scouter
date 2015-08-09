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

import javax.swing.BorderFactory;
import javax.swing.JScrollPane;

@SuppressWarnings("serial")
public class ViewScrollPane extends JScrollPane {
 
    public ViewScrollPane(Component comp, boolean white) {
        super(comp);
        if(white) {
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder());
            setBackground(Color.WHITE);
            getHorizontalScrollBar().setBackground(Color.WHITE);
        }
    }
}