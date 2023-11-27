package io.hyperfoil.tools.yaup.json.graaljs;

import io.hyperfoil.tools.yaup.AsciiArt;
import io.hyperfoil.tools.yaup.json.Json;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;


public class JsFetch implements JsThenable, JsPromiseExecutor{


   public static final Json DEFAULT_OPTIONS = Json.fromString("{" +
      "\"method\":\"GET\"," + // GET, POST, PUT, DELETE, HEAD
      "\"mode\":\"cors\","+ //no-cors, cors, same-origin
      "\"cache\":\"no-cache\","+ //default, no-cache, reload, force-cahe, only-if-cached
      "\"credentials\":\"same-origin\","+ //include, same-origin, omit
      "\"headers\":{"+
      "    \"Content-Type\":\"application/json\""+ // 'Content-Type': 'application/x-www-form-urlencoded',
      "},"+
      "\"redirect\":\"follow\"," + // manual, *follow, error
      "\"referrer\":\"no-referrer\"," + // no-referrer, *client
      "\"body\":\"\"" + //
      "}",null,true);

   public class MapProxyObject implements ProxyObject {
      private final Map<String, Object> map;

      public MapProxyObject(Map<String, Object> map) {
         this.map = map;
      }

      public void putMember(String key, Value value) {
         map.put(key, value.isHostObject() ? value.asHostObject() : value);
      }

      public boolean hasMember(String key) {
         return map.containsKey(key);
      }

      public Object getMemberKeys() {
         return map.keySet().toArray();
      }

      public Object getMember(String key) {
         Object v = map.get(key);
         if (v instanceof Map) {
            return new MapProxyObject((Map<String, Object>)v);
         } else {
            return v;
         }
      }

      public Map<String, Object> getMap() {
         return map;
      }
   }

   private Value url;
   private Value options;

   public JsFetch(Value url,Value options){
      this.url = url;
      this.options = options;
   }

   @HostAccess.Export
   public void onPromiseCreation(Value onResolve, Value onReject){
      then(onResolve,onReject);
   }

   @HostAccess.Export
   public void then(Value onResolve, Value onReject){
      try{
         Object rtrn = jsApply(url,options);
         if(onResolve.hasMember("then")){
            onResolve.invokeMember("then",rtrn);
         }else{
            if(onResolve.canExecute()){
               onResolve.execute(rtrn);
            }
         }
      }catch(Exception e){
         if(onReject.hasMember("then")) {
            onReject.invokeMember("then", e.getMessage());
         }else{
            if(onReject.canExecute()){
               onReject.execute(e.getMessage());
            }
         }
      }

   }
   @HostAccess.Export
   public Object jsApply(Value url,Value options){
      if (url.isString()){
         String urlString = url.asString();
         Json optionsJson = options == null ? new Json(false) : Json.fromGraalvm(options);
         Object v  = apply(urlString,optionsJson);
         if(v instanceof Json){
            v = JsonProxy.create((Json)v);
         }
         return v;
      }
      return "ERROR url="+url+" options="+options;
   }
   public Object apply(String url, Json options) {
      if(options == null){
         options = DEFAULT_OPTIONS;
      }else {
         options.merge(DEFAULT_OPTIONS, false);
      }
      try {
         if ("ignore".equals(options.getString("tls", ""))) {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                       public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                          return new X509Certificate[0];
                       }
                       public void checkClientTrusted(
                               java.security.cert.X509Certificate[] certs, String authType) {
                       }
                       public void checkServerTrusted(
                               java.security.cert.X509Certificate[] certs, String authType) {
                       }
                    }
            };
            // Install the all-trusting trust manager
            try {
               SSLContext sc = SSLContext.getInstance("SSL");
               sc.init(null, trustAllCerts, new java.security.SecureRandom());
               HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            } catch (GeneralSecurityException e) {
               e.printStackTrace();
            }
         }
         HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
         if (con instanceof HttpsURLConnection) {
            ((HttpsURLConnection) con).setHostnameVerifier(new HostnameVerifier() {
               @Override
               public boolean verify(String s, SSLSession sslSession) {
                  return true; // yikes
               }
            });
         }
         con.setRequestMethod(options.getString("method", "GET"));
         con.setInstanceFollowRedirects("follow".equals(options.getString("redirect", "")));
         con.setConnectTimeout(30_000);
         con.setReadTimeout(30_000);

         options.getJson("headers", new Json()).forEach((key, value) -> {
            con.setRequestProperty(key.toString(), value.toString());
         });

         if ("POST".equals(options.getString("method", "GET"))) {
            con.setDoOutput(true);
            String body = options.get("body") == null ? "" : options.get("body").toString();
            try (OutputStream os = con.getOutputStream()) {
               byte[] input = body.getBytes("utf-8");
               os.write(input, 0, input.length);
            }
         }

         int status = con.getResponseCode();


         if ((status == HttpURLConnection.HTTP_MOVED_TEMP
                 || status == HttpURLConnection.HTTP_MOVED_PERM) && "follow".equals(options.getString("redirect", ""))) {
            String location = con.getHeaderField("Location");
            URL newUrl = new URL(location);
            return apply(newUrl.toString(), options);//return the result of following the redirect
         }

         Reader streamReader = null;

         if (status >= HttpURLConnection.HTTP_BAD_REQUEST) {
            streamReader = new InputStreamReader(con.getErrorStream());
         } else {
            streamReader = new InputStreamReader(con.getInputStream());
         }


         String inputLine;
         StringBuffer content = new StringBuffer();
         try (BufferedReader in = new BufferedReader(streamReader)) {
            while ((inputLine = in.readLine()) != null) {
               content.append(inputLine);
            }
         } catch (IOException e) {
            e.printStackTrace();
         }

         Map<String, List<String>> headerFields = con.getHeaderFields();

         Json result = new Json();
         result.set("status", status);
         result.set("statusText", con.getResponseMessage());
         result.set("headers", new Json());
         for (Map.Entry<String, List<String>> entries : con.getHeaderFields().entrySet()) {
            String values = "";
            for (String value : entries.getValue()) {
               values += value + ",";
               result.getJson("headers").add(entries.getKey() == null ? "" : entries.getKey(), value);
            }
         }

         if ((headerFields.containsKey("Content-Type") && headerFields.get("Content-Type").contains("application/json")) || (headerFields.containsKey("content-type") && headerFields.get("content-type").contains("application/json"))) {
            result.set("body", Json.fromString(content.toString(), new Json(false)));
         } else {
            result.set("body", content.toString());
         }
         return JsonProxy.create(result);
      } catch (SocketTimeoutException e){

         e.printStackTrace();
         return Json.fromThrowable(e);
      } catch (ProtocolException e) {
         e.printStackTrace();
         return Json.fromThrowable(e);
      } catch (MalformedURLException e) {
         e.printStackTrace();
         return Json.fromThrowable(e);
      } catch (IOException e) {
         //e.printStackTrace();
         throw new JsException(e.getMessage(),"fetch("+url+","+options+")",e);
      }
      //return null;
   }

   public static String btoa(Value input){
      String str = input == null ? "" : input.asString();
      return new String(Base64.getEncoder().encode(str.getBytes()), Charset.defaultCharset());
   }
   public static String atob(Value input){
      String str = input == null ? "" : input.asString();
      return new String(Base64.getDecoder().decode(str.getBytes()), Charset.defaultCharset());
   }

}
