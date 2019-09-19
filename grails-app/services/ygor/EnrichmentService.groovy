package ygor

import de.hbznrw.ygor.export.Validator
import de.hbznrw.ygor.export.structure.Pod
import groovyx.net.http.HTTPBuilder
import javax.servlet.http.HttpSession
import groovyx.net.http.*
import org.springframework.web.multipart.commons.CommonsMultipartFile
import org.codehaus.groovy.grails.web.util.WebUtils
import de.hbznrw.ygor.tools.*

class EnrichmentService {
    
    def grailsApplication
    GokbService gokbService
    
    void addFileAndFormat(CommonsMultipartFile file, String delimiter, String quote, String quoteMode, String dataTyp) {
        
        def en = new Enrichment(getSessionFolder(), file.originalFilename)
        en.setStatus(Enrichment.ProcessingState.PREPARE)
        
        def tmp = [:]
        tmp << ['delimiter': delimiter]
        tmp << ['quote':     quote]
        tmp << ['quoteMode': quoteMode]
        tmp << ['dataTyp': dataTyp]
        
        def formats = getSessionFormats()
        formats << ["${en.originHash}":tmp]
        
        def enrichments = getSessionEnrichments()
        enrichments << ["${en.originHash}": en]
        
        file.transferTo(new File(en.originPathName))
    }
    
    File getFile(Enrichment enrichment, Enrichment.FileType type) {
        
        enrichment.getFile(type)
    }
    
    void deleteFileAndFormat(Enrichment enrichment) {
        
        if(enrichment) {
            def origin = enrichment.getFile(Enrichment.FileType.ORIGIN)
            if(origin)
                origin.delete()
            getSessionEnrichments()?.remove("${enrichment.originHash}")
            getSessionFormats()?.remove("${enrichment.originHash}")
        }
    }
    
    void prepareFile(Enrichment enrichment, Map pm){
        
        def ph = enrichment.dataContainer.pkg.packageHeader
        
        ph.v.name.v          = new Pod(pm['pkgTitle'][0])
        if ("" != pm['pkgVariantName'][0].trim()) {
            ph.v.variantNames << new Pod(pm['pkgVariantName'][0])
            enrichment.dataContainer.info.isil = pm['pkgVariantName'][0]
        }
        if("" != pm['pkgCuratoryGroup1'][0].trim()){
            ph.v.curatoryGroups << new Pod(pm['pkgCuratoryGroup1'][0])
        }
        if("" != pm['pkgCuratoryGroup2'][0].trim()){
            ph.v.curatoryGroups << new Pod(pm['pkgCuratoryGroup2'][0])
        }

        setPlatformMap(pm, ph)

        def pkgNomProvider = pm['pkgNominalProvider'][0]
        if(pkgNomProvider){
            ph.v.nominalProvider.v = pkgNomProvider
            ph.v.nominalProvider.m = Validator.isValidString(ph.v.nominalProvider.v)
        }

        if(pm['namespace_title_id']) {
            enrichment.dataContainer.info.namespace_title_id = pm['namespace_title_id'][0]
        }

        enrichment.setStatus(Enrichment.ProcessingState.UNTOUCHED)
    }

    private void setPlatformMap(Map pm, ph) {

      log.debug("Getting platforms for: ${pm['pkgNominalPlatform'][0]}")

      def tmp = pm['pkgNominalPlatform'][0].split(';')
      def platformID = tmp[0]
      def qterm =  tmp[1]


      def platforms = gokbService.getPlatformMap(qterm, false).records
      def pkgNomPlatform = null

      log.debug("Got platforms: ${platforms}")

      platforms.each {
        log.debug("Handling platform ${it}..")

        if (it.name == qterm && it.status == "Current" && it.oid == platformID) {
          if(pkgNomPlatform) {
            log.warn("Mehrere Plattformen mit dem gleichen Namen gefunden ...")
          }else{
            log.debug("Setze ${it.name} als nominalPlatform.")
            pkgNomPlatform = it
          }
        }
        else {
          if (it.name != qterm) {
            log.debug("No name match: ${it.name} - ${qterm}")
          }
          if(it.status != "Current") {
            log.debug("Wrong status: ${it.status}")
          }
          if (it.oid != platformID) {
            log.debug("No OID match: ${it.oid} - ${platformID}")
          }
        }
      }

      if (pkgNomPlatform) {
        setUrlIfValid(pkgNomPlatform.url, ph)
        ph.v.nominalPlatform.name = pkgNomPlatform.name
        ph.v.nominalPlatform.m = Validator.isValidURL(ph.v.nominalPlatform.url)
      }else{
        log.error("package platform not set!")
      }
    }

    private void setUrlIfValid(value, ph) {

        try {
            URL url = new URL(value)
            ph.v.nominalPlatform.url = value
        }
        catch (MalformedURLException e) {
            ph.v.nominalPlatform.url = ""
        }
    }

    void stopProcessing(Enrichment enrichment) {

        enrichment.thread.isRunning = false
    }
    
    List sendFile(Enrichment enrichment, Object fileType, def user, def pwd) {
        
        def result = []
        def json
        
        if(fileType == Enrichment.FileType.JSON_PACKAGE_ONLY){
            json = enrichment?.getFile(Enrichment.FileType.JSON_PACKAGE_ONLY) ?: null

            if (json) {
              result << exportFileToGOKb(enrichment, json, grailsApplication.config.gokbApi.xrPackageUri, user, pwd)
            }
        }
        else if(fileType == Enrichment.FileType.JSON_TITLES_ONLY){
            json = enrichment?.getFile(Enrichment.FileType.JSON_TITLES_ONLY) ?: null

            if (json) {
              result << exportFileToGOKb(enrichment, json, grailsApplication.config.gokbApi.xrTitleUri, user, pwd)
            }
        }

        result
    }


    private Map exportFileToGOKb(Enrichment enrichment, Object json, String url, def user, def pwd){
        
        log.info("exportFile: " + enrichment.resultHash + " -> " + url)

        def http = new HTTPBuilder(url)
        http.auth.basic user, pwd
        
        http.request(Method.POST, ContentType.JSON) { req ->
            headers.'User-Agent' = 'ygor'
            headers.'Connection' = 'close'

            body = json.getText()
            response.success = { resp, html ->
                log.info("server response: ${resp.statusLine}")
                log.debug("server:          ${resp.headers.'Content-Type'}")
                log.debug("server:          ${resp.headers.'Server'}")
                log.debug("content length:  ${resp.headers.'Content-Length'}")
                if (resp.headers.'Content-Type' == 'application/json;charset=UTF-8') {
                    if(resp.status < 400){
                        return ['info':html]
                    }
                    else {
                        return ['warning':html]
                    }
                }
                else {
                    return ['error': ['message':"Authentication error!", 'result':"ERROR"]]
                }
            }
            response.failure = { resp, html ->
                log.error("server response: ${resp.statusLine}")
                if (resp.headers.'Content-Type' == 'application/json;charset=UTF-8') {
                    return ['error': html]
                }
                else {
                    return ['error': ['message':"Authentication error!", 'result':"ERROR"]]
                }
            }
        }
    }
    
    def getSessionEnrichments(){
        HttpSession session = SessionToolkit.getSession()
        if(!session.enrichments){
            session.enrichments = [:]
        }
        session.enrichments
    }
    
    def getSessionFormats(){
        HttpSession session = SessionToolkit.getSession()
        if(!session.formats){
            session.formats = [:]
        }
        session.formats
    }
    
    /**
     * Return session depending directory for file upload.
     * Creates if not existing.
     */

    File getSessionFolder() {
        
        def session = WebUtils.retrieveGrailsWebRequest().session
        def path = grailsApplication.config.ygor.uploadLocation + File.separator + session.id
        def sessionFolder = new File(path)
        if(!sessionFolder.exists()) {
            sessionFolder.mkdirs()
        }
        sessionFolder
    }
}
