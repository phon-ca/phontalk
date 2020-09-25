package ca.phon.phontalk.plugin;

import java.awt.Window;

import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;

import ca.phon.app.modules.EntryPointArgs;
import ca.phon.app.session.editor.SessionEditor;
import ca.phon.plugin.IPluginExtensionFactory;
import ca.phon.plugin.IPluginExtensionPoint;
import ca.phon.plugin.IPluginMenuFilter;
import ca.phon.plugin.PluginAction;
import ca.phon.project.Project;
import ca.phon.ui.CommonModuleFrame;
import ca.phon.ui.menu.MenuBuilder;

public class PhonTalkMenuFilter implements IPluginExtensionPoint<IPluginMenuFilter>, IPluginExtensionFactory<IPluginMenuFilter>, IPluginMenuFilter {

	@Override
	public void filterWindowMenu(Window owner, JMenuBar menu) {
		final MenuBuilder builder = new MenuBuilder(menu);
		
		builder.addSeparator("File@Recent Projects/", "PhonTalk");
		PluginAction exportAct = new PluginAction(ExportProjectEntryPt.EP_NAME);
		exportAct.putValue(PluginAction.NAME, ExportProjectWizard.DIALOG_TITLE);
		exportAct.putValue(PluginAction.SHORT_DESCRIPTION, ExportProjectWizard.DIALOG_MESAGE);
		builder.addItem("File@PhonTalk", exportAct);
		
		PluginAction importAct = new PluginAction(ImportProjectEntryPt.EP_NAME);
		importAct.putValue(PluginAction.NAME, ImportProjectWizard.DIALOG_TITLE);
		importAct.putValue(PluginAction.SHORT_DESCRIPTION, ImportProjectWizard.DIALOG_MESAGE);
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
