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
package scouter.client.stack.views;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

import scouter.client.Images;
import scouter.client.stack.actions.SaveXMLEditorAction;
import scouter.client.stack.base.MainProcessor;
import scouter.client.stack.base.PreferenceManager;
import scouter.client.stack.config.XMLReader;
import scouter.client.stack.data.StackFileInfo;
import scouter.client.stack.utils.ResourceUtils;
import scouter.client.util.ColoringWord;
import scouter.client.util.ImageUtil;
import scouter.client.util.CustomLineStyleListener;

public class XMLEditorView extends ViewPart {
	public final static String ID = XMLEditorView.class.getName();
	
	private ArrayList<ColoringWord> m_defaultHighlightings;
	
	private StyledText m_text;
	private String m_fileName;
	private SaveXMLEditorAction m_saveAction;
	
	private CustomLineStyleListener m_listener;
	
	public void createPartControl(Composite parent) {
		m_text = new StyledText(parent, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		initDefaultHighlightings();
		m_listener = new CustomLineStyleListener(true, m_defaultHighlightings, false);
		m_text.addLineStyleListener(m_listener);
		m_text.addKeyListener(new KeyListener() {
			public void keyReleased(KeyEvent e) {
			}
			public void keyPressed(KeyEvent e) {
				if(e.stateMask == SWT.CTRL){
					if(e.keyCode == 's'){
						saveConfigurations();
					}else if(e.keyCode == 'a'){
						m_text.selectAll();
					}
				}
			}
		});
		
		String fileName = null;
		MainProcessor mainProcessor = MainProcessor.instance();
		if(!mainProcessor.isDefaultConfiguration()){
			fileName = checkFile(PreferenceManager.get().getCurrentParserConfig());
		}
		
		if(fileName == null){
			StackFileInfo stackFileInfo = mainProcessor.getSelectedStackFileInfo();
			if(stackFileInfo == null){
				fileName = XMLReader.DEFAULT_XMLCONFIG;
			}else{
				fileName = checkFile(stackFileInfo.getParserConfig().getConfigFilename());
			}
		}
		setFileName(fileName);
		
		initialToolBar();
		loadConfig();
	}
	
	private String checkFile(String fileName){
		if(fileName == null){
			return null;
		}
		
		File file = new File(fileName);
		if(!file.exists() || !file.isFile()){
			String message = new StringBuilder(100).append(fileName).append(" file is not exist. then editor use a default xml configuration.").toString();
			ResourceUtils.confirmMessage(this.getSite().getShell(), message);
			return XMLReader.DEFAULT_XMLCONFIG;
		}
		return fileName;
	}

	private void setFileName(String fileName){
		m_fileName = fileName;
		this.setPartName("ConfigStackAnalyzer-" + m_fileName);
	}
	
	private void loadConfig() {
		BufferedInputStream in = null;
		int fileSize = 0;
		byte [] data = null;
		try {
			if(XMLReader.DEFAULT_XMLCONFIG.equals(m_fileName)){
				in = new BufferedInputStream(ResourceUtils.getDefaultXMLConfig());

				data = new byte[102400];
				int ch;
				while((ch = in.read()) >=0){
					data[fileSize] =  (byte)ch;
					fileSize ++;
				}				
			}else{
				File file = new File(m_fileName);
				fileSize = (int)file.length();
				in = new BufferedInputStream(new FileInputStream(file));
				
				data = new byte[fileSize];
				in.read(data);
			}		
			m_text.setText(new String(data, 0, fileSize, "UTF-8"));
		}catch(Exception ex){
			ex.printStackTrace();
		}finally{
			if(in != null){
				try { in.close();}catch(Exception ex){}
			}
		}
	}	
	
	private void saveAsConfigurations(){
		String newFileName = ResourceUtils.openFileSaveDialog(new String [] {"XML Parser Configuration"}, new String [] {"*.xml"}, ".", "scouter_stackanalyzer");
		if(newFileName == null){
			return;
		}
		
		
		ResourceUtils.saveFile(newFileName, m_text.getText());
		
		if(XMLReader.DEFAULT_XMLCONFIG.equals(m_fileName)){
			ResourceUtils.setVisiable(this, m_saveAction.getId(), true);
		}
		
		setFileName(newFileName);
		PreferenceManager.get().setCurrentParserConfig(newFileName);
		MainProcessor.instance().displayContent(null);
	}

	public void saveConfigurations(){
		ResourceUtils.saveFile(m_fileName, m_text.getText());
	}
	

		
	private void initialToolBar() {
		IToolBarManager man = getViewSite().getActionBars().getToolBarManager();

		m_saveAction = new SaveXMLEditorAction(this, "Save", ImageUtil.getImageDescriptor(Images.save));
		man.add(m_saveAction);
		
		if(XMLReader.DEFAULT_XMLCONFIG.equals(m_fileName)){
			ResourceUtils.setVisiable(this, m_saveAction.getId(), false);
		}				
		
		man.add(new Action("SaveAs", ImageUtil.getImageDescriptor(Images.saveas)) {
			public void run() {
				saveAsConfigurations();
			}
		});		
	}

	public void setFocus() {
		IStatusLineManager slManager= getViewSite().getActionBars().getStatusLineManager();
		slManager.setMessage("CTRL + S : save configurations, CTRL + A : select all text");
	}

	
	private void initDefaultHighlightings(){
		m_defaultHighlightings = new ArrayList<ColoringWord>(20);
		m_defaultHighlightings.add(new ColoringWord("<scouter>", SWT.COLOR_BLUE, true));
		m_defaultHighlightings.add(new ColoringWord("</scouter>", SWT.COLOR_BLUE, true));
		m_defaultHighlightings.add(new ColoringWord("<parser", SWT.COLOR_BLUE, true));
		m_defaultHighlightings.add(new ColoringWord("</parser>", SWT.COLOR_BLUE, true));
		m_defaultHighlightings.add(new ColoringWord("<time", SWT.COLOR_BLUE, true));
		m_defaultHighlightings.add(new ColoringWord("</time>", SWT.COLOR_BLUE, true));
		m_defaultHighlightings.add(new ColoringWord("<workerThread>", SWT.COLOR_BLUE, true));
		m_defaultHighlightings.add(new ColoringWord("</workerThread>", SWT.COLOR_BLUE, true));
		m_defaultHighlightings.add(new ColoringWord("<workingThread", SWT.COLOR_BLUE, true));
		m_defaultHighlightings.add(new ColoringWord("</workingThread>", SWT.COLOR_BLUE, true));
		m_defaultHighlightings.add(new ColoringWord("<log>", SWT.COLOR_BLUE, true));
		m_defaultHighlightings.add(new ColoringWord("</log>", SWT.COLOR_BLUE, true));
		m_defaultHighlightings.add(new ColoringWord("<sql>", SWT.COLOR_BLUE, true));
		m_defaultHighlightings.add(new ColoringWord("</sql>", SWT.COLOR_BLUE, true));
		m_defaultHighlightings.add(new ColoringWord("<service", SWT.COLOR_BLUE, true));
		m_defaultHighlightings.add(new ColoringWord("</service>", SWT.COLOR_BLUE, true));
		m_defaultHighlightings.add(new ColoringWord("<singleStack>", SWT.COLOR_BLUE, true));
		m_defaultHighlightings.add(new ColoringWord("</singleStack>", SWT.COLOR_BLUE, true));
		m_defaultHighlightings.add(new ColoringWord("<excludeStack>", SWT.COLOR_BLUE, true));
		m_defaultHighlightings.add(new ColoringWord("</excludeStack>", SWT.COLOR_BLUE, true));
		m_defaultHighlightings.add(new ColoringWord("<analyze>", SWT.COLOR_BLUE, true));
		m_defaultHighlightings.add(new ColoringWord("</analyze>", SWT.COLOR_BLUE, true));
		m_defaultHighlightings.add(new ColoringWord("<analyzeStack", SWT.COLOR_BLUE, true));
		m_defaultHighlightings.add(new ColoringWord("</analyzeStack", SWT.COLOR_BLUE, true));
		m_defaultHighlightings.add(new ColoringWord("<list>", SWT.COLOR_BLUE, true));
		m_defaultHighlightings.add(new ColoringWord("</list>", SWT.COLOR_BLUE, true));
		m_defaultHighlightings.add(new ColoringWord("<listMain>", SWT.COLOR_BLUE, true));
		m_defaultHighlightings.add(new ColoringWord("</listMain>", SWT.COLOR_BLUE, true));
		m_defaultHighlightings.add(new ColoringWord("<jmx>", SWT.COLOR_BLUE, true));
		m_defaultHighlightings.add(new ColoringWord("</jmx>", SWT.COLOR_BLUE, true));
		m_defaultHighlightings.add(new ColoringWord("<count>", SWT.COLOR_BLUE, true));
		m_defaultHighlightings.add(new ColoringWord("</count>", SWT.COLOR_BLUE, true));
		m_defaultHighlightings.add(new ColoringWord("<interval>", SWT.COLOR_BLUE, true));
		m_defaultHighlightings.add(new ColoringWord("</interval>", SWT.COLOR_BLUE, true));
		m_defaultHighlightings.add(new ColoringWord("<path>", SWT.COLOR_BLUE, true));
		m_defaultHighlightings.add(new ColoringWord("</path>", SWT.COLOR_BLUE, true));
		m_defaultHighlightings.add(new ColoringWord("<server>", SWT.COLOR_BLUE, true));
		m_defaultHighlightings.add(new ColoringWord("</server>", SWT.COLOR_BLUE, true));
		m_defaultHighlightings.add(new ColoringWord("<ip>", SWT.COLOR_BLUE, true));
		m_defaultHighlightings.add(new ColoringWord("</ip>", SWT.COLOR_BLUE, true));
		m_defaultHighlightings.add(new ColoringWord("<port>", SWT.COLOR_BLUE, true));
		m_defaultHighlightings.add(new ColoringWord("</port>", SWT.COLOR_BLUE, true));
	}
}

