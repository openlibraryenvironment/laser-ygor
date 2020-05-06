<%@ page import="ygor.Enrichment" %>

<meta name="layout" content="enrichment">

<p class="lead">${packageName}</p>

    <div id="showUploadResults">
        <button type="button" class="btn btn-info btn-block" data-toggle="collapse" data-target="#btn-accord">
            <g:message code="listDocuments.gokb.response"/>
        </button>
        <g:set var="nrOfRecords" value="${greenRecords.size() + yellowRecords.size()}"/>
        <div class="collapse in" id="progress-section">
            <div id="progress-${resultHash}" class="progress">
                <div class="progress-bar progress-bar-striped active" role="progressbar" aria-valuenow="0"
                     aria-valuemin="0"aria-valuemax="${nrOfRecords}" style="width:0%;">0%</div>
            </div>
        </div>
            <table class="table" id="feedbackTable">
                <tbody>
                    <script>
                        var data = {}
                        var feedbackTable = document.getElementById("feedbackTable").getElementsByTagName('tbody')[0];
                        var rowTexts = new Map();
                        var jobInfoHandle = setInterval( function(){
                            console.log("get jobInfo of resultHash: ${resultHash}");
                            jQuery.ajax({
                                type: 'GET',
                                url: '${grailsApplication.config.grails.app.context}/statistic/getJobInfo',
                                id: '${jobId}',
                                data: 'jobId=${jobId}',
                                success: function (data) {
                                    if (data != null && !jQuery.isEmptyObject(data)) {
                                        if (data["error"] == "Request is missing an id."){
                                            return;
                                        }
                                        if (data["response_finished"] == "true") {
                                            rowTexts.set("${message(code: 'listDocuments.gokb.response.ok')}", data["response_ok"]);
                                            rowTexts.set("${message(code: 'listDocuments.gokb.response.error')}", data["response_error"]);
                                            let count=1;
                                            for (let errorDetail of data["error_details"]){
                                                rowTexts.set(count.toString(), errorDetail);
                                                count++;
                                            }
                                            jQuery('#progress-${resultHash} > .progress-bar').attr('hidden', 'hidden');
                                            jQuery('#progress-section').attr('hidden', 'hidden');
                                            jQuery('.progress').attr('hidden', 'hidden');
                                        }
                                        else {
                                            jQuery('#progress-${resultHash} > .progress-bar').attr('aria-valuenow', data["progress"]);
                                            jQuery('#progress-${resultHash} > .progress-bar').attr('style', 'width:' + data["progress"] + '%');
                                            jQuery('#progress-${resultHash} > .progress-bar').text(data["progress"] + '%');
                                        }
                                    }
                                },
                                error: function (XMLHttpRequest, textStatus, errorThrown) {
                                    console.error("ERROR - Could not get job info, failing Ajax request.");
                                    console.error(textStatus + " : " + errorThrown);
                                    console.error(data);
                                    clearInterval(jobInfoHandle);
                                }
                            });
                            if (rowTexts.size > 0){
                                for (let text of rowTexts.entries()) {
                                    let row = feedbackTable.insertRow()
                                    row.insertCell(0).appendChild(document.createTextNode(text[0]));
                                    row.insertCell(1).appendChild(document.createTextNode(text[1]));
                                }
                                clearInterval(jobInfoHandle);
                            }
                        }, 2000);
                    </script>
                </tbody>
            </table>
        </div>
        <br/>
    </div>


<div class="row">
    <g:set var="lineCounter" value="${0}"/>
    <g:set var="displayZDB" value="true"/>

    <g:if test="${redRecords != null && !(redRecords.isEmpty())}">
        <div class="col-xs-12">
            <div class="panel panel-default">
                <div class="panel-heading-red">
                    <h3 class="panel-title">${redRecords.size} <g:message code="statistic.show.records.red"/></h3>
                </div>

                <div class="statistics-data">
                    <table id="red-records" class="statistics-details">
                        <thead><tr>
                            <th>Title</th>
                            <g:if test="${displayZDB}">
                                <th>ZDB</th>
                                <th>ZDB ID</th>
                            </g:if>
                            <th>eISSN</th>
                        </tr></thead>
                        <tbody>
                        <g:set var="lineCounter" value="${0}"/>
                        <g:each in="${redRecords}" var="record">
                            <tr class="${(lineCounter % 2) == 0 ? 'even hover' : 'odd hover'}">
                                <td class="statistics-cell">
                                    <g:link action="edit" params="[resultHash: resultHash]"
                                            id="${record.value.getAt(4)}">${org.apache.commons.lang.StringUtils.isEmpty(record.value.getAt(0)) ?
                                            "<"+message(code: 'missing')+">" : record.value.getAt(0)}</g:link>
                                </td>
                                <g:if test="${displayZDB}">
                                    <td><g:if test="${record.value.getAt(1)}">
                                        <a href="${record.value.getAt(1)}" class="link-icon"></a>
                                    </g:if></td>
                                    <td class="statistics-cell"><g:if test="${record.value.getAt(2) != null}">
                                        ${record.value.getAt(2)}<br/>
                                    </g:if></td>
                                </g:if>
                                <td class="statistics-cell"><g:if test="${record.value.getAt(3) != null}">
                                    ${record.value.getAt(3)}<br/>
                                </g:if></td>
                            </tr>
                            <g:set var="lineCounter" value="${lineCounter + 1}"/>
                        </g:each>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </g:if>

    <g:if test="${displayZDB && yellowRecords != null && !(yellowRecords.isEmpty())}">
        <div class="col-xs-12">
            <div class="panel panel-default">
                <div class="panel-heading-yellow">
                    <h3 class="panel-title">${yellowRecords.size} <g:message code="statistic.show.records.yellow"/></h3>
                </div>
                <div class="statistics-data">
                    <table id="yellow-records"class="statistics-details">
                        <thead><tr>
                            <th>Title</th>
                            <th>ZDB</th>
                            <th>ZDB ID</th>
                            <th>eISSN</th>
                        </tr></thead>
                        <tbody>
                        <g:set var="lineCounter" value="${0}"/>
                        <g:each in="${yellowRecords}" var="record">
                            <tr class="${(lineCounter % 2) == 0 ? 'even hover' : 'odd hover'}">
                                <td class="statistics-cell">
                                    <g:link action="edit" params="[resultHash: resultHash]"
                                            id="${record.value.getAt(4)}">${org.apache.commons.lang.StringUtils.isEmpty(record.value.getAt(0)) ?
                                            "<"+message(code: 'missing')+">" : record.value.getAt(0)}</g:link>
                                </td>
                                <td><g:if test="${record.value.getAt(1)}">
                                    <a href="${record.value.getAt(1)}" class="link-icon"></a>
                                </g:if></td>
                                <td class="statistics-cell"><g:if test="${record.value.getAt(2) != null}">
                                    ${record.value.getAt(2)}<br/>
                                </g:if></td>
                                <td class="statistics-cell"><g:if test="${record.value.getAt(3) != null}">
                                    ${record.value.getAt(3)}<br/>
                                </g:if></td>
                            </tr>
                            <g:set var="lineCounter" value="${lineCounter + 1}"/>
                        </g:each>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </g:if>

    <g:if test="${greenRecords != null && !(greenRecords.isEmpty())}">
        <div class="col-xs-12">
            <div class="panel panel-default">
                <div class="panel-heading-green">
                    <h3 class="panel-title">${greenRecords.size} <g:message code="statistic.show.records.green"/></h3>
                </div>

                <div class="statistics-data">
                    <table id="green-records" class="statistics-details">
                        <thead><tr>
                            <th>Title</th>
                            <th>ZDB</th>
                            <th>ZDB ID</th>
                            <th>eISSN</th>
                        </tr></thead>
                        <tbody>
                        <g:set var="lineCounter" value="${0}"/>
                        <g:each in="${greenRecords}" var="record">
                            <tr class="${(lineCounter % 2) == 0 ? 'even hover' : 'odd hover'}">
                                <td class="statistics-cell">
                                <g:link action="edit" params="[resultHash: resultHash]"
                                        id="${record.value.getAt(4)}">${org.apache.commons.lang.StringUtils.isEmpty(record.value.getAt(0)) ?
                                        "<"+message(code: 'missing')+">" : record.value.getAt(0)}</g:link>
                                <g:if test="${displayZDB}">
                                    <td><g:if test="${record.value.getAt(1)}">
                                        <a href="${record.value.getAt(1)}" class="link-icon"></a>
                                    </g:if></td>
                                    <td class="statistics-cell"><g:if test="${record.value.getAt(2) != null}">
                                        ${record.value.getAt(2)}<br/>
                                    </g:if></td>
                                </g:if>
                                <td class="statistics-cell"><g:if test="${record.value.getAt(3) != null}">
                                    ${record.value.getAt(3)}<br/>
                                </g:if></td>
                            </tr>
                            <g:set var="lineCounter" value="${lineCounter + 1}"/>
                        </g:each>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </g:if>

    <g:form>
        <g:hiddenField name="originHash" value="${originHash}"/>
        <g:hiddenField name="resultHash" value="${resultHash}"/>
        <div class="col-xs-12" style="margin-bottom: 20px">
            <g:if test="${grailsApplication.config.ygor.enableGokbUpload}">
                <button type="button" class="btn btn-success btn-same-width" data-toggle="modal" gokbdata="titles"
                        data-target="#credentialsModal"><g:message code="listDocuments.button.sendTitlesFile"/></button>
                <button type="button" class="btn btn-success btn-same-width" data-toggle="modal" gokbdata="package"
                        data-target="#credentialsModal"><g:message code="listDocuments.button.sendPackageFile"/></button>

                <div class="modal fade" id="credentialsModal" role="dialog">
                    <div class="modal-dialog">
                        <div class="modal-content">
                            <div class="modal-header">
                                <button type="button" class="close" data-dismiss="modal">&times;</button>
                                <h4 class="modal-title"><g:message code="listDocuments.gokb.credentials"/></h4>
                            </div>

                            <div class="modal-body">
                                <g:form>
                                    <div class="input-group">
                                        <span class="input-group-addon"><g:message
                                                code="listDocuments.gokb.username"/></span>
                                        <g:textField name="gokbUsername" size="24" class="form-control"/>
                                    </div>

                                    <div class="input-group">
                                        <span class="input-group-addon"><g:message
                                                code="listDocuments.gokb.password"/></span>
                                        <g:passwordField name="gokbPassword" size="24" class="form-control"/>
                                    </div>
                                    <br/>
                                    <div align="right">
                                        <button type="button" class="btn btn-default btn-same-width" data-dismiss="modal"><g:message
                                                code="listDocuments.button.cancel"/></button>
                                        <g:actionSubmit action="" value="${message(code: 'listDocuments.button.send')}"
                                                        class="btn btn-success btn-same-width"
                                                        name="cred-modal-btn-send" data-toggle="tooltip"
                                                        data-placement="top"
                                                        title="JavaScript ${message(code: 'technical.required')}."/>
                                    </div>
                                </g:form>
                            </div>
                        </div>
                    </div>
                </div>
                <br/>
                <br/>
                <script>
                    $('#credentialsModal').on('show.bs.modal', function (event) {
                        var uri = $(event.relatedTarget)[0].getAttribute("gokbdata");
                        if (uri.localeCompare('package') == 0) {
                            $(this).find('.modal-body .btn.btn-success').attr('name', '_action_sendPackageFile');
                        } else if (uri.localeCompare('titles') == 0) {
                            $(this).find('.modal-body .btn.btn-success').attr('name', '_action_sendTitlesFile');
                        }
                    })
                </script>
            </g:if>
            <g:else>
                <g:actionSubmit action="sendPackageFile"
                                value="${message(code: 'listDocuments.button.sendPackageFile')}"
                                class="btn btn-success disabled btn-same-width"
                                data-toggle="tooltip" data-placement="top"
                                title="Deaktiviert: ${grailsApplication.config.gokbApi.xrPackageUri}"
                                disabled="disabled"/>
                <g:actionSubmit action="sendTitlesFile" value="${message(code: 'listDocuments.button.sendTitlesFile')}"
                                class="btn btn-success disabled btn-same-width"
                                data-toggle="tooltip" data-placement="top"
                                title="Deaktiviert: ${grailsApplication.config.gokbApi.xrTitleUri}"
                                disabled="disabled"/>
                <br/>
                <br/>
            </g:else>
            <g:actionSubmit action="downloadTitlesFile"
                            value="${message(code: 'listDocuments.button.downloadTitlesFile')}"
                            class="btn btn-default btn-same-width"/>
            <g:actionSubmit action="downloadPackageFile"
                            value="${message(code: 'listDocuments.button.downloadPackageFile')}"
                            class="btn btn-default btn-same-width"/>
            <g:actionSubmit action="downloadRawFile" value="${message(code: 'listDocuments.button.downloadRawFile')}"
                            class="btn btn-default btn-same-width"/>
            <br/>
            <br/>
            <g:actionSubmit action="correctFile" value="${message(code: 'listDocuments.button.correctFile')}"
                            class="btn btn-warning btn-same-width"/>
            <g:actionSubmit action="deleteFile" value="${message(code: 'listDocuments.button.deleteFile')}"
                            class="btn btn-danger btn-same-width"/>
        </div>
        <script>
            var bwidth=0
            $(".btn-same-width").each(function(i,v){
                if($(v).width()>bwidth) bwidth=$(v).width();
            });
            $(".btn-same-width").width(bwidth);
        </script>
    </g:form>

    <div class="col-xs-12">
        <div class="panel panel-default">
            <div class="panel-heading">
                <h3 class="panel-title"><g:message code="statistic.show.meta"/></h3>
            </div>

            <div id="statistics-meta" class="panel-body">
                <table>
                    <tr class="${(lineCounter % 2) == 0 ? 'active' : 'info active'}">
                        <td class="statistics-cell"><strong><g:message code="statistic.show.main.filename"/></strong>
                        </td>
                        <td class="statistics-cell">${filename}</td>
                    </tr>
                    <tr class="${(lineCounter % 2) == 0 ? 'active' : 'info active'}">
                        <td class="statistics-cell"><strong><g:message code="statistic.show.main.creation"/></strong>
                        </td>
                        <td class="statistics-cell">${date}</td>
                    </tr>
                    <tr class="${(lineCounter % 2) == 0 ? 'active' : 'info active'}">
                        <td class="statistics-cell"><strong><g:message code="statistic.show.main.ygorversion"/></strong>
                        </td>
                        <td class="statistics-cell">${ygorVersion}<br/></td>
                    </tr>
                    <tr class="${(lineCounter % 2) == 0 ? 'active' : 'info active'}">
                        <td class="statistics-cell"><strong><g:message code="statistic.show.main.hash"/></strong></td>
                        <td class="statistics-cell">${resultHash}</td>
                    </tr>
                </table>
            </div>
        </div>
    </div>
</div>
<asset:javascript src="jquery.dataTables.js"/>
<asset:stylesheet src="jquery.dataTables.css"/>

<script>
    $(document).ready(function () {
        $('#red-records').DataTable( {
            ajax: {
                url: "/ygor/statistic/records?resultHash=${resultHash}&colour=RED"
            },
            serverSide: true, stateSave: true, order: [1, "asc"], length: 10,
            draw: 1,
            language: {
                lengthMenu: "${message(code: 'datatables.lengthMenu')}",
                zeroRecords: "",
                info: "${message(code: 'datatables.pageOfPages')}",
                infoEmpty: "${message(code: 'datatables.noRecordsAvailable')}",
                infoFiltered: "${message(code: 'datatables.filteredFromMax')}",
                search: "${message(code: 'datatables.search')}",
                paginate: {
                    first: "${message(code: 'datatables.first')}",
                    last: "${message(code: 'datatables.last')}",
                    next: "${message(code: 'datatables.next')}",
                    previous: "${message(code: 'datatables.previous')}"
                }
            },
            "columns": [
                {
                    "type": "string",
                    "width": "67%"
                },
                {
                    "type": "html",
                    "width": "8%"
                },
                {
                    "type": "string",
                    "width": "11%"
                },
                {
                    "type": "string",
                    "width": "14%"
                }
            ]
        });
        $('#yellow-records').DataTable( {
            ajax: {
                url: "/ygor/statistic/records?resultHash=${resultHash}&colour=YELLOW"
            },
            serverSide: true, stateSave: true, order: [1, "asc"], length: 10,
            draw: 1,
            language: {
                lengthMenu: "${message(code: 'datatables.lengthMenu')}",
                zeroRecords: "",
                info: "${message(code: 'datatables.pageOfPages')}",
                infoEmpty: "${message(code: 'datatables.noRecordsAvailable')}",
                infoFiltered: "${message(code: 'datatables.filteredFromMax')}",
                search: "${message(code: 'datatables.search')}",
                paginate: {
                    first: "${message(code: 'datatables.first')}",
                    last: "${message(code: 'datatables.last')}",
                    next: "${message(code: 'datatables.next')}",
                    previous: "${message(code: 'datatables.previous')}"
                }
            },
            "columns": [
                {
                    "type": "string",
                    "width": "67%"
                },
                {
                    "type": "html",
                    "width": "8%"
                },
                {
                    "type": "string",
                    "width": "11%"
                },
                {
                    "type": "string",
                    "width": "14%"
                }
            ]
        });
        $('#green-records').dataTable( {
            ajax: {
                url: "/ygor/statistic/records?resultHash=${resultHash}&colour=GREEN"
            },
            serverSide: true, stateSave: true, order: [1, "asc"], length: 10,
            draw: 1,
            language: {
                lengthMenu: "${message(code: 'datatables.lengthMenu')}",
                zeroRecords: "",
                info: "${message(code: 'datatables.pageOfPages')}",
                infoEmpty: "${message(code: 'datatables.noRecordsAvailable')}",
                infoFiltered: "${message(code: 'datatables.filteredFromMax')}",
                search: "${message(code: 'datatables.search')}",
                paginate: {
                    first: "${message(code: 'datatables.first')}",
                    last: "${message(code: 'datatables.last')}",
                    next: "${message(code: 'datatables.next')}",
                    previous: "${message(code: 'datatables.previous')}"
                }
            },
            "columns": [
                {
                    "type": "string",
                    "width": "67%"
                },
                {
                    "type": "html",
                    "width": "8%"
                },
                {
                    "type": "string",
                    "width": "11%"
                },
                {
                    "type": "string",
                    "width": "14%"
                }
            ]
        });
    });
</script>
