package ca.phon.phontalk.app;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import org.jdesktop.swingx.VerticalLayout;

import ca.phon.phontalk.PhonTalkSettings;
import ca.phon.syllabifier.*;
import ca.phon.util.Language;

/**
 * Settings UI for phontalk
 *
 */
public class PhonTalkSettingPanel extends JPanel {
	
	private final PhonTalkSettings settings;
	
	private JPanel xml2PhonOptionsPanel;
	
	private JPanel phon2XmlOptionsPanel;
	
	// xml2Phon UI
	private JCheckBox syllabifyAndAlignBox;
	
	private JComboBox<Syllabifier> syllabifierBox;
	
	// phon2Xml UI
	private JCheckBox includeAlignmentTier;
	
	public PhonTalkSettingPanel() {
		this(new PhonTalkSettings());
	}
	
	public PhonTalkSettingPanel(PhonTalkSettings settings) {
		super();
		this.settings = settings;
		
		init();
	}
	
	private void init() {
		setLayout(new VerticalLayout());
		setupXml2PhonPanel();
		xml2PhonOptionsPanel.setBorder(BorderFactory.createTitledBorder("XML -> Phon Options"));
		add(xml2PhonOptionsPanel);

//		setupPhon2XmlPanel();
//		phon2XmlOptionsPanel.setBorder(BorderFactory.createTitledBorder("Phon -> XML Options"));
//		add(phon2XmlOptionsPanel);
	}
	
	public PhonTalkSettings getSettings() {
		return this.settings;
	}

	private void setupXml2PhonPanel() {
		xml2PhonOptionsPanel = new JPanel(new GridBagLayout());
		
		syllabifyAndAlignBox = new JCheckBox("Syllabify and align IPA transcriptions");
		syllabifyAndAlignBox.setSelected(getSettings().isSyllabifyAndAlign());
		syllabifyAndAlignBox.addActionListener( (e) -> {
			getSettings().setSyllabifyAndAlign(syllabifyAndAlignBox.isSelected());
			syllabifierBox.setEnabled(syllabifyAndAlignBox.isSelected());
		});
		
		final SyllabifierLibrary syllabifierLibrary = SyllabifierLibrary.getInstance();
		Syllabifier defSyllabifier = null;
		final Iterator<Syllabifier> syllabifiers = syllabifierLibrary.availableSyllabifiers();
		List<Syllabifier> sortedSyllabifiers = new ArrayList<Syllabifier>();
		while(syllabifiers.hasNext()) {
			final Syllabifier syllabifier = syllabifiers.next();
			if(syllabifier.getLanguage().equals(Language.parseLanguage(getSettings().getSyllabifer())))
				defSyllabifier = syllabifier;
			sortedSyllabifiers.add(syllabifier);
		}
		Collections.sort(sortedSyllabifiers, new SyllabifierComparator());
		
		syllabifierBox = new JComboBox<>(sortedSyllabifiers.toArray(new Syllabifier[0]));
		syllabifierBox.setRenderer(new SyllabifierCellRenderer());
		if(defSyllabifier != null)
			syllabifierBox.setSelectedItem(defSyllabifier);
		syllabifierBox.addItemListener(new SyllabifierLanguageListener());
		syllabifierBox.setEnabled(syllabifyAndAlignBox.isSelected());
		
		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridheight = 1;
		gbc.gridwidth = 2;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.gridx = 0;
		gbc.gridy = 0;
		
		xml2PhonOptionsPanel.add(syllabifyAndAlignBox, gbc);
		
		++gbc.gridy;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		xml2PhonOptionsPanel.add(new JLabel("Syllabifier:"), gbc);
		
		++gbc.gridx;
//		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.NONE;
		xml2PhonOptionsPanel.add(syllabifierBox, gbc);
	}
	
	private void setupPhon2XmlPanel() {
		phon2XmlOptionsPanel = new JPanel();
	}
	
	private class SyllabifierComparator implements Comparator<Syllabifier> {

		@Override
		public int compare(Syllabifier o1, Syllabifier o2) {
			return o1.toString().compareTo(o2.toString());
		}

	}
	
	private class SyllabifierCellRenderer extends DefaultListCellRenderer {

		@Override
		public Component getListCellRendererComponent(JList list,
				Object value, int index, boolean isSelected,
				boolean cellHasFocus) {
			final JLabel retVal = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected,
					cellHasFocus);

			if(value != null) {
				final Syllabifier syllabifier = (Syllabifier)value;
				final String text = syllabifier.getName() + " (" + syllabifier.getLanguage().toString() + ")";
				retVal.setText(text);
			}

			return retVal;
		}

	}
	
	private class SyllabifierLanguageListener implements ItemListener {
		@Override
		public void itemStateChanged(ItemEvent e) {
			if(e.getStateChange() != ItemEvent.SELECTED) return;

			Syllabifier syllabifier = (Syllabifier)e.getItem();
			getSettings().setSyllabifier(syllabifier.getLanguage().toString());
		}
	}
}
