package ca.phon.phontalk.app;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import ca.phon.phontalk.plugin.PhonTalkFrame;
import ca.phon.util.OSInfo;

public class PhonTalkApp {

	public static void main(String[] args) {
		SwingUtilities.invokeLater( () -> {
			if(!OSInfo.isMacOs()) {
				try {
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			final PhonTalkFrame frame = new PhonTalkFrame();
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setSize(1024, 768);
			frame.setVisible(true);
		});
	}
	
}
