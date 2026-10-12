# -*- encoding=utf8 -*-
__author__ = "H"
import re
import time
from airtest.core.api import *
from poco.drivers.android.uiautomation import AndroidUiautomationPoco

# 连接 MuMu
auto_setup(__file__)

# 初始化 Poco
poco = AndroidUiautomationPoco(use_airtest_input=True, screenshot_each_action=False)

QUERY = "江门海底捞"

# SEARCH_text = text(QUERY)

SEARCH_BUTTON2 = Template(r"tpl1790523925165.png", record_pos=(0.419, -0.737), resolution=(910, 1600))

SHARE_BUTTON_POS = [0.8945054945054945, 0.040625]

COPY_BUTTON = Template(r"tpl1790524311728.png", record_pos=(-0.434, 0.766), resolution=(910, 1600))


BACK_BUTTON =Template(r"tpl1790524345266.png", record_pos=(-0.457, -0.811), resolution=(910, 1600))


CLEAR_BUTTON = Template(r"tpl1790524399090.png", record_pos=(0.353, -0.793), resolution=(910, 1600))

# 保存帖子标题和链接的对应关系
post_results = []

# 已经点击过的帖子标题
visited_titles = set()

# 复制剪贴板
DIANPING_URL_PATTERN = re.compile(
    # 本次修复：大众点评分享可能返回官方长链接，也可能返回 dpurl.cn 短链接。
    r"https?://(?:(?:[\w-]+\.)?dianping\.com|dpurl\.cn)/[^\s\"'<>]+",
    re.IGNORECASE,
)


def extract_dianping_url(value):
    """从大众点评分享文本中提取纯店铺链接。"""
    if not value:
        return None

    match = DIANPING_URL_PATTERN.search(str(value))
    if not match:
        return None

    # 剪贴板内容可能把链接放在句子末尾，去掉复制时带上的标点。
    return match.group(0).rstrip(".,!?;:，。！？；：、)]}>")


def normalize_text(value):
    """本次优化：统一空白和常见标点，避免同一名称因格式不同匹配失败。"""
    if not value:
        return ""
    return re.sub(r"[\s·•,，。.!！?？()（）【】\[\]{}<>《》\-—_/]+", "", str(value)).lower()


def read_clipboard_raw():
    """本次优化：单独读取原始剪贴板，便于识别旧剪贴板和本次复制结果。"""
    try:
        value = get_clipboard()
        print("Airtest读取到的原始内容：", repr(value))
        return str(value) if value else None
    except Exception as error:
        print("Airtest读取剪贴板失败：", repr(error))
        return None


def read_clipboard():
    """
    读取 MuMu / Android 剪贴板。
    """

    value = read_clipboard_raw()
    url = extract_dianping_url(value)
    if url:
        print("提取到大众点评链接：", url)
        return url
    print("剪贴板中没有识别到大众点评链接")
    return None


def reset_search_page():
    """本次优化：每次请求前恢复到搜索页，避免上一次详情页污染下一次查询。"""
    for _ in range(3):
        try:
            if exists(SEARCH_BUTTON2) or exists(CLEAR_BUTTON):
                break
        except Exception:
            pass
        keyevent("BACK")
        sleep(1)

    try:
        if exists(CLEAR_BUTTON):
            touch(CLEAR_BUTTON)
            sleep(0.5)
    except Exception as error:
        print("清理大众点评搜索框失败：", repr(error))


def submit_search():
    """本次优化：明确提交搜索，不再只输入文字后固定等待。"""
    try:
        if exists(SEARCH_BUTTON2):
            touch(SEARCH_BUTTON2)
            return
    except Exception as error:
        print("点击搜索按钮失败，改用回车提交：", repr(error))
    keyevent("ENTER")


def focus_search_input():
    """本次优化：明确点击搜索输入框，避免 text(query) 写入上一次残留焦点。"""
    try:
        inputs = []
        for node in poco(type="android.widget.EditText"):
            try:
                position = node.get_position()
                if position[1] < 0.35:
                    inputs.append((position[1], node))
            except Exception:
                continue
        if inputs:
            inputs.sort(key=lambda value: value[0])
            inputs[0][1].click()
            return True
    except Exception as error:
        print("聚焦大众点评搜索框失败：", repr(error))
    return False


def detail_texts():
    """本次优化：读取详情页可见文字，用于校验点击到的商户/地点。"""
    texts = []
    try:
        for node in poco(type="android.widget.TextView"):
            try:
                value = node.get_text().strip()
                if value and value not in texts:
                    texts.append(value)
            except Exception:
                continue
    except Exception as error:
        print("读取详情页文字失败：", repr(error))
    return texts


def detail_matches(target_name, texts):
    """本次修复：只根据详情页实际文字校验名称，不能使用搜索结果标题作为依据。"""
    target = normalize_text(target_name)
    if not target:
        return False

    normalized_texts = [normalize_text(value) for value in texts]
    return any(target in value for value in normalized_texts)


def click_search_candidate(node):
    """本次修复：优先点击搜索结果的可点击父容器，避免只点击标题文字无效。"""
    current = node
    for _ in range(4):
        try:
            clickable = current.attr("clickable")
            if clickable is True or str(clickable).lower() == "true":
                current.click()
                return True
        except Exception:
            pass
        try:
            current = current.parent()
        except Exception:
            break

    # 如果 UI 层级没有暴露可点击父节点，最后退回标题中心坐标点击。
    try:
        touch(node.get_position())
        return True
    except Exception as error:
        print("点击大众点评搜索结果失败：", repr(error))
        return False


def wait_detail_page(target_name, timeout=8):
    """本次修复：必须检测到详情页文字，确认跳转成功后才允许点击分享按钮。"""
    deadline = time.time() + timeout
    detail_markers = ("地址", "电话", "营业时间", "收藏", "分享", "导航", "商户信息")
    while time.time() < deadline:
        texts = detail_texts()
        joined_text = "|".join(texts)
        if detail_matches(target_name, texts) and any(marker in joined_text for marker in detail_markers):
            return texts
        sleep(0.5)
    return None


# 进入详情页
def search_result(query, target_name=None, context_terms=None):
    # 本次优化：等待异步搜索结果，避免固定 sleep 过短导致正常结果被误判为不存在。
    candidates = []
    for attempt in range(8):
        candidates = find_posts(query, target_name, context_terms)
        if candidates:
            break
        sleep(1)
        if attempt == 3:
            try:
                # 本次优化：首屏没有命中时下滑一次，覆盖结果在首屏以下的情况。
                swipe([0.5, 0.82], [0.5, 0.35], duration=0.5)
                sleep(1)
            except Exception as error:
                print("滚动搜索结果失败：", repr(error))
    if not candidates:
        print("没有找到目标结果")
        snapshot(filename="search_not_found.png")
        return None

    # 本次优化：最多尝试三个候选，详情页不匹配时返回列表继续尝试。
    for candidate in candidates[:3]:
        target = candidate["node"]
        title = candidate["title"]
        print("尝试候选结果：", title, "分数：", candidate["score"])
        try:
            if not click_search_candidate(target):
                keyevent("BACK")
                sleep(1)
                continue

            texts = wait_detail_page(target_name)
            if texts is not None:
                return {"title": title, "texts": texts}

            print("详情页与目标不一致，放弃当前候选：", title)
            keyevent("BACK")
            sleep(1)
        except Exception as error:
            print("打开候选结果失败：", repr(error))
            keyevent("BACK")
            sleep(1)
    return None

# 过滤搜索词
def build_match_terms(query, target_name=None, context_terms=None):
    """
    根据搜索词生成用于匹配帖子的关键词。
    """

    query = (query or "").strip()
    target_name = (target_name or "").strip()
    context_terms = context_terms or []

    # 本次优化：优先使用明确目标名称，避免“广州 + 永庆坊”被当成一个模糊标题匹配。
    if target_name:
        return [target_name] + [term for term in context_terms if term]

    # 有空格时，按空格拆分
    tokens = query.split()

    if len(tokens) > 1:
        return tokens

    # 没有空格时，优先使用前三个字作为核心词
    # 江南西娱乐活动 -> 江南西
    terms = []
    
    if len(query) >= 5:
        terms.append(query[:5])
    
    if len(query) >= 4:
        terms.append(query[:4])

    if len(query) >= 3:
        terms.append(query[:3])

    if len(query) >= 2:
        terms.append(query[:2])

    if query not in terms:
        terms.append(query)

    return terms

# 查找包含关键词的帖子，排除搜索框和搜索建议。
def find_posts(query, target_name=None, context_terms=None):
    """
    查找小红书搜索结果中的笔记标题。
    排除搜索框、搜索建议和顶部控件。
    """

    terms = build_match_terms(query, target_name, context_terms)

    print("用于匹配的关键词：", terms)

    # 直接读取所有文字控件，再在 Python 中筛选
    targets = poco(
        type="android.widget.TextView"
    )

    candidates = []

    for node in targets:
        try:
            text_value = node.get_text().strip()
            position = node.get_position()
            node_type = node.attr("type")

            if not text_value:
                continue

            # 本次修复：搜索结果第一张卡片可能位于 y=0.13~0.15，不能再统一排除 0.20 以上区域。
            # 只排除搜索框所在的最顶部区域；搜索分类/建议等非精确结果会在目标匹配后继续过滤。
            if position[1] < 0.08:
                continue

            # 本次修复：不再按文字等于 query 排除结果。
            # 搜索框已经通过 position[1] < 0.20 过滤，首屏中与 query 完全一致的文字可能就是正确结果。
            
            # 排除“相关推荐”等非店铺标题
            if "相关推荐" in text_value:
                continue

            # 本次修复：顶部区域只接受目标名称完全匹配的结果，避免把分类标签或建议项当成商户。
            # 底部保留到 0.90，兼容首屏底部的正常搜索卡片。
            if position[1] > 0.90:
                continue

            normalized_title = normalize_text(text_value)

            # 本次优化：目标名称必须完整命中；不再接受只有单个短片段命中的结果。
            target = normalize_text(target_name)
            if target and target not in normalized_title:
                continue

            exact_match = bool(target and normalized_title == target)
            if position[1] < 0.20 and not exact_match:
                continue

            # 计算匹配关键词数量
            matched_terms = [
                term for term in terms
                if normalize_text(term) and normalize_text(term) in normalized_title
            ]

            if not matched_terms:
                continue

            # 匹配越多，分数越高；目标名称完整匹配时给予最高优先级。
            score = len(matched_terms) * 20
            if exact_match:
                score += 1000
            elif target and target in normalized_title:
                score += 150
            elif terms and normalize_text(terms[0]) in normalized_title:
                score += 100

            candidates.append(
                {
                    "node": node,
                    "title": text_value,
                    "top": position[1],
                    "score": score,
                    "exact_match": exact_match
                }
            )

            print(
                "候选帖子：",
                text_value,
                "匹配：",
                matched_terms,
                "分数：",
                score,
                "位置：",
                position
            )

        except Exception as error:
            print("处理控件失败：", error)

    if not candidates:
        return []

    # 先按分数从高到低，再按页面位置从上到下
    candidates.sort(
        key=lambda item: (
            not item["exact_match"],
            -item["score"],
            item["top"]
        )
    )

    return candidates


def find_post(query, target_name=None, context_terms=None):
    """兼容旧调用：返回排序后的第一个候选。"""
    candidates = find_posts(query, target_name, context_terms)
    if not candidates:
        return None
    best = candidates[0]
    return best["node"], best["title"], best["top"]


def wait_new_clipboard(previous_url=None, timeout=8, require_changed=True):
    """本次优化：等待本次复制结果，避免把上一次请求的 URL 当成当前结果。"""
    deadline = time.time() + timeout
    while time.time() < deadline:
        value = read_clipboard_raw()
        url = extract_dianping_url(value)
        if url and (not require_changed or previous_url is None or url != previous_url):
            return url
        sleep(0.5)
    return None


def clear_clipboard():
    """本次优化：尽量清空 Android 剪贴板；旧版 Airtest 不支持时由 URL 变化校验兜底。"""
    try:
        set_clipboard("")
        return True
    except Exception as error:
        print("当前 Airtest 不支持直接清空剪贴板，将使用旧值比对：", repr(error))
        return False

# 运行主函数(http调用)
def run_dianping(query, target_name=None, place_type=None, nearby_place=None):
    query = (query or "").strip()
    target_name = (target_name or query).strip()

    if not query:
        return {
            "success": False,
            "message": "query 不能为空"
        }

    try:
        reset_search_page()
        focus_search_input()
        text(query)
        submit_search()
        sleep(1)

        result = search_result(
            query,
            target_name=target_name,
            context_terms=[nearby_place] if nearby_place else None,
        )

        if not result:
            reset_search_page()
            return {
                "success": False,
                "status": "NO_RESULT",
                "query": query,
                "message": "没有找到并核验目标详情页"
            }

        old_url = extract_dianping_url(read_clipboard_raw())
        clipboard_cleared = clear_clipboard()
        # 本次优化：SHARE_BUTTON 模板已弃用，统一使用已确认的分享按钮固定坐标。
        poco.click(SHARE_BUTTON_POS)
        wait(COPY_BUTTON, timeout=10)
        touch(COPY_BUTTON)
        value = wait_new_clipboard(old_url, require_changed=not clipboard_cleared)

        keyevent("BACK")
        reset_search_page()

        if not value:
            return {
                "success": False,
                "status": "COPY_FAILED" if old_url is None or clipboard_cleared else "STALE_CLIPBOARD",
                "query": query,
                "matchedName": result["title"],
                "message": "详情页已核验，但没有读取到本次复制的新链接"
            }

        return {
            "success": True,
            "status": "VERIFIED",
            "query": query,
            "value": value,
            "matchedName": result["title"],
            "placeType": place_type,
        }

    except Exception as error:
        return {
            "success": False,
            "status": "UI_STATE_ERROR",
            "query": query,
            "message": str(error)
        }
    finally:
        # 本次优化：无论成功、失败还是 UI 异常，都尽量恢复搜索页，保证下一次请求从干净状态开始。
        try:
            reset_search_page()
        except Exception as cleanup_error:
            print("恢复大众点评搜索页失败：", repr(cleanup_error))

def main():
    result = run_dianping("广州永庆坊")
    print(result)

    

#     value = read_clipboard()
#     print("复制信息：",value)

if __name__ == "__main__":
    main()
