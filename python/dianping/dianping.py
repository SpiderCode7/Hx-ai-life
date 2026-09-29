# -*- encoding=utf8 -*-
__author__ = "H"
import re
from airtest.core.api import *
from poco.drivers.android.uiautomation import AndroidUiautomationPoco

# 连接 MuMu
auto_setup(__file__)

# 初始化 Poco
poco = AndroidUiautomationPoco(use_airtest_input=True, screenshot_each_action=False)

QUERY = "江门海底捞"

# SEARCH_text = text(QUERY)

SEARCH_BUTTON2 = Template(r"tpl1790523925165.png", record_pos=(0.419, -0.737), resolution=(910, 1600))


SHARE_BUTTON = Template(r"tpl1790540364603.png", record_pos=(0.395, -0.81), resolution=(910, 1600))

COPY_BUTTON = Template(r"tpl1790524311728.png", record_pos=(-0.434, 0.766), resolution=(910, 1600))


BACK_BUTTON =Template(r"tpl1790524345266.png", record_pos=(-0.457, -0.811), resolution=(910, 1600))


CLEAR_BUTTON = Template(r"tpl1790524399090.png", record_pos=(0.353, -0.793), resolution=(910, 1600))

# 保存帖子标题和链接的对应关系
post_results = []

# 已经点击过的帖子标题
visited_titles = set()

# 复制剪贴板
def read_clipboard():
    """
    读取 MuMu / Android 剪贴板。
    """

    try:
        value = get_clipboard()

        print("Airtest读取到的原始内容：", repr(value))

        if value:
            return str(value)

    except Exception as error:
        print("Airtest读取剪贴板失败：", repr(error))
        
# 进入详情页
def search_result(query):
    result = find_post(query)

    if result is None:
        print("没有找到目标笔记")
        snapshot(filename="search_not_found.png")
        return False

    # result 是一个三元组
    target, title, top = result

    print("找到目标控件")
    print("控件文字：", title)
    print("控件位置：", target.get_position())

    # 点击搜索结果
    target.click()

    print("已经点击目标笔记")

    sleep(5)

    try:
        wait(SHARE_BUTTON, timeout=10, interval=1)
        print("已进入店铺详情页")
        return True
    except TargetNotFoundError:
        print("没有检测到店铺详情页的分享按钮")
        snapshot(filename="shop_detail_not_found.png")
        return False

# 过滤搜索词
def build_match_terms(query):
    """
    根据搜索词生成用于匹配帖子的关键词。
    """

    query = query.strip()

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
def find_post(query):
    """
    查找小红书搜索结果中的笔记标题。
    排除搜索框、搜索建议和顶部控件。
    """

    terms = build_match_terms(query)

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

            # 排除顶部搜索框和搜索建议
            if position[1] < 0.20:
                continue

            # 排除搜索框文字
            if text_value == query:
                continue
            
            # 排除“相关推荐”等非店铺标题
            if "相关推荐" in text_value:
                continue

            # 店铺搜索结果只在页面主要列表区域
            if position[1] < 0.20 or position[1] > 0.85:
                continue

            # 计算匹配关键词数量
            matched_terms = [
                term for term in terms
                if term and term in text_value
            ]

            if not matched_terms:
                continue

            # 匹配越多，分数越高
            score = len(matched_terms) * 20

            # 核心词命中额外加分
            if terms[0] in text_value:
                score += 100

            candidates.append(
                {
                    "node": node,
                    "title": text_value,
                    "top": position[1],
                    "score": score
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
        return None

    # 先按分数从高到低，再按页面位置从上到下
    candidates.sort(
        key=lambda item: (
            -item["score"],
            item["top"]
        )
    )

    best = candidates[0]

    return (
        best["node"],
        best["title"],
        best["top"]
    )

# 运行主函数(http调用)
def run_dianping(query):
    query = query.strip()

    if not query:
        return {
            "success": False,
            "message": "query 不能为空"
        }

    try:
        text(query)
        sleep(2)

        success = search_result(query)

        if not success:
            return {
                "success": False,
                "query": query,
                "message": "没有进入店铺详情页"
            }

        touch(SHARE_BUTTON)
        wait(COPY_BUTTON, timeout=10)
        touch(COPY_BUTTON)
        sleep(1)

        value = read_clipboard()

        keyevent("BACK")
        wait(CLEAR_BUTTON, timeout=10)
        touch(CLEAR_BUTTON)

        if not value:
            return {
                "success": False,
                "query": query,
                "message": "复制成功，但没有读取到剪贴板内容"
            }

        return {
            "success": True,
            "query": query,
            "value": value
        }

    except Exception as error:
        return {
            "success": False,
            "query": query,
            "message": str(error)
        }

def main():
    result = run_dianping("江门海底捞")
    print(result)

    

#     value = read_clipboard()
#     print("复制信息：",value)

if __name__ == "__main__":
    main()