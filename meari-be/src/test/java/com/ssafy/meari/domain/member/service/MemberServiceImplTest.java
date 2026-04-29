package com.ssafy.meari.domain.member.service;

import com.ssafy.meari.domain.member.entity.Member;
import com.ssafy.meari.domain.member.entity.NativeLanguage;
import com.ssafy.meari.domain.member.mapper.MemberMapper;
import com.ssafy.meari.domain.member.repository.MemberRepository;
import com.ssafy.meari.global.auth.request.SignupRequestDto;
import com.ssafy.meari.global.auth.response.EmailCheckResponseDto;
import com.ssafy.meari.global.auth.response.NicknameCheckResponseDto;
import com.ssafy.meari.global.auth.service.RefreshTokenService;
import com.ssafy.meari.global.error.ErrorCode;
import com.ssafy.meari.global.error.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * MemberServiceImpl 단위 테스트
 * - 회원가입 중복 검증, 비밀번호 인코딩, 회원 삭제 흐름 검증
 * - DB / Redis / Encoder 모두 Mock
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MemberServiceImpl 단위 테스트")
class MemberServiceImplTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberMapper memberMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private MemberServiceImpl memberService;

    private SignupRequestDto signupDto(String email, String nickname) {
        return new SignupRequestDto(email, "password!", nickname, NativeLanguage.KR);
    }

    @Nested
    @DisplayName("signup")
    class Signup {

        @Test
        @DisplayName("성공 - 비밀번호 인코딩 후 Member 저장")
        void success() {
            // given
            SignupRequestDto dto = signupDto("new@example.com", "newbie");
            given(memberRepository.existsByEmail(dto.email())).willReturn(false);
            given(memberRepository.existsByNickname(dto.nickname())).willReturn(false);
            given(passwordEncoder.encode(dto.password())).willReturn("encoded-password");

            // when
            memberService.signup(dto);

            // then
            ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
            verify(memberRepository).save(captor.capture());
            Member saved = captor.getValue();
            assertThat(saved.getEmail()).isEqualTo(dto.email());
            assertThat(saved.getNickname()).isEqualTo(dto.nickname());
            assertThat(saved.getPassword()).isEqualTo("encoded-password");
            assertThat(saved.getNativeLanguage()).isEqualTo(NativeLanguage.KR);
        }

        @Test
        @DisplayName("실패 - 이메일 중복 시 DUPLICATED_USER, save 호출되지 않음")
        void fail_duplicatedEmail() {
            // given
            SignupRequestDto dto = signupDto("dup@example.com", "n");
            given(memberRepository.existsByEmail(dto.email())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> memberService.signup(dto))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATED_USER);

            verify(memberRepository, never()).save(any());
            verify(passwordEncoder, never()).encode(anyString());
        }

        @Test
        @DisplayName("실패 - 닉네임 중복 시 DUPLICATED_USER_NICKNAME, save 호출되지 않음")
        void fail_duplicatedNickname() {
            // given
            SignupRequestDto dto = signupDto("ok@example.com", "dup-nick");
            given(memberRepository.existsByEmail(dto.email())).willReturn(false);
            given(memberRepository.existsByNickname(dto.nickname())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> memberService.signup(dto))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATED_USER_NICKNAME);

            verify(memberRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("checkEmailExists / checkNicknameExists")
    class Checks {

        @Test
        @DisplayName("이메일 존재 → has=true")
        void emailExists() {
            given(memberRepository.existsByEmail("a@a.com")).willReturn(true);

            EmailCheckResponseDto result = memberService.checkEmailExists("a@a.com");

            assertThat(result.hasEmail()).isTrue();
        }

        @Test
        @DisplayName("이메일 미존재 → has=false")
        void emailNotExists() {
            given(memberRepository.existsByEmail("none@a.com")).willReturn(false);

            EmailCheckResponseDto result = memberService.checkEmailExists("none@a.com");

            assertThat(result.hasEmail()).isFalse();
        }

        @Test
        @DisplayName("닉네임 존재 → has=true")
        void nicknameExists() {
            given(memberRepository.existsByNickname("nick")).willReturn(true);

            NicknameCheckResponseDto result = memberService.checkNicknameExists("nick");

            assertThat(result.hasNickname()).isTrue();
        }
    }

    @Nested
    @DisplayName("deleteMember")
    class DeleteMember {

        @Test
        @DisplayName("성공 - Redis refresh token 삭제 후 Member 삭제")
        void success() {
            // given
            Member member = Member.builder()
                .email("user@example.com")
                .password("encoded")
                .nickname("user")
                .nativeLanguage(NativeLanguage.KR)
                .build();
            given(memberRepository.findById(1L)).willReturn(Optional.of(member));

            // when
            memberService.deleteMember(1L);

            // then
            verify(refreshTokenService).deleteRefreshToken(member.getEmail());
            verify(memberRepository).delete(member);
        }

        @Test
        @DisplayName("실패 - 회원이 없으면 NOT_FOUND_MEMBER")
        void fail_memberNotFound() {
            given(memberRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> memberService.deleteMember(999L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND_MEMBER);

            verify(refreshTokenService, never()).deleteRefreshToken(anyString());
            verify(memberRepository, never()).delete(any());
        }
    }
}
