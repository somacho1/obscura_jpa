package dev.jpa.obscura_jpa.member;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * 회원 Service
 *
 * 회원가입, 회원조회, 회원수정,
 * 회원탈퇴 및 중복검사 로직을 처리한다.
 */
@Service
@RequiredArgsConstructor
public class ObMemberService {

    private final ObMemberRepository obMemberRepository;

    /**
     * 회원가입
     *
     * - 아이디 중복 검사
     * - 이메일 중복 검사
     * - 기본 권한 USER 설정
     * - 기본 상태 정상(1) 설정
     * - 가입일 설정
     */
    public ObMemberDTO createMember(ObMemberDTO dto) {

        // 아이디 중복 검사
        if (obMemberRepository.existsById(dto.getId())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }

        // 이메일 중복 검사
        if (obMemberRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // DTO -> Entity
        ObMember member = ObMember.builder()
            .id(dto.getId())
            .email(dto.getEmail())
            .password(dto.getPassword())
            .name(dto.getName())
            .phone(dto.getPhone())
            .role("USER")
            .statusNo(1)
            .lastLogin(null)
            .cdate(LocalDateTime.now())
            .build();

        // DB 저장
        ObMember savedMember = obMemberRepository.save(member);

        return toDTO(savedMember);
    }

    /**
     * 회원번호로 회원 조회
     */
    public Optional<ObMemberDTO> findByNo(Long no) {

        return obMemberRepository.findById(no)
            .map(this::toDTO);
    }

    /**
     * 로그인 아이디로 회원 조회
     */
    public Optional<ObMemberDTO> findById(String id) {

        return obMemberRepository.findById(id)
            .map(this::toDTO);
    }

    /**
     * 전체 회원 조회
     */
    public List<ObMemberDTO> findAll() {

        return obMemberRepository.findAllByOrderByNoDesc()
            .stream()
            .map(this::toDTO)
            .toList();
    }

    /**
     * 회원정보 수정
     *
     * 현재 단계에서는 아래 항목을 수정한다.
     *
     * - 이메일
     * - 비밀번호
     * - 이름
     * - 전화번호
     *
     * 아이디, 권한, 회원상태, 가입일은
     * 일반 회원이 직접 수정하지 않는다.
     */
    public ObMemberDTO updateMember(Long no, ObMemberDTO dto) {

        // 회원번호로 기존 회원 조회
        ObMember member = obMemberRepository.findById(no)
            .orElseThrow(() ->
                new IllegalArgumentException("회원 정보를 찾을 수 없습니다.")
            );

        // 이미 탈퇴한 회원인지 확인
        if (member.getStatusNo() == 0) {
            throw new IllegalArgumentException("이미 탈퇴한 회원입니다.");
        }

        /*
         * 이메일을 변경하는 경우
         * 현재 회원을 제외한 다른 회원과 중복되는지 확인
         */
        if (dto.getEmail() != null &&
            !dto.getEmail().isBlank() &&
            obMemberRepository.existsByEmailAndNoNot(dto.getEmail(), no)) {

            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // 이메일 수정
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            member.setEmail(dto.getEmail());
        }

        /*
         * 비밀번호 수정
         *
         * 현재는 암호화 작업 전이므로 그대로 저장한다.
         * 추후 BCrypt 적용 시 이 부분에서 암호화 처리한다.
         */
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            member.setPassword(dto.getPassword());
        }

        // 이름 수정
        if (dto.getName() != null && !dto.getName().isBlank()) {
            member.setName(dto.getName());
        }

        // 전화번호 수정
        if (dto.getPhone() != null && !dto.getPhone().isBlank()) {
            member.setPhone(dto.getPhone());
        }

        // 변경된 Entity 저장
        ObMember updatedMember = obMemberRepository.save(member);

        return toDTO(updatedMember);
    }

    /**
     * 회원탈퇴
     *
     * 실제 DB 데이터를 삭제하지 않고
     * STATUSNO를 0으로 변경하는 소프트 삭제 방식이다.
     */
    public void withdrawMember(Long no) {

        // 회원 조회
        ObMember member = obMemberRepository.findById(no)
            .orElseThrow(() ->
                new IllegalArgumentException("회원 정보를 찾을 수 없습니다.")
            );

        // 이미 탈퇴한 회원
        if (member.getStatusNo() == 0) {
            throw new IllegalArgumentException("이미 탈퇴한 회원입니다.");
        }

        // 회원상태를 탈퇴(0)로 변경
        member.setStatusNo(0);

        obMemberRepository.save(member);
    }

    /**
     * 아이디 중복 검사
     */
    public boolean existsById(String id) {

        return obMemberRepository.existsById(id);
    }

    /**
     * 이메일 중복 검사
     */
    public boolean existsByEmail(String email) {

        return obMemberRepository.existsByEmail(email);
    }

    /**
     * Entity -> DTO 변환
     *
     * 비밀번호는 API 응답에 노출하지 않는다.
     */
    private ObMemberDTO toDTO(ObMember member) {

        return ObMemberDTO.builder()
            .no(member.getNo())
            .id(member.getId())
            .email(member.getEmail())
            .password(null)
            .name(member.getName())
            .phone(member.getPhone())
            .role(member.getRole())
            .statusNo(member.getStatusNo())
            .lastLogin(member.getLastLogin())
            .cdate(member.getCdate())
            .build();
    }
}