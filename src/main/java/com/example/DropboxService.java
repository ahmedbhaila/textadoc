package com.example;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;

import com.dropbox.core.DbxAppInfo;
import com.dropbox.core.DbxAuthFinish;
import com.dropbox.core.DbxClient;
import com.dropbox.core.DbxEntry;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.DbxSessionStore;
import com.dropbox.core.DbxStandardSessionStore;
import com.dropbox.core.DbxWebAuth;

public class DropboxService {
	@Autowired
	DbxAppInfo dbxInfo;
	
	@Autowired
	HttpSession httpSession;
	
	@Value("${dropbox.redirect.url}")
	String redirectUrl;
	
	@Autowired
	RedisTemplate<String, String> redisTemplate;
	
	protected DbxWebAuth webAuth;
	protected DbxRequestConfig config;
	
	@PostConstruct
	public void init() {
		config = new DbxRequestConfig("textadoc",
				Locale.getDefault().toString());
		String sessionKey = "dropbox-auth-csrf-token";
		DbxSessionStore csrfTokenStore = new DbxStandardSessionStore(httpSession, sessionKey);
		
		webAuth = new DbxWebAuth(config, dbxInfo, redirectUrl, csrfTokenStore);
	}
	
	public String getAuthUrl() {
		return webAuth.start();
	}
	
	public void handleCallback(Map<String, String> paramMap) throws Exception {
		// I wish I could use "fancy" lambdas ops here
		
		Map<String, String[]> params = new HashMap<String, String[]>();
		paramMap.forEach((k,v) -> {
			params.put(k, new String[]{v});
		});
		
		DbxAuthFinish webAuthFinish = webAuth.finish(params);
		redisTemplate.opsForValue().set("dropbox:token", webAuthFinish.accessToken);
	}
	
	public List<String> getListings() throws Exception {
		String token = redisTemplate.opsForValue().get("dropbox:token");
		DbxClient dbxClient = new DbxClient(config, token);
		DbxEntry.WithChildren listing = dbxClient.getMetadataWithChildren("/", false);
		List<String> files = new ArrayList<String>();
		for (DbxEntry child : listing.children) {
			files.add(child.name);
		}
		return files;
	}
	
	public String getFileLink(String file) throws Exception {
		String token = redisTemplate.opsForValue().get("dropbox:token");
		DbxClient dbxClient = new DbxClient(config, token);
		return dbxClient.createShareableUrl("/" + file);
	}
	
	public void downloadFile(String file) throws Exception {
		String token = redisTemplate.opsForValue().get("dropbox:token");
		DbxClient dbxClient = new DbxClient(config, token);
		
		FileOutputStream outputStream = new FileOutputStream(file);
        try {
            DbxEntry.File downloadedFile = dbxClient.getFile("/" + file, null,
                outputStream);
            //System.out.println("Metadata: " + downloadedFile.toString());
        } 
        catch(Exception e) {
        	System.err.println(e.getMessage());
        }
        finally {
        	outputStream.close();
        }
	}
}
