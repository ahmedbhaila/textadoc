package com.example;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.dropbox.core.DbxAppInfo;
import com.dropbox.core.DbxAuthFinish;
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
	
	protected DbxWebAuth webAuth;
	
	@PostConstruct
	public void init() {
		DbxRequestConfig config = new DbxRequestConfig("textadoc",
				Locale.getDefault().toString());
		String sessionKey = "dropbox-auth-csrf-token";
		DbxSessionStore csrfTokenStore = new DbxStandardSessionStore(httpSession, sessionKey);
		
		webAuth = new DbxWebAuth(config, dbxInfo, redirectUrl, csrfTokenStore);
	}
	
	public String getAuthUrl() {
		return webAuth.start();
	}
	
	public String handleCallback(Map<String, String> paramMap) throws Exception {
		// I wish I could use "fancy" lambdas ops here
		
		Map<String, String[]> params = new HashMap<String, String[]>();
		paramMap.forEach((k,v) -> {
			params.put(k, new String[]{v});
		});
		
		DbxAuthFinish webAuthFinish = webAuth.finish(params);
		return webAuthFinish.accessToken;
	}
}
