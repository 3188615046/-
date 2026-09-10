package com.ncvt.kebiao;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.ncvt.kebiao.data.remote.ScrapeResult;
import com.ncvt.kebiao.data.remote.ZhengfangScraper;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;
import javax.crypto.Cipher;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class JavaScraperTest {
    @Test
    public void loginEncryptsPasswordAndRetainsSessionForTimetable() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keys = generator.generateKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keys.getPublic();
        String keyJson = "{\"modulus\":\"" + Base64.getEncoder().encodeToString(publicKey.getModulus().toByteArray())
                + "\",\"exponent\":\"" + Base64.getEncoder().encodeToString(publicKey.getPublicExponent().toByteArray()) + "\"}";
        AtomicReference<RecordedRequest> submitted = new AtomicReference<>();
        AtomicReference<RecordedRequest> timetableRequest = new AtomicReference<>();
        try (MockWebServer server = new MockWebServer(); ZhengfangScraper scraper = new ZhengfangScraper()) {
            server.setDispatcher(new Dispatcher() {
                @Override
                public MockResponse dispatch(RecordedRequest request) {
                    String path = request.getRequestUrl().encodedPath();
                    if (path.endsWith("login_getPublicKey.html")) return new MockResponse().setBody(keyJson);
                    if (path.endsWith("yhgl_cxXxqrCheck.html")) return new MockResponse().setBody("false");
                    if (path.endsWith("login_logoutAccount.html")) return new MockResponse().setBody("ok");
                    if (path.endsWith("login_slogin.html") && "POST".equals(request.getMethod())) {
                        submitted.set(request);
                        return new MockResponse().setResponseCode(302).addHeader("Location", "/jwglxt/home");
                    }
                    if (path.endsWith("login_slogin.html")) {
                        return new MockResponse().addHeader("Set-Cookie", "SESSION=demo; Path=/jwglxt; HttpOnly")
                                .setBody("<form action='/jwglxt/xtgl/login_slogin.html'>"
                                        + "<input name='csrftoken' value='token'><input name='yhm'><input name='mm'>"
                                        + "<input name='mmsfjm' value='1'></form>");
                    }
                    if (path.endsWith("xskbcx_cxXsKb.html") && "POST".equals(request.getMethod())) {
                        timetableRequest.set(request);
                        return new MockResponse().setBody("{\"kbList\":[{\"kcmc\":\"Java\",\"xqj\":2,"
                                + "\"jc\":\"1-2\",\"zcd\":\"1-16周\"}]}");
                    }
                    return new MockResponse().setBody("<html>Timetable</html>");
                }
            });
            server.start();
            ScrapeResult ready = scraper.fetchLoginPage(server.url("/jwglxt/xtgl/login_slogin.html").toString());
            assertTrue(ready instanceof ScrapeResult.LoginPageReady);
            assertFalse(((ScrapeResult.LoginPageReady) ready).requiresCaptcha);
            assertTrue(scraper.login("demo-account", "p@ss+word", "") instanceof ScrapeResult.LoginSuccess);
            RecordedRequest login = submitted.get();
            assertNotNull(login);
            assertTrue(login.getHeader("Cookie").contains("SESSION=demo"));
            HttpUrl form = HttpUrl.get("http://localhost/?" + login.getBody().readUtf8());
            assertEquals("demo-account", form.queryParameter("yhm"));
            assertEquals("token", form.queryParameter("csrftoken"));
            assertNotEquals("p@ss+word", form.queryParameter("mm"));
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, keys.getPrivate());
            assertEquals("p@ss+word", new String(cipher.doFinal(Base64.getDecoder().decode(form.queryParameter("mm"))),
                    java.nio.charset.StandardCharsets.UTF_8));
            ScrapeResult result = scraper.fetchCourses("2025-2026", "12", "N2151");
            assertTrue(result instanceof ScrapeResult.CoursesFetched);
            assertEquals("Java", ((ScrapeResult.CoursesFetched) result).courses.get(0).name);
            HttpUrl payload = HttpUrl.get("http://localhost/?" + timetableRequest.get().getBody().readUtf8());
            assertEquals("2025", payload.queryParameter("xnm"));
            assertEquals("12", payload.queryParameter("xqm"));
        }
    }

    @Test
    public void captchaAndLoginErrorsAreReported() throws Exception {
        try (MockWebServer server = new MockWebServer(); ZhengfangScraper scraper = new ZhengfangScraper()) {
            server.setDispatcher(new Dispatcher() {
                @Override
                public MockResponse dispatch(RecordedRequest request) {
                    String path = request.getRequestUrl().encodedPath();
                    if (path.endsWith("kaptcha")) return new MockResponse().setBody("captcha-image");
                    if (path.endsWith("yhgl_cxXxqrCheck.html")) return new MockResponse().setBody("false");
                    if (path.endsWith("login_logoutAccount.html")) return new MockResponse().setBody("ok");
                    if ("POST".equals(request.getMethod())) {
                        return new MockResponse().setBody("<p id='tips'>验证码错误</p>"
                                + "<input name='yhm'><input name='mm'>");
                    }
                    return new MockResponse().setBody("<form action='/jwglxt/xtgl/login_slogin.html'>"
                            + "<input name='mmsfjm' value='0'><input name='yhm'><input name='mm'><input name='yzm'></form>");
                }
            });
            server.start();
            ScrapeResult ready = scraper.fetchLoginPage(server.url("/jwglxt").toString());
            assertTrue(((ScrapeResult.LoginPageReady) ready).requiresCaptcha);
            assertNotNull(scraper.fetchCaptcha());
            ScrapeResult result = scraper.login("demo", "password", "1234");
            assertTrue(result instanceof ScrapeResult.LoginFailed);
            assertEquals("验证码错误", ((ScrapeResult.LoginFailed) result).message);
        }
    }
}
