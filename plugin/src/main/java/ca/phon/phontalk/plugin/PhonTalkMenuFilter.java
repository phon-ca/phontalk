package ca.phon.phontalk.plugin;

import java.awt.Window;

import javax.swing.JMenuBar;

import ca.phon.app.session.editor.SessionEditor;
import ca.phon.plugin.IPluginExtensionFactory;
import ca.phon.plugin.IPluginExtensionPoint;
import ca.phon.plugin.IPluginMenuFilter;
import ca.phon.ui.menu.MenuBuilder;

public class PhonTalkMenuFilter implements IPluginExtensionPoint<IPluginMenuFilter>, IPluginExtensionFactory<IPluginMenuFilter>, IPluginMenuFilter {

	@Override
	public void filterWindowMenu(Window owner, JMenuBar menu) {
		if(!(owner instanceof SessionEditor)) {
			return;
		}
		
		final MenuBuilder builder = new MenuBuilder(menu);
		builder.addMenuItem("Tools", new PhonTalkInfoAction((SessionEditor)owner));
	}

	@Override
	public IPluginMenuFilter createObject(Object... args) {
		return this;
	}

	@Override
	public Class<?> getExtensionType() {
		return IPluginMenuFilter.class;
	}

	@Override
	public IPluginExtensionFactory<IPluginMenuFilter> getFactory() {
		return (Object...args) -> { return PhonTalkMenuFilter.this; };
	}

}
