package com.example.library.config;

import com.example.library.entity.Book;
import com.example.library.entity.Member;
import com.example.library.entity.Role;
import com.example.library.entity.User;
import com.example.library.repository.BookRepository;
import com.example.library.repository.MemberRepository;
import com.example.library.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/** Runs on startup: makes sure an admin exists and (optionally) adds demo data. */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository users;
    private final MemberRepository members;
    private final BookRepository books;
    private final PasswordEncoder encoder;

    @Value("${app.admin.username}")
    private String adminUsername;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Value("${app.seed-demo-data}")
    private boolean seedDemoData;

    @Override
    public void run(String... args) {
        if (!users.existsByUsername(adminUsername)) {
            createUser(adminUsername, adminPassword, Role.ADMIN);
            log.info("Created admin account '{}'", adminUsername);
        }

        if (!seedDemoData) {
            return;
        }

        if (books.count() == 0) {
            books.saveAll(List.of(
                    book("Clean Code", "Robert C. Martin", "9780132350884", "Programming", 3),
                    book("Effective Java", "Joshua Bloch", "9780134685991", "Java", 2),
                    book("Head First Java", "Kathy Sierra", "9780596009205", "Java", 4),
                    book("Spring in Action", "Craig Walls", "9781617297571", "Java", 2),
                    book("The Pragmatic Programmer", "Andrew Hunt", "9780135957059", "Programming", 3),
                    book("Introduction to Algorithms", "Thomas H. Cormen", "9780262046305", "Computer Science", 2),
                    book("Database System Concepts", "Abraham Silberschatz", "9780078022159", "Databases", 2),
                    book("Operating System Concepts", "Abraham Silberschatz", "9781119800361", "Computer Science", 3)));
            log.info("Added sample books");
        }

        if (!users.existsByUsername("student")) {
            User user = createUser("student", "student123", Role.MEMBER);
            Member member = new Member();
            member.setName("Demo Student");
            member.setEmail("student@example.com");
            member.setPhone("9999999999");
            member.setUser(user);
            members.save(member);
            log.info("Added demo member account 'student'");
        }
    }

    private User createUser(String username, String rawPassword, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(encoder.encode(rawPassword));
        user.setRole(role);
        return users.save(user);
    }

    private Book book(String title, String author, String isbn, String category, int copies) {
        Book b = new Book();
        b.setTitle(title);
        b.setAuthor(author);
        b.setIsbn(isbn);
        b.setCategory(category);
        b.setTotalCopies(copies);
        b.setAvailableCopies(copies);
        return b;
    }
}
