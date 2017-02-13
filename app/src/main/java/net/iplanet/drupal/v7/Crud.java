package net.iplanet.drupal.v7;

import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;

/**
 * Created by Administratzailea on 2/13/2017.
 */

public class Crud {

    public HttpResponse post_Drupal(String query, String session_name, String session_id, String token) throws Exception  {

        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(Tokens.serv_end_Pnt_NODE);

        StringEntity se = new StringEntity(query);
        se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
        httppost.setEntity(se);

        //region SET COOKIES
        BasicHttpContext mHttpContext = new BasicHttpContext();
        CookieStore mCookieStore = new BasicCookieStore();
        BasicClientCookie cookie = new BasicClientCookie(session_name, session_id);
        cookie.setVersion(0);
        cookie.setDomain(Tokens.serv_end_Pnt_DOMAIN);
        cookie.setPath("/");
        mCookieStore.addCookie(cookie);
        cookie = new BasicClientCookie("has_js", "1");
        mCookieStore.addCookie(cookie);
        mHttpContext.setAttribute(ClientContext.COOKIE_STORE, mCookieStore);
        httppost.setHeader("X-CSRF-Token", token);
        //endregion

        HttpResponse response = httpclient.execute(httppost, mHttpContext);
        return response;

    }

}
