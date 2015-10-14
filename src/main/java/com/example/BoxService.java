package com.example;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;

import com.box.sdk.BoxAPIConnection;
import com.box.sdk.BoxFile;
import com.box.sdk.BoxFolder;
import com.box.sdk.BoxItem;
import com.box.sdk.BoxSharedLink;
import com.box.sdk.BoxSharedLink.Access;

class BoxFileData {
	String name;
	String downloadLink;
	public BoxFileData(String name, String downloadLink) {
		this.downloadLink = downloadLink;
		this.name = name;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDownloadLink() {
		return downloadLink;
	}
	public void setDownloadLink(String downloadLink) {
		this.downloadLink = downloadLink;
	}
}
public class BoxService {
	
	@Value("${box.oauth.url}")
	String authUrl;
	
	@Value("${box.client.id}")
	String clientId;
	
	@Value("${box.client.secret}")
	String clientSecret;
	
	@Autowired
	RedisTemplate<String, String> redisTemplate;
	
	BoxAPIConnection boxApi;
	
	
	public String getAuthUrl() {
		return authUrl;
	}
	
	public void handleCallback(String code, String state) {
		boxApi = new BoxAPIConnection(clientId,clientSecret,code);
		redisTemplate.opsForValue().set("box:code", code);
	}
	
	public List<BoxFileData> getFiles() {
		String boxCode = (String)redisTemplate.opsForValue().get("box:code");
		//boxApi = new BoxAPIConnection(clientId,clientSecret,boxCode);
		List<BoxFileData> files = new ArrayList<BoxFileData>();
		
		// get root folder
		BoxFolder rootFolder = BoxFolder.getRootFolder(boxApi);
		
		// get file names under this folder
		rootFolder.getID();
		for (BoxItem.Info itemInfo : rootFolder) {
		    if (itemInfo instanceof BoxFile.Info) {
		        BoxFile.Info fileInfo = (BoxFile.Info) itemInfo;
		        
		        if(fileInfo.getSharedLink() == null) {
		        	BoxSharedLink boxSharedLink = new BoxSharedLink();
		        	boxSharedLink.setAccess(Access.OPEN);
		        	
		        	fileInfo.setSharedLink(boxSharedLink);
		        	
		        }
		        
		        files.add(new BoxFileData(fileInfo.getName(), fileInfo.getSharedLink().getDownloadURL()));
		    }
		}
		return files;
	}
}
