package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.TextView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.os.AsyncTask;
import java.net.*;
import java.io.*;
import com.example.myapplication.databinding.ActivityMainBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import retrofit2.Call;
import retrofit2.http.GET;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class MainActivity extends AppCompatActivity {

    static {
        System.loadLibrary("native-lib");
        System.loadLibrary("node");
    }
    public static boolean _startedNodeAlready=false;
    private ActivityMainBinding binding;
    private Socket mSocket;

    private MessageAdapter messageAdapter;
    private RecyclerView messagesView;
    private List<Message> messages = new ArrayList<>();
    private Server selectedServer;

    private WebView myWebView;

    Client[] clients = new Client[128];
    
    private ServerAdapter serverAdapter;
    private EditText searchEditText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        try
        {
            this.getSupportActionBar().hide();
        }
        catch (NullPointerException e){}

        setContentView(R.layout.activity_main);

        //starts node js
        if( !_startedNodeAlready ) {
            _startedNodeAlready=true;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    String nodeDir=getApplicationContext().getFilesDir().getAbsolutePath()+"/nodejs-project";
                    if (wasAPKUpdated()) {
                        File nodeDirReference=new File(nodeDir);
                        if (nodeDirReference.exists()) {
                            deleteFolderRecursively(new File(nodeDir));
                        }
                        copyAssetFolder(getApplicationContext().getAssets(), "nodejs-project", nodeDir);
                        saveLastUpdateTime();
                    }
                    startNodeWithArguments(new String[]{"node",
                            nodeDir+"/main.js"
                    });
                }
            }).start();
        }

        try {
            mSocket = IO.socket("http://localhost:3000");
            mSocket.connect();
            mSocket.on("getMasters", onGetMasters);
            mSocket.on("chatMessage", onNewMessage);
            mSocket.on("snapshot", onSnapshot);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        //Search EditText
        searchEditText = findViewById(R.id.searchEditText);
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (serverAdapter != null) {
                    serverAdapter.filter(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        //Connect button
        final Button buttonVersions = (Button) findViewById(R.id.btVersions);
        buttonVersions.setOnClickListener(v -> {
            if (selectedServer != null) {
                JSONObject communicationData = new JSONObject();
                try {
                    communicationData.put("address", selectedServer.getAddresses());
                    mSocket.emit("communication", communicationData);
                    switchToIngameView();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    JSONObject getMasters = new JSONObject();
                    getMasters.put("getMasters", "getMasters");
                    mSocket.emit("communication", getMasters);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void switchToIngameView() {
        setContentView(R.layout.ingame);

        messageAdapter = new MessageAdapter(this, messages);
        messagesView = findViewById(R.id.chatRecyclerView);
        messagesView.setLayoutManager(new LinearLayoutManager(this));
        messagesView.setAdapter(messageAdapter);

        //Send Message button
        final Button sendMessageButton = (Button) findViewById(R.id.sendButton);
        sendMessageButton.setOnClickListener(v -> {
            EditText editText = findViewById(R.id.chatEditText);
            String message = editText.getText().toString();

            try {
                JSONObject sendMessage = new JSONObject();
                sendMessage.put("sendMessage", message);
                mSocket.emit("communication", sendMessage);
                editText.setText("");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });

        // webview
        myWebView = (WebView) findViewById(R.id.webview);
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        WebView.setWebContentsDebuggingEnabled(true);
        myWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                myWebView.evaluateJavascript(javascript.init, null);
            }
        });

        // Используем название карты из выбранного сервера
        String mapName = "HDP_Obstaculos"; // Значение по умолчанию
        if (selectedServer != null && selectedServer.getInfo() != null && 
            selectedServer.getInfo().getMap() != null && 
            selectedServer.getInfo().getMap().getName() != null) {
            mapName = selectedServer.getInfo().getMap().getName();
        }
        
        myWebView.loadUrl("https://ddnet.org/mappreview/?map=" + mapName);
    }

    private Emitter.Listener onSnapshot = args -> runOnUiThread(() -> {
        try {
            myWebView.evaluateJavascript("updateSnapshot("+args[0]+")",null);
        } catch (Exception e) {
        }
    });

    private Emitter.Listener onNewMessage = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(() -> {
                JSONObject data = (JSONObject) args[0];
                try {
                    String messageText = data.getString("message");
                    String authorName = data.optJSONObject("author").optJSONObject("ClientInfo").optString("name", "Unknown");
                    Message message = new Message(messageText, authorName);
                    messages.add(message);
                    messageAdapter.notifyItemInserted(messages.size() - 1);
                    messagesView.scrollToPosition(messages.size() - 1);
                } catch (Exception e) {
                }
            });
        }
    };

    private Emitter.Listener onGetMasters = args -> runOnUiThread(new Runnable() {
        @Override
        public void run() {
            try {
                Comparator<Server> serverComparator = (s1, s2) -> {
                    int clientsCount1 = (s1.getInfo().getClients() != null) ? s1.getInfo().getClients().size() : 0;
                    int clientsCount2 = (s2.getInfo().getClients() != null) ? s2.getInfo().getClients().size() : 0;
                    return Integer.compare(clientsCount2, clientsCount1);
                };

                PriorityQueue<Server> serverQueue = new PriorityQueue<>(serverComparator);
                JSONObject jsonObject =  (JSONObject) args[0];
                JSONArray serversArray = jsonObject.getJSONArray("servers");

                for (int i = 0; i < serversArray.length(); i++) {
                    JSONObject serverObj = serversArray.getJSONObject(i);
                    Server server = new Server();
                    server.setAddresses(getListFromStringJSONArray(serverObj.getJSONArray("addresses")));
                    
                    // Парсим локацию
                    if (serverObj.has("location")) {
                        server.setLocation(serverObj.getString("location"));
                    }
                    
                    server.setInfo(parseServerInfo(serverObj.getJSONObject("info")));
                    serverQueue.add(server);
                }

                List<Server> sortedServers = new ArrayList<>();
                while (!serverQueue.isEmpty()) {
                    sortedServers.add(serverQueue.poll());
                }
                setupRecyclerView(sortedServers);
            } catch (Exception e) {
                String asd = e.toString();
            }
        }
    });

    private void setupRecyclerView(List<Server> servers) {
        RecyclerView recyclerView = findViewById(R.id.masterServers);
        serverAdapter = new ServerAdapter(servers, new OnItemClickListener() {
            @Override
            public void onItemClick(Server server) {
                selectedServer = server;
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(serverAdapter);
    }

    private List<String> getListFromStringJSONArray(JSONArray jsonArray) throws JSONException {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            list.add(jsonArray.getString(i));
        }
        return list;
    }

    private Server.ServerInfo parseServerInfo(JSONObject infoObj) throws JSONException {
        Server.ServerInfo info = new Server.ServerInfo();
        info.setName(infoObj.getString("name"));
        
        if (infoObj.has("game_type")) {
            info.setGame_type(infoObj.getString("game_type"));
        }
        
        if (infoObj.has("max_clients")) {
            info.setMax_clients(infoObj.getInt("max_clients"));
        }
        
        if (infoObj.has("max_players")) {
            info.setMax_players(infoObj.getInt("max_players"));
        }
        
        // Парсим карту
        if (infoObj.has("map")) {
            JSONObject mapObj = infoObj.getJSONObject("map");
            Server.Map map = new Server.Map();
            if (mapObj.has("name")) {
                map.setName(mapObj.getString("name"));
            }
            info.setMap(map);
        }
        
        // Парсим список игроков
        if (infoObj.has("clients")) {
            JSONArray clientsArray = infoObj.getJSONArray("clients");
            List<Server.Client> clients = new ArrayList<>();
            for (int i = 0; i < clientsArray.length(); i++) {
                JSONObject clientObj = clientsArray.getJSONObject(i);
                Server.Client client = new Server.Client();
                if (clientObj.has("name")) {
                    client.setName(clientObj.getString("name"));
                }
                clients.add(client);
            }
            info.setClients(clients);
        }
        
        return info;
    }

    //methods for node js
    public native Integer startNodeWithArguments(String[] arguments);
    
    private boolean wasAPKUpdated() {
        SharedPreferences prefs = getApplicationContext().getSharedPreferences("NODEJS_MOBILE_PREFS", Context.MODE_PRIVATE);
        long previousLastUpdateTime = prefs.getLong("NODEJS_MOBILE_APK_LastUpdateTime", 0);
        long lastUpdateTime = 1;
        try {
            PackageInfo packageInfo = getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0);
            lastUpdateTime = packageInfo.lastUpdateTime;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return (lastUpdateTime != previousLastUpdateTime);
    }

    private void saveLastUpdateTime() {
        long lastUpdateTime = 1;
        try {
            PackageInfo packageInfo = getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0);
            lastUpdateTime = packageInfo.lastUpdateTime;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        SharedPreferences prefs = getApplicationContext().getSharedPreferences("NODEJS_MOBILE_PREFS", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("NODEJS_MOBILE_APK_LastUpdateTime", lastUpdateTime);
        editor.commit();
    }
    
    private static boolean deleteFolderRecursively(File file) {
        try {
            boolean res=true;
            for (File childFile : file.listFiles()) {
                if (childFile.isDirectory()) {
                    res &= deleteFolderRecursively(childFile);
                } else {
                    res &= childFile.delete();
                }
            }
            res &= file.delete();
            return res;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    private static boolean copyAssetFolder(AssetManager assetManager, String fromAssetPath, String toPath) {
        try {
            String[] files = assetManager.list(fromAssetPath);
            boolean res = true;

            if (files.length==0) {
                res &= copyAsset(assetManager, fromAssetPath, toPath);
            } else {
                new File(toPath).mkdirs();
                for (String file : files)
                    res &= copyAssetFolder(assetManager, fromAssetPath + "/" + file, toPath + "/" + file);
            }
            return res;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    private static boolean copyAsset(AssetManager assetManager, String fromAssetPath, String toPath) {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = assetManager.open(fromAssetPath);
            new File(toPath).createNewFile();
            out = new FileOutputStream(toPath);
            copyFile(in, out);
            in.close();
            in = null;
            out.flush();
            out.close();
            out = null;
            return true;
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }
}