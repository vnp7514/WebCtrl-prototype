/**
 * SystemapiServiceLocator.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package SystemWSDL;

public class SystemapiServiceLocator extends org.apache.axis.client.Service implements SystemWSDL.SystemapiService {

    public SystemapiServiceLocator() {
    }


    public SystemapiServiceLocator(org.apache.axis.EngineConfiguration config) {
        super(config);
    }

    public SystemapiServiceLocator(java.lang.String wsdlLoc, javax.xml.namespace.QName sName) throws javax.xml.rpc.ServiceException {
        super(wsdlLoc, sName);
    }

    // Use to get a proxy class for System
    private java.lang.String System_address = "https://webctrl.ad.rit.edu:1443/_common/webservices/System";

    public java.lang.String getSystemAddress() {
        return System_address;
    }

    // The WSDD service name defaults to the port name.
    private java.lang.String SystemWSDDServiceName = "System";

    public java.lang.String getSystemWSDDServiceName() {
        return SystemWSDDServiceName;
    }

    public void setSystemWSDDServiceName(java.lang.String name) {
        SystemWSDDServiceName = name;
    }

    public SystemWSDL.Systemapi getSystem() throws javax.xml.rpc.ServiceException {
       java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(System_address);
        }
        catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getSystem(endpoint);
    }

    public SystemWSDL.Systemapi getSystem(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
        try {
            SystemWSDL.SystemSoapBindingStub _stub = new SystemWSDL.SystemSoapBindingStub(portAddress, this);
            _stub.setPortName(getSystemWSDDServiceName());
            return _stub;
        }
        catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    public void setSystemEndpointAddress(java.lang.String address) {
        System_address = address;
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        try {
            if (SystemWSDL.Systemapi.class.isAssignableFrom(serviceEndpointInterface)) {
                SystemWSDL.SystemSoapBindingStub _stub = new SystemWSDL.SystemSoapBindingStub(new java.net.URL(System_address), this);
                _stub.setPortName(getSystemWSDDServiceName());
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
        if ("System".equals(inputPortName)) {
            return getSystem();
        }
        else  {
            java.rmi.Remote _stub = getPort(serviceEndpointInterface);
            ((org.apache.axis.client.Stub) _stub).setPortName(portName);
            return _stub;
        }
    }

    public javax.xml.namespace.QName getServiceName() {
        return new javax.xml.namespace.QName("https://webctrl.ad.rit.edu:1443/_common/webservices/System", "SystemapiService");
    }

    private java.util.HashSet ports = null;

    public java.util.Iterator getPorts() {
        if (ports == null) {
            ports = new java.util.HashSet();
            ports.add(new javax.xml.namespace.QName("https://webctrl.ad.rit.edu:1443/_common/webservices/System", "System"));
        }
        return ports.iterator();
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(java.lang.String portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        
if ("System".equals(portName)) {
            setSystemEndpointAddress(address);
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
