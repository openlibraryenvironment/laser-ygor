package de.hbznrw.ygor.iet.connector

import groovy.util.slurpersupport.GPathResult

import java.util.ArrayList

import org.apache.commons.csv.CSVRecord

import de.hbznrw.ygor.iet.Envelope
import de.hbznrw.ygor.iet.enums.*
import de.hbznrw.ygor.iet.interfaces.*
import de.hbznrw.ygor.iet.export.*


/**
 * Controlling API calls using services.dnb.de/sru
 * 
 * @author David Klober
 *
 */
class SruPicaConnector extends ConnectorAbstract {
	
	private String requestUrl       = "http://sru.gbv.de/gvk?version=1.2&operation=searchRetrieve&maximumRecords=10"
	private String queryIdentifier  = 'query=pica.iss%3D'
    
    private String formatIdentifier = 'picaxml'
    private GPathResult response
    private picaRecords             = []
    
	SruPicaConnector(BridgeInterface bridge) {
		super(bridge)
	}
    
    
    // ConnectorInterface
    
    @Override
    String getAPIQuery(String issn) {
        return requestUrl + "&recordSchema=" + formatIdentifier + "&" + queryIdentifier + issn
    }
    
    // TODO fix return value
    
    @Override
    Envelope poll(String issn) {
        try {
            String text = new URL(getAPIQuery(issn)).getText()
            response = new XmlSlurper().parseText(text)
            
            picaRecords = []
            response.records.record.each { r ->
                def test = r.recordData.record.datafield.findAll{it.'@tag' == '016H'}.subfield.findAll{it.'@code' == '0'}
                if("Elektronische Ressource".equals(test?.text()))
                    picaRecords << r
            }
            
        } catch(Exception e) {
            return getEnvelopeWithStatus(Status.STATUS_ERROR)
        }
        
        getEnvelopeWithStatus(Status.STATUS_OK)
    }
    
	@Override
	Envelope query(Query query) {
		try {
            getEnvelope(query)
		} catch(Exception e) {
			return getEnvelopeWithStatus(Status.STATUS_ERROR)
		}
	}
            
    // FormatAdapterInterface

    // <zs:searchRetrieveResponse>
    //   <zs:records>
    //     <zs:record>          <------------------ x times
    //       <zs:recordData>
    //         <record>
    //           <datafield tag="039D">
    //             <subfield code="c">Online-Ausg.</subfield>
    //             <subfield code="a">International journal of accounting and information management</subfield>
    //             <subfield code="C">ZDB</subfield>
    //             <subfield code="6">24063605</subfield>
    //           </datafield>
       
    @Override
    Envelope getEnvelope(Query query) {
        if(response == null)
            return getEnvelopeWithStatus(Status.STATUS_NO_RESPONSE)
            
        switch(query){
            case Query.ZDBID:
                return getFirstResultOnly('006Z', '0')
                break;
            case Query.GBV_GVKPPN:
                return getFirstResultOnly('003@', '0')
                break;
            case Query.GBV_EISSN:
                return getFirstResultOnly('005A', '0')
                break;
            case Query.GBV_PISSN:
                return getFirstResultOnly('005P', '0')
                break;
            case Query.GBV_TITLE:
                return getTitle()
                break;
            case Query.GBV_PUBLISHER:
                return getPublisherAsFatEnvelope()
                break;
            case Query.GBV_PUBLISHED_FROM:
                return getFirstResultOnly('011@', 'a')
                break;
            case Query.GBV_PUBLISHED_TO:
                return getFirstResultOnly('011@', 'b')
                break;
            case Query.GBV_TIPP_URL:
                return getAllTippURL()
                break;
            case Query.GBV_PLATFORM_URL:
                return getAllPlattformURL()
                break;
            case Query.GBV_TIPP_COVERAGE:
                return getTippCoverageAsFatEnvelope()
                break;
        }
        
        getEnvelopeWithStatus(Status.UNKNOWN_REQUEST)
    }
    
    private Envelope getFirstResultOnly(String tag, String code) {
        def result = []
        
        picaRecords.each { record ->
            result << getFirstPicaValue(record.recordData.record, tag, code)
        }
        getEnvelopeWithMessage(result.minus(null).unique())
    }
    
    private String getFirstPicaValue(Object record, String tag, String code) {
        def df = record.datafield.find{it.'@tag' == tag}
        def sf = df.subfield.find{it.'@code' == code}

        println " .. getPicaValue(" +  tag + "" + code + ") = " + sf
        return sf ? sf.text() : null
    }  
    
    private ArrayList getAllPicaValues(Object record, String tag, String code) {
        def result = []
        def sf = record.datafield.findAll{it.'@tag' == tag}.subfield.findAll{it.'@code' == code}
        
        sf.each { f ->
            result << f.text()      
        }
        
        println " .. getPicaValues(" +  tag + "" + code + ") = " + result
        result
    }
    
    private Envelope getTitle() {
        def result = []
        
        // correction
        picaRecords.each { record ->
            result << getFirstPicaValue(record.recordData.record, '025@', 'a')
        }
        // or .. main title
        if(result.minus(null).isEmpty()) {
            picaRecords.each { record ->
                result << getFirstPicaValue(record.recordData.record,'021A', 'a')
            }
        }
        getEnvelopeWithMessage(result.minus(null).unique())
    }
    
    private Envelope getPublisherAsFatEnvelope() {
        def result          = []
        def resultStartDate = []
        def resultEndDate   = []
        def resultName      = []
        def resultStatus    = []
        
        picaRecords.each { record ->
            record.recordData.record.datafield.findAll{it.'@tag' == '033A'}.each { df ->
                def n = df.subfield.find{it.'@code' == 'n'}.text() // TODO or use p here ?
                def h = df.subfield.find{it.'@code' == 'h'}.text()
                
                resultName      << n ? n : null
                resultStartDate << h ? h : null
                resultEndDate   << h ? h : null
                resultStatus    << null
            }
            println " .. getPicaValues(033An) = " + resultName
            println " .. getPicaValues(033Ah) = " + resultStartDate
            
            // TODO refactor this
            
            result << getEnvelopeWithComplexMessage([
                'name':      resultName,
                'startDate': resultStartDate,
                'endDate':   resultEndDate,
                'status':    resultStatus,
            ])
        }
       
        getEnvelopeWithMessage(result)
    } 
    
    private Envelope getAllTippURL() {
        def result = []
        
        picaRecords.each { record ->
            result += getAllPicaValues(record.recordData.record, '009P', 'a') // TODO
        }
        getEnvelopeWithMessage(result.minus(null).unique())
    }
    
    private Envelope getAllPlattformURL() {
        def result = []
        
        picaRecords.each { record ->
            result += getAllPicaValues(record.recordData.record, '009P', '0') // TODO
        }
        getEnvelopeWithMessage(result.minus(null).unique())
    }
    
    private Envelope getTippCoverageAsFatEnvelope() { 
        def result              = []     
        def resultCoverageNote  = []
        def resultEmbargo       = []
        def resultEndDate       = []
        def resultEndIssue      = []
        def resultEndVolume     = []
        def resultStartDate     = []
        def resultStartIssue    = []
        def resultStartVolume   = []
        
        picaRecords.each { record ->
            record.recordData.record.datafield.findAll{it.'@tag' == '009P'}.each { df ->
                def x = df.subfield.find{it.'@code' == 'x'}.text()
                def z = df.subfield.find{it.'@code' == 'z'}.text()

                resultCoverageNote  << z ? z : null
                resultEmbargo       << null
                resultEndDate       << x ? x : null
                resultEndIssue      << null
                resultEndVolume     << x ? x : null
                resultStartDate     << x ? x : null
                resultStartIssue    << null
                resultStartVolume   << x ? x : null
            }
            
            println " .. getPicaValues(009Px) = " + resultStartDate
            println " .. getPicaValues(009Pz) = " + resultCoverageNote
            
            // TODO refactor this
            
            result << getEnvelopeWithComplexMessage([
                'coverageNote': resultCoverageNote,
                'embargo':      resultEmbargo,
                'endDate':      resultEndDate,
                'endIssue':     resultEndIssue,
                'endVolume':    resultEndVolume,
                'startDate':    resultStartDate,
                'startIssue':   resultStartIssue,
                'startVolume':  resultStartVolume
            ])
        }
        
        getEnvelopeWithMessage(result)
    }
}
