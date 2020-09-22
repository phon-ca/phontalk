package ca.phon.phontalk.plugin;

import java.util.Map;

import ca.phon.plugin.IPluginEntryPoint;

public class ImportProjectEntryPt implements IPluginEntryPoint {

	public final static String EP_NAME = "PhonTalk_ImportProject";
	
	@Override
	public String getName() {
		return EP_NAME;
	}

	@Override
	public void pluginStart(Map<String, Object> args) {
		ImportProjectWizard wizard = new ImportProjectWizard();
		wizard.pack();
		wizard.setSize(800, 600);
		wizard.centerWindow();
		
		wizard.setVisible(true);
	}

}
