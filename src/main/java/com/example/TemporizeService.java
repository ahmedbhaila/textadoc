package com.example;

import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class TemporizeService {
	
	@Value("${temporize.username}")
	String username;
	
	@Value("${temporize.password}")
	String password;
	
	@Autowired
	RestTemplate restTemplate;
	
	private static final String TEMPORIZE_API = "https://api.temporize.net/v1/events/{date}/{url}";
	
	
	public void schedule(String dateTime, String url) {
		String plainCreds = username + ":" + password;
		byte[] plainCredsBytes = plainCreds.getBytes();
		byte[] base64CredsBytes = Base64.encodeBase64(plainCredsBytes);
		String base64Creds = new String(base64CredsBytes);
		
		HttpHeaders headers = new HttpHeaders();
		headers.add("Authorization", "Basic " + base64Creds);
		
		HttpEntity<String> request = new HttpEntity<String>(headers);
		ResponseEntity<String> response = restTemplate.exchange(TEMPORIZE_API, HttpMethod.POST, request, String.class, dateTime, url);
	
	}
}
