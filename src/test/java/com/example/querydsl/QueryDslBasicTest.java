package com.example.querydsl;

import com.example.querydsl.entity.Member;
import com.example.querydsl.entity.QMember;
import com.example.querydsl.entity.Team;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
public class QueryDslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before(){
        queryFactory = new JPAQueryFactory(em);
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);

        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    void startJPQL() {
        // member1 가져오기.
        Member findByJPQL = em.createQuery("select m from Member" +
                        " m where m.userName = :userName", Member.class)
                .setParameter("userName", "member1") // 왼쪽 name 값은 엔티티 필드 이름이 아니라 쿼리의 파라미터 값.
                .getSingleResult();
        ;

        assertThat(findByJPQL.getUserName()).isEqualTo("member1");
    }

    @Test
    void queryDslExample() {
//        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        // Q타입이 필요 -> gradle -> Task -> other -> compileQuerydsl
        QMember m = new QMember("m"); // -> "m" 어떤 QMember 인지를 구분하는 이름을 주는것.

        Member findMember = queryFactory.select(m)
                .from(m)
                .where(m.userName.eq("member1"))
                .fetchOne();

        assertThat(findMember.getUserName()).isEqualTo("member1");

    }

}
