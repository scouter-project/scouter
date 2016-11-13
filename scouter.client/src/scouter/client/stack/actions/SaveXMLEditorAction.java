/*
 *  Copyright 2016 the original author or authors. 
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
 */
package scouter.client.stack.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;

import scouter.client.stack.views.XMLEditorView;

public class SaveXMLEditorAction extends Action {
	public final static String ID = OpenXMLEditorAction.class.getName();

	XMLEditorView m_editorView = null;
	
	public SaveXMLEditorAction(XMLEditorView editorView, String label, ImageDescriptor imageDescriptor) {
		super(label, imageDescriptor);
		this.setId(ID);
		m_editorView = editorView;
	}
	
	public void run(){
		if (m_editorView != null) {
			try {
				m_editorView.saveConfigurations();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
}