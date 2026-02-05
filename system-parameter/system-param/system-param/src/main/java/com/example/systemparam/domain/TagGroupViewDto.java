package com.example.systemparam.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class TagGroupViewDto {

    private String tagCode;
    private String tagName;
    private String description;
    private Integer priority;

    private List<SystemParamDto> params = new ArrayList<>();

    public String getTagCode() { return tagCode; }
    public void setTagCode(String tagCode) { this.tagCode = tagCode; }

    public String getTagName() { return tagName; }
    public void setTagName(String tagName) { this.tagName = tagName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }

    public List<SystemParamDto> getParams() {
        return params;
    }

    public void setParams(List<SystemParamDto> params) {
        if (params == null) {
            this.params = new ArrayList<>();
        } else {
            this.params = new ArrayList<>(params);
        }
    }

    public void addParam(SystemParamDto param) {
        if (param != null) {
            this.params.add(param);
        }
    }

    public List<SystemParamDto> getParamsReadOnly() {
        return Collections.unmodifiableList(params);
    }

    @Override
    public String toString() {
        return "TagGroupViewDto{" +
                "tagCode='" + tagCode + '\'' +
                ", tagName='" + tagName + '\'' +
                ", priority=" + priority +
                ", params=" + params.size() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TagGroupViewDto that)) return false;
        return Objects.equals(tagCode, that.tagCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tagCode);
    }
}
