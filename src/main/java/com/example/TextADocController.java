package com.example;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;


@Controller
public class TextADocController {
	@Autowired
	TextDocService textDocService;
	
	@Autowired
	WhispirService whispirService;
	
	@Autowired
	DropboxService dropBox;
	
	@Autowired
	BoxService boxService;
	
	@Autowired
	PubNubService pubnubService;
	
	@RequestMapping("/textadoc/recipients")
	@ResponseBody
	public List<Recipient> getRecipients() {
		return whispirService.getRecipients();
	}
	
	@RequestMapping(value = "/textadoc/campaign", method = RequestMethod.POST)
	@ResponseBody
	public void setupCampaign(@RequestBody Campaign campaign) throws Exception {
		textDocService.setupCampaign(campaign);
	}
	
	@RequestMapping(value = "/textadoc/campaign/{campaign}/begin")
	@ResponseStatus(value = HttpStatus.OK)
	public void beginCampaign(@PathVariable("campaign") String campaign) throws Exception {
		textDocService.beginCampaign(campaign);
	}
	
	// for testing only
	@RequestMapping("/textadoc/campaign")
	@ResponseBody
	public Campaign getCampaign() {
		//textDocService.scheduleTask("2015-10-12T02:25:00Z");
		return whispirService.getCampaign();
	}
	
	// for testing only
	@RequestMapping("/textadoc/sched/{date_time}/{url}")
	@ResponseBody
	public String schedule(@PathVariable("date_time") String dateTime, @PathVariable("url") String url) throws Exception {
		return "true";
	}
	
	@RequestMapping("/textadoc/dropbox")
	@ResponseBody
	public String dropbox() {
		return dropBox.getAuthUrl();
	}
	
	
	@RequestMapping("/textadoc/notification")
	public String sendTextNotification() {
		//textDocService.sendDocumentNotification(number, name);
		return "true";
	}
	
	@RequestMapping(value="/textadoc/whispir/callback", method = RequestMethod.GET)
	@ResponseBody
	public void handleCallback(@RequestParam(required=false, defaultValue="false", value="auth") String auth) {
		System.out.println("call backed");
	}
	
	@RequestMapping(value="/textadoc/whispir/callback", method = RequestMethod.POST)
	@ResponseBody
	// @RequestBody WhispirCallbackMessage message
	public void handleCallback(@RequestBody WhispirCallbackMessage message, @RequestParam(required=false, defaultValue="false", value="auth") String auth) {
		//System.out.println("call backed" + message.toString());
		//rabbitTemplate.convertAndSend("WhispirMessageQueue", message);
		textDocService.handleResponse(message);
		
	}
	
	@RequestMapping("/textadoc/dropbox/auth")
	//@ResponseBody
	public String dropboxAuth() {
		return "redirect:" + textDocService.getDropboxAuthUrl();
	}
	
	@RequestMapping(value="/textadoc/dropbox/callback")
	public String handleDropboxCallback(@RequestParam Map<String,String> allRequestParams) throws Exception {
		//allRequestParams.forEach((k,v) -> System.out.println("k" + k + ":" + v));
		dropBox.handleCallback(allRequestParams);
		return "redirect:/admin.html";
		//return dropBox.getListings();
	}
	
	@RequestMapping(value="/textadoc/dropbox/files")
	@ResponseBody
	public List<String> handleDropboxCallback() throws Exception {
		return dropBox.getListings();
	}
	
	@RequestMapping(value="/textadoc/box/callback")
	@ResponseBody
	public void handleBoxCallback(@RequestParam Map<String,String> allRequestParams) throws Exception {
		//allRequestParams.forEach((k,v) -> System.out.println("k" + k + ":" + v));
		boxService.handleCallback(allRequestParams.get("code"), allRequestParams.get("state"));
		//return dropBox.getListings();
	}
	
	@RequestMapping(value = "/textadoc/box/files")
	@ResponseBody
	public List<BoxFileData> getFiles(){
		return boxService.getFiles();
	}
	
	@RequestMapping("/textadoc/{campaign}/file/{file:.+}")
	@ResponseBody
	public FileSystemResource getFile(@PathVariable("campaign") String campaign, @PathVariable("file") String file) throws Exception {
		textDocService.downloadFile(campaign, file);
		return new FileSystemResource(new File(file));
	}
	
	@RequestMapping("/textadoc/voice")
	@ResponseBody
	public String testVoiceCall() {
		whispirService.sendVoiceCall("17732304340", "TextADoc local");
		return "true";
	}
	
	@RequestMapping("/textadoc/updateticker") 
	@ResponseBody
	public String updateTicker() throws Exception {
		JSONObject val = new JSONObject();
		val.put("total_sent", "1");
		pubnubService.publishMessage(val, "total_sent");
		
		val = new JSONObject();
		val.put("total_downloaded", "1");
		pubnubService.publishMessage(val, "total_downloaded");
		
		JSONObject eon = new JSONObject();
		
		JSONObject val1 = new JSONObject();
		val1.put("data", 80);
		eon.put("eon", val1);
		
		
		pubnubService.publishMessage(eon, "total_downloaded_percent");
		
		return "true";
	}
	
	@RequestMapping("/textadoc/email")
	@ResponseBody
	public String sendEmail(@RequestParam("email") String email) {
		whispirService.sendSentMail(email, "Test Me");
		return "true";
	}
	
}
