package com.example.querydsl.entity.repository;

import com.example.querydsl.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom { // SpringDataJpa로 만들기

    List<Member> findByUserName(String userName);
}
