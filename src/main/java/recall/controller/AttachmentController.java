package recall.controller;

import java.nio.file.Path;
import java.util.Set;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import recall.domain.Attachment;
import recall.repository.AttachmentRepository;
import recall.support.FileStorage;
import recall.support.LoginUsername;

/**
 * 첨부 파일 업로드/다운로드/삭제. 업로드는 노트 저장과 분리되어 있어 새 노트에서도 즉시 동작한다.
 * 파일 바이트는 디스크에 스트리밍 저장/서빙하고 메타데이터만 DB 에 둔다.
 */
@RestController
public class AttachmentController {

    // 본문에 <img> 로 인라인 표시/서빙할 이미지 타입(SVG 는 스크립트 위험으로 제외 → 다운로드 처리)
    private static final Set<String> INLINE_IMAGE_TYPES =
            Set.of("image/png", "image/jpeg", "image/gif", "image/webp", "image/bmp");

    private final AttachmentRepository attachmentRepository;
    private final FileStorage fileStorage;

    public AttachmentController(AttachmentRepository attachmentRepository, FileStorage fileStorage) {
        this.attachmentRepository = attachmentRepository;
        this.fileStorage = fileStorage;
    }

    /**
     * 파일을 업로드한다. 저장 후 본문에 끼워 넣을 정보(JSON)를 돌려준다.
     */
    @PostMapping("/attachments")
    public AttachmentResponse upload(@RequestParam("file") MultipartFile file, Authentication authentication) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "빈 파일입니다.");
        }
        String owner = LoginUsername.of(authentication);
        String contentType = file.getContentType() == null ? "application/octet-stream" : file.getContentType();
        boolean image = INLINE_IMAGE_TYPES.contains(contentType.toLowerCase());
        String storedName = fileStorage.store(file);
        Attachment saved = attachmentRepository.save(Attachment.builder()
                .owner(owner)
                .originalName(safeName(file.getOriginalFilename()))
                .storedName(storedName)
                .contentType(contentType)
                .size(file.getSize())
                .image(image)
                .build());
        return new AttachmentResponse(saved.getId(), "/attachments/" + saved.getId(),
                saved.getOriginalName(), saved.isImage());
    }

    /**
     * 파일을 내려준다. 이미지는 인라인, 그 외(SVG 포함)는 다운로드로 강제한다.
     */
    @GetMapping("/attachments/{id}")
    public ResponseEntity<Resource> serve(@PathVariable Long id, Authentication authentication) {
        Attachment attachment = ownedAttachment(id, LoginUsername.of(authentication));
        Path path = fileStorage.load(attachment.getStoredName());
        Resource resource = new FileSystemResource(path);
        if (!resource.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        ContentDisposition disposition = ContentDisposition
                .builder(attachment.isImage() ? "inline" : "attachment")
                .filename(attachment.getOriginalName(), java.nio.charset.StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.parseMediaType(attachment.getContentType()))
                .body(resource);
    }

    /**
     * 첨부를 삭제한다(파일 + 메타데이터).
     */
    @DeleteMapping("/attachments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication authentication) {
        Attachment attachment = ownedAttachment(id, LoginUsername.of(authentication));
        fileStorage.delete(attachment.getStoredName());
        attachmentRepository.delete(attachment);
    }

    /**
     * 업로드 한도 초과 시 친절한 메시지로 응답한다.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public String tooLarge() {
        return "파일이 너무 큽니다. 20MB 이하만 업로드할 수 있습니다.";
    }

    private Attachment ownedAttachment(Long id, String owner) {
        Attachment attachment = attachmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (owner == null || !owner.equals(attachment.getOwner())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return attachment;
    }

    /**
     * 표시용 파일명. 경로 구분자를 제거해 화면/다운로드에서 안전하게 쓴다.
     */
    private String safeName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return "file";
        }
        String name = originalName.replace("\\", "/");
        name = name.substring(name.lastIndexOf('/') + 1);
        return name.isBlank() ? "file" : name;
    }

    /**
     * 업로드 응답(본문 토큰 삽입용).
     */
    public record AttachmentResponse(Long id, String url, String name, boolean image) {
    }
}
