package ygor

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import de.hbznrw.ygor.export.GokbExporter
import de.hbznrw.ygor.processing.MultipleProcessingThread
import de.hbznrw.ygor.export.DataContainer
import de.hbznrw.ygor.export.JsonTransformer
import de.hbznrw.ygor.tools.*
import com.fasterxml.jackson.databind.node.ObjectNode
import groovy.json.JsonOutput
import ygor.field.MappingsContainer

class Enrichment {

    static enum FileType {
        ORIGIN, 
        RESULT, 
        JSON, 
        JSON_PACKAGE_ONLY, 
        JSON_TITLES_ONLY,
        JSON_OO_RAW
    }
    
    static enum ProcessingState {
        UNTOUCHED, PREPARE, WORKING, FINISHED, ERROR
    }

    static ObjectMapper JSON_OBJECT_MAPPER = new ObjectMapper()
    
    ProcessingState status
    
    // frontend api stuff
    String apiMessage
    float  apiProgress = 0.0
    
    String originName
    String originHash
    String originPathName
    
    String resultName
    String resultHash
    String resultPathName
    String dataType

    File sessionFolder

    def thread
    MappingsContainer mappingsContainer
    def dataContainer
    def stats

    static constraints = {
    }

    Enrichment(File sessionFolder, String originalFilename) {
        this.sessionFolder     = sessionFolder
        originName             = originalFilename.replaceAll(/\s+/,'_')
        originHash             = FileToolkit.getMD5Hash(originName + Math.random())
        originPathName         = this.sessionFolder.getPath() + File.separator + originHash
        
        dataContainer          = new DataContainer()
        
        setStatus(ProcessingState.UNTOUCHED)
    }
    
    def process(HashMap options) {    
        resultName        = FileToolkit.getDateTimePrefixedFileName(originName)
        resultHash        = FileToolkit.getMD5Hash(originName + Math.random())
        resultPathName = sessionFolder.getPath() + File.separator + resultHash
        dataType       = options.get('dataTyp')

        dataContainer.info.file = originName
        dataContainer.info.ygor = options.get('ygorVersion')
        dataContainer.info.type = options.get('ygorType')

        mappingsContainer = new MappingsContainer()
        
        thread         = new MultipleProcessingThread(this, options)
        thread.start()
    }
    
    def setProgress(float progress) {
        this.apiProgress = progress
    }
    
    float getProgress() {
        apiProgress
    }

    def setMessage(String message) {
        this.apiMessage = message
    }
    
    String getMessage() {
        apiMessage
    }
        
    def setStatusByCallback(ProcessingState status) {
        setStatus(status)
    }
    
    def setStatus(ProcessingState status) {
        this.status = status
    }
    
    ProcessingState getStatus() {
        status
    }
    
    File getFile(FileType type) {
        switch(type) {
            case FileType.ORIGIN:
                return new File(originPathName)
                break
            case FileType.JSON:
                def file = new File(originPathName)
                file.write(JsonTransformer.getSimpleJSON(dataContainer, FileType.JSON, JsonTransformer.USE_PRETTY_PRINT), "UTF-8")
                return file
                break
            case FileType.JSON_PACKAGE_ONLY:
                ObjectNode result = GokbExporter.extractPackage(this)
                def file = new File(originPathName)
                file.write(JSON_OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(result), "UTF-8")
                return file
                break
            case FileType.JSON_TITLES_ONLY:
                ArrayNode result = GokbExporter.extractTitles(this)
                def file = new File(originPathName)
                file.write(JSON_OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(result), "UTF-8")
                return file
                break
            case FileType.JSON_OO_RAW:
                return new File(resultPathName)
                break
        }
    }


    void saveResult() {
        StringWriter result = new StringWriter()
        result.append("{\"sessionFolder\":\"").append(sessionFolder.absolutePath).append("\",")
        result.append("\"originalFileName\":\"").append(originName).append("\",")
        result.append("\"configuration\":{")
        result.append("\"dataType\":\"").append(dataType).append("\",")
        result.append("\"mappingsContainer\":")
        result.append(JsonToolkit.toJson(mappingsContainer))
        result.append("},\"data\":")
        result.append(JsonToolkit.toJson(dataContainer.records))
        result.append("}")
        File file = new File(resultPathName)
        file.getParentFile().mkdirs()
        file.write(JsonOutput.prettyPrint(result.toString()), "UTF-8")
    }


    static Enrichment fromRawJson(JsonNode rootNode){
        String sessionFolder = JsonToolkit.fromJson(rootNode, "sessionFolder")
        String originalFileName = JsonToolkit.fromJson(rootNode, "originalFileName")
        def en = new Enrichment(new File(sessionFolder), originalFileName)
        en.mappingsContainer = JsonToolkit.fromJson(rootNode, "configuration.mappingsContainer")
        en.resultName = FileToolkit.getDateTimePrefixedFileName(originalFileName)
        en.resultPathName = sessionFolder.concat(File.separator).concat(FileToolkit.getMD5Hash(originalFileName + Math.random()))
        en.dataContainer = DataContainer.fromJson(rootNode.path("data"), en.mappingsContainer)
        en.saveResult()
        return en
    }

}
