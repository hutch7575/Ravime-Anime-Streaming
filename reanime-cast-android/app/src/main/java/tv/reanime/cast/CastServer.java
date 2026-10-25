package tv.reanime.cast;

import android.webkit.CookieManager;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;
import org.json.JSONObject;

/**
 * Pairing server and optional on-phone HLS relay.
 * Keeps website cookies on the phone; Roku only sees an authenticated LAN playlist.
 * Does not decrypt DRM or transcode incompatible codecs.
 */
final class CastServer {
    static final int PORT = 9867;
    private final String code;
    private final ExecutorService workers = Executors.newFixedThreadPool(12);
    private final SecureRandom rng = new SecureRandom();
    private final ConcurrentHashMap<String,Resource> media = new ConcurrentHashMap<>();
    private volatile JSONObject session = new JSONObject();
    private volatile String relayStatus = "Idle";
    private volatile boolean running;
    private ServerSocket listener;

    static final class Resource {
        final String url, referer, origin, userAgent;
        Resource(String u, String r, String o, String ua) {
            url=u; referer=r; origin=o; userAgent=ua;
        }
    }

    CastServer(String pairingCode) { code=pairingCode; }

    void start() throws IOException {
        listener = new ServerSocket(PORT, 24, InetAddress.getByName("0.0.0.0"));
        running = true;
        workers.execute(() -> {
            while (running) {
                try {
                    Socket incoming=listener.accept();
                    workers.execute(() -> handle(incoming));
                } catch (IOException ex) {
                    if(!running) break;
                }
            }
        });
    }

    void stop() {
        running=false;
        try { if(listener!=null) listener.close(); } catch(IOException ignored) {}
        workers.shutdownNow();
    }

    void publish(String url, String format, String title, String subtitle,
                 String referer, String origin, String userAgent, String phoneIp) {
        hostIp=phoneIp;
        media.clear();
        relayStatus="Waiting for Roku";
        boolean relay = "hls".equalsIgnoreCase(format)
                        && phoneIp.matches("[0-9.]+")
                        && validRemote(url);
        JSONObject obj=new JSONObject();
        try {
            String playUrl=url;
            if(relay) {
                String id=register(new Resource(url,referer,origin,userAgent));
                playUrl="http://"+phoneIp+":"+PORT+"/stream/"+id+"?code="+code;
            }
            obj.put("id",Long.toString(System.currentTimeMillis()));
            obj.put("url",playUrl);
            obj.put("format",format);
            obj.put("title",title);
            obj.put("subtitle",subtitle==null?"":subtitle);
            obj.put("relay",relay);
            // Do NOT forward browser cookies, bearer tokens, or upstream request
            // headers to Roku. The phone will attach them to its own requests.
            obj.put("headers",new JSONObject());
        } catch(Exception ex) { relayStatus="Failed to prepare stream"; }
        session=obj;
    }

    private String register(Resource r) {
        byte[] bytes=new byte[15];
        rng.nextBytes(bytes);
        String id=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        media.put(id,r);
        return id;
    }

    private boolean validRemote(String text) {
        try {
            URI u=new URI(text);
            String scheme=u.getScheme(), host=u.getHost();
            if(host==null || scheme==null) return false;
            if(!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) return false;
            String h=host.toLowerCase(Locale.ROOT);
            if(h.equals("localhost") || h.endsWith(".local") || h.equals("127.0.0.1") ||
               h.startsWith("192.168.") || h.startsWith("10.") ||
               h.equals("0.0.0.0") || h.equals("::1")) return false;
            return true;
        } catch(Exception ex) { return false; }
    }

    private void handle(Socket client) {
        try(Socket sock=client) {
            sock.setSoTimeout(9000);
            BufferedReader in=new BufferedReader(new InputStreamReader(sock.getInputStream(),StandardCharsets.US_ASCII));
            String line=in.readLine();
            if(line==null || line.length()>4096) return;
            String[] request=line.split(" ");
            if(request.length<2 || !"GET".equals(request[0])) {
                respond(sock,405,"text/plain","GET only"); return;
            }
            URI path=new URI(request[1]);
            String range="";
            int read=0;
            while((line=in.readLine())!=null && !line.isEmpty()) {
                read+=line.length();
                if(read>16000)return;
                if(line.toLowerCase(Locale.ROOT).startsWith("range:"))
                    range=line.substring(6).trim();
            }
            String secret="";
            if(path.getRawQuery()!=null) {
                for(String field:path.getRawQuery().split("&")) {
                    if(field.startsWith("code="))secret=URLDecoder.decode(field.substring(5),"UTF-8");
                }
            }
            if(!code.equals(secret)) {
                respond(sock,403,"text/plain","Not paired");return;
            }
            if("/session".equals(path.getPath())) {
                JSONObject out=new JSONObject(session.toString());
                out.put("relayStatus",relayStatus);
                respond(sock,200,"application/json; charset=utf-8",out.toString());
                return;
            }
            if(path.getPath().startsWith("/stream/")) {
                String id=path.getPath().substring("/stream/".length());
                Resource r=media.get(id);
                if(r==null) {respond(sock,404,"text/plain","Expired media link");return;}
                proxy(sock,r,range);
                return;
            }
            respond(sock,404,"text/plain","Unknown path");
        } catch(Exception ex) { relayStatus="Phone relay error: "+ex.getClass().getSimpleName(); }
    }

    private void proxy(Socket sock,Resource r,String range) throws IOException {
        HttpURLConnection upstream=null;
        try {
            if(!validRemote(r.url)) {respond(sock,400,"text/plain","Unsupported URL");return;}
            upstream=(HttpURLConnection)new URL(r.url).openConnection();
            upstream.setConnectTimeout(7000);
            upstream.setReadTimeout(12000);
            upstream.setInstanceFollowRedirects(true);
            upstream.setRequestProperty("Accept-Encoding","identity");
            if(!r.referer.isEmpty())upstream.setRequestProperty("Referer",r.referer);
            if(!r.origin.isEmpty())upstream.setRequestProperty("Origin",r.origin);
            if(!r.userAgent.isEmpty())upstream.setRequestProperty("User-Agent",r.userAgent);
            if(range.startsWith("bytes=") && range.length()<120)
                upstream.setRequestProperty("Range",range);
            // Cookies remain on the phone and never reach the Roku.
            String cookies=CookieManager.getInstance().getCookie(r.url);
            if(cookies!=null && !cookies.isEmpty())upstream.setRequestProperty("Cookie",cookies);
            int status=upstream.getResponseCode();
            if(status<200 || status>=300) {
                relayStatus="Host HTTP "+status+" ("+safeHost(r.url)+")";
                respond(sock,502,"text/plain","Upstream HTTP "+status);
                return;
            }
            String finalUrl=upstream.getURL().toString();
            String mime=upstream.getContentType();
            if(mime==null)mime="application/octet-stream";
            boolean hls=r.url.toLowerCase(Locale.ROOT).contains(".m3u8")
                || mime.toLowerCase(Locale.ROOT).contains("mpegurl");
            if(hls) {
                byte[] raw=readLimited(upstream.getInputStream(),2*1024*1024);
                String playlist=new String(raw,StandardCharsets.UTF_8);
                if(!playlist.trim().startsWith("#EXTM3U")) {
                    relayStatus="Host returned non-HLS response ("+safeHost(r.url)+")";
                    respond(sock,502,"text/plain","Upstream was not an HLS playlist");
                    return;
                }
                String processed=rewriteHls(playlist,finalUrl,r);
                relayStatus="HLS playlist delivered from phone";
                respond(sock,200,"application/vnd.apple.mpegurl",processed);
            } else {
                long length=upstream.getContentLengthLong();
                OutputStream out=sock.getOutputStream();
                // Use HTTP/1.0 close framing when the host does not provide length.
                StringBuilder head=new StringBuilder("HTTP/1.0 ");
                head.append(status==206?"206 Partial Content":"200 OK").append("\r\n");
                head.append("Content-Type: ").append(mime.replace("\r","").replace("\n","")).append("\r\n");
                if(length>=0)head.append("Content-Length: ").append(length).append("\r\n");
                String contentRange=upstream.getHeaderField("Content-Range");
                if(contentRange!=null && contentRange.length()<150)
                    head.append("Content-Range: ").append(contentRange).append("\r\n");
                head.append("Access-Control-Allow-Origin: *\r\n");
                head.append("Connection: close\r\n\r\n");
                out.write(head.toString().getBytes(StandardCharsets.US_ASCII));
                try(InputStream input=upstream.getInputStream()) {
                    byte[] buffer=new byte[32768];
                    int n;
                    while((n=input.read(buffer))!=-1) out.write(buffer,0,n);
                }
                out.flush();
                relayStatus="Media segment delivered";
            }
        } catch(Exception ex) {
            relayStatus="Relay request failed: "+ex.getClass().getSimpleName();
            throw new IOException(ex);
        } finally {
            if(upstream!=null)upstream.disconnect();
        }
    }

    private String safeHost(String link) {
        try {return new URI(link).getHost();} catch(Exception ex) {return "media host";}
    }
    private byte[] readLimited(InputStream input,int max) throws IOException {
        try(InputStream in=input;ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            byte[] buf=new byte[8192];int len,total=0;
            while((len=in.read(buf))!=-1) {
                total+=len;
                if(total>max) throw new IOException("Playlist too large");
                out.write(buf,0,len);
            }
            return out.toByteArray();
        }
    }
    private String rewriteHls(String body,String playlistUrl,Resource source) throws IOException {
        URI base=URI.create(playlistUrl);
        StringBuilder out=new StringBuilder(body.length()+4096);
        // Applies to HLS tag URI="..." attributes (keys, init segments, audio,
        // subtitles) as well as standalone playlist/segment URI lines.
        Pattern attribute=Pattern.compile("URI=\"([^\"]+)\"");
        String[] lines=body.replace("\r\n","\n").split("\n",-1);
        for(String line:lines) {
            if(line.startsWith("#")) {
                Matcher matcher=attribute.matcher(line);
                StringBuffer changed=new StringBuffer();
                while(matcher.find()) {
                    String relay=relayUri(base,matcher.group(1),source);
                    matcher.appendReplacement(changed,Matcher.quoteReplacement("URI=\""+relay+"\""));
                }
                matcher.appendTail(changed);
                out.append(changed);
            } else if(!line.trim().isEmpty()) {
                out.append(relayUri(base,line.trim(),source));
            } else out.append(line);
            out.append("\n");
        }
        return out.toString();
    }
    private String relayUri(URI base,String raw,Resource origin) {
        try {
            String resolved=base.resolve(raw).toString();
            if(!validRemote(resolved))return raw;
            String id=register(new Resource(resolved,origin.referer,origin.origin,origin.userAgent));
            // The Roku fetches this relay URL; it does not see the upstream token.
            return "http://"+sessionIp()+":"+PORT+"/stream/"+id+"?code="+code;
        } catch(Exception ex) {return raw;}
    }
    private volatile String hostIp="127.0.0.1";
    private String sessionIp(){return hostIp;}
    private void respond(Socket sock,int status,String mime,String body) throws IOException {
        byte[] data=body.getBytes(StandardCharsets.UTF_8);
        String reason=status==200?"OK":status==502?"Bad Gateway":status==403?"Forbidden":"Error";
        String headers="HTTP/1.1 "+status+" "+reason+"\r\n"+
            "Content-Type: "+mime+"\r\n"+
            "Content-Length: "+data.length+"\r\n"+
            "Cache-Control: no-store\r\n"+
            "Connection: close\r\n\r\n";
        OutputStream out=sock.getOutputStream();
        out.write(headers.getBytes(StandardCharsets.US_ASCII));
        out.write(data);
        out.flush();
    }
}