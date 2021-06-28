package ca.phon.phontalk.plugin;

import java.util.Map;

import ca.phon.plugin.IPluginEntryPoint;
import ca.phon.project.Project;
import ca.phon.ui.CommonModuleFrame;

public class ExportEntryPt implements IPluginEntryPoint {

	public final static String EP_NAME = "PhonTalk_ExportProject";
	
	@Override
	public String getName() {
		return EP_NAME;
	}

	@Override
	public void pluginStart(Map<String, Object> args) {
		CommonModuleFrame cmf = CommonModuleFrame.getCurrentFrame();
		Project p = (cmf == null ? null : cmf.getExtension(Project.class));
		
		ExportWizard wizard = new ExportWizard(p);
		wizard.pack();
		wizard.setSize(800, 600);
		wizard.centerWindow();

		wizard.setVisible(true);
	}

}
