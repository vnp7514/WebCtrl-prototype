/**
 * ReportServiceLocator.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package ReportWSDL;

public class ReportServiceLocator extends org.apache.axis.client.Service implements ReportService {

    public ReportServiceLocator() {
    }


    public ReportServiceLocator(org.apache.axis.EngineConfiguration config) {
        super(config);
    }

    public ReportServiceLocator(java.lang.String wsdlLoc, javax.xml.namespace.QName sName) throws javax.xml.rpc.ServiceException {
        super(wsdlLoc, sName);
    }

    // Use to get a proxy class for Report
    private java.lang.String Report_address = "https://webctrl.ad.rit.edu:1443/_common/webservices/Report";

    public java.lang.String getReportAddress() {
        return Report_address;
    }

    // The WSDD service name defaults to the port name.
    private java.lang.String ReportWSDDServiceName = "Report";

    public java.lang.String getReportWSDDServiceName() {
        return ReportWSDDServiceName;
    }

    public void setReportWSDDServiceName(java.lang.String name) {
        ReportWSDDServiceName = name;
    }

    public Report getReport() throws javax.xml.rpc.ServiceException {
       java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(Report_address);
        }
        catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getReport(endpoint);
    }

    public Report getReport(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
        try {
            ReportSoapBindingStub _stub = new ReportSoapBindingStub(portAddress, this);
            _stub.setPortName(getReportWSDDServiceName());
            return _stub;
        }
        catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    public void setReportEndpointAddress(java.lang.String address) {
        Report_address = address;
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        try {
            if (Report.class.isAssignableFrom(serviceEndpointInterface)) {
                ReportSoapBindingStub _stub = new ReportSoapBindingStub(new java.net.URL(Report_address), this);
                _stub.setPortName(getReportWSDDServiceName());
                return _stub;
            }
        }
        catch (java.lang.Throwable t) {
            throw new javax.xml.rpc.ServiceException(t);
        }
        throw new javax.xml.rpc.ServiceException("There is no stub implementation for the interface:  " + (serviceEndpointInterface == null ? "null" : serviceEndpointInterface.getName()));
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(javax.xml.namespace.QName portName, Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        if (portName == null) {
            return getPort(serviceEndpointInterface);
        }
        java.lang.String inputPortName = portName.getLocalPart();
        if ("Report".equals(inputPortName)) {
            return getReport();
        }
        else  {
            java.rmi.Remote _stub = getPort(serviceEndpointInterface);
            ((org.apache.axis.client.Stub) _stub).setPortName(portName);
            return _stub;
        }
    }

    public javax.xml.namespace.QName getServiceName() {
        return new javax.xml.namespace.QName("https://webctrl.ad.rit.edu:1443/_common/webservices/Report", "ReportService");
    }

    private java.util.HashSet ports = null;

    public java.util.Iterator getPorts() {
        if (ports == null) {
            ports = new java.util.HashSet();
            ports.add(new javax.xml.namespace.QName("https://webctrl.ad.rit.edu:1443/_common/webservices/Report", "Report"));
        }
        return ports.iterator();
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(java.lang.String portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        
if ("Report".equals(portName)) {
            setReportEndpointAddress(address);
        }
        else 
{ // Unknown Port Name
            throw new javax.xml.rpc.ServiceException(" Cannot set Endpoint Address for Unknown Port" + portName);
        }
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(javax.xml.namespace.QName portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        setEndpointAddress(portName.getLocalPart(), address);
    }

}
