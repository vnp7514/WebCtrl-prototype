/**
 * TrendService.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package Trend;

public interface TrendService extends javax.xml.rpc.Service {
    public java.lang.String getTrendAddress();

    public Trend.Trend_PortType getTrend() throws javax.xml.rpc.ServiceException;

    public Trend.Trend_PortType getTrend(java.net.URL portAddress) throws javax.xml.rpc.ServiceException;
}
