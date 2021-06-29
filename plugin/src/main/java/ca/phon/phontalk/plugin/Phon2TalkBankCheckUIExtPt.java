package ca.phon.phontalk.plugin;

import ca.phon.app.session.check.*;
import ca.phon.plugin.*;

@SessionCheckTarget(Phon2TalkBankRecordCheck.class)
public class Phon2TalkBankCheckUIExtPt implements
		IPluginExtensionPoint<SessionCheckUI>, IPluginExtensionFactory<SessionCheckUI> {

	@Override
	public SessionCheckUI createObject(Object... args) {
		if(args.length != 1
				|| args[0].getClass() != Phon2TalkBankRecordCheck.class) {
			throw new IllegalArgumentException();
		}
		return new Phon2TalkBankCheckUI((Phon2TalkBankRecordCheck)args[0]);
	}

	@Override
	public Class<?> getExtensionType() {
		return SessionCheckUI.class;
	}

	@Override
	public IPluginExtensionFactory<SessionCheckUI> getFactory() {
		return this;
	}

}
