package com.example.library.repository;

import com.example.library.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, Long> {

    /** pattern must already be lower case and wrapped in % signs, e.g. "%java%". */
    @Query("select b from Book b where lower(b.title) like :pattern "
            + "or lower(b.author) like :pattern or lower(b.isbn) like :pattern")
    Page<Book> search(@Param("pattern") String pattern, Pageable pageable);

    boolean existsByIsbn(String isbn);

    boolean existsByIsbnAndIdNot(String isbn, Long id);
}
