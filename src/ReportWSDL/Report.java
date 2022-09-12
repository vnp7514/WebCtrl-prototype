/**
 * Report.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package ReportWSDL;

public interface Report extends java.rmi.Remote {
    public java.lang.String runReport(java.lang.String location, java.lang.String reportName, java.lang.String extension) throws java.rmi.RemoteException;
    public java.lang.String[] runReportCsvLines(java.lang.String location, java.lang.String reportName) throws java.rmi.RemoteException;
}
