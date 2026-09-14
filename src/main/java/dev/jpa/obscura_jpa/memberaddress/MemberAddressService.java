package dev.jpa.obscura_jpa.memberaddress;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import dev.jpa.obscura_jpa.member.ObMember;
import dev.jpa.obscura_jpa.member.ObMemberRepository;
import lombok.RequiredArgsConstructor;

/**
 * 회원 배송지 Service
 *
 * 회원 배송지 등록, 조회, 수정, 삭제와
 * 기본배송지 설정 로직을 처리한다.
 */
@Service
@RequiredArgsConstructor
public class MemberAddressService {

    private final MemberAddressRepository memberAddressRepository;
    private final ObMemberRepository obMemberRepository;

    /**
     * 배송지 등록
     *
     * - 회원 존재 확인
     * - 첫 배송지는 자동으로 기본배송지(Y)
     * - 새 배송지를 기본배송지(Y)로 등록하면
     *   기존 기본배송지는 N으로 변경
     */
    public MemberAddressDTO createAddress(MemberAddressDTO dto) {

        // 회원 존재 확인
        ObMember member = obMemberRepository.findById(dto.getMno())
            .orElseThrow(() ->
                new IllegalArgumentException("회원 정보를 찾을 수 없습니다.")
            );

        // 탈퇴 회원 확인
        if (member.getStatusNo() == 0) {
            throw new IllegalArgumentException("탈퇴한 회원은 배송지를 등록할 수 없습니다.");
        }

        // 현재 회원의 배송지 개수 확인
        long addressCount =
            memberAddressRepository.countByMemberNo(dto.getMno());

        String defaultYn;

        // 첫 번째 배송지는 무조건 기본배송지로 설정
        if (addressCount == 0) {
            defaultYn = "Y";
        } else {
            defaultYn = "Y".equalsIgnoreCase(dto.getDefaultYn())
                ? "Y"
                : "N";
        }

        // 새 배송지가 기본배송지라면 기존 기본배송지를 N으로 변경
        if ("Y".equals(defaultYn)) {
            removeCurrentDefaultAddress(dto.getMno());
        }

        MemberAddress address = MemberAddress.builder()
            .member(member)
            .addressName(dto.getAddressName())
            .receiver(dto.getReceiver())
            .phone(dto.getPhone())
            .zipcode(dto.getZipcode())
            .address1(dto.getAddress1())
            .address2(dto.getAddress2())
            .defaultYn(defaultYn)
            .cdate(LocalDateTime.now())
            .build();

        MemberAddress savedAddress =
            memberAddressRepository.save(address);

        return toDTO(savedAddress);
    }

    /**
     * 특정 회원의 배송지 전체 조회
     */
    public List<MemberAddressDTO> findAllByMember(Long mno) {

        return memberAddressRepository
            .findAllByMemberNoOrderByNoDesc(mno)
            .stream()
            .map(this::toDTO)
            .toList();
    }

    /**
     * 배송지번호로 배송지 조회
     */
    public Optional<MemberAddressDTO> findByNo(Long no) {

        return memberAddressRepository.findById(no)
            .map(this::toDTO);
    }

    /**
     * 특정 회원의 기본배송지 조회
     */
    public Optional<MemberAddressDTO> findDefaultAddress(Long mno) {

        return memberAddressRepository
            .findByMemberNoAndDefaultYn(mno, "Y")
            .map(this::toDTO);
    }

    /**
     * 배송지 수정
     *
     * - 배송지명
     * - 수령인
     * - 연락처
     * - 우편번호
     * - 기본주소
     * - 상세주소
     * - 기본배송지 여부
     */
    public MemberAddressDTO updateAddress(
        Long no,
        MemberAddressDTO dto
    ) {

        MemberAddress address =
            memberAddressRepository.findById(no)
                .orElseThrow(() ->
                    new IllegalArgumentException("배송지 정보를 찾을 수 없습니다.")
                );

        // 배송지명 수정
        if (dto.getAddressName() != null &&
            !dto.getAddressName().isBlank()) {

            address.setAddressName(dto.getAddressName());
        }

        // 수령인 수정
        if (dto.getReceiver() != null &&
            !dto.getReceiver().isBlank()) {

            address.setReceiver(dto.getReceiver());
        }

        // 연락처 수정
        if (dto.getPhone() != null &&
            !dto.getPhone().isBlank()) {

            address.setPhone(dto.getPhone());
        }

        // 우편번호 수정
        if (dto.getZipcode() != null &&
            !dto.getZipcode().isBlank()) {

            address.setZipcode(dto.getZipcode());
        }

        // 기본주소 수정
        if (dto.getAddress1() != null &&
            !dto.getAddress1().isBlank()) {

            address.setAddress1(dto.getAddress1());
        }

        // 상세주소 수정
        if (dto.getAddress2() != null) {
            address.setAddress2(dto.getAddress2());
        }

        /*
         * 기본배송지로 변경하는 경우
         *
         * 해당 회원의 기존 기본배송지를 N으로 변경하고
         * 현재 배송지를 Y로 설정한다.
         */
        if ("Y".equalsIgnoreCase(dto.getDefaultYn())) {

            Long mno = address.getMember().getNo();

            removeCurrentDefaultAddress(mno);

            address.setDefaultYn("Y");
        }

        MemberAddress updatedAddress =
            memberAddressRepository.save(address);

        return toDTO(updatedAddress);
    }

    /**
     * 배송지 삭제
     *
     * MEMBERADDRESS는 주문내역 자체가 아니라
     * 회원이 저장해둔 배송지 목록이므로 실제 DELETE한다.
     *
     * 기본배송지를 삭제한 경우
     * 남아있는 배송지 중 하나를 새로운 기본배송지로 지정한다.
     */
    public void deleteAddress(Long no) {

        MemberAddress address =
            memberAddressRepository.findById(no)
                .orElseThrow(() ->
                    new IllegalArgumentException("배송지 정보를 찾을 수 없습니다.")
                );

        Long mno = address.getMember().getNo();

        boolean wasDefault =
            "Y".equals(address.getDefaultYn());

        // 배송지 삭제
        memberAddressRepository.delete(address);

        /*
         * 삭제한 배송지가 기본배송지였다면
         * 남은 배송지 중 최신 배송지를 기본배송지로 설정
         */
        if (wasDefault) {

            List<MemberAddress> remainingAddresses =
                memberAddressRepository
                    .findAllByMemberNoOrderByNoDesc(mno);

            if (!remainingAddresses.isEmpty()) {

                MemberAddress newDefault =
                    remainingAddresses.get(0);

                newDefault.setDefaultYn("Y");

                memberAddressRepository.save(newDefault);
            }
        }
    }

    /**
     * 기존 기본배송지를 N으로 변경
     */
    private void removeCurrentDefaultAddress(Long mno) {

        Optional<MemberAddress> currentDefault =
            memberAddressRepository
                .findByMemberNoAndDefaultYn(mno, "Y");

        if (currentDefault.isPresent()) {

            MemberAddress address =
                currentDefault.get();

            address.setDefaultYn("N");

            memberAddressRepository.save(address);
        }
    }

    /**
     * Entity -> DTO 변환
     */
    private MemberAddressDTO toDTO(MemberAddress address) {

        return MemberAddressDTO.builder()
            .no(address.getNo())
            .mno(address.getMember().getNo())
            .addressName(address.getAddressName())
            .receiver(address.getReceiver())
            .phone(address.getPhone())
            .zipcode(address.getZipcode())
            .address1(address.getAddress1())
            .address2(address.getAddress2())
            .defaultYn(address.getDefaultYn())
            .cdate(address.getCdate())
            .build();
    }
}