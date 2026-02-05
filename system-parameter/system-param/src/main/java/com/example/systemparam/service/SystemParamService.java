package com.example.systemparam.service;
import com.example.systemparam.domain.ParamDataType;
import com.example.systemparam.domain.TagGroupViewDto;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface SystemParamService {

    String get(String key);

    Optional<String> getOptional(String key);

    String getOrDefault(String key, String defaultValue);

    <T> T getAs(String key, Class<T> targetType);

    <T> T getAsOrDefault(String key, Class<T> targetType, T defaultValue);

    Duration getDuration(String key);

    Duration getDurationOrDefault(String key, Duration defaultValue);

    <E extends Enum<E>> E getEnum(String key, Class<E> enumType);

    <E extends Enum<E>> E getEnumOrDefault(String key, Class<E> enumType, E defaultValue);

    <T> List<T> getList(String key, Class<T> elementType);

    <T> List<T> getListOrDefault(String key, Class<T> elementType, List<T> defaultValue);

    Map<String, Object> getMap(String key);

    Map<String, Object> getMapOrDefault(String key, Map<String, Object> defaultValue);

    List<Map<String, Object>> getMapList(String key);

    List<Map<String, Object>> getMapListOrDefault(
            String key, List<Map<String, Object>> defaultValue);

    List<TagGroupViewDto> getAllGroupedByTag();

    TagGroupViewDto getByTag(String tagCode);

    void set(
            String key,
            String value,
            ParamDataType type,
            String tagCode,
            Integer displayPriority,
            String description);

    void set(
            String key,
            Object value,
            ParamDataType type,
            String tagCode,
            Integer displayPriority,
            String description);


    void update(String key, String value);

    void update(String key, Object value);

    void ensureTag(String code, String name, String description, Integer priority);

    void ensureParam(
            String key,
            String value,
            String description,
            ParamDataType type,
            String tagCode,
            Integer displayPriority
    );

    void ensureParam(
            String key,
            Object value,
            String description,
            ParamDataType type,
            String tagCode,
            Integer displayPriority
    );
}
