package com.example;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;


@Controller
public class TextADocController {
	@Autowired
	TextDocService textDocService;
	
	@Autowired
	WhispirService whispirService;
	
	@RequestMapping("/textadoc/recipients")
	public List<Recipient> getRecipients() {
		return whispirService.getRecipients();
	}
	
	@RequestMapping(value = "/textadoc/campaign", method = RequestMethod.POST)
	public void setupCampaign(@RequestBody Campaign campaign) {
		textDocService.setupCampaign(campaign);
	}
	
	// for testing only
	@RequestMapping("/textadoc/campaign")
	@ResponseBody
	public Campaign getCampaign() {
		return whispirService.getCampaign();
	}
	@RequestMapping("/textadoc/notification")
	public String sendTextNotification() {
		//textDocService.sendDocumentNotification(number, name);
		return "true";
	}
	
	@RequestMapping(value="/handleCallback", method = RequestMethod.GET)
	@ResponseBody
	public void handleCallback(@RequestParam(required=false, defaultValue="false", value="auth") String auth) {
		System.out.println("call backed");
	}
	
	@RequestMapping(value="/handleCallback", method = RequestMethod.POST)
	@ResponseBody
	// @RequestBody WhispirCallbackMessage message
	public void handleCallback(@RequestBody WhispirCallbackMessage message, @RequestParam(required=false, defaultValue="false", value="auth") String auth) {
		//System.out.println("call backed" + message.toString());
		//rabbitTemplate.convertAndSend("WhispirMessageQueue", message);
	}
}