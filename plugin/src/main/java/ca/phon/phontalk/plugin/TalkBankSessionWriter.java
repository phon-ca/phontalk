package ca.phon.phontalk.plugin;

import java.io.*;

import ca.phon.app.log.LogUtil;
import ca.phon.phontalk.*;
import ca.phon.session.Session;
import ca.phon.session.io.*;

@SessionIO(
		group="org.talkbank",
		id="talkbank",
		version="2.5.0",
		mimetype="application/xml",
		extension="xml",
		name="TalkBank"
)
public class TalkBankSessionWriter implements SessionWriter {

	@Override
	public void writeSession(Session session, OutputStream out)
			throws IOException {
		final Phon2XmlConverter converter = new Phon2XmlConverter();
		
		final PhonTalkListener listener = (PhonTalkMessage msg) -> {
			LogUtil.info(msg.toString());
		};
		
		converter.sessionToStream(session, out, listener);
	}

}
