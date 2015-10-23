package com.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.TaskScheduler;

public class TextDocService {
	
	private static final String CURRENT_CAMPAIGN = "campaign:current";
	private static final String DOC_KEY = "doc:current";
	private static final String DOC_NOTIFICATION_MESSAGE_TEMPLATE_ID = "6EB29D0028104507";
	
	//private static final String DOC_NOTIFICATION_MESSAGE_CALLBACK_ID = "TourGuideCallback test";
	private static final String DOC_NOTIFICATION_MESSAGE_CALLBACK_ID = "Share a Doc - local 2";
	
	private static final String CAMPAIGN_NAME_CHANNEL = "current_campaign";
	
	
	private static final String NAME = "{name}";
	private static final String URL = "{url}";
	private static final String PIN = "{pin}";
	private static final String CAMPAIGN = "{campaign}";
	private static final String FILENAME = "{filename}";
	
	@Autowired
	RedisTemplate<String, String> redisTemplate;
	
	@Value("${welcome.message.body}")
	String messageBody;
	
	@Value("${url.message.body}")
	String urlBody;
	
	@Value("${error.message.body}")
	String errorBody;
	
	@Value("${schedule.service.url}")
	String serviceUrl;
	
	@Value("${host.url}")
	String hostUrl;
	
	@Value("${whispir.email.subject}")
	String emailSubject;
	
	@Autowired
	WhispirService whispirService;
	
	@Autowired
	TaskScheduler scheduler;
	
	@Autowired
	DropboxService dropboxService;
	
	@Autowired
	PubNubService pubnubService;
	
	
	public void setDocToSend(String url) {
		redisTemplate.opsForValue().set(DOC_KEY, url);
	}
	
	public void sendDocumentNotification(String campaign, String number, String name, String pin, String notificationType)   {
		if(notificationType.equals("SMS")) {
			String message = messageBody.replace(NAME, name);
			message = messageBody.replace(PIN, pin);
			whispirService.sendSMS(number, DOC_NOTIFICATION_MESSAGE_TEMPLATE_ID, DOC_NOTIFICATION_MESSAGE_CALLBACK_ID, message);
		}
		else {
			whispirService.sendVoiceCall(number, DOC_NOTIFICATION_MESSAGE_CALLBACK_ID);
		}
		
		// increment score for sent
		redisTemplate.opsForValue().increment(campaign + ":sent", 1);
		
		try{
			// send pubnub notification
			JSONObject val = new JSONObject();
			val.put("total_sent", String.valueOf(redisTemplate.opsForValue().get(campaign + ":sent")));
			pubnubService.publishMessage(val, "total_sent");
		}
		catch(Exception e) {
			System.err.println(e.getMessage());
		}
		
	}
	
	public void setupCampaign(Campaign campaign) throws Exception {
		
		// delete old score
		redisTemplate.delete(campaign.getName() + ":sent");
		redisTemplate.delete(campaign.getName() + ":downloaded");
		
		String campaignName = campaign.getName();
		
		// set document url for this campaign
		
		//redisTemplate.opsForValue().set(campaignName + ":url", dropboxService.getFileLink(campaign.getFileURL()));
		String url = hostUrl.replace(FILENAME,campaign.getFileURL());
		url = url.replace(CAMPAIGN, campaign.getName());
		
		redisTemplate.opsForValue().set(campaignName + ":url", url);
		redisTemplate.opsForHash().put(campaignName, "email", campaign.getEmail());
		
		// generate PIN for each recipient
		campaign.getRecipients().forEach(r -> r.setPin(generatePin()));
		
		// persist recipients info
		campaign.getRecipients().forEach(
				r -> {
					// store all props
					String recipientKey = campaignName + ":recipient:" + r.getNumber();
					redisTemplate.opsForHash().put(recipientKey, "name", r.getName());
					redisTemplate.opsForHash().put(recipientKey, "pin", r.getPin());
					redisTemplate.opsForHash().put(recipientKey, "retrieved", "false");
					redisTemplate.opsForHash().put(recipientKey, "sent", "false");
					redisTemplate.opsForHash().put(recipientKey, "phone", r.getNumber());
					redisTemplate.opsForHash().put(recipientKey, "notification", r.getNotification());
				}
		);
		
		// Schedule if available
		if(campaign.getStartDate() != null && !campaign.getStartDate().equals("")) {
			try{
				//tempService.schedule(campaign.getStartDate(), URLEncoder.encode(serviceUrl.replace(CAMPAIGN, campaignName), "UTF-8"));
				//UriUtils.encodeHttpUrl(serviceUrl.replace(CAMPAIGN, campaignName), "UTF-8");
				//tempService.schedule(campaign.getStartDate(), serviceUrl.replace(CAMPAIGN, campaignName));
				scheduleTask(campaign.getStartDate(), campaign.getName());

			}
			catch(Exception e) {
				System.err.println(e.getMessage());
			}
		}
		else {
			
			// set current campaign to this one
			redisTemplate.opsForValue().set(CURRENT_CAMPAIGN, campaignName);
			
			// begin campaign
			beginCampaign(campaign);
			
		}
		
	}
	
	protected void beginCampaign(Campaign campaign) throws Exception {
		// notify new campaign
		JSONObject campaignName = new JSONObject();
		campaignName.put("current_campaign", campaign.getName());
		pubnubService.publishMessage(campaignName, CAMPAIGN_NAME_CHANNEL);
		campaign.getRecipients().forEach(r -> sendDocumentNotification(campaign.getName(), r.getNumber(), r.getName(), r.getPin(), r.getNotification()));
		
		// mark sent to true
		String currentCampaign = (String) redisTemplate.opsForValue().get(CURRENT_CAMPAIGN);
		campaign.getRecipients().forEach(r -> redisTemplate.opsForHash().put(currentCampaign + ":" + r.getNumber(), "sent", "true"));
		
		// send email to admin
		whispirService.sendSentMail(campaign.getEmail(), emailSubject.replace(CAMPAIGN, currentCampaign));
	}
	
	public void beginCampaign(String campaign) throws Exception {
		// set current campaign to this one
		redisTemplate.opsForValue().set(CURRENT_CAMPAIGN, campaign);
		
		beginCampaign(getCampaign(campaign));
	}
	
	private Campaign getCampaign(String campaign) {
		Campaign c = new Campaign();
		c.setFileURL((String)redisTemplate.opsForValue().get(campaign + ":url"));
		c.setName(campaign);
		
		// get recipients
		List<Recipient> recipients = new ArrayList<Recipient>();
		Set<String> recipientKeys = redisTemplate.keys(campaign + ":recipient:*");
		recipientKeys.forEach(k -> {
			Recipient recp = new Recipient();
			
			recp.setNumber((String) redisTemplate.opsForHash().get(k, "phone"));
			recp.setName((String)redisTemplate.opsForHash().get(k, "name"));
			recp.setPin((String)redisTemplate.opsForHash().get(k, "pin"));
			recp.setRetrieved(Boolean.valueOf((String)redisTemplate.opsForHash().get(k, "retrieved")));
			recp.setSent(Boolean.valueOf((String)redisTemplate.opsForHash().get(k, "sent")));
			
			recipients.add(recp);
			
			
		});
		c.setRecipients(recipients);
		return c;
	}
	
	protected String generatePin() {
		return String.valueOf((int)(Math.random()*9000)+1000);
	}
	
	public void handleResponse(WhispirCallbackMessage message) {
		String pin = message.getResponseMessage().getContent();
		String phone = message.getSource().getMobile();
		
		// get current campaign
		String currentCampaign = redisTemplate.opsForValue().get(CURRENT_CAMPAIGN);
		
		// check for voice call 
		if(message.getResponseMessage().getChannel().equals("Voice")){
			// check for response == 2
			if(pin.equals("2")) {
				// pin is a match: send document link
				whispirService.sendSMS(phone, DOC_NOTIFICATION_MESSAGE_TEMPLATE_ID, null, 
						urlBody.replace(URL, redisTemplate.opsForValue().get(currentCampaign + ":url")));
			}
			else {
				// send error message to this user
				whispirService.sendSMS(phone, DOC_NOTIFICATION_MESSAGE_TEMPLATE_ID, null, errorBody);
			}
		}
		else{
			// this is a text message source
			// check pin for this recipient
			String recipientPin = (String)redisTemplate.opsForHash().get(currentCampaign + ":recipient:" + phone, "pin");
			if(recipientPin.equals(pin)) {
				
				// pin is a match: send document link
				whispirService.sendSMS(phone, DOC_NOTIFICATION_MESSAGE_TEMPLATE_ID, null, 
						urlBody.replace(URL, redisTemplate.opsForValue().get(currentCampaign + ":url")));
			}
			else {
				// send error message to this user
				whispirService.sendSMS(phone, DOC_NOTIFICATION_MESSAGE_TEMPLATE_ID, null, errorBody);
			}
		}
		
		
		
		
	}
	
	public void scheduleTask(String date, String campaign) {
		DateTime dateTime = ISODateTimeFormat.dateTimeParser().parseDateTime(date);

		scheduler.schedule(new Runnable() {
			
			@Override
			public void run() {
				System.out.println("I was called"); // for testing, remove when done
				try{
					beginCampaign(campaign);
				}
				catch(Exception e) {
					System.err.println(e.getMessage());
				}
			}
		}, dateTime.toDate());
	}
	
	public void downloadFile(String campaign, String file) throws Exception {
		dropboxService.downloadFile(file);
		
		// increment score for sent
		redisTemplate.opsForValue().increment(campaign + ":downloaded", 1);
		
		// send pubnub notification
		JSONObject val = new JSONObject();
		int downloaded = Integer.valueOf(redisTemplate.opsForValue().get(campaign + ":downloaded"));
		val.put("total_downloaded", downloaded);
		pubnubService.publishMessage(val, "total_downloaded");
		
		// calculate downloaded so far
		int total = redisTemplate.keys(campaign + ":recipient:*").size();
		double percent = downloaded/total * 100;
		percent = percent > 100.0 ? percent = 100.0 : percent;
		
		if(percent == 100.0) {
			// all recipients have downloaded files. Send email to admin
			whispirService.sendDownloadedMail((String)redisTemplate.opsForHash().get(campaign, "email"), emailSubject.replace(CAMPAIGN, campaign));
		}
		
		JSONObject eon = new JSONObject();
		val = new JSONObject();
		val.put("data", percent);
		eon.put("eon", val);		
		pubnubService.publishMessage(eon, "total_downloaded_percent");
		
	}
	
	public String getDropboxAuthUrl() {
		return dropboxService.getAuthUrl();
	}
}
