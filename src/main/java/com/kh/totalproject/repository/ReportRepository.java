package com.kh.totalproject.repository;

import com.kh.totalproject.entity.Board;
import com.kh.totalproject.entity.ReportBoard;
import com.kh.totalproject.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<ReportBoard, Long> {
    @Query("SELECT r FROM ReportBoard r WHERE r.user.userKey = :userKey")
    Page<ReportBoard> findByUserKey(Long userKey, Pageable pageable);

    boolean existsByUserAndBoard(User user, Board board);
}
