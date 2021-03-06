package com.example;

import java.net.URI;

import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.client.RestTemplate;

import com.dropbox.core.DbxAppInfo;
import com.pubnub.api.Pubnub;

@SpringBootApplication
public class TextADocApplication {

    public static void main(String[] args) {
        SpringApplication.run(TextADocApplication.class, args);
    }
    
    @Value("${dropbox.app.key}")
    String dropboxAppKey;
    
    
    @Value("${dropbox.app.secret}")
    String dropboxAppSecret;
    
    @Value("")
    String redirectUrl;
    
    @Value("${pubnub.subscribe.key}")
	String pubNubSubscribeKey;
	
	@Value("${pubnub.publish.key}")
	String pubNubPublishKey;
    
    
    @Bean
	public JedisConnectionFactory jedisConnectionFactory() throws Exception {
		URI redisUri = new URI(System.getenv("REDISCLOUD_URL"));
		JedisConnectionFactory jedisFactory = new JedisConnectionFactory();
		jedisFactory.setHostName(redisUri.getHost());
		jedisFactory.setPort(redisUri.getPort());
		jedisFactory.setPassword(redisUri.getUserInfo().split(":", 2)[1]);
		return jedisFactory;
	}

	@Bean
	public StringRedisSerializer stringSerializer() {
		return new StringRedisSerializer();
	}

	@Bean
	public RedisTemplate<String, String> redisTemplate() throws Exception {
		RedisTemplate<String, String> redisTemplate = new RedisTemplate<String, String>();
		redisTemplate.setConnectionFactory(jedisConnectionFactory());
		redisTemplate.setKeySerializer(stringSerializer());
		redisTemplate.setHashKeySerializer(stringSerializer());
		redisTemplate.setHashValueSerializer(stringSerializer());
		redisTemplate.setValueSerializer(stringSerializer());
		return redisTemplate;
	}
	
	@Bean
	public TextDocService textDocService() {
		return new TextDocService();
	}
	
	@Bean 
	public WhispirService whispirService() {
		return new WhispirService();
	}
	
	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate(clientFactory());
	}
	
	@Bean
	public HttpComponentsClientHttpRequestFactory clientFactory() {
		HttpComponentsClientHttpRequestFactory http = new HttpComponentsClientHttpRequestFactory();

		http.setHttpClient(HttpClients.createMinimal());
		http.setConnectTimeout(0);
		http.setConnectionRequestTimeout(0);
		http.setReadTimeout(0);
		return http;
	}
	
	@Bean
	public TaskScheduler taskScheduler() {
		return new ThreadPoolTaskScheduler();
	}
	
	@Bean
	public DbxAppInfo dropBoxApp() {
		DbxAppInfo dbxInfo = new DbxAppInfo(dropboxAppKey, dropboxAppSecret);
		return dbxInfo;
	}
	
	@Bean
	public DropboxService dropboxService() {
		return new DropboxService();
	}
	
	@Bean
	public BoxService boxService() {
		return new BoxService();
	}
	
	@Bean
	public PubNubService pubnubService() {
		return new PubNubService();
	}
	
	@Bean
	public Pubnub pubNub() {
		Pubnub pubnub = new Pubnub(pubNubSubscribeKey, pubNubPublishKey);
		return pubnub;
	}
}
