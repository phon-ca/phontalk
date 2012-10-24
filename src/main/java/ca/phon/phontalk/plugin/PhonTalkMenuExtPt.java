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
		comments="PhonTalk plugin menu entries")
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
			
			final Action tbAct = new Phon2TalkbankAction();
			final JMenuItem tbItem = new JMenuItem(tbAct);
			
			final Action phoAct = new Talkbank2PhonAction();
			final JMenuItem phoItem = new JMenuItem(phoAct);
			
			pluginsMenu.add(tbItem);
			pluginsMenu.add(phoItem);
		}
	}
	
}
