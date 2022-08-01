package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDTO;
import study.querydsl.dto.QMemberDTO;
import study.querydsl.dto.UserDTO;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;

import java.util.List;

import static com.querydsl.jpa.JPAExpressions.*;
import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.*;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @PersistenceContext
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before() {
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

        em.flush();
        em.clear();
    }

    @Test
    public void startJPQL() throws Exception {
        //given
        String qlString = "select m from Member m where m.username = :username";

        //when
        Member findMember = em.createQuery(qlString, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        //then
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQuerydsl() {
        //given

        //when
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        //then
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search() throws Exception {
        //given

        //when
        Member findMember = queryFactory
                .selectFrom(member)
                .where(
                        member.username.eq("member1"),
                        member.age.eq(10)
                )
                .fetchOne();

        //then
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void resultFetchTest() throws Exception {

        queryFactory
                .selectFrom(member)
                .fetch();

//        queryFactory
//                .selectFrom(member)
//                .fetchOne();

        queryFactory
                .selectFrom(member)
                .fetchFirst();

        queryFactory
                .select(member.count())
                .from(member)
                .fetchOne();
    }

    @Test
    public void sort() throws Exception {
        //given
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        //when
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        //then
        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    @Test
    public void paging() throws Exception {
        //given

        //when
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();

        //then
        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    public void aggregation() throws Exception {
        //given

        //when
        List<Tuple> result = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);

        //then
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
    }

    @Test
    public void groupBy() throws Exception {
        //given

        //when
        List<Tuple> result = queryFactory
                .select(
                        team.name,
                        member.age.avg()
                )
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        //then
        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);
        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }
    
    @Test
    public void join() throws Exception {
        //given
        
        //when
        List<Member> result = queryFactory.selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        //then
        assertThat(result)
                .extracting("username")
                .contains("member1", "member2");
    }

    @Test
    public void theta_join() throws Exception {
        //given
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        //when
        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        //then
        assertThat(result)
                .extracting("username")
                .contains("teamA", "teamB");
    }

    @Test
    public void join_on_filtering() throws Exception {
        //given

        //when
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA"))
                .fetch();

        //then
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    public void join_on_no_relation() throws Exception {
        //given
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        //when
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team)
                .on(member.username.eq(team.name))
                .fetch();

        //then
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void withoutFetch() throws Exception {
        //given
        em.flush();
        em.clear();

        //when
        Member result = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(result.getTeam());

        //then
        assertThat(loaded).isFalse();
    }

    @Test
    public void withFetch() throws Exception {
        //given
        em.flush();
        em.clear();

        //when
        Member result = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(result.getTeam());

        //then
        assertThat(loaded).isTrue();
    }

    @Test
    public void subQuery() throws Exception {
        //given
        QMember memberSub = new QMember("memberSub");

        //when
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(
                        member.age.goe(
                                select(memberSub.age.avg())
                                        .from(memberSub)
                        )
                )
                .fetch();

        //then
        assertThat(result)
                .extracting("age")
                .contains(30, 40);
    }

    @Test
    public void subQueryIn() throws Exception {
        //given
        QMember memberSub = new QMember("memberSub");

        //when
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(
                        member.age.in(
                                select(memberSub.age)
                                        .from(memberSub)
                                        .where(memberSub.age.gt(10))
                        )
                )
                .fetch();

        //then
        assertThat(result)
                .extracting("age")
                .contains(20, 30, 40);
    }

    @Test
    public void selectSubQuery() throws Exception {
        //given
        QMember memberSub = new QMember("memberSub");

        //when
        List<Tuple> result = queryFactory
                .select(member.username,
                        select(memberSub.age.avg())
                                .from(memberSub)
                )
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }

        //then
    }

    @Test
    public void basicCase() throws Exception {
        //given

        //when
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        //then
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }


    @Test
    public void complexCase() throws Exception {
        //given

        //when
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        //then
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void constant() throws Exception {
        //given

        //when
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        //then
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    public void concat() throws Exception {
        //given

        //when
        List<String> fetch = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .fetch();

        //then
        for (String s : fetch) {
            System.out.println("s = " + s);
        }
    }
    
    @Test
    public void projection() throws Exception {
        //given
        
        //when
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        //then
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void tupleProjection() throws Exception {
        //given

        //when
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        //then
        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username = " + username);
            System.out.println("age = " + age);
        }
    }

    @Test
    public void findDTOByJPQL() throws Exception {
        //given

        //when
        List<MemberDTO> result = em.createQuery("select new study.querydsl.dto.MemberDTO(m.username, m.age) from Member m", MemberDTO.class)
                .getResultList();

        //then
        for (MemberDTO memberDTO : result) {
            System.out.println("memberDTO = " + memberDTO);
        }
    }

    @Test
    public void findDTOBySetter() throws Exception {
        //given

        //when
        List<MemberDTO> fetch = queryFactory
                .select(Projections.bean(MemberDTO.class, member.username, member.age))
                .from(member)
                .fetch();

        //then
        for (MemberDTO memberDTO : fetch) {
            System.out.println("memberDTO = " + memberDTO);
        }
    }

    @Test
    public void findDTOByField() throws Exception {
        //given

        //when
        List<MemberDTO> fetch = queryFactory
                .select(Projections.fields(MemberDTO.class, member.username, member.age))
                .from(member)
                .fetch();

        //then
        for (MemberDTO memberDTO : fetch) {
            System.out.println("memberDTO = " + memberDTO);
        }
    }

    @Test
    public void findDTOByConstructor() throws Exception {
        //given

        //when
        List<MemberDTO> fetch = queryFactory
                .select(Projections.constructor(MemberDTO.class, member.username, member.age))
                .from(member)
                .fetch();

        //then
        for (MemberDTO memberDTO : fetch) {
            System.out.println("memberDTO = " + memberDTO);
        }
    }

    @Test
    public void findUserDTO() throws Exception {
        //given
        QMember memberSub = new QMember("memberSub");

        //when
        List<UserDTO> fetch = queryFactory
                .select(Projections.bean(UserDTO.class,
                        member.username.as("name"),
                        ExpressionUtils.as(
                                JPAExpressions
                                        .select(memberSub.age.max())
                                        .from(memberSub),
                                "age"
                        ))
                )
                .from(member)
                .fetch();

        //then
        for (UserDTO userDTO : fetch) {
            System.out.println("userDTO = " + userDTO);
        }
    }

    @Test
    public void findDTOByQueryProjection() throws Exception {
        //given

        //when
        List<MemberDTO> result = queryFactory
                .select(new QMemberDTO(member.username, member.age))
                .from(member)
                .fetch();

        //then
        for (MemberDTO memberDTO : result) {
            System.out.println("memberDTO = " + memberDTO);
        }
    }

    @Test
    public void dynamicQuery_BooleanBuilder() throws Exception {
        //given
        String usernameParam = "member1";
        int ageParam = 10;

        //when
        List<Member> result = searchMember1(usernameParam, ageParam);

        //then
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameParam, Integer ageParam) {

        BooleanBuilder booleanBuilder = new BooleanBuilder();
        if(usernameParam != null) {
            booleanBuilder.and(member.username.eq(usernameParam));
        }

        if(ageParam != null) {
            booleanBuilder.and(member.age.eq(ageParam));
        }

        return queryFactory
                .selectFrom(member)
                .where(booleanBuilder)
                .fetch();
    }

    @Test
    public void dynamicQuery_WhereParam() throws Exception {
        //given
        String usernameParam = "member1";
        int ageParam = 10;

        //when
        List<Member> result = searchMember2(usernameParam, ageParam);

        //then
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameParam, Integer ageParam) {
        return queryFactory
                .selectFrom(member)
                .where(allEq(usernameParam, ageParam))
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameParam) {
        if(usernameParam == null) return null;
        return member.username.eq(usernameParam);
    }

    private BooleanExpression ageEq(Integer ageParam) {
        if(ageParam != null) return null;
        return member.age.eq(ageParam);
    }

    private BooleanExpression allEq(String usernameParam, Integer ageParam) {
        return usernameEq(usernameParam).and(ageEq(ageParam));
    }

    @Test
    public void bulkUpdate() throws Exception {
        //given

        //when
        long count = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(25))
                .execute();

        System.out.println("count = " + count);

        em.flush();
        em.clear();

        //then
        List<Member> result = queryFactory
                .selectFrom(member)
                .fetch();

        for (Member member : result) {
            System.out.println("member = " + member);
        }
    }

    @Test
    public void bulkAdd() throws Exception {
        //given

        //when
        queryFactory
                .update(member)
                .set(member.age, member.age.add(10))
                .execute();

        em.flush();
        em.clear();

        //then
        List<Member> result = queryFactory
                .selectFrom(member)
                .fetch();

        for (Member member : result) {
            System.out.println("member = " + member);
        }
    }

    @Test
    public void bulkDelete() throws Exception {
        //given

        //when
        queryFactory
                .delete(member)
                .where(member.age.lt(25))
                .execute();

        em.flush();
        em.clear();

        //then
        List<Member> result = queryFactory
                .selectFrom(member)
                .fetch();

        for (Member member : result) {
            System.out.println("member = " + member);
        }
    }
}
