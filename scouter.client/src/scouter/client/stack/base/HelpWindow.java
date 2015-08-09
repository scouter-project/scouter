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

import java.awt.BorderLayout;
import java.awt.Container;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;

@SuppressWarnings("serial")
public class HelpWindow extends JFrame{
	JEditorPane m_htmlPane = null;
	
	   static public void createHelpWindow() {
	        @SuppressWarnings("unused")
	        HelpWindow window = new HelpWindow();
	    }

	    static public void startHelpWindow() {
	        javax.swing.SwingUtilities.invokeLater(new Runnable() {
	            public void run() {
	                createHelpWindow();
	            }
	        });
	    }

	    public HelpWindow() {
	        super();
	        this.setTitle("About SDPA");
	        init();
	    }

	    private void init() {
	        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

	        m_htmlPane = new JEditorPane();

	        m_htmlPane.setContentType("text/html");

	        Container content = getContentPane();
	        JScrollPane scrollPane = new JScrollPane(m_htmlPane);
	        content.add(scrollPane, BorderLayout.CENTER);
	        
	        m_htmlPane.setEditable(false);
	        m_htmlPane.setText(readHelpFile());
	        setVisible(true);
	        setSize(700, 300);
	        this.validate();
	    }
	    
	    private String readHelpFile(){
	    	StringBuilder buffer = new StringBuilder(1024000);
            InputStream is = MainFrame.class.getResourceAsStream("/scouter/client/stack/doc/SDPA-help.html");
            
            BufferedReader br = null;
            String line = null;
            try {
                br = new BufferedReader(new InputStreamReader(is));
                while ( (line = br.readLine()) != null) {
                    buffer.append(line);
                    buffer.append("\n");
                }                
            } catch ( IllegalArgumentException ex ) {
                ex.printStackTrace();
            } catch ( IOException ex ) {
                ex.printStackTrace();
            } finally {
                try {
                    if ( br != null ) {
                        br.close();
                        is.close();
                    }
                } catch ( IOException ex ) {
                    ex.printStackTrace();
                }
            }        
	    	return buffer.toString();
	    }
}