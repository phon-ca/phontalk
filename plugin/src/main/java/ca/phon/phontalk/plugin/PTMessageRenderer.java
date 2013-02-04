/*
 * Phon - An open source tool for research in phonology.
 * Copyright (C) 2008 The Phon Project, Memorial University <http://phon.ling.mun.ca>
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

import java.awt.Component;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import ca.phon.phontalk.PhonTalkMessage;
import ca.phon.util.PathExpander;
import ca.phon.util.iconManager.IconManager;
import ca.phon.util.iconManager.IconSize;

public class PTMessageRenderer extends DefaultTreeCellRenderer {

	private ImageIcon warningIcon;
	private ImageIcon errIcon;
	private ImageIcon fileIcon;

	public PTMessageRenderer() {
		super();
		initIcons();
	}
	
	private void initIcons() {
		final IconManager im = IconManager.getInstance();
		
		warningIcon = im.getIcon("statis/dialog-warning", IconSize.SMALL);
		errIcon = im.getIcon("status/dialog-error", IconSize.SMALL);
		fileIcon = im.getIcon("mimetypes/txt", IconSize.SMALL);
	}
	
	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value,
			boolean sel, boolean expanded, boolean leaf, int row,
			boolean hasFocus) {
		JLabel retVal = (JLabel)super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf,
				row, hasFocus);
		
		
		if(value instanceof PhonTalkMessage) {
			final PhonTalkMessage msg = PhonTalkMessage.class.cast(value);
			
			String text = msg.getMessage();
			if(msg.getColNumber() >= 0) {
				text = "col: " + msg.getColNumber() + " " + text;
			}
			if(msg.getLineNumber() >= 0) {
				text = "line: " + msg.getLineNumber() + " " + text;
			}
			retVal.setText(msg.getMessage());
			
			switch(msg.getSeverity()) {
			case INFO:
				break;
				
			case WARNING:
				retVal.setIcon(warningIcon);
				break;
				
			case SEVERE:
				retVal.setIcon(errIcon);
				break;
				
			default:
				break;
			}
		} else {

			final PathExpander pe = new PathExpander();
			retVal.setIcon(fileIcon);
			retVal.setText(pe.compressPath(retVal.getText()));
		}
		
		return retVal;
	}

	
	
}
