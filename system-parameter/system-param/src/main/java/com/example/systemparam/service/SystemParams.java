package com.example.systemparam.service;

import com.example.systemparam.converter.ParamTypeConverter;
import com.example.systemparam.domain.ParamDataType;
import com.example.systemparam.domain.SystemParamDto;
import com.example.systemparam.domain.TagGroupDto;
import com.example.systemparam.domain.TagGroupViewDto;
import com.example.systemparam.exception.ParamNotFoundException;
import com.example.systemparam.exception.ParamTypeMismatchException;
import com.example.systemparam.port.SystemParamRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.math.BigDecimal;
import java.util.*;

public final class SystemParams implements SystemParamService {

    private static final String UNGROUPED = "UNGROUPED";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final SystemParamRepository repository;

    public SystemParams(SystemParamRepository repository) {
        if (repository == null) throw new IllegalArgumentException("repository is required");
        this.repository = repository;
    }

    @Override
    public String get(String key) {
        SystemParamDto param = repository.findByKey(key);
        if (param == null) throw new ParamNotFoundException(key);
        return param.getValue();
    }

    @Override
    public Optional<String> getOptional(String key) {
        SystemParamDto param = repository.findByKey(key);
        if (param == null) return Optional.empty();
        return Optional.ofNullable(param.getValue());
    }

    @Override
    public String getOrDefault(String key, String defaultValue) {
        return getOptional(key).orElse(defaultValue);
    }

    @Override
    public <T> T getAs(String key, Class<T> targetType) {
        SystemParamDto param = repository.findByKey(key);
        if (param == null) throw new ParamNotFoundException(key);
        return ParamTypeConverter.convert(param, targetType);
    }

    @Override
    public <T> T getAsOrDefault(String key, Class<T> targetType, T defaultValue) {
        try {
            return getAs(key, targetType);
        } catch (ParamNotFoundException | ParamTypeMismatchException e) {
            return defaultValue;
        }
    }

    @Override
    public Duration getDuration(String key) {
        return getAs(key, Duration.class);
    }

    @Override
    public Duration getDurationOrDefault(String key, Duration defaultValue) {
        try {
            return getDuration(key);
        } catch (ParamNotFoundException | ParamTypeMismatchException e) {
            return defaultValue;
        }
    }

    @Override
    public <E extends Enum<E>> E getEnum(String key, Class<E> enumType) {
        SystemParamDto param = repository.findByKey(key);
        if (param == null) throw new ParamNotFoundException(key);
        return ParamTypeConverter.convertEnum(param, enumType);
    }

    @Override
    public <E extends Enum<E>> E getEnumOrDefault(String key, Class<E> enumType, E defaultValue) {
        try {
            return getEnum(key, enumType);
        } catch (ParamNotFoundException | ParamTypeMismatchException e) {
            return defaultValue;
        }
    }

    @Override
    public <T> List<T> getList(String key, Class<T> elementType) {
        SystemParamDto param = repository.findByKey(key);
        if (param == null) throw new ParamNotFoundException(key);
        return ParamTypeConverter.convertList(param, elementType);
    }

    @Override
    public <T> List<T> getListOrDefault(String key, Class<T> elementType, List<T> defaultValue) {
        try {
            return getList(key, elementType);
        } catch (ParamNotFoundException | ParamTypeMismatchException e) {
            return defaultValue;
        }
    }

    @Override
    public Map<String, Object> getMap(String key) {
        return ParamTypeConverter.toMap(key, get(key));
    }

    @Override
    public Map<String, Object> getMapOrDefault(String key, Map<String, Object> defaultValue) {
        try {
            return getMap(key);
        } catch (ParamNotFoundException | ParamTypeMismatchException e) {
            return defaultValue;
        }
    }

    @Override
    public List<Map<String, Object>> getMapList(String key) {
        return ParamTypeConverter.toMapList(key, get(key));
    }

    @Override
    public List<Map<String, Object>> getMapListOrDefault(String key, List<Map<String, Object>> defaultValue) {
        try {
            return getMapList(key);
        } catch (ParamNotFoundException | ParamTypeMismatchException e) {
            return defaultValue;
        }
    }

    @Override
    public List<TagGroupViewDto> getAllGroupedByTag() {
        List<SystemParamDto> paramList = repository.findAllParams();
        List<TagGroupDto> tagMetadata = repository.findAllTags();

        LinkedHashMap<String, TagGroupViewDto> groups = new LinkedHashMap<>();

        if (tagMetadata != null) {
            for (TagGroupDto meta : tagMetadata) {
                if (meta == null) continue;

                String code = meta.getTagCode();
                if (code == null || code.isBlank()) continue;

                TagGroupViewDto g = new TagGroupViewDto();
                g.setTagCode(code);
                g.setTagName(meta.getTagName() == null ? code : meta.getTagName());
                g.setDescription(meta.getDescription());
                g.setPriority(meta.getPriority());
                g.setParams(new ArrayList<>());
                groups.put(code, g);
            }
        }

        if (paramList != null) {
            for (SystemParamDto p : paramList) {
                if (p == null) continue;

                String tagCode = normalizeTagCode(p.getTagCode());
                TagGroupViewDto g = groups.get(tagCode);

                if (g == null) {
                    g = new TagGroupViewDto();
                    g.setTagCode(tagCode);
                    g.setTagName(tagCode);
                    g.setDescription(null);
                    g.setPriority(Integer.MAX_VALUE);
                    g.setParams(new ArrayList<>());
                    groups.put(tagCode, g);
                }

                g.getParams().add(p);
            }
        }

        ArrayList<TagGroupViewDto> out = new ArrayList<>(groups.values());
        out.sort(Comparator.comparingInt(g -> g.getPriority() == null ? Integer.MAX_VALUE : g.getPriority()));

        for (TagGroupViewDto g : out) {
            List<SystemParamDto> params = g.getParams();
            if (params == null) continue;

            params.sort(
                    Comparator
                            .comparingInt((SystemParamDto p) ->
                                    p.getDisplayPriority() == null ? Integer.MAX_VALUE : p.getDisplayPriority()
                            )
                            .thenComparing(SystemParamDto::getKey, Comparator.nullsLast(String::compareTo))
            );
        }

        return out;
    }

    @Override
    public TagGroupViewDto getByTag(String tagCode) {
        String normalized = normalizeTagCode(tagCode);

        List<SystemParamDto> paramList = repository.findAllParams();
        List<TagGroupDto> tagMetadata = repository.findAllTags();

        TagGroupViewDto view = null;

        if (tagMetadata != null) {
            for (TagGroupDto meta : tagMetadata) {
                if (meta == null) continue;
                if (safeEquals(normalizeTagCode(meta.getTagCode()), normalized)) {
                    view = new TagGroupViewDto();
                    view.setTagCode(normalized);
                    view.setTagName(meta.getTagName() == null ? normalized : meta.getTagName());
                    view.setDescription(meta.getDescription());
                    view.setPriority(meta.getPriority());
                    view.setParams(new ArrayList<>());
                    break;
                }
            }
        }

        ArrayList<SystemParamDto> collected = new ArrayList<>();
        if (paramList != null) {
            for (SystemParamDto p : paramList) {
                if (p == null) continue;
                if (safeEquals(normalizeTagCode(p.getTagCode()), normalized)) {
                    collected.add(p);
                }
            }
        }

        if (view == null && collected.isEmpty()) {
            return null;
        }

        if (view == null) {
            view = new TagGroupViewDto();
            view.setTagCode(normalized);
            view.setTagName(normalized);
            view.setDescription(null);
            view.setPriority(Integer.MAX_VALUE);
            view.setParams(new ArrayList<>());
        }

        collected.sort(
                Comparator
                        .comparingInt((SystemParamDto p) ->
                                p.getDisplayPriority() == null ? Integer.MAX_VALUE : p.getDisplayPriority()
                        )
                        .thenComparing(SystemParamDto::getKey, Comparator.nullsLast(String::compareTo))
        );

        view.setParams(collected);
        return view;
    }

    @Override
    public void set(
            String key,
            String value,
            ParamDataType type,
            String tagCode,
            Integer displayPriority,
            String description
    ) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("key required");
        if (type == null) throw new IllegalArgumentException("type required");

        String normalizedTag = normalizeTagCode(tagCode);

        SystemParamDto dto = repository.findByKey(key);
        if (dto == null) {
            dto = new SystemParamDto();
            dto.setKey(key);
        }

        String normalizedValue = normalizeValue(value, type);
        validateWrite(key, normalizedValue, type);

        dto.setValue(normalizedValue);
        dto.setType(type);
        dto.setTagCode(normalizedTag);
        dto.setDisplayPriority(displayPriority);
        dto.setDescription(description);

        repository.save(dto);
    }


    @Override
    public void set(
            String key,
            Object value,
            ParamDataType type,
            String tagCode,
            Integer displayPriority,
            String description
    ) {
        set(key, normalizeValue(value, type), type, tagCode, displayPriority, description);
    }

    @Override
    public void update(String key, Object value) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("key required");

        SystemParamDto dto = repository.findByKey(key);
        if (dto == null) throw new ParamNotFoundException(key);

        ParamDataType type = dto.getType() == null ? ParamDataType.TEXT : dto.getType();
        String normalizedValue = normalizeValue(value, type);
        validateWrite(key, normalizedValue, type);

        dto.setValue(normalizedValue);
        repository.save(dto);
    }

    @Override
    public void ensureParam(
            String key,
            Object value,
            String description,
            ParamDataType type,
            String tagCode,
            Integer displayPriority
    ) {
        ensureParam(key, normalizeValue(value, type), description, type, tagCode, displayPriority);
    }


    @Override
    public void update(String key, String value) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("key required");

        SystemParamDto dto = repository.findByKey(key);
        if (dto == null) throw new ParamNotFoundException(key);

        ParamDataType type = dto.getType() == null ? ParamDataType.TEXT : dto.getType();

        String normalizedValue = normalizeValue(value, type);
        validateWrite(key, normalizedValue, type);

        dto.setValue(normalizedValue);
        repository.save(dto);
    }

    @Override
    public void ensureTag(String code, String name, String description, Integer priority) {
        String normalized = normalizeTagCode(code);
        if (normalized == null || normalized.isBlank()) throw new IllegalArgumentException("tagCode required");

        List<TagGroupDto> all = repository.findAllTags();
        TagGroupDto existing = null;

        if (all != null) {
            for (TagGroupDto t : all) {
                if (t == null) continue;
                if (safeEquals(normalizeTagCode(t.getTagCode()), normalized)) {
                    existing = t;
                    break;
                }
            }
        }

        if (existing == null) {
            TagGroupDto created = new TagGroupDto();
            created.setTagCode(normalized);
            created.setTagName(name);
            created.setDescription(description);
            created.setPriority(priority);
            repository.save(created);
            return;
        }

        boolean changed = false;

        if (!safeEquals(existing.getTagName(), name)) { existing.setTagName(name); changed = true; }
        if (!safeEquals(existing.getDescription(), description)) { existing.setDescription(description); changed = true; }
        if (!safeEquals(existing.getPriority(), priority)) { existing.setPriority(priority); changed = true; }

        if (changed) repository.save(existing);
    }

    @Override
    public void ensureParam(
            String key,
            String value,
            String description,
            ParamDataType type,
            String tagCode,
            Integer displayPriority
    ) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("key required");
        if (type == null) throw new IllegalArgumentException("type required");

        String normalizedTag = normalizeTagCode(tagCode);
        SystemParamDto existing = repository.findByKey(key);

        if (existing == null) {
            SystemParamDto created = new SystemParamDto();
            created.setKey(key);
            String normalizedValue = normalizeValue(value, type);
            validateWrite(key, normalizedValue, type);
            created.setValue(normalizedValue);
            created.setDescription(description);
            created.setType(type);
            created.setTagCode(normalizedTag);
            created.setDisplayPriority(displayPriority);
            repository.save(created);
            return;
        }

        boolean changed = false;

        if (!safeEquals(existing.getDescription(), description)) { existing.setDescription(description); changed = true; }
        if (existing.getType() != type) {
            String existingValue = existing.getValue();
            String normalizedExistingValue = normalizeValue(existingValue, type);
            validateWrite(key, normalizedExistingValue, type);
            existing.setValue(normalizedExistingValue);
            existing.setType(type);
            changed = true;
        }
        if (!safeEquals(normalizeTagCode(existing.getTagCode()), normalizedTag)) { existing.setTagCode(normalizedTag); changed = true; }
        if (!safeEquals(existing.getDisplayPriority(), displayPriority)) { existing.setDisplayPriority(displayPriority); changed = true; }

        if (changed) repository.save(existing);
    }


    private static String normalizeValue(Object value, ParamDataType type) {
        if (value == null) return null;
        if (value instanceof String s) return s;

        if (type == null) return String.valueOf(value);

        return switch (type) {
            case TEXT -> String.valueOf(value);

            case NUMBER -> {
                if (value instanceof Number n) {
                    BigDecimal bd = new BigDecimal(String.valueOf(n)).stripTrailingZeros();
                    yield bd.toPlainString();
                }
                yield String.valueOf(value);
            }

            case BOOLEAN -> {
                if (value instanceof Boolean b) yield b ? "true" : "false";
                if (value instanceof Number n) {
                    int i = n.intValue();
                    if (i == 1) yield "true";
                    if (i == 0) yield "false";
                    throw new IllegalArgumentException("boolean expects true/false or 1/0");
                }
                yield String.valueOf(value);
            }

            case JSON -> {
                try {
                    yield MAPPER.writeValueAsString(value);
                } catch (Exception e) {
                    throw new IllegalArgumentException("invalid json value", e);
                }
            }
        };
    }

    private void validateWrite(String key, String value, ParamDataType type) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("key required");
        if (type == null) throw new IllegalArgumentException("type required");

        if (type == ParamDataType.TEXT) return;

        if (type == ParamDataType.JSON) return;

        SystemParamDto tmp = new SystemParamDto();
        tmp.setKey(key);
        tmp.setType(type);
        tmp.setValue(value);

        switch (type) {
            case BOOLEAN -> ParamTypeConverter.convert(tmp, Boolean.class);
            case NUMBER -> ParamTypeConverter.convert(tmp, java.math.BigDecimal.class);
            default -> {
            }
        }
    }

    private static String normalizeTagCode(String tagCode) {
        if (tagCode == null || tagCode.isBlank()) return UNGROUPED;
        return tagCode.trim();
    }

    private static boolean safeEquals(Object a, Object b) {
        return Objects.equals(a, b);
    }

    public SystemParamRepository repository() {
        return repository;
    }
}
