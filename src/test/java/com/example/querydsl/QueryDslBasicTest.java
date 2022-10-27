package com.example.querydsl;

import com.example.querydsl.entity.Member;
import com.example.querydsl.entity.QMember;
import com.example.querydsl.entity.Team;
import com.querydsl.core.QueryResults;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import java.util.List;

import static com.example.querydsl.entity.QMember.*;
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
//        QMember m = new QMember("m"); // -> "m" 어떤 QMember 인지를 구분하는 이름을 주는것. -> 같은테이블을 조인해야되는 경우에만 이렇게 이름을 지정해서 사용.

//        QMember m = QMember.member;
//
//        Member findMember = queryFactory.select(m)
//                .from(m)
//                .where(m.userName.eq("member1"))
//                .fetchOne();

        // static import 이렇게 쓰는걸 권장.
        Member findMember = queryFactory.select(member)
                .from(member)
                .where(member.userName.eq("member1"))
                .fetchOne();

        assertThat(findMember.getUserName()).isEqualTo("member1");

    }

    @Test
    void search() {
        Member findMember = queryFactory.selectFrom(member)
                .where(member.userName.eq("member1")
                .and(member.age.eq(10)))
                .fetchOne()
        ;

        assertThat(findMember.getUserName()).isEqualTo("member1");
    }

    @Test
    void searchAndParam() {
        Member findMember = queryFactory.selectFrom(member)
                .where( // 이렇게 여러개로 넘기면 다 and 로 연결된다.
                        member.userName.eq("member1"),
                        member.age.eq(10)
                )
                .fetchOne()
                ;
        /* fetch() -> 결과 list fetchOne() -> 단건 조회
        *  fetchFirst() : limit(1).fetchOne()
        *  fetchResult() : 페이징 정보 포함, total count 쿼리 추가 실행
        *  fetchCount() : count 쿼리로 변경해서 count 수 조회
        * */

        assertThat(findMember.getUserName()).isEqualTo("member1");
    }

    @Test
    void resultFetchTest() { // command + option + v -> 결과의 타입과 변수 자동 생성

//        List<Member> fetch = queryFactory.selectFrom(member)
//                .fetch();
//
//        Member fetchOne = queryFactory.selectFrom(QMember.member)
//                .fetchOne();
//
//        Member fetchFirst = queryFactory.selectFrom(member).fetchFirst();

//        QueryResults<Member> results = queryFactory
//                .selectFrom(member)
//                .fetchResults();
//
//        results.getTotal(); // 전체 몇페이지인지를 가져올떄 사용하는 전체 total 쿼리 -> 성능이 중요한 코드에서는 쓰면 안되고 그냥 쿼리 두방을 따로따로 날려야함.
//        List<Member> contents = results.getResults();

        long total = queryFactory.selectFrom(member).fetchCount();

    }
    /**
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 올림차순(asc)
     * 3. 단 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
     */
    @Test
    void sort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryFactory.selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(
                        member.age.desc(),
                        member.userName.asc().nullsLast()
                )
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        assertThat(member5.getUserName()).isEqualTo("member5");
        assertThat(member6.getUserName()).isEqualTo("member6");
        assertThat(memberNull.getUserName()).isNull();
    }

    @Test
    void paging1() {
        List<Member> result = queryFactory.selectFrom(member)
                .orderBy(member.userName.desc())
                .offset(1) // offset -> 앞에 몇개를 스킵할건지 -> 몇번째 부터 시작할건지 보통 0부터 시작 1 -> 하나를 skip
                .limit(2)
                .fetch();

        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    void paging2() { // 전체 조회수
        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.userName.desc())
                .offset(1) // offset -> 앞에 몇개를 스킵할건지 -> 몇번째 부터 시작할건지 보통 0부터 시작 1 -> 하나를 skip
                .limit(2)
                .fetchResults();

        assertThat(queryResults.getTotal()).isEqualTo(4);
        assertThat(queryResults.getLimit()).isEqualTo(2);
        assertThat(queryResults.getOffset()).isEqualTo(1);
        assertThat(queryResults.getResults().size()).isEqualTo(2);

    }

}
