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
package scouter.client.views;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import scouter.client.Images;
import scouter.client.net.INetReader;
import scouter.client.net.TcpProxy;
import scouter.client.util.ColoringWord;
import scouter.client.util.CustomLineStyleListener;
import scouter.client.util.ExUtil;
import scouter.client.util.ScouterUtil;
import scouter.client.util.UIUtil;
import scouter.io.DataInputX;
import scouter.lang.pack.MapPack;
import scouter.net.RequestCmd;
import scouter.util.FileUtil;
import scouter.util.StringUtil;

public class ObjectDumpFileDetailView extends ViewPart {

	public final static String ID = ObjectDumpFileDetailView.class.getName();

	int serverId;
	int objHash;
	String filename;
	StyledText text;

	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		String secondaryId = site.getSecondaryId();
		if (secondaryId != null) {
			String secIds[] = secondaryId.split("&");
			serverId = Integer.parseInt(secIds[0]);
			objHash = Integer.parseInt(secIds[1]);
		}
	}

	public void setInput(String filename) {
		this.filename = filename;
		this.setPartName(filename);
		load();
	}

	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new GridLayout(1, true));
		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		comp.setLayout(UIUtil.formLayout(5, 5));
		Button saveAsBtn = new Button(comp, SWT.PUSH);
		saveAsBtn.setImage(Images.save);
		saveAsBtn.setText("&Save As");
		saveAsBtn.setLayoutData(UIUtil.formData(null, -1, null, -1, 100, -5, null, -1));
		saveAsBtn.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				saveAs();
			}
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
		final Text searchText = new Text(comp, SWT.BORDER);
		searchText.setLayoutData(UIUtil.formData(null, -1, 0, 2, saveAsBtn, -5, null, -1, 100));
		final CustomLineStyleListener styleListener = new CustomLineStyleListener(false, new ArrayList<ColoringWord>(), false, false, SWT.COLOR_YELLOW);
		searchText.addKeyListener(new KeyListener() {
			public void keyReleased(KeyEvent e) {
				if (StringUtil.isNotEmpty(searchText.getText())) {
					styleListener.setSearchString(searchText.getText());
					text.redraw();
				} else {
					styleListener.setSearchString(null);
					text.redraw();
				}
			}
			public void keyPressed(KeyEvent e) {}
		});
		text = new StyledText(parent, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL
				| SWT.H_SCROLL);
		text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		text.addLineStyleListener(styleListener);
	}

	private void load() {
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
				try {
					MapPack param = new MapPack();
					param.put("objHash", objHash);
					param.put("name", filename);
					final List<Byte> bytesList = new ArrayList<Byte>();
					tcp.process(RequestCmd.OBJECT_DUMP_FILE_DETAIL, param,
							new INetReader() {
								public void process(DataInputX in)
										throws IOException {
									byte[] blob = in.readBlob();
									for (int i = 0; i < blob.length; i++) {
										bytesList.add(blob[i]);
									}
								}
							});
					ExUtil.exec(text, new Runnable() {
						public void run() {
							Byte[] bytes = bytesList.toArray(new Byte[bytesList
									.size()]);
							final byte[] result = new byte[bytes.length];
					        for (int i = 0; i < bytes.length; i++) {
					            result[i] = bytes[i].byteValue();
					        }
							String content = new String(result);
							text.setText(content);
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					TcpProxy.putTcpProxy(tcp);
				}
			}
		});
	}

	private void saveAs() {
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
				.getShell();
		FileDialog dialog = new FileDialog(shell, SWT.SAVE);
		dialog.setOverwrite(true);
		dialog.setFileName(filename);
		dialog.setFilterExtensions(new String[] { "*.txt", "*.*" });
		dialog.setFilterNames(new String[] { "Text File(*.txt)", "All Files" });
		String fileSelected = dialog.open();
		if (fileSelected != null) {
			FileUtil.save(fileSelected, text.getText().getBytes());
		}
	}

	public void setFocus() {
	}
	
}
