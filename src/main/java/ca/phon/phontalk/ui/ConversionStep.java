package ca.phon.phontalk.ui;

import java.awt.BorderLayout;

import ca.phon.gui.DialogHeader;
import ca.phon.gui.components.PhonLoggerConsole;
import ca.phon.gui.wizard.WizardStep;

/**
 * Convert the project and displays a console for the
 * converter's output.
 *
 */
public class ConversionStep extends WizardStep {
	
	/* UI */
	private PhonLoggerConsole console;
	
	private PhonTalkWizardModel model;
	
	public ConversionStep(PhonTalkWizardModel model) {
		this.model = model;
		
		init();
	}
	
	private void init() {
		console = new PhonLoggerConsole();
		
		setLayout(new BorderLayout());
		add(console, BorderLayout.CENTER);
		add(new DialogHeader("Convert", "Data is being processed.  This may take some time."), BorderLayout.NORTH);
	}

	public PhonLoggerConsole getConsole() {
		return console;
	}
}
