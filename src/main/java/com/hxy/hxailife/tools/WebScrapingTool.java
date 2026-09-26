package com.hxy.hxailife.tools;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class WebScrapingTool {

    @Tool(description = "Scrape the content of a web page")
    public String scrapeWebPage(@ToolParam(description = "URL of the web page to scrape") String url){
        Document document = null;

        ChromeOptions options = new ChromeOptions();
        // 强烈建议在爬虫稳定后开启“无头模式”（即不弹出可视化的浏览器窗口，在后台静默运行）
        // options.addArguments("--headless=new");

        // 2. 启动 Chrome 浏览器 (Selenium 4 会自动帮你处理 WebDriver 驱动问题)
        WebDriver driver = new ChromeDriver(options);

        try {
            // 3. 让浏览器访问目标网址
            System.out.println("正在访问网页...");
            driver.get(url);

            // 4. 【关键步骤】等待 JavaScript 渲染完成
            // 简单粗暴的做法是让主线程睡几秒（实际开发中建议使用 WebDriverWait 显式等待某个元素出现）
            Thread.sleep(5000);

            // 5. 获取经过 JS 渲染后的最终 HTML 源码！
            String htmlSource = driver.getPageSource();

            // 6. 将拿到的完整源码交给 Jsoup 进行解析
            document = Jsoup.parse(htmlSource);

            // 测试：打印网页原本隐藏在 JS 里的真实标题或内容
            System.out.println("网页真实标题: " + document.title());
            // System.out.println(doc.html()); // 你可以看到 <div id="app"> 里面不再是空的了

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            // 7. 务必关闭浏览器，释放内存！否则后台会产生大量无用进程
            driver.quit();
        }

        return document.html();

//        try {
//            document = Jsoup.connect(url).get();
//        } catch (IOException e) {
//            return "Error scraping the web page" + e.getMessage();
//        }
//        return document.html();
    }
}
