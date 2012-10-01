/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.phon.phontalk.macos;

import ca.phon.phontalk.ui.AboutDialog;

import com.apple.eawt.Application;
import com.apple.eawt.ApplicationAdapter;
import com.apple.eawt.ApplicationEvent;

/**
 *
 * @author ghedlund
 */
public class MacOSAppHelper extends Application {

	public MacOSAppHelper() {
		super();

		this.addApplicationListener(new HelpListener());
		this.addApplicationListener(new QuitListener());
	}

	/** The About Listener */
	private class HelpListener extends ApplicationAdapter {
		@Override
		public void 	handleAbout(ApplicationEvent event) {
			// run the about module
			AboutDialog aboutDialog = new AboutDialog();
			aboutDialog.pack();
			aboutDialog.setVisible(true);

			event.setHandled(true);
		}
	}

	/** Quit listener */
	private class QuitListener extends ApplicationAdapter {
		@Override
		public void handleQuit(ApplicationEvent event) {
			System.exit(0);
		}
	}

}
