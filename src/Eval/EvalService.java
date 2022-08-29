/**
 * EvalService.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package Eval;

public interface EvalService extends javax.xml.rpc.Service {
    public java.lang.String getEvalAddress();

    public Eval.Eval_PortType getEval() throws javax.xml.rpc.ServiceException;

    public Eval.Eval_PortType getEval(java.net.URL portAddress) throws javax.xml.rpc.ServiceException;
}
