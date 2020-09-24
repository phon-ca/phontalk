package ca.phon.phontalk.plugin;

import java.util.Map;

import ca.phon.plugin.IPluginEntryPoint;

public class ExportProjectEntryPt implements IPluginEntryPoint {

	public final static String EP_NAME = "PhonTalk_ExportProject";
	
	@Override
	public String getName() {
		return EP_NAME;
	}

	@Override
	public void pluginStart(Map<String, Object> args) {
		ExportProjectWizard wizard = new ExportProjectWizard();
		wizard.pack();
		wizard.setSize(800, 600);
		wizard.centerWindow();
		
		wizard.setVisible(true);
	}

}
