/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package rmi;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Proxy;
import java.net.Socket;
import rmi.RemoteThread.Future;
import rmi.classimpl.LocalHelloServer;

public class InvokationObject implements Serializable {

    protected String host = null;
    protected int portDest = 0;
    private ObjectOutputStream out = null;
    private ObjectInputStream in = null;
    public Socket channel = null;
    public String objectName = null;

    private void connectTo() throws IOException {
        if (channel != null) {
            return;
        }

        System.out.println("Binding the socket" + host + " : " + portDest);
        channel = new Socket(host, portDest);
        channel.setSoTimeout(1000000);
    }

    public void close() throws IOException {
        channel.close();
        out.flush();
        out.close();
        in.close();

        out = null;
        in = null;
        channel = null;
    }

    public ObjectOutputStream getOutputStream() throws IOException {
        connectTo();
        if (out == null) {
            out = new ObjectOutputStream(channel.getOutputStream());
        }
        return out;
    }

    public ObjectInputStream getInputStream() throws IOException {
        connectTo();
        if (in == null) {
            in = new ObjectInputStream(channel.getInputStream());
        }

        return in;
    }

    public Object[] remoteCall(String name, Class<?>[] cls, Object[] args, boolean isOneway) throws IOException {
        ObjectOutputStream out = getOutputStream();
        ObjectInputStream in = getInputStream();
        RemoteHeader header = new RemoteHeader();
        header.order = "CALL_METHOD";
        header.methodName = name;
        header.bindedTo = objectName;
        out.writeObject(header);
        out.writeObject(cls);
        out.writeObject(args);
        out.flush();
        
        if(isOneway) {
            close();
            return null;
        }

        Object[] rets = null;
        try {

            rets = (Object[]) in.readObject();
            close();
            return rets;
        } catch (ClassNotFoundException ex) {
            return rets;
        }
    }

    public Object proxifyVariable(Object msg, Class<?> aClass) {
      return Proxy.newProxyInstance(RemoteProxy.class.getClassLoader(), new Class[] { aClass }, new RemoteProxy(msg));
    }

    public void replayOn(Object proxy, Object subject) {
        try {
            ((RemoteProxy) Proxy.getInvocationHandler(proxy)).replayOn(subject);
        } catch (Throwable ex) {
            
        }
    }

    public <T> Future<T> asyncRemoteCall(LocalHelloServer aThis, String funcName, Class<?>[] cls, Object[] args) {
        RemoteThread<T> th = new RemoteThread<>(aThis, funcName, cls, args);
        th.start();
    
        return th.getFuture();
    }
}