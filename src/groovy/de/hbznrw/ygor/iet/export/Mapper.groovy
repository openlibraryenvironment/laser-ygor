package de.hbznrw.ygor.iet.export

import java.util.HashMap
import de.hbznrw.ygor.iet.Envelope
import de.hbznrw.ygor.iet.enums.*
import de.hbznrw.ygor.iet.export.structure.*
import de.hbznrw.ygor.iet.bridge.*
import de.hbznrw.ygor.tools.DateToolkit
import groovy.util.logging.Log4j
import de.hbznrw.ygor.tools.*


@Log4j
class Mapper {
    
    static void mapToTitle(DataContainer dc, Title title, Query query, Envelope env) {

        if(query in [Query.ZDBID, Query.EZBID, Query.GBV_EISSN, Query.GBV_PISSN, Query.GBV_GVKPPN]) {
            def tmp = TitleStruct.getNewIdentifier()
            
            if(Query.ZDBID == query)
                tmp.type.v = ZdbBridge.IDENTIFIER
            else if(Query.EZBID == query)
                tmp.type.v = EzbBridge.IDENTIFIER
            else if(Query.GBV_EISSN == query)
                tmp.type.v = TitleStruct.EISSN
            else if(Query.GBV_PISSN == query)
                tmp.type.v = TitleStruct.PISSN
            else if(Query.GBV_GVKPPN == query)
                tmp.type.v = "gvk_ppn"
                
            tmp.type.m  = Status.IGNORE
            tmp.value.v = Normalizer.normIdentifier(env.message, tmp.type.v)
            tmp.value.m = Validator.isValidIdentifier(tmp.value.v, tmp.type.v)
            
            // TODO: handle multiple ezbid matches
            
            title.identifiers << tmp // no pod
        }
        
        else if(query == Query.GBV_TITLE) {
            title.name.v = Normalizer.normString(env.message)
            title.name.m = Validator.isValidString(title.name.v)
        }
        
        else if(query == Query.GBV_PUBLISHER) {
            def dummy     = null
            def dummyDate = null
            
            env.message.each{ e ->
               e.messages['name'].eachWithIndex{ elem, i ->
                   def tmp = TitleStruct.getNewPublisherHistory()
                   
                   tmp.name.v = Normalizer.normString(e.messages['name'][i])
               
                   tmp.startDate.v = Normalizer.normDate(e.messages['startDate'][i], Normalizer.IS_START_DATE)
                   tmp.startDate.m = Validator.isValidDate(tmp.startDate.v)
                          
                   tmp.endDate.v = Normalizer.normDate(e.messages['endDate'][i], Normalizer.IS_END_DATE)
                   tmp.endDate.m = Validator.isValidDate(tmp.endDate.v)
                                   
                   if([e.messages['startDate'][i], e.messages['endDate'][i]].contains("anfangs")){
                       dummy = tmp
                   } else {
                       // store lowest start date for dummy calculation
                       if(dummyDate == null || (tmp.startDate.m == Status.VALIDATOR_DATE_IS_VALID && dummyDate > tmp.startDate.v))
                           dummyDate = tmp.startDate.v
                           
                       title.publisher_history << tmp // no pod
                   }
                }
            }
            
            if(dummy){
                if(dummyDate){
                    dummy.endDate.v   = DateToolkit.getDateMinusOneMinute(dummyDate)
                    dummy.endDate.m   = Validator.isValidDate(dummy.endDate.v)
                    dummy.startDate.v = ''
                    dummy.startDate.m = Validator.isValidDate(dummy.startDate.v)
                    
                    log.info("adding virtual end date to title.publisher_history: ${dummy.endDate.v}")
                    title.publisher_history << dummy // no pod
                }
            }

        }
        
        else if(query == Query.GBV_PUBLISHED_FROM) {
            title.publishedFrom.v = Normalizer.normDate(env.message, Normalizer.IS_START_DATE)
            title.publishedFrom.m = Validator.isValidDate(title.publishedFrom.v)
        }
        
        else if(query == Query.GBV_PUBLISHED_TO) {
            title.publishedTo.v = Normalizer.normDate(env.message, Normalizer.IS_END_DATE)
            title.publishedTo.m = Validator.isValidDate(title.publishedTo.v)
        }
        
        else if(query == Query.GBV_HISTORY_EVENTS) {
            def tmp =  TitleStruct.getNewHistoryEvent()

            env.message.each{ e ->
                e.messages['title'].eachWithIndex{ elem, i ->
                    
                    def hex = TitleStruct.getNewHistoryEventGeneric()
                    hex.title.v = Normalizer.normString(e.messages['title'][i])
                    hex.title.m = Validator.isValidString(hex.title.v)
                    
                    if("Vorg.".equals(e.messages['type'][i])){
                        tmp.from << hex
                    }
                    else if("Forts.".equals(e.messages['type'][i])){
                        tmp.to << hex
                    }

                    def ident = TitleStruct.getNewIdentifier()
                    
                    ident.type.m  = Status.IGNORE
                    ident.type.v  = e.messages['identifierType'][i].toLowerCase()
                    ident.value.v = Normalizer.normIdentifier(e.messages['identifierValue'][i], ident.type.v)
                    ident.value.m = Validator.isValidIdentifier(ident.value.v, ident.type.v)                   
                    
                    hex.identifiers << ident
                }
            }
            
            title.history_events << new Pod(tmp)
        }
    }
    
    static void mapToTipp(DataContainer dc, Tipp tipp, Query query, Envelope env) {

        if(query in [Query.ZDBID, Query.GBV_EISSN]) {
            def tmp = TitleStruct.getNewIdentifier()
            
            if(Query.ZDBID == query)
                tmp.type.v = ZdbBridge.IDENTIFIER
            else if(Query.GBV_EISSN == query)
                tmp.type.v = TitleStruct.EISSN

            tmp.type.m  = Status.IGNORE
            tmp.value.v = Normalizer.normIdentifier(env.message, tmp.type.v)
            tmp.value.m = Validator.isValidIdentifier(tmp.value.v, tmp.type.v)

            tipp.title.v.identifiers << tmp // no pod
        }
        
        else if(query == Query.GBV_TITLE) {
            tipp.title.v.name.v = Normalizer.normString(env.message)
            tipp.title.v.name.m = Validator.isValidString(tipp.title.v.name.v)
        }
        
        else if(query == Query.GBV_TIPP_URL) {
            tipp.url.v = Normalizer.normTippURL(env.message, dc.pkg.packageHeader.v.nominalPlatform.v)
            tipp.url.m = Validator.isValidURL(tipp.url.v)
        }

        else if(query == Query.GBV_TIPP_COVERAGE) {     
            
            env.message.each{ e ->
                e.messages['coverageNote'].eachWithIndex{ elem, i ->
                    
                    def tmp = PackageStruct.getNewTippCoverage()
                    // TODO
                    tmp.coverageNote.v = Normalizer.normString(e.messages['coverageNote'][i])
                    tmp.coverageNote.m = Normalizer.normString(
                        (e.states.find{it.toString().startsWith('coverageNote_')}).toString().replaceFirst('coverageNote_', '')
                        )
                    
                    if(e.messages['startDate'][i]){
                        tmp.startDate.v = Normalizer.normDate(e.messages['startDate'][i], Normalizer.IS_START_DATE)
                        tmp.startDate.m = Validator.isValidDate(tmp.startDate.v)   
                    }
                    if(e.messages['endDate'][i]){
                        tmp.endDate.v = Normalizer.normDate(e.messages['endDate'][i], Normalizer.IS_END_DATE)
                        tmp.endDate.m = Validator.isValidDate(tmp.endDate.v)
                    }
                    if(e.messages['startVolume'][i]){
                        tmp.startVolume.v = Normalizer.normCoverageVolume(e.messages['startVolume'][i], Normalizer.IS_START_DATE)
                        tmp.startVolume.m = Validator.isValidNumber(tmp.startVolume.v)
                    }
                    if(e.messages['endVolume'][i]){
                        tmp.endVolume.v = Normalizer.normCoverageVolume(e.messages['endVolume'][i], Normalizer.IS_END_DATE)
                        tmp.endVolume.m = Validator.isValidNumber(tmp.endVolume.v)
                    } 
                    
                    def valid = Validator.isValidCoverage(tmp.startDate, tmp.endDate, tmp.startVolume, tmp.endVolume) ? Status.VALIDATOR_COVERAGE_IS_VALID : Status.VALIDATOR_COVERAGE_IS_INVALID
                    
                    
                    if(Status.VALIDATOR_COVERAGE_IS_INVALID == valid && tmp.startDate.v == tmp.endDate.v && tmp.startVolume.v == tmp.endVolume.v) {
                        // prefilter to reduce crappy results
                        log.debug("! ignore crappy tipp coverage")
                    }
                    else {
                        tipp.coverage << new Pod(tmp, valid)
                    }
                }
            }
        }
    }
      
    static void mapHistoryEvents(DataContainer dc, Title title, Object stash) {
        
        log.info("mapping history events for title: " + title.name.v)

        // todo: handle multiple history events
        def historyEvents = []
        
        title.history_events.each{ he ->
            
            def x = TitleStruct.getNewHistoryEventGeneric()
            x.title.v = title.name.v
            x.title.m = title.name.m

            title.identifiers.each{ ident ->
                if([ZdbBridge.IDENTIFIER, TitleStruct.EISSN].contains(ident.type.v))
                    x.identifiers << ident
            }
            
            // set identifiers
            // set missing eissn
            // set missing title
            // set date
            if(he.v.from.size() > 0){
                he.v.to << x
                he.v.from.each { from ->
                    def identifiers = []
                    from.identifiers.each{ ident ->
                        identifiers << ident
                        if(ident.type.v == ZdbBridge.IDENTIFIER){
                            def target = stash[ZdbBridge.IDENTIFIER].get("${ident.value.v}")
                            target = dc.titles.get("${target}")
    
                            if(target){
                                target.v.identifiers.each{ targetIdent ->
                                    if(targetIdent.type.v == TitleStruct.EISSN){
                                        identifiers << targetIdent
                                    }
                                }
                                from.title.v = target.v.name.v
                                from.title.m = target.v.name.m 
                            }
                        }
                    }
                    from.identifiers = identifiers
                }
                he.v.date.v = title.publishedFrom.v
                he.v.date.m = title.publishedFrom.m
            }
            
            // set identifiers
            // set missing eissn
            // set date
            else if(he.v.to.size() > 0){
                he.v.from << x
                he.v.to.each { to ->
                    def identifiers = []
                    to.identifiers.each{ ident ->
                        identifiers << ident
                        if(ident.type.v == ZdbBridge.IDENTIFIER){
                            def target = stash[ZdbBridge.IDENTIFIER].get("${ident.value.v}")
                            target = dc.titles.get("${target}")
    
                            if(target){
                                target.v.identifiers.each{ targetIdent ->
                                    if(targetIdent.type.v == TitleStruct.EISSN){
                                        identifiers << targetIdent
                                    }
                                }
                                he.v.date.v = target.v.publishedFrom.v
                                he.v.date.m = target.v.publishedFrom.m
                            }
                        }
                    }
                    to.identifiers = identifiers
                }
            }
           
            def valid = Validator.isValidHistoryEvent(he)
            
            if(Status.VALIDATOR_HISTORYEVENT_IS_INVALID == valid && he.v.date.m == Status.UNDEFINED && he.v.from.size() == 0 && he.v.to.size() == 0) {
                // prefilter to reduce crappy results
                log.debug("! ignore crappy title history event")
            } 
            else {
                he.m = valid
                historyEvents << he
            }
        }
        
        title.history_events = historyEvents
    }
    
    static void mapPlatform(Tipp tipp) { 
        
        log.info("mapping platform for tipp: " + tipp.title.v.name.v)
        
        if(tipp.url.m == Status.VALIDATOR_URL_IS_VALID){
            def tmp = PackageStruct.getNewTippPlatform()

            tmp.primaryUrl.v = Normalizer.normURL(tipp.url.v)
            tmp.primaryUrl.m = Validator.isValidURL(tmp.primaryUrl.v)
            
            tmp.name.v = Normalizer.normString(tmp.primaryUrl.v)
            tmp.name.m = Validator.isValidString(tmp.name.v)
            
            tipp.platform = new Pod(tmp)
        }
    }
    
    static void mapOrganisations(HashMap orgMap, Title title) {

        log.info("mapping publisher history organisations for title: " + title.name.v)
        
        // TODO: store state for statistics
        
        title.publisher_history.each { ph ->
            log.debug("checking: " + ph.name.v)
            def prefLabelMatch = false
            
            orgMap?.any { prefLabel ->
                if(ph.name.v == prefLabel) {
                    log.debug("matched prefLabel: " + prefLabel)
                    ph.name.v = Normalizer.normString(prefLabel)
                    ph.name.m = Validator.isValidString(ph.name.v)
                    prefLabelMatch = true
                    return true
                }
            }
            if(!prefLabelMatch){
                // catch multiple altLabel matches ..
                def prefLabels = []
                orgMap?.any { prefLabel, altLabels ->
                    altLabels?.any { altLabel ->
                        if(ph.name.v == altLabel) {
                            log.debug("matched altLabel: " + altLabel + " -> set prefLabel: " + prefLabel)
                            prefLabels << prefLabel
                        }
                    }
                }
                ph.name.v = Normalizer.normString(prefLabels)
                ph.name.m = Validator.isValidString(ph.name.v)
            }
        }
    }
  
    static HashMap getOrganisationMap() {
        
        def resource = FileToolkit.getResourceByClassPath('/de/hbznrw/ygor/resources/ONLD.jsonld')
        def orgJson  = JsonToolkit.parseFileToJson(resource.file.path)
        def orgMap   = [:]

        orgJson.'@graph'.each { e ->
            orgMap.put(e.'skos:prefLabel', e.'skos:altLabel')
        }
        orgMap
    }
    
    static Title getExistingTitleByPrimaryIdentifier(DataContainer dc, String key) {
        if(dc.titles.containsKey("${key}"))
            return dc.titles.get("${key}").v

        null
    }
    
    static Tipp getExistingTippByPrimaryIdentifier(DataContainer dc, String key) {
        if(dc.pkg.tipps.containsKey("${key}"))
            return dc.pkg.tipps.get("${key}").v

        null
    }
}
