package com.ncvt.kebiao.data.remote;

import android.util.Log;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.LocalDate;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Cipher;

/** Blocking network operations; callers run these on their ViewModel executor. */
public final class ZhengfangScraper implements AutoCloseable {
    private static final String TAG = "KeBiaoScraper";
    private static final String DEFAULT_BASE_URL = "http://jw.sub.ncvt.net/jwglxt";
    private static final String DEFAULT_MENU_CODE = "N2151";
    private static final String ACCEPT_LANGUAGE = "zh-CN,zh;q=0.9,en;q=0.8";
    private static final String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36";

    private final Map<List<String>, Cookie> cookieStore = new LinkedHashMap<>();
    private final OkHttpClient client = new OkHttpClient.Builder()
            .cookieJar(new CookieJar() {
                @Override
                public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                    saveCookies(cookies);
                }
                @Override
                public List<Cookie> loadForRequest(HttpUrl url) {
                    return loadCookies(url);
                }
            })
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build();

    private String origin = "";
    private String baseUrl = "";
    private PublicKey rsaPublicKey;
    private String loginFormAction = "";
    private final List<Map.Entry<String, String>> loginFormInputs = new ArrayList<>();
    private final List<Map.Entry<String, String>> allLoginInputs = new ArrayList<>();

    public ScrapeResult fetchLoginPage(String url) {
        try {
            baseUrl = normalizeBaseUrl(url);
            URI uri = URI.create(baseUrl);
            origin = uri.getScheme() + "://" + uri.getAuthority();
            synchronized (cookieStore) { cookieStore.clear(); }
            rsaPublicKey = null;
            loginFormAction = "";
            loginFormInputs.clear();
            allLoginInputs.clear();
            try (Response response = client.newCall(request(baseUrl + "/xtgl/login_slogin.html")
                    .header("Accept-Language", ACCEPT_LANGUAGE).get().build()).execute()) {
                if (!response.isSuccessful()) {
                    return new ScrapeResult.Error("登录页访问失败: HTTP " + response.code());
                }
                String html = bodyText(response);
                Document document = Jsoup.parse(html);
                parseLoginForm(document);
                boolean requiresCaptcha = !document.select(
                        "input[name=yzm], #yzm, img[src*=kaptcha], img[id*=captcha]").isEmpty()
                        || html.contains("验证码") || html.toLowerCase(Locale.ROOT).contains("kaptcha");
                if (requiresPasswordEncryption()) fetchPublicKey();
                return new ScrapeResult.LoginPageReady(requiresCaptcha);
            }
        } catch (IOException e) {
            return new ScrapeResult.Error("网络错误: " + e.getMessage());
        } catch (Exception e) {
            return new ScrapeResult.Error("登录页初始化失败: " + e.getMessage());
        }
    }

    public byte[] fetchCaptcha() {
        try (Response response = client.newCall(request(baseUrl + "/kaptcha?_=" + System.currentTimeMillis())
                .header("Referer", baseUrl + "/xtgl/login_slogin.html").get().build()).execute()) {
            return response.isSuccessful() && response.body() != null ? response.body().bytes() : null;
        } catch (Exception e) {
            Log.w(TAG, "Unable to load captcha", e);
            return null;
        }
    }

    public ScrapeResult login(String account, String password, String captcha) {
        try {
            if (loginFormAction.isEmpty()) return new ScrapeResult.Error("请先初始化登录页");
            if (checkIdentityConfirmation(account)) {
                return new ScrapeResult.LoginFailed("教务系统要求先确认身份信息，请先在浏览器完成确认后再导入");
            }
            if (requiresPasswordEncryption()) {
                fetchPublicKey();
                if (rsaPublicKey == null) return new ScrapeResult.Error("无法获取登录公钥，请稍后再试");
            }
            logoutPreviousAccount();
            try (Response response = client.newCall(request(loginFormAction)
                    .header("Accept-Language", ACCEPT_LANGUAGE)
                    .header("Origin", origin)
                    .header("Referer", baseUrl + "/xtgl/login_slogin.html")
                    .post(buildLoginFormBody(account, password, captcha)).build()).execute()) {
                if (!response.isSuccessful()) {
                    return new ScrapeResult.Error("登录请求失败: HTTP " + response.code());
                }
                String body = bodyText(response);
                if (looksLikeLoginPage(body, response.request().url().toString())) {
                    return new ScrapeResult.LoginFailed(parseLoginError(body));
                }
            }
            try (Response response = client.newCall(request(timetableUrl(DEFAULT_MENU_CODE))
                    .header("Referer", baseUrl + "/").get().build()).execute()) {
                if (!response.isSuccessful()) {
                    return new ScrapeResult.Error("登录状态校验失败: HTTP " + response.code());
                }
                if (looksLikeLoginPage(bodyText(response), response.request().url().toString())) {
                    return new ScrapeResult.LoginFailed("登录状态校验失败，访问课表页时又回到了登录页");
                }
            }
            return ScrapeResult.LoginSuccess.INSTANCE;
        } catch (IOException e) {
            return new ScrapeResult.Error("登录请求失败: " + e.getMessage());
        } catch (Exception e) {
            return new ScrapeResult.Error("登录处理失败: " + e.getMessage());
        }
    }

    public ScrapeResult fetchCourses(String academicYear, String semester, String menuCode) {
        try {
            String pageUrl = timetableUrl(menuCode.trim().isEmpty() ? DEFAULT_MENU_CODE : menuCode);
            try (Response response = client.newCall(request(pageUrl).header("Referer", baseUrl + "/")
                    .get().build()).execute()) {
                if (!response.isSuccessful()) {
                    return new ScrapeResult.Error("课表页访问失败: HTTP " + response.code());
                }
                if (looksLikeLoginPage(bodyText(response), response.request().url().toString())) {
                    return new ScrapeResult.Error("登录已失效，无法进入课表页");
                }
            }
            RequestBody payload = new FormBody.Builder()
                    .add("xnm", normalizeAcademicYear(academicYear))
                    .add("xqm", normalizeSemester(semester)).add("kzlx", "ck").add("xsdm", "").build();
            try (Response response = client.newCall(request(pageUrl).header("Referer", pageUrl)
                    .header("X-Requested-With", "XMLHttpRequest")
                    .header("Accept", "application/json, text/javascript, */*; q=0.01")
                    .post(payload).build()).execute()) {
                if (!response.isSuccessful()) {
                    return new ScrapeResult.Error("课表接口请求失败: HTTP " + response.code());
                }
                String body = bodyText(response);
                if (body.trim().startsWith("<")) {
                    return new ScrapeResult.Error(looksLikeLoginPage(body, response.request().url().toString())
                            ? "请求课表接口时登录已失效" : "课表接口返回了网页内容，请检查菜单码或学期参数");
                }
                List<CourseRaw> courses = CourseJsonParser.parseCourses(new JSONObject(body), false);
                return courses.isEmpty() ? new ScrapeResult.Error("未解析到课程数据，请检查学年、学期或菜单码")
                        : new ScrapeResult.CoursesFetched(courses);
            }
        } catch (IOException e) {
            return new ScrapeResult.Error("网络请求失败: " + e.getMessage());
        } catch (Exception e) {
            return new ScrapeResult.Error("解析课程数据出错: " + e.getMessage());
        }
    }

    public List<ResolvedTimetableConfig> resolveTimetableConfigs(String year, String semester, String menuCode) {
        String primaryYear = normalizeAcademicYear(year);
        String primarySemester = normalizeSemester(semester);
        Set<String> menus = new LinkedHashSet<>(Arrays.asList(
                menuCode.trim().isEmpty() ? DEFAULT_MENU_CODE : menuCode, DEFAULT_MENU_CODE));
        Set<ResolvedTimetableConfig> candidates = new LinkedHashSet<>();
        addCandidates(candidates, menus, primaryYear, primarySemester);
        if (primarySemester.equals("3")) addCandidates(candidates, menus, primaryYear, "12");
        if (primarySemester.equals("12")) addCandidates(candidates, menus, primaryYear, "3");
        addCandidates(candidates, menus, guessAcademicYear(), guessSemester());
        addCandidates(candidates, menus, "2025", "12");
        return new ArrayList<>(candidates);
    }

    private void addCandidates(Set<ResolvedTimetableConfig> candidates, Set<String> menus,
                               String year, String semester) {
        for (String menu : menus) {
            String start = year.equals("2025") && semester.equals("12") ? "2026-03-02" : "";
            candidates.add(new ResolvedTimetableConfig(year, semester, menu, start));
        }
    }

    private Request.Builder request(String url) {
        return new Request.Builder().url(url).header("User-Agent", UA);
    }

    private String timetableUrl(String menuCode) {
        return HttpUrl.get(baseUrl + "/kbcx/xskbcx_cxXsKb.html").newBuilder()
                .addQueryParameter("gnmkdm", menuCode).build().toString();
    }

    private static String bodyText(Response response) throws IOException {
        return response.body() == null ? "" : response.body().string();
    }

    static String normalizeBaseUrl(String url) {
        String input = url.trim().isEmpty() ? DEFAULT_BASE_URL : url.trim();
        int login = input.indexOf("/xtgl/login_slogin.html");
        if (login >= 0) return input.substring(0, login);
        int context = input.indexOf("/jwglxt/");
        if (context >= 0) return input.substring(0, context) + "/jwglxt";
        while (input.endsWith("/")) input = input.substring(0, input.length() - 1);
        return input.endsWith("/jwglxt") ? input : input + "/jwglxt";
    }

    private void saveCookies(List<Cookie> cookies) {
        synchronized (cookieStore) {
            long now = System.currentTimeMillis();
            for (Cookie cookie : cookies) {
                List<String> key = Arrays.asList(cookie.domain(), cookie.path(), cookie.name());
                if (cookie.expiresAt() <= now) cookieStore.remove(key);
                else cookieStore.put(key, cookie);
            }
        }
    }

    private List<Cookie> loadCookies(HttpUrl url) {
        synchronized (cookieStore) {
            List<Cookie> valid = new ArrayList<>();
            Iterator<Cookie> iterator = cookieStore.values().iterator();
            long now = System.currentTimeMillis();
            while (iterator.hasNext()) {
                Cookie cookie = iterator.next();
                if (cookie.expiresAt() <= now) iterator.remove();
                else if (cookie.matches(url)) valid.add(cookie);
            }
            return valid;
        }
    }

    private void parseLoginForm(Document document) {
        Element loginForm = null;
        for (Element form : document.select("form")) {
            if (loginForm == null) loginForm = form;
            if (form.attr("action").toLowerCase(Locale.ROOT).contains("login_slogin")) {
                loginForm = form;
                break;
            }
        }
        if (loginForm == null) throw new IllegalStateException("没有在登录页中找到表单");
        String action = loginForm.attr("action").trim();
        if (action.isEmpty()) action = "/xtgl/login_slogin.html";
        if (action.startsWith("http://") || action.startsWith("https://")) loginFormAction = action;
        else loginFormAction = action.startsWith("/") ? origin + action : baseUrl + "/" + action;
        for (Element input : loginForm.select("input[name]")) {
            loginFormInputs.add(entry(input.attr("name").trim(), input.attr("value").trim()));
        }
        for (Element input : document.select("input")) {
            Set<String> keys = new LinkedHashSet<>(Arrays.asList(input.attr("name").trim(), input.id().trim()));
            for (String key : keys) {
                if (!key.isEmpty()) allLoginInputs.add(entry(key, input.attr("value").trim()));
            }
        }
    }

    private static Map.Entry<String, String> entry(String name, String value) {
        return new AbstractMap.SimpleImmutableEntry<>(name, value);
    }

    private boolean requiresPasswordEncryption() {
        for (Map.Entry<String, String> input : allLoginInputs) {
            if (input.getKey().equals("mmsfjm")) return !input.getValue().equals("0");
        }
        return true;
    }

    private void fetchPublicKey() {
        rsaPublicKey = null;
        try (Response response = client.newCall(request(baseUrl + "/xtgl/login_getPublicKey.html?time="
                + System.currentTimeMillis()).header("Referer", baseUrl + "/xtgl/login_slogin.html")
                .get().build()).execute()) {
            if (!response.isSuccessful()) return;
            JSONObject json = new JSONObject(bodyText(response));
            String modulus = json.optString("modulus");
            String exponent = json.optString("exponent");
            if (!modulus.isEmpty() && !exponent.isEmpty()) {
                RSAPublicKeySpec spec = new RSAPublicKeySpec(
                        new BigInteger(1, Base64.getDecoder().decode(modulus.trim())),
                        new BigInteger(1, Base64.getDecoder().decode(exponent.trim())));
                rsaPublicKey = KeyFactory.getInstance("RSA").generatePublic(spec);
            }
        } catch (Exception e) {
            Log.w(TAG, "Unable to load login public key", e);
        }
    }

    private boolean checkIdentityConfirmation(String account) {
        try (Response response = client.newCall(request(baseUrl + "/xtgl/yhgl_cxXxqrCheck.html")
                .header("Referer", baseUrl + "/xtgl/login_slogin.html")
                .header("X-Requested-With", "XMLHttpRequest")
                .post(new FormBody.Builder().add("yhm", account).build()).build()).execute()) {
            String body = bodyText(response).trim();
            return body.equalsIgnoreCase("true") || body.equals("1");
        } catch (Exception e) {
            return false;
        }
    }

    private void logoutPreviousAccount() {
        try (Response response = client.newCall(request(baseUrl + "/xtgl/login_logoutAccount.html")
                .header("Referer", baseUrl + "/xtgl/login_slogin.html")
                .header("X-Requested-With", "XMLHttpRequest")
                .post(new FormBody.Builder().add("csrfTokenLogout", "").build()).build()).execute()) {
            // The login page may ask to clear an earlier account in this session.
        } catch (Exception e) {
            Log.d(TAG, "No previous session to close");
        }
    }

    private RequestBody buildLoginFormBody(String account, String password, String captcha) throws Exception {
        String submittedPassword = password;
        if (requiresPasswordEncryption()) {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, rsaPublicKey);
            submittedPassword = Base64.getEncoder().encodeToString(
                    cipher.doFinal(password.getBytes(StandardCharsets.UTF_8)));
        }
        FormBody.Builder payload = new FormBody.Builder();
        Set<String> added = new LinkedHashSet<>();
        for (Map.Entry<String, String> input : loginFormInputs) {
            String name = input.getKey();
            String value = input.getValue();
            if (name.equals("yhm")) value = account;
            if (name.equals("mm")) value = submittedPassword;
            if (name.equals("yzm")) {
                if (captcha.trim().isEmpty()) continue;
                value = captcha;
            }
            payload.add(name, value);
            added.add(name);
        }
        if (!added.contains("yhm")) payload.add("yhm", account);
        if (!added.contains("mm")) payload.add("mm", submittedPassword);
        if (!captcha.trim().isEmpty() && !added.contains("yzm")) payload.add("yzm", captcha);
        return payload.build();
    }

    private boolean looksLikeLoginPage(String html, String finalUrl) {
        Document document = Jsoup.parse(html);
        return finalUrl.toLowerCase(Locale.ROOT).contains("login_slogin")
                || (!document.select("#yhm, input[name=yhm]").isEmpty()
                    && !document.select("#mm, input[name=mm]").isEmpty())
                || (html.contains("用户登录") && html.toLowerCase(Locale.ROOT).contains("csrftoken"));
    }

    private String parseLoginError(String html) {
        Document document = Jsoup.parse(html);
        for (Element element : document.select("#tips, .alert-danger, .sl_danger, .bg_danger")) {
            if (!element.text().trim().isEmpty()) return element.text().trim();
        }
        return html.contains("验证码") || html.toLowerCase(Locale.ROOT).contains("kaptcha")
                ? "登录失败，系统可能要求验证码，请先确认网页端是否能正常登录"
                : "登录后仍停留在登录页，请检查账号、密码，或是否触发了额外验证";
    }

    private static String normalizeAcademicYear(String year) {
        Matcher matcher = Pattern.compile("20\\d{2}").matcher(year.trim());
        return matcher.find() ? matcher.group() : guessAcademicYear();
    }

    private static String normalizeSemester(String semester) {
        Matcher matcher = Pattern.compile("\\d{1,2}").matcher(semester.trim());
        return matcher.find() ? matcher.group() : guessSemester();
    }

    private static String guessAcademicYear() {
        LocalDate now = LocalDate.now();
        return Integer.toString(now.getMonthValue() >= 8 ? now.getYear() : now.getYear() - 1);
    }

    private static String guessSemester() {
        return LocalDate.now().getMonthValue() >= 8 ? "3" : "12";
    }

    @Override
    public void close() {
        client.dispatcher().cancelAll();
        client.dispatcher().executorService().shutdown();
        client.connectionPool().evictAll();
    }
}
