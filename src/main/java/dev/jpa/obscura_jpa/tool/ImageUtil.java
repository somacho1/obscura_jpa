package dev.jpa.obscura_jpa.tool;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

import javax.imageio.ImageIO;

/**
 * 상품 이미지 가공 공통 클래스
 *
 * 역할
 * 1. 실제 이미지 파일인지 확인
 * 2. 이미지 해상도 확인
 * 3. MAIN 이미지를 4:5 비율로 중앙 크롭
 * 4. MAIN 이미지를 1200 x 1500 크기로 리사이즈
 *
 * 원본 파일 저장은 Upload.java에서 담당한다.
 */
public class ImageUtil {

    // 상품 대표 이미지 최종 크기
    private static final int MAIN_WIDTH = 1200;
    private static final int MAIN_HEIGHT = 1500;

    // 대표 이미지 최소 원본 크기
    private static final int MIN_MAIN_WIDTH = 800;
    private static final int MIN_MAIN_HEIGHT = 1000;

    // 가공된 대표 이미지 저장 폴더
    private static final String MAIN_PATH = "uploads/products/main/";

    /**
     * MAIN 상품 이미지 생성
     *
     * 원본 이미지
     * → 실제 이미지 확인
     * → 최소 해상도 확인
     * → 4:5 중앙 크롭
     * → 1200 x 1500 리사이즈
     * → JPG 저장
     *
     * @param originalPath Upload.saveOriginal()에서 반환받은 경로
     * @return PRODUCTIMAGE.IMAGEURL에 사용할 대표 이미지 경로
     */
    public static String createMainImage(String originalPath) {
        File originalFile = getFile(originalPath);
        BufferedImage originalImage = readImage(originalFile);

        validateMainResolution(originalImage);

        BufferedImage croppedImage = cropToFourByFive(originalImage);
        BufferedImage resizedImage = resize(croppedImage, MAIN_WIDTH, MAIN_HEIGHT);

        File mainDir = new File(MAIN_PATH);

        if (!mainDir.exists() && !mainDir.mkdirs()) {
            throw new RuntimeException("대표 이미지 저장 폴더 생성에 실패했습니다.");
        }

        String fileName = UUID.randomUUID() + ".jpg";
        File saveFile = new File(mainDir, fileName);

        try {
            boolean saved = ImageIO.write(resizedImage, "jpg", saveFile);

            if (!saved) {
                throw new RuntimeException("대표 이미지 저장 형식을 처리할 수 없습니다.");
            }
        } catch (IOException e) {
            throw new RuntimeException("대표 이미지 저장에 실패했습니다.", e);
        }

        return "/uploads/products/main/" + fileName;
    }

    /**
     * 이미지 파일 읽기
     *
     * 확장자만 이미지로 변경한 파일 등
     * 실제 이미지가 아닌 파일도 여기서 차단한다.
     */
    private static BufferedImage readImage(File file) {
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("원본 이미지 파일을 찾을 수 없습니다.");
        }

        try {
            BufferedImage image = ImageIO.read(file);

            if (image == null) {
                throw new IllegalArgumentException("올바른 이미지 파일이 아닙니다.");
            }

            return image;
        } catch (IOException e) {
            throw new IllegalArgumentException("이미지 파일을 읽을 수 없습니다.", e);
        }
    }

    /**
     * MAIN 이미지 최소 해상도 검사
     *
     * 지나치게 작은 이미지를 1200 x 1500으로 확대하면
     * 화질이 깨질 수 있으므로 최소 크기를 제한한다.
     */
    private static void validateMainResolution(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        if (width < MIN_MAIN_WIDTH || height < MIN_MAIN_HEIGHT) {
            throw new IllegalArgumentException(
                "대표 이미지는 최소 " + MIN_MAIN_WIDTH + " x " + MIN_MAIN_HEIGHT + "px 이상이어야 합니다. 현재 이미지: " + width + " x " + height + "px"
            );
        }
    }

    /**
     * 원본 이미지를 4:5 비율로 중앙 크롭
     *
     * 이미지를 강제로 늘리지 않고 중앙을 기준으로 잘라
     * 상품 이미지 비율을 통일한다.
     */
    private static BufferedImage cropToFourByFive(BufferedImage image) {
        int originalWidth = image.getWidth();
        int originalHeight = image.getHeight();

        double targetRatio = 4.0 / 5.0;
        double originalRatio = (double) originalWidth / originalHeight;

        int cropWidth;
        int cropHeight;
        int x;
        int y;

        if (originalRatio > targetRatio) {
            // 원본이 4:5보다 가로로 넓은 경우 → 좌우 크롭
            cropHeight = originalHeight;
            cropWidth = (int) Math.round(cropHeight * targetRatio);
            x = (originalWidth - cropWidth) / 2;
            y = 0;
        } else {
            // 원본이 4:5보다 세로로 긴 경우 → 위아래 크롭
            cropWidth = originalWidth;
            cropHeight = (int) Math.round(cropWidth / targetRatio);
            x = 0;
            y = (originalHeight - cropHeight) / 2;
        }

        BufferedImage cropped = image.getSubimage(x, y, cropWidth, cropHeight);
        BufferedImage copy = new BufferedImage(cropWidth, cropHeight, BufferedImage.TYPE_INT_RGB);

        Graphics2D graphics = copy.createGraphics();
        graphics.drawImage(cropped, 0, 0, null);
        graphics.dispose();

        return copy;
    }

    /**
     * 이미지 리사이즈
     */
    private static BufferedImage resize(BufferedImage image, int width, int height) {
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = resized.createGraphics();

        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        graphics.drawImage(image, 0, 0, width, height, null);
        graphics.dispose();

        return resized;
    }

    /**
     * "/uploads/products/original/xxx.jpg"
     * 형태의 URL 경로를 실제 서버 파일 경로로 변환한다.
     */
    private static File getFile(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            throw new IllegalArgumentException("원본 이미지 경로가 없습니다.");
        }

        String path = imagePath.startsWith("/") ? imagePath.substring(1) : imagePath;
        return new File(path);
    }

    // 객체 생성 방지
    private ImageUtil() {
    }
}