package ca.phon.phontalk.plugin.wizard;

import ca.phon.application.project.IPhonProject;
import ca.phon.gui.wizard.WizardFrame;

public class Talkbank2PhonWizard extends WizardFrame {
	
	private static final long serialVersionUID = -8143097674166717906L;
	
	/**
	 * constructor
	 */
	public Talkbank2PhonWizard(IPhonProject project) {
		super("PhonTalk : Import from Talkbank XML");
		super.setProject(project);
	}
}
