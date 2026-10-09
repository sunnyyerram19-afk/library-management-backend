package com.example.library.service;

import com.example.library.dto.MemberDto;
import com.example.library.dto.MemberRequest;
import com.example.library.entity.Member;
import com.example.library.entity.User;
import com.example.library.exception.ConflictException;
import com.example.library.exception.ResourceNotFoundException;
import com.example.library.repository.IssueRecordRepository;
import com.example.library.repository.MemberRepository;
import com.example.library.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository members;
    private final UserRepository users;
    private final IssueRecordRepository issues;

    @Transactional(readOnly = true)
    public List<MemberDto> list() {
        return members.findAllByOrderByNameAsc().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public MemberDto get(Long id) {
        return toDto(find(id));
    }

    @Transactional
    public MemberDto create(MemberRequest req) {
        String email = req.email().trim().toLowerCase();
        if (members.existsByEmail(email)) {
            throw new ConflictException("A member with this email already exists");
        }
        Member member = new Member();
        apply(member, req);
        return toDto(members.save(member));
    }

    @Transactional
    public MemberDto update(Long id, MemberRequest req) {
        Member member = find(id);
        String email = req.email().trim().toLowerCase();
        if (members.existsByEmailAndIdNot(email, id)) {
            throw new ConflictException("Another member already uses this email");
        }
        apply(member, req);
        return toDto(members.save(member));
    }

    @Transactional
    public void delete(Long id) {
        Member member = find(id);
        if (issues.existsByMemberId(id)) {
            throw new ConflictException("Cannot delete this member because they have issue history");
        }
        User user = member.getUser();
        members.delete(member);
        members.flush();
        if (user != null) {
            users.delete(user); // remove the login account as well
        }
    }

    private Member find(Long id) {
        return members.findById(id).orElseThrow(() -> new ResourceNotFoundException("Member not found"));
    }

    private void apply(Member member, MemberRequest req) {
        member.setName(req.name().trim());
        member.setEmail(req.email().trim().toLowerCase());
        member.setPhone(req.phone());
    }

    private MemberDto toDto(Member m) {
        return new MemberDto(m.getId(), m.getName(), m.getEmail(), m.getPhone(),
                m.getUser() == null ? null : m.getUser().getUsername());
    }
}
