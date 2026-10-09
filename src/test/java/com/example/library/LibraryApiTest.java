package com.example.library;

import com.example.library.entity.IssueRecord;
import com.example.library.repository.IssueRecordRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End-to-end check of auth, roles, CRUD, issue/return and fine calculation (uses in-memory H2). */
@SpringBootTest
@AutoConfigureMockMvc
class LibraryApiTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired IssueRecordRepository issueRepo;

    private JsonNode json(String body) throws Exception {
        return om.readTree(body);
    }

    private String post(String url, String token, String body, int expected) throws Exception {
        var req = MockMvcRequestBuilders.post(url).contentType(MediaType.APPLICATION_JSON).content(body);
        if (token != null) req.header("Authorization", "Bearer " + token);
        return mvc.perform(req).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
    }

    private String get(String url, String token, int expected) throws Exception {
        var req = MockMvcRequestBuilders.get(url);
        if (token != null) req.header("Authorization", "Bearer " + token);
        return mvc.perform(req).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
    }

    private String put(String url, String token, String body, int expected) throws Exception {
        var req = MockMvcRequestBuilders.put(url).contentType(MediaType.APPLICATION_JSON).content(body == null ? "" : body);
        req.header("Authorization", "Bearer " + token);
        return mvc.perform(req).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
    }

    private void delete(String url, String token, int expected) throws Exception {
        mvc.perform(MockMvcRequestBuilders.delete(url).header("Authorization", "Bearer " + token)).andExpect(status().is(expected));
    }

    @Test
    void fullLibraryFlow() throws Exception {
        // 1. Protected endpoints need a token
        get("/api/books", null, 401);

        // 2. Login: wrong password is rejected, admin login works
        post("/api/auth/login", null, "{\"username\":\"admin\",\"password\":\"wrong\"}", 401);
        String adminToken = json(post("/api/auth/login", null,
                "{\"username\":\"admin\",\"password\":\"admin123\"}", 200)).get("token").asText();

        // 3. Admin creates a book (validation + duplicate ISBN check)
        post("/api/books", adminToken, "{\"title\":\"\",\"author\":\"A\",\"isbn\":\"X\",\"totalCopies\":1}", 400);
        String bookBody = "{\"title\":\"Test Driven Java\",\"author\":\"Jane Doe\",\"isbn\":\"TEST-ISBN-1\","
                + "\"category\":\"Java\",\"totalCopies\":2}";
        long bookId = json(post("/api/books", adminToken, bookBody, 201)).get("id").asLong();
        post("/api/books", adminToken, bookBody, 409);

        // 4. A new member registers; members may read books but not change them
        String memberToken = json(post("/api/auth/register", null,
                "{\"username\":\"tester\",\"password\":\"secret1\",\"name\":\"Test User\","
                        + "\"email\":\"tester@example.com\",\"phone\":\"123\"}", 201)).get("token").asText();
        get("/api/books?search=test", memberToken, 200);
        post("/api/books", memberToken, bookBody, 403);
        get("/api/members", memberToken, 403);

        // 5. Admin finds the member and issues the book
        long memberId = -1;
        for (JsonNode m : json(get("/api/members", adminToken, 200))) {
            if ("tester".equals(m.path("username").asText())) memberId = m.get("id").asLong();
        }
        assertTrue(memberId > 0, "registered member should be listed");

        String issueBody = "{\"bookId\":" + bookId + ",\"memberId\":" + memberId + "}";
        long issueId = json(post("/api/issues", adminToken, issueBody, 201)).get("id").asLong();
        assertEquals(1, json(get("/api/books/" + bookId, adminToken, 200)).get("availableCopies").asInt());
        post("/api/issues", adminToken, issueBody, 409); // same member cannot take the same book twice

        // 6. Member sees only their own issued books
        assertEquals(1, json(get("/api/issues/my", memberToken, 200)).size());
        get("/api/issues", memberToken, 403);

        // 7. Make the loan 3 days late, then return it: fine = 3 x 5 = 15
        IssueRecord record = issueRepo.findById(issueId).orElseThrow();
        record.setDueDate(LocalDate.now().minusDays(3));
        issueRepo.save(record);

        JsonNode returned = json(put("/api/issues/" + issueId + "/return", adminToken, null, 200));
        assertEquals("RETURNED", returned.get("status").asText());
        assertEquals(0, new BigDecimal("15").compareTo(new BigDecimal(returned.get("fine").asText())));
        assertEquals(2, json(get("/api/books/" + bookId, adminToken, 200)).get("availableCopies").asInt());
        put("/api/issues/" + issueId + "/return", adminToken, null, 409); // cannot return twice

        // 8. Books with history cannot be deleted; unused books can
        delete("/api/books/" + bookId, adminToken, 409);
        long spareId = json(post("/api/books", adminToken,
                "{\"title\":\"Spare\",\"author\":\"B\",\"isbn\":\"TEST-ISBN-2\",\"totalCopies\":1}", 201)).get("id").asLong();
        delete("/api/books/" + spareId, adminToken, 204);

        // 9. Dashboard is admin only
        get("/api/dashboard", adminToken, 200);
        get("/api/dashboard", memberToken, 403);
    }
}
