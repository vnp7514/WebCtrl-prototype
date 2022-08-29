/**
 * Systemapi.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package SystemWSDL;

public interface Systemapi extends java.rmi.Remote {
    public java.lang.String getProperty(java.lang.String propertyName) throws java.rmi.RemoteException;
    public java.lang.String getWebAppGlobalDirectory(java.lang.String webAppName) throws java.rmi.RemoteException;
    public java.lang.String getWebAppStoragePublicDirectory(java.lang.String webAppName) throws java.rmi.RemoteException;
    public java.lang.String getWebAppStorageDirectory(java.lang.String webAppName) throws java.rmi.RemoteException;
}
