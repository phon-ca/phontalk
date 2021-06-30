package ca.phon.phontalk.plugin;

import java.awt.*;
import java.io.*;

import ca.phon.app.log.LogUtil;
import ca.phon.phontalk.*;
import ca.phon.plugin.IPluginExtensionFactory;
import ca.phon.plugin.IPluginExtensionPoint;
import ca.phon.session.Session;
import ca.phon.session.io.*;

@SessionIO(
		group="org.talkbank",
		id="talkbank",
		version="2.5.0",
		mimetype="application/xml",
		extension="xml",
		name="TalkBank (.xml)"
)
public class TalkBankSessionWriter implements SessionWriter, IPluginExtensionPoint<SessionWriter> {

	private PhonTalkListener listener;

	final PhonTalkListener defaultListener = (PhonTalkMessage msg) -> {
		LogUtil.warning(msg.toString());
	};

	public TalkBankSessionWriter() {
		this(null);
	}

	public TalkBankSessionWriter(PhonTalkListener listener) {
		this.listener = listener;
	}

	public PhonTalkListener getListener() {
		return (this.listener == null ? defaultListener : this.listener);
	}

	@Override
	public void writeSession(Session session, OutputStream out)
			throws IOException {
		final Phon2XmlConverter converter = new Phon2XmlConverter();
		converter.sessionToStream(session, out, getListener());
	}

	@Override
	public Class<?> getExtensionType() {
		return SessionWriter.class;
	}

	@Override
	public IPluginExtensionFactory<SessionWriter> getFactory() {
		return (args) -> { return new TalkBankSessionWriter(null); };
	}

}
