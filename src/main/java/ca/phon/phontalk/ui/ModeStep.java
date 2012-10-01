package ca.phon.phontalk.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import ca.phon.gui.DialogHeader;
import ca.phon.gui.wizard.WizardStep;

/**
 * Select conversion mode.
 *
 */
public class ModeStep extends WizardStep {

	private PhonTalkWizardModel model;
	
	public ModeStep(PhonTalkWizardModel model) {
		super();
		
		this.model = model;
		init();
	}
	
	private void init() {
		setLayout(new BorderLayout());
	
		JPanel centerPanel = new JPanel();
		BoxLayout boxLayout = new BoxLayout(centerPanel, BoxLayout.PAGE_AXIS);
		centerPanel.setLayout(boxLayout);
		
		// create radio group
		ButtonGroup btngroup = new ButtonGroup();
		
		for(PhonTalkWizardModel.ConversionMode mode:PhonTalkWizardModel.ConversionMode.values()) {
			JRadioButton btn = new JRadioButton();
			btn.setText(mode.toString());
			btn.addActionListener(new ConversionModeAction(mode));
			btngroup.add(btn);
			if(mode == model.get(PhonTalkWizardModel.CONVERSION_MODE))
				btn.setSelected(true);
			centerPanel.add(btn);
			centerPanel.add(Box.createRigidArea(new Dimension(0, 10)));
		}
		
		add(centerPanel, BorderLayout.CENTER);
		add(new DialogHeader("Select mode", "Select conversion mode"), BorderLayout.NORTH);
	}
	
	private class ConversionModeAction implements ActionListener {

		PhonTalkWizardModel.ConversionMode mode;
		
		public ConversionModeAction(PhonTalkWizardModel.ConversionMode mode) {
			this.mode = mode;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			JRadioButton btn = (JRadioButton)e.getSource();
			if(btn.isSelected()) {
				model.put(PhonTalkWizardModel.CONVERSION_MODE, mode);
			}
		}
		
	}
}
