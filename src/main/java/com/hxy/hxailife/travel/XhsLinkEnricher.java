package com.hxy.hxailife.travel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hxy.hxailife.travel.dto.TravelPlanSaveDTO;
import com.hxy.hxailife.travel.vo.TravelPlanGenerationItemVO;
import com.hxy.hxailife.travel.vo.TravelPlanGenerationVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/** 只从最终节点的独立搜索结果绑定笔记，不采信模型转写的链接。 */
@Component
@Slf4j
public class XhsLinkEnricher {

    private static final Pattern CITY = Pattern.compile("[\\p{IsHan}]{2,12}?市");
    private static final Pattern STREET = Pattern.compile("[\\p{IsHan}]{2,12}?(?:大道|路|街|巷)");
    private static final Pattern STREET_NUMBER = Pattern.compile("[\\p{IsHan}]{2,12}?(?:大道|路|街|巷)\\d+号");
    private static final Pattern NOTE = Pattern.compile(
            "标题[:：]\\s*(.*?)\\R作者[:：].*?\\R内容[:：]\\s*(.*?)\\R点赞[:：].*?\\R(?:评论[:：].*?\\R)?链接[:：]\\s*(https?://\\S+)\\s*(?:\\R---)?\\s*$",
            Pattern.DOTALL);

    private final ObjectMapper objectMapper;

    public XhsLinkEnricher(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** Spring MCP 可能给工具名添加服务前缀。 */
    public static boolean isTool(ToolCallback tool, String name) {
        String actual = tool.getToolDefinition().name();
        return actual.equals(name) || actual.endsWith("_" + name);
    }

    public void enrich(TravelPlanGenerationVO result, TravelPlanSaveDTO form, ToolCallback[] tools) {
        if (result == null || result.getItems() == null) {
            return;
        }
        boolean enabled = form != null && Integer.valueOf(1).equals(form.getXhsEnabled());
        ToolCallback search = enabled ? Arrays.stream(tools)
                .filter(tool -> isTool(tool, "search_notes")).findFirst().orElse(null) : null;
        for (TravelPlanGenerationItemVO item : result.getItems()) {
            if (item == null) {
                continue;
            }
            item.setXhsSearchQuery(null);
            item.setXhsSearchUrl(null);
            if (!enabled || (item.getType() != GenerationItemType.DESTINATION
                    && item.getType() != GenerationItemType.MEAL) || blank(item.getName())) {
                continue;
            }
            String city = city(item.getAddress());
            if (city.isEmpty()) {
                city = city(form.getStartPlaceAddress());
            }
            String query = String.join(" ", item.getName().trim(), city,
                    item.getAddress() == null ? "" : item.getAddress().trim()).trim();
            item.setXhsSearchQuery(query);
            if (search == null) {
                continue;
            }
            try {
                log.info("按最终节点查询小红书，type={}, name={}, query={}", item.getType(), item.getName(), query);
                String response = search.call(objectMapper.writeValueAsString(Map.of("keywords", query, "limit", 3)));
                log.info("三条小红书信息："+ response);
                List<Note> notes = new ArrayList<>();
                readNotes(objectMapper.readTree(response), notes);
                for (Note note : notes) {
                    if (matches(note, item, city) && isNoteUrl(note.url())) {
                        item.setXhsSearchUrl(note.url().trim());
                        break;
                    }
                }
                if (item.getXhsSearchUrl() == null) {
                    log.info("小红书搜索未找到对应笔记，type={}, name={}, candidates={}",
                            item.getType(), item.getName(), notes.size());
                }
            } catch (Exception exception) {
                // 登录失效、工具超时或结果解析失败只影响当前节点链接，不让整份攻略失败。
                log.warn("小红书节点补链失败，type={}, name={}", item.getType(), item.getName(), exception);
            }
        }
    }

    private void readNotes(JsonNode node, List<Note> notes) throws JsonProcessingException {
        if (node == null || node.path("isError").asBoolean(false)) {
            return;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                readNotes(child, notes);
            }
        } else if (node.isObject()) {
            if (node.has("title") && node.has("url")) {
                notes.add(new Note(node.path("title").asText(""), node.path("content").asText(""),
                        node.path("url").asText("")));
            } else if (node.has("text")) {
                readText(node.get("text").asText(), notes);
            } else if (node.has("content")) {
                readNotes(node.get("content"), notes);
            }
        } else if (node.isTextual()) {
            readText(node.asText(), notes);
        }
    }

    public void readText(String text, List<Note> notes) throws JsonProcessingException {
        if (text.trim().startsWith("{") || text.trim().startsWith("[")) {
            readNotes(objectMapper.readTree(text), notes);
            return;
        }
        for (String block : text.split("(?m)^---\\s*$")) {
            var match = NOTE.matcher(block.trim());
            if (match.matches()) {
                notes.add(new Note(match.group(1), match.group(2), match.group(3)));
            }
        }
    }

    public boolean matches(Note note, TravelPlanGenerationItemVO item, String city) {
        // 不用搜索词、作者名或 URL 作匹配证据。
        String evidence = normalize(note.title() + " " + note.content());
        String name = normalize(item.getName());
        if (name.length() < 2) {
            return false;
        }
        String address = item.getAddress() == null ? "" : item.getAddress();
        String localAddress = address.replaceFirst("^.*[省市区县]", "");
        var streetNumber = STREET_NUMBER.matcher(localAddress);
        String targetStreetNumber = streetNumber.find() ? normalize(streetNumber.group()) : null;
        boolean exactAddress = targetStreetNumber != null && evidence.contains(targetStreetNumber);
        String baseName = normalize(item.getName().split("[（(]", 2)[0]);
        String title = normalize(note.title());
        String content = normalize(note.content());
        boolean cinema = item.getType() == GenerationItemType.DESTINATION
                && (name.contains("电影") || name.contains("影城"));
        // 标题或正文直接提到主体店名时，可省略括号中的分店名和地址。
        if (!baseName.equals(name) && baseName.length() >= 2
                && (title.contains(baseName) || content.contains(baseName))) {
            if (cinema && !title.contains(baseName) && content.contains("导航")) {
                return false;
            }
            var mentionedStreetNumber = STREET_NUMBER.matcher(note.title() + " " + note.content());
            if (targetStreetNumber != null) {
                String targetStreet = targetStreetNumber.replaceFirst("\\d+号$", "");
                while (mentionedStreetNumber.find()) {
                    String mentioned = normalize(mentionedStreetNumber.group());
                    if (mentioned.contains(targetStreet) && !mentioned.contains(targetStreetNumber)) {
                        return false;
                    }
                }
            }
            return true;
        }
        if (!evidence.contains(name) && !(baseName.length() >= 2 && evidence.contains(baseName) && exactAddress)) {
            return false;
        }
        // 地点只作为导航参照时不算对应笔记，例如邮筒笔记中附带提及影院地址。
        if (cinema) {
            if (!title.contains(baseName) && !title.contains("电影")
                    && !title.contains("影院") && !title.contains("影城")
                    && content.contains("导航")) {
                return false;
            }
        }
        var street = STREET.matcher(localAddress);
        if (!blank(address)) {
            if (exactAddress || evidence.contains(normalize(address))
                    || (street.find() && evidence.contains(normalize(street.group())))) {
                return true;
            }
            // 景点笔记常只写景点名和城市，不写街道门牌。
            return item.getType() == GenerationItemType.DESTINATION
                    && !city.isEmpty() && evidence.contains(normalize(city));
        }
        return !city.isEmpty() && evidence.contains(normalize(city));
    }

    private String city(String address) {
        if (address == null) {
            return "";
        }
        var match = CITY.matcher(address);
        return match.find() ? match.group().replaceFirst("^.*(?:省|自治区)", "").replaceFirst("市$", "") : "";
    }

    private String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]", "");
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isNoteUrl(String value) {
        try {
            URI uri = URI.create(value.trim());
            String host = uri.getHost();
            return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    && uri.getUserInfo() == null && uri.getPort() == -1
                    && ("www.xiaohongshu.com".equalsIgnoreCase(host) || "xiaohongshu.com".equalsIgnoreCase(host))
                    && uri.getPath().matches("/(?:explore|discovery/item)/[a-zA-Z0-9]+/?")
                    && value.length() <= 2048;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public record Note(String title, String content, String url) {
    }
}
