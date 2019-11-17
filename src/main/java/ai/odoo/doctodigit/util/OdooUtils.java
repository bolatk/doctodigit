package ai.odoo.doctodigit.util;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import java.net.MalformedURLException;
import java.net.URL;

public class OdooUtils {

    public static int login(String url, String db, String login, String password) throws XmlRpcException,
            MalformedURLException {
        XmlRpcClient client = new XmlRpcClient();
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        config.setEnabledForExtensions(true);
        config.setServerURL(new URL(url + "/xmlrpc/2/common"));
        client.setConfig(config);
        Object[] params = new Object[] {db, login, password};
        Object uid = client.execute("login", params);
        if (uid instanceof Integer)
            return (int) uid;
        return -1;
    }
}
