package com.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.jayway.jsonpath.JsonPath;



public class WhispirService {
	
	@Value("${whispir.api.key}")
	String apiKey;
	
	@Value("${whispir.auth.user}")
	String authUser;
	
	@Value("${whispir.message.template.name}")
	String messageTemplateName;
	
	
	@Autowired
	RestTemplate restTemplate;
	
	private static final String WHISPIR_API_URL = "https://api.whispir.com/messages?apikey={api_key}";
	private static final String WHISPIR_API_CONTACTS_URL = "https://api.whispir.com/contacts?apikey={api_key}&fields=firstName,lastName,workMobilePhone1";	
	
	public Campaign getCampaign() {
		// for testing : dummy recipient
		Recipient r = new Recipient();
		r.setName("Ahmed");
		r.setNumber("17732304340");

		Campaign c = new Campaign();
		c.setFileURL("http://www.simpsoncrazy.com/content/characters/poster/full.jpg");
		c.setName("TestCampaign");
		c.setRecipients(Arrays.asList(new Recipient[] { r }));
		
		return c;
				
	}
	public List<Recipient> getRecipients() {
		
		HttpHeaders headers = new HttpHeaders();
		headers.set("Content-Type", "application/vnd.whispir.contact-v1+json");
		headers.set("Accept", "application/vnd.whispir.contact-v1+json");
		headers.set("Authorization", "Basic " + authUser);
		
		
		HttpEntity<String> entity = new HttpEntity<String>(null, headers);
		
		ResponseEntity<String> response = restTemplate.exchange(WHISPIR_API_CONTACTS_URL, HttpMethod.GET, entity, String.class, apiKey);

		
		List<Recipient> recps = new ArrayList<Recipient>();
		
		if(response.getStatusCode().equals(HttpStatus.OK)){
			Recipient r = new Recipient();
			String responseBody = response.getBody();
			r.setName((String)((JSONArray)JsonPath.read(responseBody, "$..firstName")).get(0) + " "
					+ (String)((JSONArray)JsonPath.read(responseBody, "$..lastName")).get(0));
			r.setNumber((String)((JSONArray)JsonPath.read(responseBody, "$..workMobilePhone1")).get(0));
			
			recps.add(r);
			
			System.out.println("Whispir response is " + response.getBody());
		}
		
		return recps;
	}
	
	public void sendVoiceCall(String phoneNumber, String callbackId) {
		
		HttpHeaders headers = new HttpHeaders();
		headers.set("Content-Type", "application/vnd.whispir.message-v1+json");
		headers.set("Authorization", "Basic " + authUser);
		
		JSONObject request = new JSONObject();
		request.put("to", phoneNumber);
		request.put("messageTemplateName", messageTemplateName);
		JSONObject voiceRequest = new JSONObject();
		
		voiceRequest.put("header", "Text a Document");
		voiceRequest.put("type", "ConfCall:,ConfAccountNo:,ConfPinNo:,ConfModPinNo:,Pin:");
		
		voiceRequest.put("body", "You have been selected to recieve a file");
		if(callbackId != null) {
			request.put("callbackId", callbackId);
		}

		request.put("voice", voiceRequest);
		HttpEntity<String> entity = new HttpEntity<String>(request.toJSONString(), headers);

		ResponseEntity<String> response = restTemplate.exchange(WHISPIR_API_URL, HttpMethod.POST, entity, String.class, apiKey);
		System.out.println("Whispir response is " + response.getBody());
		
	}
	
	public void sendSMS(String phoneNumber, String templateId, String callbackId, String body) {
		HttpHeaders headers = new HttpHeaders();
		headers.set("Content-Type", "application/vnd.whispir.message-v1+json");
		headers.set("Authorization", "Basic " + authUser);
		
		JSONObject request = new JSONObject();
		request.put("to", phoneNumber);
		request.put("messageTemplateId", templateId);
		request.put("body", body);
		if(callbackId != null) {
			request.put("callbackId", callbackId);
		}

		HttpEntity<String> entity = new HttpEntity<String>(request.toJSONString(), headers);

		ResponseEntity<String> response = restTemplate.exchange(WHISPIR_API_URL, HttpMethod.POST, entity, String.class, apiKey);
		System.out.println("Whispir response is " + response.getBody());
	}
	
	public void handleMessage(WhispirCallbackMessage message) {
		String content = message.getResponseMessage().getContent();
		if(content.equals("1")) {
			// user wants to subscribe to alerts
		}
	}
	
	

	/*
	public void sendVoiceCall(String phoneNumber, List<CityMonument> monuments) {
		
		HttpHeaders headers = new HttpHeaders();
		headers.set("Content-Type", "application/vnd.whispir.message-v1+json");
		headers.set("Authorization", "Basic " + authUser);
		
		JSONObject request = new JSONObject();
		request.put("to", phoneNumber);
		request.put("messageTemplateName", messageTemplateName);
		JSONObject voiceRequest = new JSONObject();
		
		if(monuments.size() == 1) {
			voiceRequest.put("header", "We found a place that might interest you");
		}
		else {
			voiceRequest.put("header", "We found places that might interest you");	
		}
		voiceRequest.put("type", "ConfCall:,ConfAccountNo:,ConfPinNo:,ConfModPinNo:,Pin:");
		String body = "";
		for (CityMonument cityMonument : monuments) {
			body += cityMonument.getName()  + " is located at " + cityMonument.getAddress() + cityMonument.getAbout();
		}
		
		voiceRequest.put("body", body);

		request.put("voice", voiceRequest);
		HttpEntity<String> entity = new HttpEntity<String>(request.toJSONString(), headers);

		ResponseEntity<String> response = restTemplate.exchange(WHISPIR_API_URL, HttpMethod.POST, entity, String.class, apiKey);
		System.out.println("Whispir response is " + response.getBody());
		
	}
	public void sendSMS(String phoneNumber, String templateId, String callbackId, List<CityMonument> monuments) {
		HttpHeaders headers = new HttpHeaders();
		headers.set("Content-Type", "application/vnd.whispir.message-v1+json");
		headers.set("Authorization", "Basic " + authUser);
		
		JSONObject request = new JSONObject();
		request.put("to", phoneNumber);
		request.put("messageTemplateId", templateId);
		
		String body = "";
		for (CityMonument cityMonument : monuments) {
			body += cityMonument.getName()  + " is located at " + cityMonument.getAddress() + cityMonument.getAbout();
		}
		
		request.put("body", body);
		if(callbackId != null) {
			request.put("callbackId", callbackId);
		}

		HttpEntity<String> entity = new HttpEntity<String>(request.toJSONString(), headers);

		ResponseEntity<String> response = restTemplate.exchange(WHISPIR_API_URL, HttpMethod.POST, entity, String.class, apiKey);
		System.out.println("Whispir response is " + response.getBody());
	}*/
	
}
