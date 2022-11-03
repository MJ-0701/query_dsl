package com.example.querydsl.entity.repository;

import com.example.querydsl.controller.dto.MemberSearchCondition;
import com.example.querydsl.controller.dto.MemberTeamDto;
import com.example.querydsl.controller.dto.QMemberDto;
import com.example.querydsl.controller.dto.QMemberTeamDto;
import com.example.querydsl.entity.Member;
import com.example.querydsl.entity.QMember;
import com.example.querydsl.entity.QTeam;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;

import static com.example.querydsl.entity.QMember.member;
import static com.example.querydsl.entity.QTeam.team;
import static org.springframework.util.StringUtils.*;

@Repository
public class MemberJpaRepository {


    private final EntityManager em;

    // @Bean 으로 등록해서 사용하면 롬복의 @RequiredArgumentConstructor 사용 가능
    private final JPAQueryFactory queryFactory;

    public MemberJpaRepository(EntityManager em) {
        this.em = em;
        this.queryFactory = new JPAQueryFactory(em);
    }

    public void save(Member member) {
        em.persist(member);
    }

    public Optional<Member> findById(Long id) {
        Member findMember = em.find(Member.class, id);
        return Optional.ofNullable(findMember);
    }

    public List<Member> findAll() {
        return em.createQuery("select m from Member m", Member.class).getResultList();
    }

    public List<Member> findByUserName(String userName) {
        return em.createQuery("select m from Member m where m.userName = :userName", Member.class)
                .setParameter("userName", userName)
                .getResultList();
    }

    public List<Member> findAllQuerydsl() {
        return queryFactory.selectFrom(member).fetch();
    }

    public List<Member> findByUserNameQuerydsl(String userName) {
        return queryFactory
                .selectFrom(member)
                .where(member.userName.eq(userName))
                .fetch();
    }


    public List<MemberTeamDto> searchByBuilder(MemberSearchCondition condition) {

        BooleanBuilder builder = new BooleanBuilder();
        if (hasText(condition.getUserName())) {
            builder.and(member.userName.eq(condition.getUserName()));
        }

        if (hasText(condition.getTeamName())) {
            builder.and(team.name.eq(condition.getTeamName()));
        }

        if (condition.getAgeGoe() != null) {
            builder.and(member.age.goe(condition.getAgeGoe()));
        }
        if (condition.getAgeLoe() != null) {
            builder.and(member.age.loe(condition.getAgeLoe()));
        }

        return queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.userName,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")
                ))
                .from(member)
                .leftJoin(member.team, team)
                .where(builder)
                .fetch();
    }

    public List<MemberTeamDto> search(MemberSearchCondition condition) {
        return queryFactory
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
                .fetch();
    }

    public List<Member> searchMember(MemberSearchCondition condition) { // 재사용 예제
        return queryFactory
                .selectFrom(member)
                .leftJoin(member.team, team)
                .where( // 메서드가 재사용 되는게 정말 큰 장점.
                        userNameEq(condition.getUserName()),
                        teamNameEq(condition.getTeamName()),
                        ageBetween(condition.getAgeGoe(), condition.getAgeLoe()) // 이런식으로 메서드들을 묶어서 사용 가능 -> 4개를 다 묶어서 condition만 사용하는것도 물론 가능하다.-> null check만 유의하자.
                )
                .fetch();
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
