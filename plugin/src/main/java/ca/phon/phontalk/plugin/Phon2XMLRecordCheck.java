package ca.phon.phontalk.plugin;

import ca.phon.app.log.LogUtil;
import ca.phon.phontalk.*;
import ca.phon.phontalk.parser.*;
import ca.phon.plugin.IPluginExtensionFactory;
import ca.phon.plugin.IPluginExtensionPoint;
import ca.phon.plugin.PhonPlugin;
import ca.phon.plugin.Rank;
import ca.phon.session.Participant;
import ca.phon.session.Record;
import ca.phon.session.Session;
import ca.phon.session.SessionFactory;
import ca.phon.session.check.SessionCheck;
import ca.phon.session.check.SessionValidator;
import ca.phon.session.check.ValidationEvent;
import ca.phon.worker.PhonTask;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.apache.logging.log4j.Level;

import javax.xml.bind.ValidationException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

@PhonPlugin(name="Phon -> TalkBank XML Check", comments="Check if each session can be exported to TalkBank XML individually")
@Rank(200)
public class Phon2XMLRecordCheck implements SessionCheck, IPluginExtensionPoint<SessionCheck> {

	@Override
	public Class<?> getExtensionType() {
		return SessionCheck.class;
	}

	@Override
	public IPluginExtensionFactory<SessionCheck> getFactory() {
		return (args) -> this;
	}

	@Override
	public boolean performCheckByDefault() {
		return false;
	}

	@Override
	public boolean checkSession(SessionValidator validator, Session session) {
		for(int i = 0; i < session.getRecordCount(); i++) {
			checkRecord(validator, session, i);
		}

		// don't modify anything
		return false;
	}

	/**
	 * Check Record
	 * Perform a round-trip of the data with only this
	 * single record. Any errors encountered will be
	 * reported to the validator.
	 *
	 * @param validator
	 * @param session
	 * @param recordIndex
	 * @return true if record was modified
	 */
	public boolean checkRecord(SessionValidator validator, Session session, int recordIndex) {
		Record r = session.getRecord(recordIndex);

		SessionFactory factory = SessionFactory.newFactory();

		Session testSession = factory.createSession();
		factory.copySessionInformation(session, testSession);
		factory.copySessionMetadata(session, testSession);
		factory.copySessionTierInformation(session, testSession);
		for(Participant p:session.getParticipants()) {
			testSession.addParticipant(factory.cloneParticipant(p));
		}
		testSession.addRecord(r);

		try {
			final File tempXmlFile = File.createTempFile("phontalk", ".xml");
			tempXmlFile.deleteOnExit();
			// convert session to temporary xml file
			final TalkBankSessionWriter tbWriter = new TalkBankSessionWriter();
			tbWriter.writeSession(testSession, new FileOutputStream(tempXmlFile));

			final TalkbankValidator xmlValidator = new TalkbankValidator();
			final DefaultErrorHandler errHandler = new DefaultErrorHandler(tempXmlFile, msg -> {
				ValidationEvent ve = new ValidationEvent(ValidationEvent.Severity.ERROR, session,
						"(Phon -> XML): " + msg.getMessage());
				ve.setRecord(recordIndex);

				validator.fireValidationEvent(ve);
			});
			xmlValidator.validate(tempXmlFile, errHandler);
		} catch (IOException | ValidationException e) {
			ValidationEvent ve = new ValidationEvent(ValidationEvent.Severity.ERROR, session,
					"(Phon -> XML): " + e.getMessage());
			ve.setRecord(recordIndex);
			validator.fireValidationEvent(ve);
		}

		return false;
	}

	@Override
	public Properties getProperties() {
		return new Properties();
	}

	@Override
	public void loadProperties(Properties props) {

	}

}
