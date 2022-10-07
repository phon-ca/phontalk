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
import jakarta.xml.bind.ValidationException;

import java.io.*;
import java.util.Properties;

@PhonPlugin(name="Phon -> TalkBank Check", comments="Check if each record in a session can be exported to TalkBank individually")
@Rank(200)
public class Phon2TalkBankRecordCheck implements SessionCheck, IPluginExtensionPoint<SessionCheck> {

	/**
	 * Check export to CHAT using Chatter as well?
	 */
	private boolean checkExportToCHAT = false;

	@Override
	public Class<?> getExtensionType() {
		return SessionCheck.class;
	}

	@Override
	public IPluginExtensionFactory<SessionCheck> getFactory() {
		return (args) -> this;
	}

	public boolean isCheckExportToCHAT() {
		return checkExportToCHAT;
	}

	public void setCheckExportToCHAT(boolean checkExportToCHAT) {
		this.checkExportToCHAT = checkExportToCHAT;
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
			final PhonTalkListener validationListener = msg -> {
				ValidationEvent ve = new ValidationEvent(ValidationEvent.Severity.ERROR, session,
						"(Phon -> XML): " + msg.getMessage());
				ve.setRecord(recordIndex);

				validator.fireValidationEvent(ve);
			};

			final TalkBankSessionWriter tbWriter = new TalkBankSessionWriter(validationListener);
			tbWriter.writeSession(testSession, new FileOutputStream(tempXmlFile));

			final TalkbankValidator xmlValidator = new TalkbankValidator();
			final DefaultErrorHandler errHandler = new DefaultErrorHandler(tempXmlFile, validationListener);
			if(xmlValidator.validate(tempXmlFile, errHandler)) {
				if (isCheckExportToCHAT()) {
					final File tempChaFile = File.createTempFile("chatter", ".cha");
					tempChaFile.deleteOnExit();

					Xml2CHATConverter chatConverter = new Xml2CHATConverter();
					chatConverter.convertFile(tempXmlFile, tempChaFile, new PhonTalkListener() {
						@Override
						public void message(PhonTalkMessage msg) {
							ValidationEvent ve = new ValidationEvent(ValidationEvent.Severity.ERROR, session,
									"(XML -> CHAT): " + msg.getMessage());
							ve.setRecord(recordIndex);

							validator.fireValidationEvent(ve);
						}
					});
					if (!tempChaFile.exists() || tempChaFile.length() == 0) {
						// no data written
						ValidationEvent ve = new ValidationEvent(ValidationEvent.Severity.ERROR, session,
								"(XML -> CHAT): No data in CHAT output");
						ve.setRecord(recordIndex);

						validator.fireValidationEvent(ve);
					}
				}
			}
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
