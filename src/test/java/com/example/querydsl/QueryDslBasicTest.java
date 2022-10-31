package com.example.querydsl;

import com.example.querydsl.entity.Member;
import com.example.querydsl.entity.QMember;
import com.example.querydsl.entity.QTeam;
import com.example.querydsl.entity.Team;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;

import java.util.List;

import static com.example.querydsl.entity.QMember.*;
import static com.example.querydsl.entity.QTeam.*;
import static com.querydsl.jpa.JPAExpressions.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
public class QueryDslBasicTest {

    @PersistenceContext
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

    /* 집합 함수
    * JPQL
    * SELECT
    * COUNT(m), // 회원수
    * SUN(m.age), // 나이 합
    * AVG(m.age), // 평균 나이
    * MAX(m.age) // 최대 나이
    * MIN(m.age) // 최소 나이
    * from Member m
    * */

    @Test
    void aggregation() throws Exception {

        List<Tuple> result = queryFactory.select( // 튜플 -> 데이터 타입이 여러개일때 (실무에선 dto로 뽑아오기 떄문에 잘 쓰이진 않음.)
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min())
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);

    }

    /*
    * 팀의 이름과 각 팀의 평균 연령을구해라.
    * */
    @Test
    void group() throws Exception{

        List<Tuple> result = queryFactory.select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    /*
    * 기본 조인
    * join(조인 대상, 별칭으로 사용할 Q타입)
    * 팀 A에 소속된 모든 회원
    * */

    @Test
    void join() throws Exception {

        List<Member> result = queryFactory.selectFrom(member)
                .join(member.team, team)
//                .leftJoin(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();
        assertThat(result).extracting("userName").containsExactly("member1", "member2");
    }

    /*
    * 세타 조인
    * 회원의 이름이 팀 이름과 같은 회원 조회(억지성 예제)
    * 연관관계 없이 조인
    * 외부조인 불가능 -> 조인 on을 사용하면 외부조인 가능
    * */
    @Test
    void thetaJoin() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Member> result = queryFactory
                .select(member) // 기존에는 멤버와 연관관계가 있는 team 을 찍은 담에 team을 가져왔지만 세타 조인은
                .from(member, team) // 두개를 그냥 나열
                .where(member.userName.eq(team.name))
                .fetch();

        assertThat(result)
                .extracting("userName")
                .containsExactly("teamA", "teamB");
    }

    /*
    * 조인 - on절
    * ON절을 활용한 조인
    * 조인 대상 필터링
    * 연관관계 없는 엔티티 외부 조인
    * */

    /*
    * 예) 회원과 팀을 저인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
    * JPQL : select m ,t from Member m left join m.team t on t.name = 'teamA'
    * */
    @Test
    void joinOnFiltering() {
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA"))
//                .join(member.team, team).on(team.name.eq("teamA")) // -> join(member.team, team).where(team.name.eq("teamA")) 와 결과가 같다.
                .fetch();
        for (Tuple tuple : result) {
            System.out.println("tuple : " + tuple);
        }
    }

    /*
    * 연관관계 없는 엔티티 외부 조인
    * ex) 회원의 이름과 팀의 이름이 같은 대상 '외부 조인'
    * JPQL : select m,t from Member m left join Team t on m.userName = t.name
    * SQL : SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.user_name = t.name    *
    * */
    @Test
    void joinOnNoRelation() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.userName.eq(team.name))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple : " + tuple);
        }
    }

    /*
    * 페치 조인
    * 페치 조인은 SQL에서 제공하는 기능은 아니지만 SQL조인을 활용해서 연관된 엔티티를 SQL 한번에 조회하는 기능이다. 주로 성능 최적화에 사용하는 방법.
    *
    * */

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    void fetchJoinNotUse() {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.userName.eq("member1"))
                .fetchOne();
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());// 이미 로딩된 엔티티인지 결과 반환
        assertThat(loaded).as("페치 조인 미적용").isFalse(); // 페치조인 미적용일때는 false가 나와야함.
    }

    @Test
    void fetchJoinUse() {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin() // 사용방법은 이렇게 뒤에 페치조인을 붙여주면 된다. 페치조인은 정말 많이 사용하므로 꼭 알아둘것.
                .where(member.userName.eq("member1"))
                .fetchOne();
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam()); // 이미 로딩된 엔티티인지 결과 반환
        assertThat(loaded).as("페치 조인 적용").isTrue();
    }

    /*
    * 서브쿼리
    * com.querydsl.jpa.JPAExpressions 사용
    * 나이가 가장 많은 회원 조회
    * */
    @Test
    void subQuery() {
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory.
                selectFrom(member)
                .where(member.age.eq(
                        select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result)
                .extracting("age")
                .containsExactly(40);
    }

    /*
    * 나이가 평균 이상인 회원
    * */
    @Test
    void subQueryGoe() {
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory.
                selectFrom(member)
                .where(member.age.goe(
                        select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result)
                .extracting("age")
                .containsExactly(30, 40);
    }


    @Test
    void subQueryIn() {
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory.
                selectFrom(member)
                .where(member.age.in(
                        select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();

        assertThat(result)
                .extracting("age")
                .containsExactly(20, 30, 40);
    }

    @Test
    void selectSubQuery() {

        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = queryFactory.select(member.userName,
                        select(memberSub.age.avg())
                                .from(memberSub))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple : " + tuple);
        }

    }

    /*
    * Jpa 서브쿼리 한계
    * JPA JPQL 서브쿼리의 한계점으로 from 절의 서브쿼리(인라인 뷰)는 지원하지 않는다.
    * 당연히 Querydsl도 지원하지 않는다. 하이버네이트 구현체를 사용하면 select절의 서브 쿼리는 지원한다.
    * Querydsl도 하이버네이트 구현체를 사용하면 selet 절의 서브쿼리를 지원한다.
    * form 절의 서브쿼리 해결방안 :
    * 1. 서브쿼리를 join으로 변경한다. (간으한 상황도 있고, 불가능한 상황도 있다,)
    * 2. 애플리케이션에서 쿼리를 2번 분리해서 실핸한다.
    * 3. nativeSQL을 사용한다.
    * */


    /*
    * Case 문
    * select, 조건절(where)에서 사용 가능
    * */
    @Test
    void basicCase() {

        List<String> result = queryFactory.select(member.age
                .when(10).then("열상")
                .when(20).then("스무살")
                .otherwise("기타")
        ).from(member).fetch();

        for (String s : result) {
            System.out.println("s : " + s);
        }
    }

    @Test
    void complexCase() { // 웬만하면 디비에서 하지 말고 애플리케이션에서 케이스문 으로 디비에서 케이스문을 쓰는 경우는 거의 없다.
        List<String> result = queryFactory.select(new CaseBuilder()
                .when(member.age.between(0, 20)).then("0 ~ 20")
                .when(member.age.between(21, 30)).then("21 ~ 30")
                .otherwise("기타")
        ).from(member).fetch();

        for (String s : result) {
            System.out.println("s : " + s);
        }
    }

    /*
    * 상수, 문자 더하기
    * 상수가 필요하면 Expressions.constant(xxx) 사용
    * */

    @Test
    void constant() {
        List<Tuple> result = queryFactory.select(member.userName, Expressions.constant("A"))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple : " + tuple);
        }
    }

    @Test
    void concat() {
        // userName_age
        List<String> result = queryFactory.select(member.userName.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.userName.eq("member1"))
                .fetch();

        for (String s : result) {
            System.out.println("s : " + s);
        }
    }

    /*
    * 참고 : member.age.stringValue() 부분이 중요한데, 문자가 아닌 다른 타입들은 stringValue() 로 문자로
    * 변환할 수 있다. 이 방법은 ENUM을 처리할 떄도 자주 사용한다.
    * */






}
