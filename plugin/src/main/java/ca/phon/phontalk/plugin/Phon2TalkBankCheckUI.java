package ca.phon.phontalk.plugin;

import ca.phon.app.session.check.*;
import ca.phon.plugin.*;
import ca.phon.session.check.CheckAlignment;
import org.jdesktop.swingx.VerticalLayout;

import javax.swing.*;
import java.awt.*;

public class Phon2TalkBankCheckUI extends JPanel implements SessionCheckUI {

	private JCheckBox checkCHATConversionBox;

	private Phon2TalkBankRecordCheck check;

	public Phon2TalkBankCheckUI(Phon2TalkBankRecordCheck check) {
		super();

		this.check = check;

		init();
	}

	private void init() {
		setLayout(new VerticalLayout());

		checkCHATConversionBox = new JCheckBox("Also check conversion to CHAT (.cha) - may take a long time");
		checkCHATConversionBox.setSelected(check.isCheckExportToCHAT());
		checkCHATConversionBox.addActionListener((e) -> {
			check.setCheckExportToCHAT(checkCHATConversionBox.isSelected());
		});
		add(checkCHATConversionBox);
	}

	@Override
	public Component getComponent() {
		return this;
	}

}
