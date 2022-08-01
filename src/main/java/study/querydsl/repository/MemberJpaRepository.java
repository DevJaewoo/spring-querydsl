package study.querydsl.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDTO;
import study.querydsl.dto.QMemberTeamDTO;
import study.querydsl.entity.Member;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

import static org.springframework.util.StringUtils.*;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.*;

@Repository
@RequiredArgsConstructor
public class MemberJpaRepository {

    private final EntityManager em;
    private final JPAQueryFactory queryFactory;

    public void save(Member member) {
        em.persist(member);
    }

    public Optional<Member> findById(Long id) {
        return Optional.ofNullable(em.find(Member.class, id));
    }

    public List<Member> findAll() {
        return em.createQuery("select m from Member m", Member.class)
                .getResultList();
    }

    public List<Member> findAll_QueryDSL() {
        return queryFactory
                .selectFrom(member)
                .fetch();
    }

    public List<Member> findByUsername(String username) {
        return em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", username)
                .getResultList();
    }

    public List<Member> findByUsername_QueryDSL(String username) {
        return queryFactory
                .selectFrom(member)
                .where(member.username.eq(username))
                .fetch();
    }

    public List<MemberTeamDTO> searchByBuilder(MemberSearchCondition memberSearchCondition) {

        BooleanBuilder booleanBuilder = new BooleanBuilder();
        if(hasText(memberSearchCondition.getUsername())) {
            booleanBuilder.and(member.username.eq(memberSearchCondition.getUsername()));
        }

        if(hasText(memberSearchCondition.getTeamName())) {
            booleanBuilder.and(team.name.eq(memberSearchCondition.getTeamName()));
        }

        if(memberSearchCondition.getAgeGoe() != null) {
            booleanBuilder.and(member.age.goe(memberSearchCondition.getAgeGoe()));
        }

        if(memberSearchCondition.getAgeLoe() != null) {
            booleanBuilder.and(member.age.loe(memberSearchCondition.getAgeLoe()));
        }

        return queryFactory
                .select(new QMemberTeamDTO(member.id, member.username, member.age, team.id, team.name))
                .from(member, team)
                .where(booleanBuilder)
                .fetch();
    }

    public List<MemberTeamDTO> searchByWhere(MemberSearchCondition memberSearchCondition) {
        return queryFactory
                .select(new QMemberTeamDTO(member.id, member.username, member.age, team.id, team.name))
                .from(member, team)
                .where(
                        usernameEq(memberSearchCondition.getUsername()),
                        teamNameEq(memberSearchCondition.getTeamName()),
                        ageGoe(memberSearchCondition.getAgeGoe()),
                        ageLoe(memberSearchCondition.getAgeLoe())
                )
                .fetch();
    }

    private BooleanExpression allEq(String username, String teamName, Integer ageGoe, Integer ageLoe) {
        return usernameEq(username).and(teamNameEq(teamName)).and(ageGoe(ageGoe)).and(ageLoe(ageLoe));
    }

    private BooleanExpression usernameEq(String username) {
        return hasText(username) ? member.username.eq(username) : null;
    }

    private BooleanExpression teamNameEq(String teamName) {
        return hasText(teamName) ? team.name.eq(teamName) : null;
    }

    private BooleanExpression ageGoe(Integer ageGoe) {
        return (ageGoe != null) ? member.age.goe(ageGoe) : null;
    }

    private BooleanExpression ageLoe(Integer ageLoe) {
        return (ageLoe != null) ? member.age.goe(ageLoe) : null;
    }
}
