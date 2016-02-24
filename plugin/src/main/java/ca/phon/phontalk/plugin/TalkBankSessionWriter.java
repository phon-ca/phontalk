package ca.phon.phontalk.plugin;

import java.io.IOException;
import java.io.OutputStream;

import ca.phon.phontalk.Phon2XmlConverter;
import ca.phon.phontalk.PhonTalkListener;
import ca.phon.phontalk.PhonTalkMessage;
import ca.phon.session.Session;
import ca.phon.session.io.SessionIO;
import ca.phon.session.io.SessionWriter;

@SessionIO(
		group="org.talkbank",
		id="talkbank",
		version="2.1.0",
		mimetype="application/xml",
		extension="xml",
		name="TalkBank 2.1.0"
)
public class TalkBankSessionWriter implements SessionWriter {

	@Override
	public void writeSession(Session session, OutputStream out)
			throws IOException {
		final Phon2XmlConverter converter = new Phon2XmlConverter();
		
		final PhonTalkListener listener = (PhonTalkMessage msg) -> {
			// TODO add messages to session warnings
			System.out.println(msg);
		};
		
		converter.sessionToStream(session, out, listener);
	}

}
