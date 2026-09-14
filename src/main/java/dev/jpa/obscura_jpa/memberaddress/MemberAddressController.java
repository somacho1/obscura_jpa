package dev.jpa.obscura_jpa.memberaddress;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

/**
 * 회원 배송지 Controller
 *
 * 배송지 등록, 조회, 수정, 삭제,
 * 기본배송지 조회 API를 제공한다.
 */
@RestController
@RequestMapping("/api/member-addresses")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MemberAddressController {

    private final MemberAddressService memberAddressService;

    /**
     * 배송지 등록
     *
     * POST /api/member-addresses
     */
    @PostMapping
    public ResponseEntity<?> createAddress(
        @RequestBody MemberAddressDTO dto
    ) {

        try {

            MemberAddressDTO savedAddress =
                memberAddressService.createAddress(dto);

            return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(savedAddress);

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(e.getMessage());
        }
    }

    /**
     * 특정 회원의 배송지 전체 조회
     *
     * GET /api/member-addresses/member/1
     */
    @GetMapping("/member/{mno}")
    public ResponseEntity<List<MemberAddressDTO>> findAllByMember(
        @PathVariable("mno") Long mno
    ) {

        return ResponseEntity.ok(
            memberAddressService.findAllByMember(mno)
        );
    }

    /**
     * 배송지번호로 배송지 조회
     *
     * GET /api/member-addresses/1
     */
    @GetMapping("/{no}")
    public ResponseEntity<?> findByNo(
        @PathVariable("no") Long no
    ) {

        Optional<MemberAddressDTO> address =
            memberAddressService.findByNo(no);

        if (address.isEmpty()) {

            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body("배송지 정보를 찾을 수 없습니다.");
        }

        return ResponseEntity.ok(
            address.get()
        );
    }

    /**
     * 특정 회원의 기본배송지 조회
     *
     * GET /api/member-addresses/member/1/default
     */
    @GetMapping("/member/{mno}/default")
    public ResponseEntity<?> findDefaultAddress(
        @PathVariable("mno") Long mno
    ) {

        Optional<MemberAddressDTO> address =
            memberAddressService.findDefaultAddress(mno);

        if (address.isEmpty()) {

            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body("기본배송지가 없습니다.");
        }

        return ResponseEntity.ok(
            address.get()
        );
    }

    /**
     * 배송지 수정
     *
     * PUT /api/member-addresses/1
     */
    @PutMapping("/{no}")
    public ResponseEntity<?> updateAddress(
        @PathVariable("no") Long no,
        @RequestBody MemberAddressDTO dto
    ) {

        try {

            MemberAddressDTO updatedAddress =
                memberAddressService.updateAddress(no, dto);

            return ResponseEntity.ok(
                updatedAddress
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(e.getMessage());
        }
    }

    /**
     * 배송지 삭제
     *
     * DELETE /api/member-addresses/1
     */
    @DeleteMapping("/{no}")
    public ResponseEntity<?> deleteAddress(
        @PathVariable("no") Long no
    ) {

        try {

            memberAddressService.deleteAddress(no);

            return ResponseEntity
                .noContent()
                .build();

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(e.getMessage());
        }
    }
}