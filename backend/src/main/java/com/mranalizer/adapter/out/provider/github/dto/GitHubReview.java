package com.mranalizer.adapter.out.provider.github.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubReview {

    private User user;
    private String state;

    @JsonProperty("submitted_at")
    private ZonedDateTime submittedAt;

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public ZonedDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(ZonedDateTime submittedAt) { this.submittedAt = submittedAt; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class User {
        private String login;

        public String getLogin() { return login; }
        public void setLogin(String login) { this.login = login; }
    }
}
