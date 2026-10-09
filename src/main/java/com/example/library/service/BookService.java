package com.example.library.service;

import com.example.library.dto.BookRequest;
import com.example.library.dto.PageResponse;
import com.example.library.entity.Book;
import com.example.library.exception.BadRequestException;
import com.example.library.exception.ConflictException;
import com.example.library.exception.ResourceNotFoundException;
import com.example.library.repository.BookRepository;
import com.example.library.repository.IssueRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository books;
    private final IssueRecordRepository issues;

    @Transactional(readOnly = true)
    public PageResponse<Book> search(String search, int page, int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("title").ascending());
        String text = search == null ? "" : search.trim().toLowerCase();
        return PageResponse.of(books.search("%" + text + "%", pageable));
    }

    @Transactional(readOnly = true)
    public Book get(Long id) {
        return books.findById(id).orElseThrow(() -> new ResourceNotFoundException("Book not found"));
    }

    @Transactional
    public Book create(BookRequest req) {
        String isbn = req.isbn().trim();
        if (books.existsByIsbn(isbn)) {
            throw new ConflictException("A book with this ISBN already exists");
        }
        Book book = new Book();
        apply(book, req);
        book.setAvailableCopies(req.totalCopies());
        return books.save(book);
    }

    @Transactional
    public Book update(Long id, BookRequest req) {
        Book book = get(id);
        String isbn = req.isbn().trim();
        if (books.existsByIsbnAndIdNot(isbn, id)) {
            throw new ConflictException("Another book already uses this ISBN");
        }

        // Keep "available" in step with the new total, but never let it go below zero.
        int issuedNow = book.getTotalCopies() - book.getAvailableCopies();
        if (req.totalCopies() < issuedNow) {
            throw new BadRequestException(
                    "Total copies cannot be less than the " + issuedNow + " copies currently issued");
        }
        apply(book, req);
        book.setAvailableCopies(req.totalCopies() - issuedNow);
        return books.save(book);
    }

    @Transactional
    public void delete(Long id) {
        Book book = get(id);
        if (issues.existsByBookId(id)) {
            throw new ConflictException("Cannot delete this book because it has issue history");
        }
        books.delete(book);
    }

    private void apply(Book book, BookRequest req) {
        book.setTitle(req.title().trim());
        book.setAuthor(req.author().trim());
        book.setIsbn(req.isbn().trim());
        book.setCategory(req.category() == null ? null : req.category().trim());
        book.setTotalCopies(req.totalCopies());
    }
}
