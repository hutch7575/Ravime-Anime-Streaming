package tv.reanime.cast;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.*;
import android.webkit.*;
import android.webkit.CookieManager;
import android.widget.*;
import java.net.*;
import java.util.*;
import org.json.*;

/** Displays the real website and offers direct media detection for a custom Roku receiver. */
public final class MainActivity extends Activity {
    private static final String HOME = "https://reanime.to/";
    private final LinkedHashMap<String,Media> media = new LinkedHashMap<>();
    private final LinkedHashMap<String,Media> subtitles = new LinkedHashMap<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private WebView web;
    private FrameLayout root;
    private LinearLayout layout;
    private ScrollView panel;
    private TextView status, pairing, count;
    private CastServer server;
    private boolean serverActive = false, scanning = false;
    private String code, chosenSubtitle = "";
    private View fullScreenView;
    private WebChromeClient.CustomViewCallback fullScreenCallback;

    static final class Media {
        String url, type;
        Media(String u, String t) { url=u; type=t; }
        String label() {
            try {
                URI uri=new URI(url);
                String path=uri.getPath();
                if(path==null) path="";
                return type.toUpperCase(Locale.ROOT)+" · "+uri.getHost()+" "+(path.length()>32?path.substring(path.length()-32):path);
            } catch(Exception ex) { return url; }
        }
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(0xFF0C0C12);
        getWindow().setNavigationBarColor(0xFF0C0C12);
        code = Integer.toString(100000 + new java.security.SecureRandom().nextInt(900000));
        try { server = new CastServer(code); server.start(); serverActive=true; } catch(Exception ex) { serverActive=false; }
        createUi();
        web.loadUrl(HOME);
        handler.postDelayed(this::scanPeriodic,2500);
    }
    int dp(int n) { return (int)(n*getResources().getDisplayMetrics().density+0.5f); }
    TextView label(String txt, int size, int color) {
        TextView x=new TextView(this);
        x.setText(txt); x.setTextSize(size); x.setTextColor(color);
        x.setGravity(Gravity.CENTER_VERTICAL); x.setPadding(dp(10),0,dp(10),0);
        return x;
    }
    Button button(String txt, Runnable task) {
        Button b=new Button(this);
        b.setAllCaps(false); b.setText(txt); b.setTextColor(Color.WHITE);
        b.setOnClickListener(v->task.run());
        return b;
    }
    void createUi() {
        root=new FrameLayout(this); root.setBackgroundColor(0xFF0F0F15); setContentView(root);
        layout=new LinearLayout(this);layout.setOrientation(LinearLayout.VERTICAL);
        root.addView(layout,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout toolbar=new LinearLayout(this); toolbar.setBackgroundColor(0xFF16161E);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        layout.addView(toolbar,new LinearLayout.LayoutParams(-1,dp(54)));
        TextView brand=label("Re;Anime",18,0xFFB799FF);
        brand.setTypeface(null,Typeface.BOLD);
        toolbar.addView(brand,new LinearLayout.LayoutParams(0,-1,1));
        toolbar.addView(button("‹",()->{if(web.canGoBack())web.goBack();}),new LinearLayout.LayoutParams(dp(48),-1));
        toolbar.addView(button("↻",()->web.reload()),new LinearLayout.LayoutParams(dp(48),-1));
        toolbar.addView(button("Cast",this::togglePanel),new LinearLayout.LayoutParams(dp(90),-1));

        web=new WebView(this);
        web.setBackgroundColor(Color.BLACK);
        layout.addView(web,new LinearLayout.LayoutParams(-1,0,1));
        WebSettings s=web.getSettings();
        s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setLoadWithOverviewMode(true);s.setUseWideViewPort(true);
        s.setSupportMultipleWindows(true);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web,true);
        web.setWebViewClient(new WebViewClient(){
            @Override public void onPageFinished(WebView v,String url) {
                setStatus("Website loaded"); scanVideoElements();CookieManager.getInstance().flush();
            }
            @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest req) {
                String u=req.getUrl()==null?"":req.getUrl().toString();
                String kind=detectKind(u);
                if(kind!=null)handler.post(()->collect(u,kind));
                return null;
            }
        });
        web.setWebChromeClient(new WebChromeClient(){
            @Override public void onShowCustomView(View view,CustomViewCallback callback) {
                if(fullScreenView!=null){callback.onCustomViewHidden();return;}
                fullScreenView=view;fullScreenCallback=callback;
                root.addView(view,new FrameLayout.LayoutParams(-1,-1));
                layout.setVisibility(View.GONE);
            }
            @Override public void onHideCustomView() {
                if(fullScreenView==null)return;
                root.removeView(fullScreenView); fullScreenView=null;
                layout.setVisibility(View.VISIBLE);
                if(fullScreenCallback!=null)fullScreenCallback.onCustomViewHidden();
                fullScreenCallback=null;
            }
            @Override public boolean onCreateWindow(WebView v,boolean dialog,boolean gesture,Message msg){
                WebView pop=new WebView(MainActivity.this);
                pop.getSettings().setJavaScriptEnabled(true);
                pop.getSettings().setDomStorageEnabled(true);
                pop.setWebViewClient(new WebViewClient(){
                    @Override public boolean shouldOverrideUrlLoading(WebView p,WebResourceRequest request){
                        if("https".equalsIgnoreCase(request.getUrl().getScheme()))web.loadUrl(request.getUrl().toString());
                        return true;
                    }
                    @Override public void onPageFinished(WebView p,String u){
                        if(u.startsWith("https://"))web.loadUrl(u);
                        p.destroy();
                    }
                });
                ((WebView.WebViewTransport)msg.obj).setWebView(pop);msg.sendToTarget();return true;
            }
        });
        panel=new ScrollView(this);panel.setVisibility(View.GONE);
        layout.addView(panel,new LinearLayout.LayoutParams(-1,dp(265)));
        LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(10),dp(6),dp(10),dp(12));content.setBackgroundColor(0xFF181820);
        panel.addView(content);
        TextView heading=label("PAIR WITH ROKU RECEIVER",12,0xFFB799FF);
        content.addView(heading);
        pairing=label("",14,Color.WHITE);content.addView(pairing);
        count=label("",13,0xFFD1D1D8);content.addView(count);
        status=label("",12,0xFFBBBBCC);content.addView(status);
        LinearLayout r=new LinearLayout(this);content.addView(r);
        r.addView(button("Select video",this::chooseVideo),new LinearLayout.LayoutParams(0,dp(54),1));
        r.addView(button("Select subtitles",this::chooseSubtitle),new LinearLayout.LayoutParams(0,dp(54),1));
        LinearLayout r2=new LinearLayout(this);content.addView(r2);
        r2.addView(button("Test Roku player",this::testVideo),new LinearLayout.LayoutParams(0,dp(54),1));
        r2.addView(button("Enter stream URL",this::enterUrl),new LinearLayout.LayoutParams(0,dp(54),1));
        updateInfo();
    }
    void togglePanel(){panel.setVisibility(panel.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE);updateInfo();}
    void setStatus(String message){if(status!=null)status.setText(message);}
    void updateInfo(){
        if(pairing==null)return;
        pairing.setText("Phone IP: "+localIp()+"   ·   Pair code: "+code);
        count.setText("Detected: "+media.size()+" video streams, "+subtitles.size()+" subtitle tracks");
        if(!serverActive)setStatus("Local pairing server unavailable; restart app");
    }
    String localIp(){
        try{
            Enumeration<NetworkInterface> all=NetworkInterface.getNetworkInterfaces();
            String other=null;
            while(all.hasMoreElements()){
                NetworkInterface nif=all.nextElement();
                if(!nif.isUp()||nif.isLoopback())continue;
                Enumeration<InetAddress> addrs=nif.getInetAddresses();
                while(addrs.hasMoreElements()){
                    InetAddress a=addrs.nextElement();
                    if(!(a instanceof Inet4Address)||a.isLoopbackAddress())continue;
                    if(nif.getName().startsWith("wlan"))return a.getHostAddress();
                    if(a.isSiteLocalAddress())other=a.getHostAddress();
                }
            }
            if(other!=null)return other;
        }catch(Exception ignore){}
        return "Connect phone to Wi-Fi";
    }
    static String detectKind(String url){
        if(url==null||!(url.startsWith("https://")||url.startsWith("http://")))return null;
        try{
            String p=new URI(url).getPath().toLowerCase(Locale.ROOT);
            if(p.endsWith(".m3u8"))return "hls";
            if(p.endsWith(".mp4")||p.endsWith(".m4v"))return "mp4";
            if(p.endsWith(".mpd"))return "dash";
            if(p.endsWith(".vtt")||p.endsWith(".srt")||p.endsWith(".ttml"))return "subtitle";
        }catch(Exception ignore){}
        return null;
    }
    void collect(String url,String type){
        if(url==null||url.length()>4096||detectKind(url)==null)return;
        LinkedHashMap<String,Media> map="subtitle".equals(type)?subtitles:media;
        if(!map.containsKey(url)){
            if(map.size()>=40)map.remove(map.keySet().iterator().next());
            map.put(url,new Media(url,type));updateInfo();
        }
    }
    void scanPeriodic(){
        if(isFinishing()||isDestroyed())return;
        scanVideoElements();handler.postDelayed(this::scanPeriodic,3500);
    }
    void scanVideoElements(){
        if(scanning||web==null)return;scanning=true;
        String js="(function(){var a=[];document.querySelectorAll('video').forEach(function(v){"+
          "[v.currentSrc,v.src].forEach(function(u){if(u)a.push(u);});"+
          "v.querySelectorAll('source,track').forEach(function(e){if(e.src)a.push(e.src);});});"+
          "document.querySelectorAll('track[src]').forEach(function(e){a.push(e.src);});return JSON.stringify(a.slice(0,60));})()";
        web.evaluateJavascript(js,res->{
            scanning=false;
            try{
                String json=(String)new JSONTokener(res).nextValue();
                JSONArray arr=new JSONArray(json);
                for(int i=0;i<arr.length();i++){
                    String url=arr.optString(i);
                    String type=detectKind(url);
                    if(type!=null)collect(url,type);
                }
            }catch(Exception ignore){}
        });
    }
    void chooseVideo(){
        if(media.isEmpty()){
            new AlertDialog.Builder(this).setTitle("No direct video stream detected")
              .setMessage("Start an episode and try again. DRM, blob streams, or cross-origin players may not cast.")
              .setPositiveButton("OK",null).show();return;
        }
        ArrayList<Media> items=new ArrayList<>(media.values());
        Collections.reverse(items);
        String[] options=new String[items.size()];
        for(int i=0;i<items.size();i++)options[i]=items.get(i).label();
        new AlertDialog.Builder(this).setTitle("Cast video to Roku")
          .setItems(options,(d,i)->send(items.get(i)))
          .setNegativeButton("Cancel",null).show();
    }
    void chooseSubtitle(){
        ArrayList<Media> items=new ArrayList<>(subtitles.values());
        String[] options=new String[items.size()+1];options[0]="None / embedded";
        for(int i=0;i<items.size();i++)options[i+1]=items.get(i).label();
        new AlertDialog.Builder(this).setTitle("External subtitle")
          .setItems(options,(d,i)->{
              chosenSubtitle=i==0?"":items.get(i-1).url;
              setStatus(i==0?"No separate subtitle selected":"Subtitle selected");
          }).setNegativeButton("Cancel",null).show();
    }
    void send(Media m){
        if(!serverActive){setStatus("Pairing server unavailable");return;}
        String title=web.getTitle();
        server.publish(m.url,m.type,title==null?"Re;Anime":title,chosenSubtitle);
        setStatus("Sent to Roku receiver · "+m.type.toUpperCase(Locale.ROOT));
    }
    void enterUrl(){
        EditText entry=new EditText(this);entry.setSingleLine(true);
        entry.setHint("https://.../video.m3u8");entry.setInputType(17);
        new AlertDialog.Builder(this).setTitle("Direct stream URL")
          .setView(entry).setPositiveButton("Cast",(d,w)->{
              String u=entry.getText().toString().trim();
              String t=detectKind(u);
              if(t==null||t.equals("subtitle"))setStatus("Enter an HLS, DASH or MP4 URL");
              else send(new Media(u,t));
          }).setNegativeButton("Cancel",null).show();
    }
    void testVideo(){
        send(new Media("https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8","hls"));
    }
    @Override public void onBackPressed(){
        if(fullScreenView!=null){web.getWebChromeClient().onHideCustomView();return;}
        if(web!=null&&web.canGoBack()){web.goBack();return;}
        super.onBackPressed();
    }
    @Override protected void onDestroy(){
        handler.removeCallbacksAndMessages(null);
        if(server!=null)server.stop();
        if(web!=null)web.destroy();
        super.onDestroy();
    }
}
