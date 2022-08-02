package study.querydsl.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDTO;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class MemberRepositoryTest {
    
    @PersistenceContext EntityManager em;
    @Autowired MemberRepository memberRepository;

    @Test
    public void basicTest() throws Exception {
        //given
        Member member = new Member("member1", 10);
        memberRepository.save(member);

        //when
        Member result1 = memberRepository.findById(member.getId()).get();
        List<Member> result2 = memberRepository.findAll();
        List<Member> result3 = memberRepository.findByUsername(member.getUsername());

        //then
        assertThat(result1).isEqualTo(member);
        assertThat(result2).containsExactly(member);
        assertThat(result3).containsExactly(member);
    }

    @Test
    public void searchByBuilder() throws Exception {
        //given
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

        //when
        MemberSearchCondition memberSearchCondition = new MemberSearchCondition();
        Pageable pageable = PageRequest.of(1, 3);

        Page<MemberTeamDTO> result = memberRepository.searchPageComplex(memberSearchCondition, pageable);

        //then
        //assertThat(result.toList()).extracting("username").containsExactly("member1", "member2", "member3");
        assertThat(result.toList()).extracting("username").containsExactly("member4");
    }
}
