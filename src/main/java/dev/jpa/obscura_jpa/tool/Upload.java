package dev.jpa.obscura_jpa.tool;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

/**
 * 이미지 업로드 공통 클래스
 *
 * 역할
 * 1. 파일 존재 여부 확인
 * 2. 파일 용량 확인
 * 3. 이미지 MIME 타입 확인
 * 4. 이미지 확장자 확인
 * 5. UUID 파일명 생성
 * 6. 원본 이미지 저장
 *
 * 이미지 크롭/리사이즈는 ImageUtil.java에서 처리한다.
 */
public class Upload {

    // 원본 이미지 저장 폴더
    private static final String ORIGINAL_PATH = "uploads/products/original/";

    // 이미지 1장 최대 용량: 10MB
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    // 허용 확장자
    private static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png", "webp");

    // 허용 MIME 타입
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of("image/jpeg", "image/png", "image/webp");

    /**
     * 이미지 원본 저장
     *
     * @param file 업로드 이미지
     * @return 저장된 원본 이미지 경로
     */
    public static String saveOriginal(MultipartFile file) {
        validate(file);

        String extension = getExtension(file.getOriginalFilename());
        String fileName = UUID.randomUUID() + "." + extension;
        File uploadDir = new File(ORIGINAL_PATH);

        if (!uploadDir.exists() && !uploadDir.mkdirs()) {
            throw new RuntimeException("이미지 업로드 폴더 생성에 실패했습니다.");
        }

        File saveFile = new File(uploadDir, fileName);

        try {
            file.transferTo(saveFile.getAbsoluteFile());
        } catch (IOException e) {
            throw new RuntimeException("이미지 파일 저장에 실패했습니다.", e);
        }

        return "/uploads/products/original/" + fileName;
    }

    /**
     * 이미지 파일 검증
     */
    public static void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 이미지가 없습니다.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("이미지는 한 장당 최대 10MB까지 업로드할 수 있습니다.");
        }

        String contentType = file.getContentType();

        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("JPG, JPEG, PNG, WEBP 이미지 파일만 업로드할 수 있습니다.");
        }

        String extension = getExtension(file.getOriginalFilename());

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("허용되지 않는 이미지 확장자입니다.");
        }
    }

    /**
     * 파일명에서 확장자 추출
     *
     * example.jpg → jpg
     */
    private static String getExtension(String fileName) {
        if (fileName == null || fileName.isBlank() || !fileName.contains(".")) {
            throw new IllegalArgumentException("파일 확장자를 확인할 수 없습니다.");
        }

        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    // 객체 생성 방지
    private Upload() {
    }
}