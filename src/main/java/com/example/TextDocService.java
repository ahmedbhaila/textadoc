package com.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;

public class TextDocService {
	
	private static final String CURRENT_CAMPAIGN = "campaign:current";
	private static final String DOC_KEY = "doc:current";
	private static final String DOC_NOTIFICATION_MESSAGE_TEMPLATE_ID = "6EB29D0028104507";
	private static final String DOC_NOTIFICATION_MESSAGE_CALLBACK_ID = "TourGuideCallback test";
	
	private static final String NAME = "{name}";
	private static final String URL = "{url}";
	private static final String PIN = "{pin}";
	
	@Autowired
	RedisTemplate<String, String> redisTemplate;
	
	@Value("${welcome.message.body}")
	String messageBody;
	
	@Value("${url.message.body}")
	String urlBody;
	
	@Value("${error.message.body}")
	String errorBody;
	
	@Autowired
	WhispirService whispirService;
	
	public void setDocToSend(String url) {
		redisTemplate.opsForValue().set(DOC_KEY, url);
	}
	
	public void sendDocumentNotification(String number, String name, String pin) {
		messageBody = messageBody.replace(NAME, name);
		messageBody = messageBody.replace(PIN, pin);
		
		whispirService.sendSMS(number, DOC_NOTIFICATION_MESSAGE_TEMPLATE_ID, DOC_NOTIFICATION_MESSAGE_CALLBACK_ID, messageBody);
	}
	
	public void setupCampaign(Campaign campaign) {
		
		String campaignName = campaign.getName();
		
		// assuming campaign will be pushed now!
		
		// set current campaign to this one
		redisTemplate.opsForValue().set(CURRENT_CAMPAIGN, campaignName);
		
		// set document url for this campaign
		redisTemplate.opsForValue().set(CURRENT_CAMPAIGN + ":url", campaign.getFileURL());
		
		// generate PIN for each recipient
		campaign.getRecipients().forEach(r -> r.setPin(generatePin()));
		
		// persist recipients info
		campaign.getRecipients().forEach(
				r -> {
					// store all props
					String recipientKey = campaignName + ":" + r.getNumber();
					redisTemplate.opsForHash().put(recipientKey, "name", r.getName());
					redisTemplate.opsForHash().put(recipientKey, "pin", r.getPin());
					redisTemplate.opsForHash().put(recipientKey, "retrieved", "false");
					redisTemplate.opsForHash().put(recipientKey, "sent", "false");
				}
		);
		
		// begin campaign
		beginCampaign(campaign);
		
	}
	
	protected void beginCampaign(Campaign campaign) {
		campaign.getRecipients().forEach(r -> sendDocumentNotification(r.getNumber(), r.getName(), r.getPin()));
	}
	
	protected String generatePin() {
		return String.valueOf((int)(Math.random()*9000)+1000);
	}
	
	public void handleResponse(WhispirCallbackMessage message) {
		String pin = message.getResponseMessage().getContent();
		String phone = message.getSource().getMobile();
		
		// get current campaign
		String currentCampaign = redisTemplate.opsForValue().get(CURRENT_CAMPAIGN);
		
		// check pin for this recipient
		String recipientPin = (String)redisTemplate.opsForHash().get(currentCampaign + ":" + phone, "pin");
		if(recipientPin.equals(pin)) {
			// pin is a match: send document link
			whispirService.sendSMS(phone, "REPLACE_ME", null, messageBody.replace(URL, redisTemplate.opsForValue().get(CURRENT_CAMPAIGN + ":url")));
		}
		else {
			// send error message to this user
			whispirService.sendSMS(phone, "ERROR_MESSAGE_TEMPLATE", null, errorBody);
		}
	}
}