package com.example.querydsl.entity.repository;


import com.example.querydsl.controller.dto.MemberSearchCondition;
import com.example.querydsl.entity.Member;
import com.example.querydsl.entity.repository.support.Querydsl4RepositorySupport;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.example.querydsl.entity.QMember.member;
import static com.example.querydsl.entity.QTeam.team;
import static org.springframework.util.StringUtils.hasText;

@Repository
public class MemberTestRepository extends Querydsl4RepositorySupport {


    public MemberTestRepository() {
        super(Member.class);
    }

    public List<Member> basicSelect() {
        return select(member).from(member).fetch();
    }

    public List<Member> basicSelectFrom() {
        return selectFrom(member).fetch();
    }

    // 기존 QuerydslRepositorySupport 방식 -> 현재 사용하는건 직접 구현한 클래스를 사용하는거라 소팅이라던가 select 시작이라던가 이런게 다 가능하다. -> 기존 단점 보완
    public Page<Member> searchPageByApplyPage(MemberSearchCondition condition, Pageable pageable) {
        JPAQuery<Member> query = selectFrom(member)
                .leftJoin(member.team, team)
                .where(
                        userNameEq(condition.getUserName()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                );

        List<Member> content = getQuerydsl().applyPagination(pageable, query).fetch();
        return PageableExecutionUtils.getPage(content,pageable,query.fetch()::size);
    }

    // 커스텀 한 클래스를 이용하여 깔끔하게 표현 가능
    public Page<Member> applyPagination(MemberSearchCondition condition, Pageable pageable) {

        return applyPagination(pageable, query ->
                query.selectFrom(member)
                        .leftJoin(member.team, team)
                        .where(
                                userNameEq(condition.getUserName()),
                                teamNameEq(condition.getTeamName()),
                                ageGoe(condition.getAgeGoe()),
                                ageLoe(condition.getAgeLoe())
                        )
        );
    }

    public Page<Member> applyPagination2(MemberSearchCondition condition,
                                         Pageable pageable) {
        return applyPagination(pageable, contentQuery -> contentQuery
                        .selectFrom(member)
                        .leftJoin(member.team, team)
                        .where(userNameEq(condition.getUserName()),
                                teamNameEq(condition.getTeamName()),
                                ageGoe(condition.getAgeGoe()),
                                ageLoe(condition.getAgeLoe())),
                countQuery -> countQuery
                        .selectFrom(member)
                        .leftJoin(member.team, team)
                        .where(userNameEq(condition.getUserName()),
                                teamNameEq(condition.getTeamName()),
                                ageGoe(condition.getAgeGoe()),
                                ageLoe(condition.getAgeLoe()))
        );
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
