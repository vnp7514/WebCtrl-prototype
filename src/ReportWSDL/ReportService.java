/**
 * ReportService.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package ReportWSDL;

public interface ReportService extends javax.xml.rpc.Service {
    public java.lang.String getReportAddress();

    public Report getReport() throws javax.xml.rpc.ServiceException;

    public Report getReport(java.net.URL portAddress) throws javax.xml.rpc.ServiceException;
}
