/*
 * Phon - An open source tool for research in phonology.
 * Copyright (C) 2005 - 2015, Gregory Hedlund <ghedlund@mun.ca> and Yvan Rose <yrose@mun.ca>
 * Dept of Linguistics, Memorial University <https://phon.ca>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ca.phon.phontalk.plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.SwingUtilities;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import ca.phon.phontalk.DefaultPhonTalkListener;
import ca.phon.phontalk.PhonTalkError;
import ca.phon.phontalk.PhonTalkListener;
import ca.phon.phontalk.PhonTalkMessage;

/**
 * Plug-in {@link PhonTalkListener} that can also act as a {@link TreeModel}.
 */
public class PluginMessageListener extends DefaultPhonTalkListener implements TreeModel {

	/**
	 * Keep track of messages by file
	 */
	private final Map<String, List<PhonTalkMessage>> messageMap = 
			Collections.synchronizedMap(new TreeMap<String, List<PhonTalkMessage>>());
	
	// use a separate list to keep track of the files we have
	private final List<String> fileList = 
			Collections.synchronizedList(new ArrayList<String>());
	
	private final List<TreeModelListener> listeners = 
			new ArrayList<TreeModelListener>();

	@Override
	public void message(PhonTalkMessage msg) {
		super.message(msg);
		
		final String f = (msg.getFile() == null ? "General" : msg.getFile().getAbsolutePath());
		List<PhonTalkMessage> messages = messageMap.get(f);
		if(messages == null) {
			messages = new ArrayList<PhonTalkMessage>();
			messageMap.put(f, messages);
			fileList.add(f);
			
			final TreeModelEvent evt = new TreeModelEvent(this, new Object[] { this } );
			fireStructureChanges(evt);
		}
		messages.add(msg);
		
		final TreeModelEvent evt = new TreeModelEvent(this, new Object[] { this , f, msg } );
		fireNodesInserted(evt);
		
		if(msg instanceof PhonTalkError) {
			final PhonTalkError err = PhonTalkError.class.cast(msg);
			err.getCause().printStackTrace();
		}
	}
	
	public void fireNodesInserted(final TreeModelEvent evt) {
		final Runnable onEDT = new Runnable() {
			
			@Override
			public void run() {
				for(TreeModelListener listener:listeners) {
					listener.treeNodesInserted(evt);
				}
			}
		};
		SwingUtilities.invokeLater(onEDT);
	}
	
	public void fireStructureChanges(final TreeModelEvent evt) {
		final Runnable onEDT = new Runnable() {
			
			@Override
			public void run() {
				for(TreeModelListener listener:listeners) {
					listener.treeStructureChanged(evt);
				}
			}
		};
		SwingUtilities.invokeLater(onEDT);
	}
	
	public void reset() {
		synchronized(messageMap) {
			this.messageMap.clear();
		}
		synchronized(fileList) {
			this.fileList.clear();
		}
		final TreeModelEvent evt = new TreeModelEvent(this, new Object[] { this } );
		fireStructureChanges(evt);
	}
	
	@Override
	public void addTreeModelListener(TreeModelListener l) {
		listeners.add(l);
	}

	@Override
	public Object getChild(Object parent, int i) {
		Object retVal = null;
		
		if(parent == this) {
			synchronized(fileList) {
				retVal = fileList.get(i);
			}
		} else if(parent instanceof String) {
			synchronized(messageMap) {
				final List<PhonTalkMessage> messages = 
						messageMap.get(parent.toString());
				retVal = messages.get(i);
			}
		}
		
		return retVal;
	}

	@Override
	public int getChildCount(Object parent) {
		int retVal = 0;
		if(parent == this) {
			synchronized (fileList) {
				retVal = fileList.size();				
			}
		} else if(parent instanceof String) {
			synchronized(messageMap) {
				final List<PhonTalkMessage> messages = 
						messageMap.get(parent.toString());
				retVal = messages.size();
			}
		}
		return retVal;
	}

	@Override
	public int getIndexOfChild(Object parent, Object child) {
		int retVal = -1;
		
		if(parent == this) {
			synchronized (fileList) {				
				retVal = fileList.indexOf(child);
			}
		} else if(parent instanceof String) {
			synchronized(messageMap) {
				final List<PhonTalkMessage> messages = 
						messageMap.get(parent.toString());
				retVal = messages.indexOf(child);
			}
		}
		
		return retVal;
	}

	@Override
	public Object getRoot() {
		// we are our own root
		return this;
	}

	@Override
	public boolean isLeaf(Object node) {
		boolean retVal = false;
		
		synchronized (fileList) {
			retVal = fileList.size() == 0;
			if(fileList.size() > 0) {
				retVal = node instanceof PhonTalkMessage;
			}
		}
		
		return retVal;
	}

	@Override
	public void removeTreeModelListener(TreeModelListener l) {
		listeners.remove(l);
	}

	@Override
	public void valueForPathChanged(TreePath path, Object newValue) {
		// do nothing - don't allow for edits
	}
	
}
