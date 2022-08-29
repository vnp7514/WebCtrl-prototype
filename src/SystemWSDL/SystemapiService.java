/**
 * SystemapiService.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package SystemWSDL;

public interface SystemapiService extends javax.xml.rpc.Service {
    public java.lang.String getSystemAddress();

    public SystemWSDL.Systemapi getSystem() throws javax.xml.rpc.ServiceException;

    public SystemWSDL.Systemapi getSystem(java.net.URL portAddress) throws javax.xml.rpc.ServiceException;
}
