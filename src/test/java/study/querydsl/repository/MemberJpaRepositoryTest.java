package study.querydsl.repository;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberJpaRepositoryTest {

    @PersistenceContext EntityManager em;
    @Autowired MemberJpaRepository memberJpaRepository;

    @Test
    public void basicTest() throws Exception {
        //given
        Member member = new Member("member1", 10);
        memberJpaRepository.save(member);

        //when
        Member result1 = memberJpaRepository.findById(member.getId()).get();
        List<Member> result2 = memberJpaRepository.findAll();
        List<Member> result3 = memberJpaRepository.findByUsername(member.getUsername());

        //then
        assertThat(result1).isEqualTo(member);
        assertThat(result2).containsExactly(member);
        assertThat(result3).containsExactly(member);
    }

    @Test
    public void basicQueryDSLTest() throws Exception {
        //given
        Member member = new Member("member1", 10);
        memberJpaRepository.save(member);

        //when
        Member result1 = memberJpaRepository.findById(member.getId()).get();
        List<Member> result2 = memberJpaRepository.findAll_QueryDSL();
        List<Member> result3 = memberJpaRepository.findByUsername_QueryDSL(member.getUsername());

        //then
        assertThat(result1).isEqualTo(member);
        assertThat(result2).containsExactly(member);
        assertThat(result3).containsExactly(member);
    }
}