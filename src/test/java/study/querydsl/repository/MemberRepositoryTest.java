package study.querydsl.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;

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
}
