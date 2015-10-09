package com.example;

public class Recipient {
	protected String name;
	protected String number;
	protected boolean sent;
	protected boolean retrieved;
	protected String retrievalDate;
	protected String pin;
	
	public boolean isSent() {
		return sent;
	}
	public void setSent(boolean sent) {
		this.sent = sent;
	}
	public boolean isRetrieved() {
		return retrieved;
	}
	public void setRetrieved(boolean retrieved) {
		this.retrieved = retrieved;
	}
	public String getRetrievalDate() {
		return retrievalDate;
	}
	public void setRetrievalDate(String retrievalDate) {
		this.retrievalDate = retrievalDate;
	}
	public String getPin() {
		return pin;
	}
	public void setPin(String pin) {
		this.pin = pin;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getNumber() {
		return number;
	}
	public void setNumber(String number) {
		this.number = number;
	}
}
