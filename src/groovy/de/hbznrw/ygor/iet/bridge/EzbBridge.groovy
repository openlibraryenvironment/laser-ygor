package de.hbznrw.ygor.iet.bridge

import groovy.util.logging.Log4j

import java.util.ArrayList;
import java.util.HashMap
import java.util.LinkedHashMap

import org.apache.commons.csv.CSVRecord;

import de.hbznrw.ygor.iet.Envelope
import de.hbznrw.ygor.iet.connector.*
import de.hbznrw.ygor.iet.enums.Query;
import de.hbznrw.ygor.iet.formatadapter.*
import de.hbznrw.ygor.iet.interfaces.*
import de.hbznrw.ygor.iet.processor.CsvProcessor
import de.hbznrw.ygor.tools.FileToolkit

@Log4j
class EzbBridge extends BridgeAbstract implements BridgeInterface {
	
    static final IDENTIFIER = 'ezb'
    
	Query[] tasks = [
        Query.EZBID
    ]
    
	private HashMap options
	
	EzbBridge(Thread master, HashMap options) {
        this.master    = master
		this.options   = options
		this.connector = new EzbConnector(this)
		this.processor = master.processor
	}
	
	@Override
	void go() throws Exception {
		log.info("Input:  " + options.get('inputFile'))
        
        master.enrichment.dataContainer.info.api << connector.getAPIQuery('<zdbid>')
        
        processor.setBridge(this)
        processor.setConfiguration(",", null, null)
        processor.processFile(options)
	}
	
	@Override
	void go(String outputFile) throws Exception {
		log.warn("deprecated function call go(outputFile)")
	}
    
    @Override
    void processStash() throws Exception {
        log.info("processStash()")
        
        def stash = processor.getStash()
        
        stash[ZdbBridge.IDENTIFIER].each{ key, value ->
            
            if(!master.isRunning) {
                log.info('Aborted by user action.')
                return
            }
            
            increaseProgress()
            connector.poll(key)
            
            processor.processEntry(master.enrichment.dataContainer, value)
        }
    }
}
