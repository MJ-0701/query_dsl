package com.example.querydsl.entity.repository;

import com.example.querydsl.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

import java.util.List;


public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom, QuerydslPredicateExecutor<Member> { // SpringDataJpa로 Querydsl 만들기

    List<Member> findByUserName(String userName);
}
