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
package scouter.client.xlog.views;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.draw2d.IFigure;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.zest.core.viewers.GraphViewer;
import org.eclipse.zest.core.viewers.IConnectionStyleProvider;
import org.eclipse.zest.core.viewers.IGraphContentProvider;
import org.eclipse.zest.core.widgets.ZestStyles;
import org.eclipse.zest.layouts.LayoutStyles;
import org.eclipse.zest.layouts.algorithms.HorizontalTreeLayoutAlgorithm;

import scouter.client.Images;
import scouter.client.model.AgentDailyListProxy;
import scouter.client.model.AgentObject;
import scouter.client.model.TextProxy;
import scouter.client.model.XLogData;
import scouter.client.net.INetReader;
import scouter.client.net.TcpProxy;
import scouter.client.server.ServerManager;
import scouter.client.util.ColorUtil;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.ExUtil;
import scouter.client.xlog.XLogUtil;
import scouter.client.xlog.actions.OpenXLogProfileJob;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.pack.XLogPack;
import scouter.io.DataInputX;
import scouter.net.RequestCmd;
import scouter.util.DateUtil;
import scouter.util.FormatUtil;
import scouter.util.Hexa32;
import scouter.util.IPUtil;
import scouter.util.LongEnumer;
import scouter.util.LongKeyLinkedMap;
import scouter.util.LongKeyMap;

public class XLogCallView extends ViewPart {
	
	public static final String ID = XLogCallView.class.getName();
	
	private GraphViewer viewer = null;
	private String date;
	private long gxid;
	private XLogData xlogData;
	
	AgentDailyListProxy agentProxy = new AgentDailyListProxy();

	public void createPartControl(Composite parent) {
		parent.setLayout(new FillLayout());
		parent.addControlListener(new ControlListener() {
			public void controlResized(ControlEvent e) {
				viewer.applyLayout();
			}
			public void controlMoved(ControlEvent e) {
			}
		});
		viewer = new GraphViewer(parent, SWT.NONE);
		viewer.setContentProvider(new XLogFlowContentProvider());
		viewer.setLabelProvider(new XLogFlowLabelProvider());
		viewer.setLayoutAlgorithm(new HorizontalTreeLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING));
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				StructuredSelection sel = (StructuredSelection) event.getSelection();
				if (sel.getFirstElement() instanceof XLogConnection) {
					XLogData d = ((XLogConnection) sel.getFirstElement()).destXlog;
					String date = DateUtil.yyyymmdd(d.p.endTime);
					ArrayList<AgentObject> objList = agentProxy.getObjectList(date, d.serverId);
					for (AgentObject obj : objList) {
						if (obj.getObjHash() == d.p.objHash) {
							new OpenXLogProfileJob(XLogCallView.this.getViewSite().getShell().getDisplay(), d, d.serverId).schedule();
							break;
						}
					}
				}
			}
		});
	}
	
	public void searchByGxId(String date, long gxId) {
		this.date = date;
		this.gxid = gxId;
		this.setPartName("GxID - " + Hexa32.toString32(gxId));
		loadByGxId();
	}
	
	public void searchByTxId(XLogData d) {
		this.date = DateUtil.yyyymmdd(d.p.endTime);
		this.xlogData = d;
		loadByTxId();
	}
	
	public void setFocus() {
		
	}
	
	private void loadByGxId() {
		new LoadGlobalXLog().schedule();
	}
	
	private void loadByTxId() {
		new LoadChainXLog().schedule();
	}
	
	class LoadGlobalXLog extends Job {

		public LoadGlobalXLog() {
			super("Load " + date);
		}

		protected IStatus run(IProgressMonitor monitor) {
			monitor.beginTask("Find " + Hexa32.toString32(gxid), IProgressMonitor.UNKNOWN);
			final LongKeyLinkedMap<Object> xlogMap = new LongKeyLinkedMap<Object>();
			Iterator<Integer> itr = ServerManager.getInstance().getOpenServerList().iterator();
			while (itr.hasNext()) {
				final int serverId = itr.next();
				monitor.subTask(ServerManager.getInstance().getServer(serverId).getName());
				TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
				try {
					MapPack param = new MapPack();
					param.put("date", date);
					param.put("gxid", gxid);
					tcp.process(RequestCmd.XLOG_READ_BY_GXID, param, new INetReader() {
						public void process(DataInputX in) throws IOException {
							Pack p = in.readPack();
							XLogPack xlog = XLogUtil.toXLogPack(p);
							XLogData d = new XLogData(xlog, serverId);
							d.objName = TextProxy.object.getLoadText(date, d.p.objHash, d.serverId);
							xlogMap.putFirst(xlog.txid, d);
						}
					});
				} catch (Throwable th) {
					ConsoleProxy.errorSafe(th.toString());
				} finally {
					TcpProxy.putTcpProxy(tcp);
				}
			}
			ExUtil.exec(viewer.getGraphControl(), new Runnable() {
				public void run() {
					viewer.setInput(xlogMap);
				}
			});
			return Status.OK_STATUS;
		}
	}
	
	class LoadChainXLog extends Job {

		public LoadChainXLog() {
			super("Load " + date);
		}

		protected IStatus run(IProgressMonitor monitor) {
			monitor.beginTask("Find " + Hexa32.toString32(xlogData.p.txid), IProgressMonitor.UNKNOWN);
			final LongKeyLinkedMap<XLogData> xlogMap = new LongKeyLinkedMap<XLogData>();
			xlogMap.put(xlogData.p.txid, xlogData);
			long callerId = xlogData.p.caller;
			boolean found = true;
			while (found && callerId != 0) {
				found = false;
				Iterator<Integer> itr = ServerManager.getInstance().getOpenServerList().iterator();
				while (itr.hasNext()) {
					final int serverId = itr.next();
					monitor.subTask(ServerManager.getInstance().getServer(serverId).getName());
					TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
					try {
						MapPack param = new MapPack();
						param.put("date", date);
						param.put("txid", callerId);
						Pack p = tcp.getSingle(RequestCmd.XLOG_READ_BY_TXID, param);
						if (p != null) {
							XLogPack xlog = XLogUtil.toXLogPack(p);
							XLogData d = new XLogData(xlog, serverId);
							d.objName = TextProxy.object.getLoadText(date, d.p.objHash, serverId);
							xlogMap.put(xlog.txid, d);
							callerId = xlog.caller;
							found = true;
							break;
						}
					} catch (Throwable th) {
						ConsoleProxy.errorSafe(th.toString());
						callerId = 0;
					} finally {
						TcpProxy.putTcpProxy(tcp);
					}
				}
			}
			ExUtil.exec(viewer.getGraphControl(), new Runnable() {
				public void run() {
					viewer.setInput(xlogMap);
				}
			});
			return Status.OK_STATUS;
		}
	}
	
	class XLogFlowContentProvider implements IGraphContentProvider {
		
		 LongKeyLinkedMap<XLogData> xlogMap;

		public void dispose() {
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			if (newInput != null) {
				this.xlogMap = (LongKeyLinkedMap<XLogData>) newInput;
			}
		}

		public Object getSource(Object obj) {
			if (obj instanceof XLogConnection) {
				long key = ((XLogConnection) obj).sourceId;
				if (key == 0) {
					XLogData d = ((XLogConnection) obj).destXlog;
					return new StartHome(IPUtil.toString(d.p.ipaddr));
				}
				return xlogMap.get(key);
			}
			return null;
		}

		public Object getDestination(Object obj) {
			if (obj instanceof XLogConnection) {
				return ((XLogConnection) obj).destXlog;
			}
			return null;
		}

		public Object[] getElements(Object input) {
			LongEnumer keys = xlogMap.keys();
			ArrayList<XLogConnection> list = new ArrayList<XLogConnection>();
			LongKeyMap<Integer> countMap = new LongKeyMap<Integer>();
			while (keys.hasMoreElements()) {
				long key = keys.nextLong();
				XLogData d = xlogMap.get(key);
				long caller = d.p.caller;
				Integer cntInt = countMap.get(caller);
				if (cntInt == null) {
					cntInt = new Integer(1);
					countMap.put(caller, cntInt);
				} else {
					cntInt = new Integer(cntInt.intValue() + 1);
					countMap.put(caller, cntInt);
				}
				XLogConnection conn = new XLogConnection(caller, d);
				String serviceName = TextProxy.service.getLoadText(DateUtil.yyyymmdd(d.p.endTime), d.p.service, d.serverId);
				conn.name = "(" + cntInt.intValue() + ") " + serviceName.substring(serviceName.lastIndexOf("/"), serviceName.length());
				conn.elapsed = d.p.elapsed;
				conn.error = d.p.error != 0;
				list.add(conn);
			}
			return list.toArray();
		}
	}
	
	class XLogFlowLabelProvider implements ILabelProvider, IConnectionStyleProvider {
		
		public void addListener(ILabelProviderListener listener) {}
		public void dispose() {}
		public boolean isLabelProperty(Object element, String property) {
			return false;
		}
		public void removeListener(ILabelProviderListener listener) {}
		
		public int getConnectionStyle(Object rel) {
			return ZestStyles.CONNECTIONS_DIRECTED;
		}

		public Color getColor(Object rel) {
			if (rel instanceof XLogConnection) {
				XLogConnection conn = (XLogConnection) rel;
				if (conn.error || conn.elapsed > 8000) {
					return ColorUtil.getInstance().getColor(SWT.COLOR_DARK_RED);
				}
			}
			return ColorUtil.getInstance().getColor(SWT.COLOR_DARK_GRAY);
		}

		public Color getHighlightColor(Object rel) {
			return null;
		}

		public int getLineWidth(Object rel) {
			return 0;
		}

		public IFigure getTooltip(Object entity) {
			return null;
		}

		public Image getImage(Object element) {
			if (element instanceof StartHome) {
				return Images.CONFIG_USER;
			} else if (element instanceof XLogData) {
				XLogData d = (XLogData) element;
				AgentObject ao = agentProxy.getAgentObject(DateUtil.yyyymmdd(d.p.endTime), d.serverId, d.p.objHash);
				if (ao != null) {
					return Images.getObjectIcon(ao.getObjType(), true, ao.getServerId());
				}
			}
			return null;
		}

		public String getText(Object element) {
			if (element instanceof XLogData) {
				XLogData d = (XLogData) element;
				return d.objName;
			} else if (element instanceof XLogConnection) {
				XLogConnection conn = (XLogConnection) element;
				return conn.name + "(" + FormatUtil.print(conn.elapsed, "#,###") + " ms)";
			} else if (element instanceof StartHome) {
				return ((StartHome) element).ip;
			}
			return null;
		}
	}
	
	static class XLogConnection {
		long sourceId;
		XLogData destXlog;
		String name;
		long elapsed;
		boolean error = false;
		
		XLogConnection(long srcId, XLogData destXlog) {
			this.sourceId = srcId;
			this.destXlog = destXlog;
		}
	}
	
	static class StartHome {
		String ip;
		
		StartHome(String ip) {
			this.ip = ip;
		}
	}
}
