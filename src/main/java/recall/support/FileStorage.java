package recall.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 첨부 파일을 디스크에 저장/조회/삭제한다.
 * 메모리에 통째로 올리지 않도록 MultipartFile.transferTo 로 스트리밍 저장한다(작은 VM 보호).
 * 저장명은 UUID 라서 사용자 파일명이 경로에 들어가지 않는다(path traversal 방지).
 */
@Component
public class FileStorage {

    private final Path root;

    public FileStorage(@Value("${app.upload.dir:./data/uploads}") String uploadDir) {
        this.root = Path.of(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("업로드 디렉터리를 만들 수 없습니다: " + root, e);
        }
    }

    /**
     * 업로드 파일을 디스크에 저장하고 저장명(UUID + 확장자)을 돌려준다.
     */
    public String store(MultipartFile file) {
        String ext = extension(file.getOriginalFilename());
        String storedName = UUID.randomUUID().toString().replace("-", "") + ext;
        Path target = root.resolve(storedName).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("잘못된 저장 경로");
        }
        try {
            file.transferTo(target);
        } catch (IOException e) {
            throw new UncheckedIOException("파일 저장 실패: " + storedName, e);
        }
        return storedName;
    }

    /**
     * 저장된 파일의 경로. 저장명 외 다른 경로 요소가 섞이면 거부한다.
     */
    public Path load(String storedName) {
        Path target = root.resolve(storedName).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("잘못된 파일 경로");
        }
        return target;
    }

    public void delete(String storedName) {
        try {
            Files.deleteIfExists(load(storedName));
        } catch (IOException e) {
            throw new UncheckedIOException("파일 삭제 실패: " + storedName, e);
        }
    }

    private String extension(String originalName) {
        String name = StringUtils.cleanPath(originalName == null ? "" : originalName);
        int dot = name.lastIndexOf('.');
        if (dot < 0) {
            return "";
        }
        // 확장자만 영숫자로 안전하게 추출(예: ".png", ".tar.gz" 는 ".gz")
        String ext = name.substring(dot + 1).toLowerCase();
        return ext.matches("[a-z0-9]{1,10}") ? "." + ext : "";
    }
}
