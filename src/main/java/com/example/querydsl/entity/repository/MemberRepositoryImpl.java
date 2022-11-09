package com.example.querydsl.entity.repository;

import com.example.querydsl.controller.dto.MemberSearchCondition;
import com.example.querydsl.controller.dto.MemberTeamDto;
import com.example.querydsl.controller.dto.QMemberTeamDto;
import com.example.querydsl.entity.Member;
import com.querydsl.core.QueryResults;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.data.support.PageableExecutionUtils;

import javax.persistence.EntityManager;
import java.util.List;

import static com.example.querydsl.entity.QMember.member;
import static com.example.querydsl.entity.QTeam.team;
import static org.springframework.util.StringUtils.hasText;

public class MemberRepositoryImpl extends QuerydslRepositorySupport implements MemberRepositoryCustom{

    private final JPAQueryFactory queryFactory;
//
//    public MemberRepositoryImpl(EntityManager em) {
//        this.queryFactory = new JPAQueryFactory(em);
//    }

    public MemberRepositoryImpl(EntityManager em) {
        super(Member.class);
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public List<MemberTeamDto> search(MemberSearchCondition condition) {

        /*
        * QuerydslRepositorySupport -> 3쿼리 팩토리가 나오기 전 3버전에서 사용하던 방법 페이징이 편리함 단, 정렬은 안됨.
        * */
        List<MemberTeamDto> result = from(member) // 3버전에 만들어진 기능인데 3버전엔 from 부터 시작했다.
                .leftJoin(member.team, team)
                .where(
                        userNameEq(condition.getUserName()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                ).select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.userName,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")
                ))
                .fetch();
        return result;


//        return queryFactory
//                .select(new QMemberTeamDto(
//                        member.id.as("memberId"),
//                        member.userName,
//                        member.age,
//                        team.id.as("teamId"),
//                        team.name.as("teamName")
//                ))
//                .from(member)
//                .leftJoin(member.team, team)
//                .where( // 메서드가 재사용 되는게 정말 큰 장점.
//                        userNameEq(condition.getUserName()),
//                        teamNameEq(condition.getTeamName()),
//                        ageGoe(condition.getAgeGoe()),
//                        ageLoe(condition.getAgeLoe())
//                )
//                .fetch();
    }

    @Override
    public Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable) {
        QueryResults<MemberTeamDto> results = queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.userName,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")
                ))
                .from(member)
                .leftJoin(member.team, team)
                .where( // 메서드가 재사용 되는게 정말 큰 장점.
                        userNameEq(condition.getUserName()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                /*
                * fetchCount, fetchResult는 둘다 querydsl 내부에서 count용 쿼리를 만들어서 실행해야 하는데, 이때 작성한 select 쿼리를 기반으로 count 쿼리를 만들어냅니다. 그런데 이 기능이 select 구문을 단순히 count 처리하는 것으로 바꾸는 정도여서, 단순한 쿼리에서는 잘 동작하는데, 복잡한 쿼리에서는 잘 동작하지 않습니다.
                  이럴때는 명확하게 카운트 쿼리를 별도로 작성하고, 말씀하신 대로 fetch()를 사용해서 해결해야 합니다.
                * */
                .fetchResults();// -> 컨텐츠용 쿼리 + 카운트 쿼리 : 쿼리 2방 날려서 둘다 가져옴.

        List<MemberTeamDto> content = results.getResults();// 컨텐츠
        long total = results.getTotal();// 토탈 카운트

        return new PageImpl<>(content, pageable, total);


    }

    /*
    * QuerydslRepositorySupport 페이징 방법
    * */
    public Page<MemberTeamDto> searchPageSimple2(MemberSearchCondition condition, Pageable pageable) {
        JPQLQuery<MemberTeamDto> jpaQuery = from(member)
                .leftJoin(member.team, team)
                .where( // 메서드가 재사용 되는게 정말 큰 장점.
                        userNameEq(condition.getUserName()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.userName,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")
                ));

        JPQLQuery<MemberTeamDto> query = getQuerydsl().applyPagination(pageable, jpaQuery); // 페이지네이션

        List<MemberTeamDto> pagination = query.fetch(); // 페이지네이션 적용

        return null;


    }

    @Override
    public Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable) {
        List<MemberTeamDto> content = queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.userName,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")
                ))
                .from(member)
                .leftJoin(member.team, team)
                .where( // 메서드가 재사용 되는게 정말 큰 장점.
                        userNameEq(condition.getUserName()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch(); // -> 컨텐트를 꺼내는 메소드로 리팩토링 해서 재사용 하는 방법도 있음. 편한거 쓰면 됨

//        long count = queryFactory
//                .select(member)
//                .from(member)
//                .leftJoin(member.team, team)
//                .where(
//                        userNameEq(condition.getUserName()),
//                        teamNameEq(condition.getTeamName()),
//                        ageGoe(condition.getAgeGoe()),
//                        ageLoe(condition.getAgeLoe())
//                )
//                .fetch().size(); // 강의에서는 fetchCount 메소드를 사용했으나 deprecated 됐고 문서에서는 대안으로 fetch().size() 를 사용하라고 나와있다. 카운트 쿼리도 마찬가지로 메소드로 리팩토링 해서 써도 됨.

//        return new PageImpl<>(content, pageable, count);

        // 카운트 쿼리 최적화

        JPAQuery<Member> countQuery = queryFactory
                .select(member)
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        userNameEq(condition.getUserName()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                );

        return PageableExecutionUtils.getPage(content,pageable,countQuery.fetch()::size); // () -> countQuery.fetch().size(),
    }

    // Querydsl 5버전으로 수정
    public Page<MemberTeamDto> searchPageComplex2(MemberSearchCondition condition,
                                                 Pageable pageable) {
        List<MemberTeamDto> content = queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.userName,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")))
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        userNameEq(condition.getUserName()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(member.count())
                .from(member)
                .leftJoin(member.team, team)
                .where(userNameEq(condition.getUserName()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                );

        return PageableExecutionUtils.getPage(content, pageable,
                countQuery::fetchOne);
    }

    private BooleanExpression ageBetween(int ageLoe, int ageGoe) { // 보여주기식 조합 예쩨

        return ageGoe(ageGoe).and(ageLoe(ageLoe));
    }

    private BooleanExpression userNameEq(String userName) {
        return hasText(userName) ? member.userName.eq(userName) : null;
    }

    private BooleanExpression teamNameEq(String teamName) {
        return hasText(teamName) ? team.name.eq(teamName) : null;
    }

    private BooleanExpression ageGoe(Integer ageGoe) {
        return ageGoe != null ? member.age.goe(ageGoe) : null;
    }

    private BooleanExpression ageLoe(Integer ageLoe) {
        return ageLoe !=null ? member.age.loe(ageLoe) : null;
    }
}
