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
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.apache.http.protocol.HTTP;

public class Authentication {

    public JSONObject doAttemptLogin(String mUser, String mPassword) throws Exception{
        JSONObject json = new JSONObject();
        json.put("username", mUser);
        json.put("password", mPassword);
        HttpResponse response = doLogin(json);
        String jsonResponse = EntityUtils.toString(response.getEntity());
        return new JSONObject(jsonResponse);
    }

    public HttpResponse doLogin(JSONObject json) throws Exception {
        String endpoint = Tokens.serv_end_Pnt_LOGIN;
        return login_Drupal(Tokens.serv_end_Pnt_LOGIN, json);
    }

    public HttpResponse doLogout(String session_name, String session_id, String token) throws Exception {
        String endpoint = Tokens.serv_end_Pnt_LOGOUT;
        return logout_Drupal(endpoint, session_name, session_id, token);
    }

    public HttpResponse login_Drupal(String endpoint, JSONObject json) throws Exception  {
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(endpoint);
        StringEntity se = new StringEntity(json.toString());
        se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
        httppost.setEntity(se);
        HttpResponse response = httpclient.execute(httppost);
        return response;
    }

    public HttpResponse logout_Drupal(String endpoint, String session_name, String session_id, String token) throws Exception  {

        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(endpoint);

        //region SET COOKIES
        BasicHttpContext mHttpContext = new BasicHttpContext();
        BasicCookieStore mCookieStore = new BasicCookieStore();
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

        HttpResponse response = httpclient.execute(httppost);
        return response;

    }

}
