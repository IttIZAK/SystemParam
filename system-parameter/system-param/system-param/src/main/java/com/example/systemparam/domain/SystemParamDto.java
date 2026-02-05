package com.example.systemparam.domain;

public class SystemParamDto {
    private String key;
    private String value;
    private String description;
    private ParamDataType type;
    private String tagCode;
    private Integer displayPriority;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ParamDataType getType() {
        return type;
    }

    public void setType(ParamDataType type) {
        this.type = type;
    }

    public String getTagCode() {
        return tagCode;
    }

    public void setTagCode(String tagCode) {
        this.tagCode = tagCode;
    }

    public Integer getDisplayPriority() {
        return displayPriority;
    }

    public void setDisplayPriority(Integer displayPriority) {
        this.displayPriority = displayPriority;
    }

}
