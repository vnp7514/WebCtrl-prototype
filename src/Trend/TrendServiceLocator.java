/**
 * TrendServiceLocator.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package Trend;

public class TrendServiceLocator extends org.apache.axis.client.Service implements Trend.TrendService {

    public TrendServiceLocator() {
    }


    public TrendServiceLocator(org.apache.axis.EngineConfiguration config) {
        super(config);
    }

    public TrendServiceLocator(java.lang.String wsdlLoc, javax.xml.namespace.QName sName) throws javax.xml.rpc.ServiceException {
        super(wsdlLoc, sName);
    }

    // Use to get a proxy class for Trend
    private java.lang.String Trend_address = "https://webctrl.ad.rit.edu:1443/_common/webservices/Trend";

    public java.lang.String getTrendAddress() {
        return Trend_address;
    }

    // The WSDD service name defaults to the port name.
    private java.lang.String TrendWSDDServiceName = "Trend";

    public java.lang.String getTrendWSDDServiceName() {
        return TrendWSDDServiceName;
    }

    public void setTrendWSDDServiceName(java.lang.String name) {
        TrendWSDDServiceName = name;
    }

    public Trend.Trend_PortType getTrend() throws javax.xml.rpc.ServiceException {
       java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(Trend_address);
        }
        catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getTrend(endpoint);
    }

    public Trend.Trend_PortType getTrend(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
        try {
            Trend.TrendSoapBindingStub _stub = new Trend.TrendSoapBindingStub(portAddress, this);
            _stub.setPortName(getTrendWSDDServiceName());
            return _stub;
        }
        catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    public void setTrendEndpointAddress(java.lang.String address) {
        Trend_address = address;
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        try {
            if (Trend.Trend_PortType.class.isAssignableFrom(serviceEndpointInterface)) {
                Trend.TrendSoapBindingStub _stub = new Trend.TrendSoapBindingStub(new java.net.URL(Trend_address), this);
                _stub.setPortName(getTrendWSDDServiceName());
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
        if ("Trend".equals(inputPortName)) {
            return getTrend();
        }
        else  {
            java.rmi.Remote _stub = getPort(serviceEndpointInterface);
            ((org.apache.axis.client.Stub) _stub).setPortName(portName);
            return _stub;
        }
    }

    public javax.xml.namespace.QName getServiceName() {
        return new javax.xml.namespace.QName("https://webctrl.ad.rit.edu:1443/_common/webservices/Trend", "TrendService");
    }

    private java.util.HashSet ports = null;

    public java.util.Iterator getPorts() {
        if (ports == null) {
            ports = new java.util.HashSet();
            ports.add(new javax.xml.namespace.QName("https://webctrl.ad.rit.edu:1443/_common/webservices/Trend", "Trend"));
        }
        return ports.iterator();
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(java.lang.String portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        
if ("Trend".equals(portName)) {
            setTrendEndpointAddress(address);
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
