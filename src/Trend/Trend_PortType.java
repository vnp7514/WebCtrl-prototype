/**
 * Trend_PortType.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package Trend;

public interface Trend_PortType extends java.rmi.Remote {
    public java.lang.String[] getTrendData(java.lang.String trendLogPath, java.lang.String sTime, java.lang.String eTime, boolean limitFromStart, int maxRecords) throws java.rmi.RemoteException;
}
