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

import java.awt.Window;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import ca.phon.phontalk.plugin.action.Phon2TalkbankAction;
import ca.phon.phontalk.plugin.action.Talkbank2PhonAction;
import ca.phon.system.plugin.IPluginExtensionFactory;
import ca.phon.system.plugin.IPluginExtensionPoint;
import ca.phon.system.plugin.IPluginMenuFilter;
import ca.phon.system.plugin.PhonPlugin;

/**
 * Plug-in menu extension factory for PhonTalk menu entries.
 */
@PhonPlugin(
		author="Greg J. Hedlund <ghedlund@mun.ca>",
		name="PhonTalk",
		version="1.0",
		comments="PhonTalk plug-in",
		minPhonVersion="1.5.3")
public class PhonTalkMenuExtPt implements IPluginExtensionPoint<IPluginMenuFilter>, IPluginExtensionFactory<IPluginMenuFilter>, IPluginMenuFilter {

	@Override
	public Class<?> getExtensionType() {
		return IPluginMenuFilter.class;
	}

	@Override
	public IPluginExtensionFactory<IPluginMenuFilter> getFactory() {
		return this;
	}

	@Override
	public IPluginMenuFilter createObject(Object... arg0) {
		return this;
	}
	
	@Override
	public void filterWindowMenu(Window owner, JMenuBar menubar) {
		JMenu pluginsMenu = null;
		for(int i = 0; i < menubar.getMenuCount(); i++) {
			final JMenu menu = menubar.getMenu(i);
			if(menu.getText().equals("Plugins")) {
				pluginsMenu = menu;
				break;
			}
		}
		
		if(pluginsMenu != null) {
			if(pluginsMenu.getItemCount() > 0) {
				pluginsMenu.addSeparator();
			}
			
			final JMenuItem titleItem = new JMenuItem("-- PhonTalk 1.4 --");
			titleItem.setEnabled(false);
			
			final Action tbAct = new Phon2TalkbankAction();
			final JMenuItem tbItem = new JMenuItem(tbAct);
			
			final Action phoAct = new Talkbank2PhonAction();
			final JMenuItem phoItem = new JMenuItem(phoAct);
			
			pluginsMenu.add(titleItem);
			pluginsMenu.add(tbItem);
			pluginsMenu.add(phoItem);
		}
	}
	
}
