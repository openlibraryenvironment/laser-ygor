package de.hbznrw.ygor.iet.interfaces

import java.util.ArrayList

import com.sun.org.apache.xpath.internal.operations.Minus

import de.hbznrw.ygor.iet.Envelope
import de.hbznrw.ygor.iet.enums.Query
import de.hbznrw.ygor.iet.enums.Status

/**
 * Abstract class for defining API endpoints
 * and defining format specific queries
 *
 * @author David Klober
 *
 */
abstract class ConnectorAbstract implements ConnectorInterface {

	protected String requestUrl      = "set-in-extending-class"
	protected String requestHeader
	protected String queryIdentifier = "set-in-extending-class"
		
    static String formatIdentifier   = 'set-in-extending-class'
 
	protected BridgeInterface bridge
	
	//
	
	ConnectorAbstract(BridgeInterface bridge) {
		this.bridge = bridge
	}
	
    String getAPIQuery(String issn) {
        // TODO
    }
    Envelope poll(String issn) {
        // TODO
    }
    
    Envelope query(Query query) {  
        getEnvelopeWithStatus(Status.UNKNOWN_REQUEST)
    }
    
    Envelope getEnvelopeWithMessage(ArrayList message) {
        def state = Status.RESULT_OK
        
        switch(message.size()) {
            case 0:
                state = Status.RESULT_NO_MATCH
            break;
            case {it > 1}:
                state = Status.RESULT_MULTIPLE_MATCHES
            break;
        }
        new Envelope(state, message)
    }
    
    Envelope getEnvelopeWithComplexMessage(HashMap messages) {
        def states = []
        if(messages.isEmpty())
            states = [Status.UNDEFINED]
        
        // missing values filled with null
        // see: de.hbznrw.ygor.iet.Envelope
            
        for(item in messages) {
            def tmp = item.value.minus(null) // TODO CHECK, or use e.g. Status.EMPTY_SLOT
            switch(tmp.size()) {
                case 0:
                    states << item.key + '_' + Status.RESULT_NO_MATCH
                break;
                case 1:
                    states << item.key + '_' + Status.RESULT_OK
                    break;
                case {it > 1}:
                    states << item.key + '_' + Status.RESULT_MULTIPLE_MATCHES
                break;
            }
        }
        new Envelope(states, messages)
    }
    
    Envelope getEnvelopeWithStatus(Status state) {
        new Envelope(state, [])
    }
}
