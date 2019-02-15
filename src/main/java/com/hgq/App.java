package com.hgq;

import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.commons.cli.*;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Hello world!
 */
public class App {
    private static final Options OPTIONS = new Options();
    private static final String BASE_URL = "http://oa.gzlawyer.org";
    private static final String PAPER_COURSE_URL = BASE_URL + "/service/rest/dm.MVC/gam.PaperCourse@mvc-list/execute?businessId=gam.PaperCourse%40mvc-list&page=";
    private static final String PAPER_COURSE_VIEW_URL = BASE_URL + "/views/gam/paperCourse-mvc-view.jsp?id=";
    private static final String ADD_TRAINING_RECORD_URL = BASE_URL + "/service/rest/dm.DataService/gam.PaperCourse@addTrainingRecord/invoke?_csrftoken=";
    private static final Map<String, String> HEADERS = new LinkedHashMap<>();

    static {
        //HEADERS.put("Cookie", "bad_id36c8e930-c0aa-11e8-a90d-9d1e547c2986=84b9ba31-2ff8-11e9-b425-5f04bb957a92; nice_id36c8e930-c0aa-11e8-a90d-9d1e547c2986=84b9ba32-2ff8-11e9-b425-5f04bb957a92; JSESSIONID=914C53D1530EF322B470D16BA67F21C1; href=http%3A%2F%2Foa.gzlawyer.org%2Fworkbench%2F%23%2F; accessId=7192eb20-c0a9-11e8-a90d-9d1e547c2986; pageViewNum=1; bad_id7192eb20-c0a9-11e8-a90d-9d1e547c2986=c18d0431-2ff8-11e9-9eea-5fae95389fe6; nice_id7192eb20-c0a9-11e8-a90d-9d1e547c2986=c18d0432-2ff8-11e9-9eea-5fae95389fe6; b_Admin_visibility=hidden");
        HEADERS.put("Host", "oa.gzlawyer.org");
        HEADERS.put("Origin", BASE_URL);
        HEADERS.put("Referer", "http://oa.gzlawyer.org/workbench/");
        HEADERS.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36");
    }

    public static void main(String[] args) throws IOException {
        String separator = System.lineSeparator();
        System.out.println();
        System.out.println();
        Option page = Option.builder("p")
                .hasArgs()
                .argName("page number")
                .desc("<必填> 学习第几页，多个用逗号分隔" + separator + "例如，1,8,10,14 表示学习第1,8,10,14页的文章")
                .valueSeparator(',')
                .required()
                .build();
        Option cookie = Option.builder("c")
                .hasArg()
                .argName("cookies")
                .desc("<必填> cookie信息，需要用双引号括起来")
                .required()
                .build();
        Option token = Option.builder("token")
                .hasArg()
                .argName("csrf token")
                .desc("跨域token信息")
                .build();
        Option t1 = Option.builder("t1")
                .hasArg()
                .argName("seconds")
                .desc("学习最短时间，单位秒，默认60，必须大于0，小于300")
                .build();
        Option t2 = Option.builder("t2")
                .hasArg()
                .argName("seconds")
                .desc("学习最长时间，单位秒，默认120，必须大于0，少于300")
                .build();

        OPTIONS.addOption(page);
        OPTIONS.addOption(cookie);
        OPTIONS.addOption(token);
        OPTIONS.addOption(t1);
        OPTIONS.addOption(t2);

        CommandLine command;
        try {
            command = new DefaultParser().parse(OPTIONS, args);
            runCommand(command);
        } catch (Exception e) {
            System.err.println(e.getMessage() + separator);
            printUsage();
        }
    }

    private static void printUsage() {

        String selfPath = App.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -Dfile.encoding=utf-8 -jar " + selfPath, OPTIONS, true);
        System.out.println();
        System.exit(1);
    }

    private static void runCommand(CommandLine commandLine) throws IOException {
        String cookie = commandLine.getOptionValue("c");
        String[] ps = commandLine.getOptionValues("p");
        List<Integer> pages;
        try {
            pages = Arrays.stream(ps).map(Integer::valueOf).collect(Collectors.toList());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("-p 的值必须为整数，当前值为\"" + Arrays.toString(commandLine.getOptionValues("p")) + "\"");
        }
        int t1 = 60;
        if (commandLine.hasOption("t1")) {
            try {
                t1 = Integer.valueOf(commandLine.getOptionValue("t1"));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("-t1 的值必须为整数，当前值为\"" + commandLine.getOptionValue("t1") + "\"");
            }
            if (t1 <= 0 || t1 >= 300) {
                throw new IllegalArgumentException("-t1 的值必须大于0，小于300，当前值为\"" + commandLine.getOptionValue("t1") + "\"");
            }
        }
        int t2 = 120;
        if (commandLine.hasOption("t2")) {
            try {
                t2 = Integer.valueOf(commandLine.getOptionValue("t2"));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("-t2 的值必须为整数，当前值为\"" + commandLine.getOptionValue("t2") + "\"");
            }
            if (t2 <= 0 || t2 >= 300) {
                throw new IllegalArgumentException("-t2 的值必须大于0，小于300，当前值为\"" + commandLine.getOptionValue("t2") + "\"");
            }
        }
        String token = "";
        if (commandLine.hasOption("token")) {
            token = commandLine.getOptionValue("token");
        }

        for (Integer page : pages) {
            Document document = Jsoup.connect(PAPER_COURSE_URL + page + "&_=" + System.currentTimeMillis())
                    .header("Cookie", cookie)
                    .headers(HEADERS).get();
            if (document.title().startsWith("系统登录")) {
                throw new IllegalArgumentException("登陆信息已失效，请重新设置Cookie");
            }
            Element b = document.body();
            List<String> titles = b.select("tr > td:eq(1)").stream().map(Element::text).collect(Collectors.toList());
            Elements elements = b.getElementsByClass("custom-checkbox");
            List<String> ids = elements.stream().map(e -> e.attr("data-entityId")).collect(Collectors.toList());
            for (int i = 0; i < ids.size(); i++) {
                String id = ids.get(i);
                try {
                    document = Jsoup.connect(PAPER_COURSE_VIEW_URL + id)
                            .header("Cookie", cookie)
                            .headers(HEADERS).get();
                    if (document.title().startsWith("系统登录")) {
                        throw new IllegalArgumentException("登陆信息已失效，请重新设置Cookie");
                    }
                    System.out.println("==>> 学习 《"+titles.get(i)+"》 中...   当前文章ID: " + id );
                    Elements p = document.select("body > div > p");
                    String text = p.text();
                    if (text.startsWith("您已阅读过此论文")) {
                        System.out.println(text);
                        System.out.println("<<== 直接忽略此论文");
                        System.out.println();
                        System.out.println();
                        continue;
                    }
                    String js = document.select("script:not([src])").html();
                    int start = js.indexOf("paperCourseId") - 1;
                    int end = js.indexOf("}", start) + 1;
                    String params = js.substring(start, end);
                    int count = t1;
                    if(t2 > t1) {
                        count = t1 + ThreadLocalRandom.current().nextInt(t2 - t1);
                    }
                    for (; count > 0; count--) {
                        System.out.println("距离获得学分还有" + count + "秒");
                        TimeUnit.SECONDS.sleep(1);
                    }
                    JsonNode body = Unirest.post(ADD_TRAINING_RECORD_URL + token)
                            .header("content-type", "application/json")
                            .header("Cookie", cookie)
                            .headers(HEADERS)
                            .body(new JSONObject(params))
                            .asJson().getBody();
                    System.out.println("<<== 学习 《"+titles.get(i)+"》 结束，  学习结果：" + body);
                    System.out.println();
                    System.out.println();
                } catch (UnirestException e) {
                    e.printStackTrace();
                    break;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

    }
}
