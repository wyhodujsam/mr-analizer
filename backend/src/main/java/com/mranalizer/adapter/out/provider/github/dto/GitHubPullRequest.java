package com.mranalizer.adapter.out.provider.github.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubPullRequest {

    @JsonProperty("number")
    private int number;

    @JsonProperty("title")
    private String title;

    @JsonProperty("body")
    private String body;

    @JsonProperty("user")
    private User user;

    @JsonProperty("head")
    private Ref head;

    @JsonProperty("base")
    private Ref base;

    @JsonProperty("state")
    private String state;

    @JsonProperty("merged_at")
    private ZonedDateTime mergedAt;

    @JsonProperty("created_at")
    private ZonedDateTime createdAt;

    @JsonProperty("updated_at")
    private ZonedDateTime updatedAt;

    @JsonProperty("labels")
    private List<Label> labels;

    @JsonProperty("html_url")
    private String htmlUrl;

    @JsonProperty("additions")
    private int additions;

    @JsonProperty("deletions")
    private int deletions;

    @JsonProperty("changed_files")
    private int changedFilesCount;

    public int getNumber() { return number; }
    public void setNumber(int number) { this.number = number; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Ref getHead() { return head; }
    public void setHead(Ref head) { this.head = head; }

    public Ref getBase() { return base; }
    public void setBase(Ref base) { this.base = base; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public ZonedDateTime getMergedAt() { return mergedAt; }
    public void setMergedAt(ZonedDateTime mergedAt) { this.mergedAt = mergedAt; }

    public ZonedDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }

    public ZonedDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(ZonedDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<Label> getLabels() { return labels; }
    public void setLabels(List<Label> labels) { this.labels = labels; }

    public String getHtmlUrl() { return htmlUrl; }
    public void setHtmlUrl(String htmlUrl) { this.htmlUrl = htmlUrl; }

    public int getAdditions() { return additions; }
    public void setAdditions(int additions) { this.additions = additions; }

    public int getDeletions() { return deletions; }
    public void setDeletions(int deletions) { this.deletions = deletions; }

    public int getChangedFilesCount() { return changedFilesCount; }
    public void setChangedFilesCount(int changedFilesCount) { this.changedFilesCount = changedFilesCount; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class User {
        @JsonProperty("login")
        private String login;

        public String getLogin() { return login; }
        public void setLogin(String login) { this.login = login; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Ref {
        @JsonProperty("ref")
        private String ref;

        public String getRef() { return ref; }
        public void setRef(String ref) { this.ref = ref; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Label {
        @JsonProperty("name")
        private String name;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}
