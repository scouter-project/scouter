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

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import scouter.client.Images;
import scouter.client.stack.base.MainProcessor;
import scouter.client.util.ImageUtil;
import scouter.client.util.RCPUtil;
import scouter.client.xlog.SaveProfileJob;
import scouter.client.xlog.views.XLogFullProfileView;

public class WorkspaceExplorer extends ViewPart {

	private TreeViewer viewer;
	private int serverId;
	IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
	File workingDir = RCPUtil.getWorkingDirectory();

	public static final String ID = WorkspaceExplorer.class.getName();

	public void createPartControl(Composite parent) {
		viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.setContentProvider(new WorkspaceContentProvider());
		viewer.setLabelProvider(new WorkspaceLabelProvider());
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				IStructuredSelection selection = (IStructuredSelection) event.getSelection();
				Object element = selection.getFirstElement();
				if (element != null && element instanceof File && ((File) element).isFile()) {
					File selectedFile = (File) element;
					openFile(selectedFile);
				}else if(element != null && element instanceof File && ((File) element).isDirectory()) {
					TreeViewer viewer = (TreeViewer) event.getViewer();
				    IStructuredSelection thisSelection = (IStructuredSelection) event.getSelection(); 
				    Object selectedNode = thisSelection.getFirstElement(); 
				    viewer.setExpandedState(selectedNode, !viewer.getExpandedState(selectedNode));
				}
			}
		});
		viewer.getTree().addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.stateMask == SWT.CTRL) {
					if (e.keyCode == 'c' || e.keyCode == 'C') {
						new CopySelectedFilesAction().run();
					}
				}
			}
		});
		viewer.setInput(sortDirectoriesAndFiles(workingDir.listFiles(new ContentFilter())));
		fillTreeViewerCoolbar();
		createContextMenu(viewer, new IMenuListener() {
            public void menuAboutToShow(IMenuManager manager){
                fillTreeViewerContextMenu(manager);
            }
        });
	}
	
	public void setInput(int serverId){
		this.serverId = serverId;
		refreshViewer();
	}

	private void fillTreeViewerCoolbar() {
		IToolBarManager man = getViewSite().getActionBars().getToolBarManager();
		man.add(new Action("Expand All", ImageUtil.getImageDescriptor(Images.expand)) {
			public void run() {
				viewer.expandAll();
			}
		});
		man.add(new Action("Collapse All", ImageUtil.getImageDescriptor(Images.collapse)) {
			public void run() {
				viewer.collapseAll();
			}
		});
		man.add(new Separator());
		man.add(new Action("Refresh", ImageUtil.getImageDescriptor(Images.refresh)) {
			public void run() {
				refreshViewer();
			}
		});
		
	}
	
	public void refreshViewer() {
		Object[] elements = viewer.getExpandedElements();
		TreePath[] treePaths = viewer.getExpandedTreePaths();
		viewer.getTree().clearAll(true);
		viewer.setInput(sortDirectoriesAndFiles(workingDir.listFiles(new ContentFilter())));
		viewer.refresh();
		viewer.setExpandedElements(elements);
		viewer.setExpandedTreePaths(treePaths);
	}
	
	public void openFile(File selectedFile) {
		 if(SaveProfileJob.xLogFileName.equals(selectedFile.getName()) || SaveProfileJob.profileFileName.equals(selectedFile.getName())) {
			try {
				XLogFullProfileView view = (XLogFullProfileView) window.getActivePage().showView(XLogFullProfileView.ID, selectedFile.getParentFile().getName(), IWorkbenchPage.VIEW_ACTIVATE);
				if (view != null) {
					view.setInput(selectedFile.getParentFile(), serverId, false);
				}
			} catch (Exception e) {
				e.printStackTrace();
			} 
		}else if(SaveProfileJob.profileSummaryFileName.equals(selectedFile.getName())) {
			try {
				XLogFullProfileView view = (XLogFullProfileView) window.getActivePage().showView(XLogFullProfileView.ID, selectedFile.getParentFile().getName(), IWorkbenchPage.VIEW_ACTIVATE);
				if (view != null) {
					view.setInput(selectedFile.getParentFile(), serverId, true);
				}
			} catch (Exception e) {
				e.printStackTrace();
			} 
		} else if(selectedFile.getName().endsWith(".stack")) {
			MainProcessor.instance().processStackFile(selectedFile.getAbsolutePath());
		} else {
			Program.launch(selectedFile.getAbsolutePath());
		}
	}
	
	public Object[] sortDirectoriesAndFiles(File[] files) {
		List<File> fileList = Arrays.asList(files);
		Collections.sort(fileList, new Comparator<File>() {
			public int compare(File f1, File f2) {
				if (f1.isDirectory() && !f2.isDirectory()) {
					return -1;
				} else if (!f1.isDirectory() && f2.isDirectory()) {
					return 1;
				} else {
					return f1.getName().compareTo(f2.getName());
				}
			}
		});
		
		List<File> arr = new ArrayList<File>();
		for(int inx = 0 ; inx < fileList.size() ; inx++){
			File f = fileList.get(inx);
			if(f.isFile() && f.getName().equals(SaveProfileJob.xLogFileName)){
			} else{
				arr.add(f);
			}
		}
		
		return arr.toArray(new File[arr.size()]);
	}
	
	private void fillTreeViewerContextMenu(IMenuManager mgr){
		ISelection selection = viewer.getSelection();
		if (selection instanceof IStructuredSelection) {
            IStructuredSelection sel = (IStructuredSelection)selection;
            Object[] elements = sel.toArray();
            if (elements == null || elements.length < 1) {
            	return;
            }
            Object lastElement = elements[elements.length - 1];
            if (lastElement instanceof File) {
            	if (((File) lastElement).isFile()) {
            		mgr.add(new OpenSelectedFileAction((File) lastElement));
            		mgr.add(new Separator());
	            	mgr.add(new CopySelectedFilesAction());
	            	mgr.add(new CopyWSPathSelectedFilesAction());
	            	mgr.add(new Separator());
	            	mgr.add(new DeleteSelectedFileAction());
            	} else if (((File) lastElement).isDirectory()) {
            		mgr.add(new DeleteSelectedFileAction());
            	}
            }
        }
    }
	
	 private void createContextMenu(Viewer viewer, IMenuListener listener){
        MenuManager contextMenu = new MenuManager();
        contextMenu.setRemoveAllWhenShown(true);
        contextMenu.addMenuListener(listener);
        Menu menu = contextMenu.createContextMenu(viewer.getControl());
        viewer.getControl().setMenu(menu);
        getSite().registerContextMenu(contextMenu, viewer);
    }

	public void setFocus() {

	}

	public class WorkspaceContentProvider implements ITreeContentProvider {

		public Object[] getChildren(Object parent) {
			if (parent instanceof File) {
				File file = (File) parent;
				return sortDirectoriesAndFiles(file.listFiles(new ContentFilter()));
			}
			return null;
		}

		public Object[] getElements(Object inputElement) {
			return (Object[]) inputElement;
		}

		public Object getParent(Object element) {
			File file = (File) element;
			return file.getParentFile();
		}

		public boolean hasChildren(Object parent) {
			File file = (File) parent;
			return file.isDirectory();
		}

		public void dispose() {

		}

		public void inputChanged(Viewer arg0, Object arg1, Object arg2) {
		}
	}

	public class WorkspaceLabelProvider extends LabelProvider {
		
		public Image getImage(Object element) {
			if (element instanceof File) {
				File file = (File)element;
				if (file.isDirectory()) {
                    return Images.folder;
                }
				
				String fileName = ((File) element).getName();
				if (fileName.length() > 0) {
					if(SaveProfileJob.profileFileName.equals(fileName)){
						return Images.PROFILE_FULL;
					}else if(SaveProfileJob.profileSummaryFileName.equals(fileName)){
						return Images.PROFILE_SUMMARY;
					}
				}
				
                ImageDescriptor ds = PlatformUI.getWorkbench().getEditorRegistry().getSystemExternalEditorImageDescriptor(file.getName());
                if (ds != null) {
                    return ds.createImage();
                }
                return Images.unknown;
			}
			return super.getImage(element);
		}

		public String getText(Object element) {
			String fileName = ((File) element).getName();
			if (fileName.length() > 0) {
				if(SaveProfileJob.profileFileName.equals(fileName)){
					return "Full Profiles";
				} else if(SaveProfileJob.profileSummaryFileName.equals(fileName)){
					return "Profile Summary";
				} else if(SaveProfileJob.xLogFileName.equals(fileName)){
					return null;
				}
				return fileName;
			}
			return ((File) element).getPath();
		}
		
	}
	
	public class ContentFilter implements FileFilter {

        public boolean accept(File file){
        	String fileName = file.getName();
			if(fileName.endsWith(".index") || fileName.endsWith(".zip") || fileName.endsWith(".threads")){
				return false;
			}
            return file.isFile() || !fileName.startsWith(".");
        }

    }
	
	public class OpenSelectedFileAction extends Action {
		
		File file;
		
		public OpenSelectedFileAction(File file) {
			super("&Open");
			this.file = file;
		}

		public void run() {
			openFile(file);
		}
	}
	
	public class DeleteSelectedFileAction extends Action {
		
		public DeleteSelectedFileAction() {
			super("&Delete");
		}

		public void run() {
			ISelection sel = viewer.getSelection();
	        if (sel instanceof StructuredSelection) {
	        	@SuppressWarnings("unchecked")
				Iterator<File> i = ((StructuredSelection)sel).iterator();
	        	while (i.hasNext()) {
	                File file = i.next();
	                if (file.isDirectory()) {
	                	deleteDirectory(file);
	                } else {
	                	file.delete();
	                }
	            }
	        }
	        refreshViewer();
		}
		
		public void deleteDirectory(File path) {
	        File[] files = path.listFiles();
	        for (File file : files) {
	            if (file.isDirectory()) {
	                deleteDirectory(file);
	            } else {
	                file.delete();
	            }
	        }
	        path.delete();
	    }
	}
	
	public class CopyWSPathSelectedFilesAction extends Action {
		
		private String sep = System.getProperty("line.separator").toString();
		
		public CopyWSPathSelectedFilesAction() {
			super("&Copy Path");
		}

		public void run() {
			ISelection sel = viewer.getSelection();
			if (sel instanceof StructuredSelection) {
	            StringBuilder sb = new StringBuilder();
	            @SuppressWarnings("unchecked")
	            Iterator<File> i = ((StructuredSelection)sel).iterator();
	            while (i.hasNext()) {
	                File file = i.next();
	                sb.append(file.getAbsolutePath().replace("\\", "/"));
	                if (i.hasNext()) {
	                    sb.append(sep);
	                }
	            }
	            Clipboard clipboard = new Clipboard(Display.getDefault());
	            clipboard.setContents(new Object[] { sb.toString() }, new Transfer[] { TextTransfer.getInstance() });
	            clipboard.dispose();
	        }
		}
	}
	
	public class CopySelectedFilesAction extends Action {
		
		public CopySelectedFilesAction() {
			super("&Copy");
		}

		public void run() {
			ISelection sel = viewer.getSelection();
	        if (sel instanceof StructuredSelection) {
	            if (sel.isEmpty())
	                return;
	            List<String> paths = new ArrayList<String>();
	            @SuppressWarnings("unchecked")
	            Iterator<File> i = ((StructuredSelection)sel).iterator();
	            while (i.hasNext()) {
	                File file = i.next();
	                if (file.isDirectory()) continue;
	                paths.add(file.getAbsolutePath());
	            }
	            Clipboard clipboard = new Clipboard(Display.getDefault());
	            clipboard.setContents(new Object[] { (String[])paths.toArray(new String[paths.size()]) }, new Transfer[] { FileTransfer.getInstance() });
	            clipboard.dispose();
	        }
		}
	}
	
	public void afterDescriptionCreated() {
		refreshViewer();
	}
	
	
}
