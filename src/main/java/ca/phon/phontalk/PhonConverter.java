package ca.phon.phontalk;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.phon.application.transcript.ITranscript;

/**
 * Interface for converters into Phon's format.
 *
 */
public abstract class PhonConverter {
	
	/** List of error handlers */
	private List<ErrorHandler> handlers =
		Collections.synchronizedList(new ArrayList<ErrorHandler>());
	
	/** Properties */
	private Map<String, Object> properties =
		Collections.synchronizedMap(new HashMap<String, Object>());
	
	/**
	 * Convert the given Phon session into an
	 * xml string.
	 * 
	 */
	public abstract String convertTranscript(ITranscript t);
	
	/*
	 * Events 
	 */
	/**
	 * Add a new error handler to the conveter.
	 * 
	 * @param handler
	 */
	public void addErrorHandler(ErrorHandler handler) {
		if(!handlers.contains(handler))
			handlers.add(handler);
	}
	
	/**
	 * Remove a given error handler.
	 * 
	 * @param handler
	 */
	public void removeErrorHandler(ErrorHandler handler) {
		if(handlers.contains(handler))
			handlers.remove(handler);
	}
	
	
	/**
	 * Get error handlers.
	 * 
	 * @return the list of handlers
	 */
	public List<ErrorHandler> getErrorHandlers() {
		return handlers;
	}
	
	protected void fireError(PhonTalkError err) {
		ErrorHandler[] _handlers = handlers.toArray(new ErrorHandler[0]);
		for(ErrorHandler h:_handlers)
			h.converterError(err);
	}
	
	/**
	 * Get properties.
	 * 
	 * 
	 */
	public Map<String, Object> getProperties() {
		return properties;
	}
	
	/**
	 * Set the value of a property.
	 * 
	 * @param propName
	 * @param propValue
	 */
	public void setProperty(String propName, Object value) {
		properties.put(propName, value);
	}
	
	/**
	 * Get the value of a property.
	 * 
	 * @param propName
	 * @return 
	 */
	public Object getProperty(String propName) {
		return properties.get(propName);
	}
	
}
