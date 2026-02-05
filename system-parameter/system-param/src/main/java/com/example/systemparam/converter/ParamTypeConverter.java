package com.example.systemparam.converter;

import com.example.systemparam.domain.ParamDataType;
import com.example.systemparam.domain.SystemParamDto;
import com.example.systemparam.exception.ParamTypeMismatchException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ParamTypeConverter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Pattern SIMPLE_DURATION =
            Pattern.compile("^\\s*(\\d+)\\s*(ms|s|m|h|d)\\s*$", Pattern.CASE_INSENSITIVE);

    private ParamTypeConverter() {}

    @SuppressWarnings("unchecked")
    public static <T> T convert(SystemParamDto dto, Class<T> targetType) {
        Objects.requireNonNull(dto, "dto");
        Objects.requireNonNull(targetType, "targetType");

        String key = dto.getKey();
        String value = dto.getValue();
        ParamDataType type = dto.getType() == null ? ParamDataType.TEXT : dto.getType();

        return switch (type) {
            case TEXT -> (T) convertText(key, value, targetType);
            case NUMBER -> (T) convertNumber(key, value, targetType);
            case BOOLEAN -> (T) convertBoolean(key, value, targetType);
            case JSON -> (T) readJson(key, value, targetType);
        };
    }

    public static <E extends Enum<E>> E convertEnum(SystemParamDto dto, Class<E> enumType) {
        Objects.requireNonNull(dto, "dto");
        Objects.requireNonNull(enumType, "enumType");

        return convertEnumValue(dto.getKey(), dto.getValue(), enumType);
    }

    public static <T> List<T> convertList(SystemParamDto dto, Class<T> elementType) {
        Objects.requireNonNull(dto, "dto");
        Objects.requireNonNull(elementType, "elementType");

        String key = dto.getKey();
        String value = dto.getValue();
        ParamDataType type = dto.getType() == null ? ParamDataType.TEXT : dto.getType();

        if (value == null || value.isBlank()) return List.of();

        if (type == ParamDataType.JSON) {
            return readJsonList(key, value, elementType);
        }

        String[] parts = value.split(",");
        ArrayList<T> out = new ArrayList<>();
        for (String p : parts) {
            String item = p == null ? "" : p.trim();
            if (item.isEmpty()) continue;

            SystemParamDto tmp = new SystemParamDto();
            tmp.setKey(key);
            tmp.setType(type);
            tmp.setValue(item);

            out.add(convert(tmp, elementType));
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> toMap(String key, String raw) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(raw, "raw");

        try {
            return MAPPER.readValue(raw, Map.class);
        } catch (Exception e) {
            throw new ParamTypeMismatchException(key, "JSON", raw);
        }
    }

    public static Map<String, Object> toMap(String raw) {
        return toMap("(unknown)", raw);
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> toMapList(String key, String raw) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(raw, "raw");

        try {
            JavaType type = MAPPER.getTypeFactory().constructCollectionType(List.class, Map.class);
            Object obj = MAPPER.readValue(raw, type);
            return (List<Map<String, Object>>) obj;
        } catch (Exception e) {
            throw new ParamTypeMismatchException(key, "JSON[]", raw);
        }
    }

    public static List<Map<String, Object>> toMapList(String raw) {
        return toMapList("(unknown)", raw);
    }

    private static Object convertText(String key, String value, Class<?> targetType) {
        if (targetType == String.class) return value;
        if (targetType == Duration.class) return parseDuration(key, value);

        if (targetType.isEnum()) {
            @SuppressWarnings("unchecked")
            Class<? extends Enum<?>> enumType = (Class<? extends Enum<?>>) targetType;
            return convertEnumValueRaw(key, value, enumType);
        }

        throw new ParamTypeMismatchException(key, "TEXT", value);
    }

    private static Object convertNumber(String key, String value, Class<?> targetType) {
        if (targetType == String.class) return value;
        if (value == null) return null;

        String v = value.trim();
        try {
            if (targetType == Integer.class || targetType == int.class) return Integer.valueOf(v);
            if (targetType == Long.class || targetType == long.class) return Long.valueOf(v);
            if (targetType == Double.class || targetType == double.class) return Double.valueOf(v);
            if (targetType == BigDecimal.class) return new BigDecimal(v);
        } catch (RuntimeException ignore) {}

        throw new ParamTypeMismatchException(key, "NUMBER", value);
    }

    private static Object convertBoolean(String key, String value, Class<?> targetType) {
        if (targetType != Boolean.class && targetType != boolean.class) {
            throw new ParamTypeMismatchException(key, "BOOLEAN", value);
        }
        if (value == null) return null;

        String v = value.trim();
        if ("1".equals(v) || "true".equalsIgnoreCase(v)) return Boolean.TRUE;
        if ("0".equals(v) || "false".equalsIgnoreCase(v)) return Boolean.FALSE;

        throw new ParamTypeMismatchException(key, "BOOLEAN", value);
    }

    private static Duration parseDuration(String key, String raw) {
        if (raw == null) return null;

        String s = raw.trim();
        if (s.isEmpty()) return Duration.ZERO;

        try {
            return Duration.parse(s);
        } catch (Exception ignore) {}

        Matcher m = SIMPLE_DURATION.matcher(s);
        if (m.matches()) {
            long n = Long.parseLong(m.group(1));
            String u = m.group(2).toLowerCase(Locale.ROOT);

            return switch (u) {
                case "ms" -> Duration.ofMillis(n);
                case "s" -> Duration.ofSeconds(n);
                case "m" -> Duration.ofMinutes(n);
                case "h" -> Duration.ofHours(n);
                case "d" -> Duration.ofDays(n);
                default -> throw new ParamTypeMismatchException(key, "DURATION", raw);
            };
        }

        try {
            return Duration.ofMillis(Long.parseLong(s));
        } catch (Exception ignore) {}

        throw new ParamTypeMismatchException(key, "DURATION", raw);
    }

    private static Object readJson(String key, String json, Class<?> targetType) {
        try {
            return MAPPER.readValue(json, targetType);
        } catch (Exception e) {
            throw new ParamTypeMismatchException(key, "JSON", json);
        }
    }

    private static <T> List<T> readJsonList(String key, String json, Class<T> elementType) {
        try {
            JavaType type = MAPPER.getTypeFactory().constructCollectionType(List.class, elementType);
            return MAPPER.readValue(json, type);
        } catch (Exception e) {
            throw new ParamTypeMismatchException(key, "JSON[]", json);
        }
    }

    private static <E extends Enum<E>> E convertEnumValue(String key, String raw, Class<E> enumType) {
        if (raw == null) return null;

        String value = raw.trim();
        try {
            return Enum.valueOf(enumType, value);
        } catch (Exception ignore) {
            for (E c : enumType.getEnumConstants()) {
                if (c.name().equalsIgnoreCase(value)) return c;
            }
            throw new ParamTypeMismatchException(key, "ENUM", raw);
        }
    }

    private static Enum<?> convertEnumValueRaw(String key, String raw, Class<? extends Enum<?>> enumType) {
        if (raw == null) return null;

        String value = raw.trim();
        try {
            @SuppressWarnings({"rawtypes", "unchecked"})
            Enum<?> e = Enum.valueOf((Class) enumType, value);
            return e;
        } catch (Exception ignore) {
            for (Enum<?> c : enumType.getEnumConstants()) {
                if (c.name().equalsIgnoreCase(value)) return c;
            }
            throw new ParamTypeMismatchException(key, "ENUM", raw);
        }
    }
}
