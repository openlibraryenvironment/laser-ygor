package de.hbznrw.ygor.iet;

import de.hbznrw.ygor.iet.bridge.*
import de.hbznrw.ygor.iet.interfaces.BridgeInterface
import de.hbznrw.ygor.iet.interfaces.ProcessorInterface
import de.hbznrw.ygor.iet.processor.CsvProcessor
import ygor.Enrichment
import java.nio.file.Files
import java.nio.file.Paths

class MultipleProcessingThread extends Thread {

    public  ProcessorInterface processor
    private BridgeInterface bridge
    
    public isRunning = true
    
	private enrichment
	private indexOfKey
    private typeOfKey
	private options

    private int progressTotal   = 0
    private int progressCurrent = 0
    
	MultipleProcessingThread(Enrichment en, HashMap options) {
		this.enrichment = en
        
        this.processor = new CsvProcessor()
		this.indexOfKey = options.get('indexOfKey')
        this.typeOfKey  = options.get('typeOfKey')
		this.options    = options.get('options')
	}
	
	public void run() {
		if(null == enrichment.originPathName)
			System.exit(0)
	
		if(null == indexOfKey)
			System.exit(0)
		
		enrichment.setStatus(Enrichment.ProcessingState.WORKING)
		
		println('Starting ..')
        
		try {  
            // TODO fix calculation
            LineNumberReader lnr = new LineNumberReader(
                new FileReader(new File(enrichment.originPathName))
                );
            lnr.skip(Long.MAX_VALUE);
            progressTotal = lnr.getLineNumber() * options.size()
            lnr.close();

            options.each{
                option ->
                    switch(option) {
                        case GbvBridge.IDENTIFIER:
                            bridge = new GbvBridge(this, new HashMap(
                                inputFile:  enrichment.originPathName,
                                indexOfKey: indexOfKey,
                                typeOfKey:  typeOfKey
                                )
                            )
                            break
                        case EzbBridge.IDENTIFIER:
                            bridge = new EzbBridge(this, new HashMap(
                                inputFile:  enrichment.originPathName, 
                                indexOfKey: indexOfKey, 
                                typeOfKey:  typeOfKey
                                )
                            )
                            break
                        case ZdbBridge.IDENTIFIER:
                            bridge = new ZdbBridge(this, new HashMap(
                                inputFile:  enrichment.originPathName,
                                indexOfKey: indexOfKey,
                                typeOfKey:  typeOfKey
                                )
                            )
                            break
                    }
                  
                    if(bridge)
                        bridge.go()
            }
           								
		} catch(Exception e) {
			enrichment.setStatusByCallback(Enrichment.ProcessingState.ERROR)
			
			println(e.getMessage())
			println(e.getStackTrace())
			
			println('Aborted.')
			return
		}
		println('Done.')
		
		enrichment.setStatusByCallback(Enrichment.ProcessingState.FINISHED)
	}
    
    void increaseProgress() {
        progressCurrent++
        enrichment.setProgress((progressCurrent / progressTotal) * 100)
    }
}
