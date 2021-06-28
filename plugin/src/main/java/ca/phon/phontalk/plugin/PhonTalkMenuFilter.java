package ca.phon.phontalk.plugin;

import java.awt.Window;

import javax.swing.JMenuBar;

import ca.phon.plugin.IPluginExtensionFactory;
import ca.phon.plugin.IPluginExtensionPoint;
import ca.phon.plugin.IPluginMenuFilter;
import ca.phon.plugin.PluginAction;
import ca.phon.ui.menu.MenuBuilder;

public class PhonTalkMenuFilter implements IPluginExtensionPoint<IPluginMenuFilter>, IPluginExtensionFactory<IPluginMenuFilter>, IPluginMenuFilter {

	@Override
	public void filterWindowMenu(Window owner, JMenuBar menu) {
		final MenuBuilder builder = new MenuBuilder(menu);
		
		builder.addSeparator("File@Recent Projects/", "PhonTalk");
		PluginAction exportAct = new PluginAction(ExportEntryPt.EP_NAME);
		exportAct.putValue(PluginAction.NAME, ExportWizard.DIALOG_TITLE);
		exportAct.putValue(PluginAction.SHORT_DESCRIPTION, ExportWizard.DIALOG_MESAGE);
		builder.addItem("File@PhonTalk", exportAct);
		
		PluginAction importAct = new PluginAction(ImportEntryPt.EP_NAME);
		importAct.putValue(PluginAction.NAME, ImportWizard.DIALOG_TITLE);
		importAct.putValue(PluginAction.SHORT_DESCRIPTION, ImportWizard.DIALOG_MESAGE);
		builder.addItem("File@PhonTalk", importAct);
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
