package com.example.systemparam.domain;

import java.util.List;

public class TagGroupDto {
    private String tagCode;
    private String tagName;
    private String description;
    private Integer priority;
    private List<SystemParamDto> params;

    public String getTagCode() {
        return tagCode;
    }

    public void setTagCode(String tagCode) {
        this.tagCode = tagCode;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public List<SystemParamDto> getParams() {
        return params;
    }

    public void setParams(List<SystemParamDto> params) {
        this.params = params;
    }
}
