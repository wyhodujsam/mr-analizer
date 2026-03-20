package com.mranalizer.adapter.out.provider.github.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubFile {

    @JsonProperty("filename")
    private String filename;

    @JsonProperty("additions")
    private int additions;

    @JsonProperty("deletions")
    private int deletions;

    @JsonProperty("status")
    private String status;

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public int getAdditions() { return additions; }
    public void setAdditions(int additions) { this.additions = additions; }

    public int getDeletions() { return deletions; }
    public void setDeletions(int deletions) { this.deletions = deletions; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
