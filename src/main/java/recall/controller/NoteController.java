package recall.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import recall.domain.Attachment;
import recall.domain.Category;
import recall.domain.Memo;
import recall.mapper.CategoryMapper;
import recall.mapper.MemoMapper;
import recall.repository.AttachmentRepository;
import recall.repository.CategoryRepository;
import recall.repository.MemoRepository;
import recall.support.FileStorage;
import recall.support.LoginUsername;

/**
 * 노트 3분할 화면과 카테고리/노트 CRUD, 검색을 처리하는 컨트롤러.
 * 응답은 화면 일부를 교체하는 HTMX fragment 또는 전체 페이지다.
 */
@Controller
public class NoteController {

    // 본문 속 첨부 참조 토큰에서 id 추출: /attachments/123
    private static final Pattern ATTACHMENT_REF = Pattern.compile("/attachments/(\\d+)");

    private final CategoryRepository categoryRepository;
    private final MemoRepository memoRepository;
    private final AttachmentRepository attachmentRepository;
    private final CategoryMapper categoryMapper;
    private final MemoMapper memoMapper;
    private final FileStorage fileStorage;

    public NoteController(CategoryRepository categoryRepository, MemoRepository memoRepository,
                          AttachmentRepository attachmentRepository, CategoryMapper categoryMapper,
                          MemoMapper memoMapper, FileStorage fileStorage) {
        this.categoryRepository = categoryRepository;
        this.memoRepository = memoRepository;
        this.attachmentRepository = attachmentRepository;
        this.categoryMapper = categoryMapper;
        this.memoMapper = memoMapper;
        this.fileStorage = fileStorage;
    }

    /**
     * 전체 3분할 화면. 카테고리가 하나도 없으면 기본 카테고리를 만든다.
     */
    @GetMapping({"/", "/notes"})
    public String notes(Model model, Authentication authentication) {
        String owner = owner(authentication);
        if (categoryRepository.countByOwner(owner) == 0) {
            categoryRepository.save(Category.builder().name("기본").owner(owner).sortOrder(0).build());
        }
        populate(model, owner, firstCategory(owner), null);
        return "notes";
    }

    /**
     * 카테고리를 선택해 해당 카테고리의 노트 목록을 보여준다.
     */
    @GetMapping("/notes/category/{id}")
    public String selectCategory(@PathVariable Long id, Model model, Authentication authentication) {
        String owner = owner(authentication);
        populate(model, owner, ownedCategory(id, owner), null);
        return "notes :: workspace";
    }

    /**
     * 노트를 선택한다. 목록 하이라이트와 상세를 함께 갱신하기 위해 workspace 전체를 반환한다.
     */
    @GetMapping("/notes/memo/{id}")
    public String selectMemo(@PathVariable Long id, Model model, Authentication authentication) {
        String owner = owner(authentication);
        Memo memo = ownedMemo(id, owner);
        populate(model, owner, memo.getCategory(), memo);
        return "notes :: workspace";
    }

    /**
     * 제목 또는 내용으로 노트를 검색한다. 빈 검색어면 기본 목록으로 돌아간다.
     */
    @GetMapping("/notes/search")
    public String search(@RequestParam(name = "q", required = false, defaultValue = "") String q,
                         Model model, Authentication authentication) {
        String owner = owner(authentication);
        String query = q.trim();
        if (query.isEmpty()) {
            populate(model, owner, firstCategory(owner), null);
        } else {
            model.addAttribute("categories",
                    categoryMapper.toDtoList(categoryRepository.findByOwnerOrderBySortOrderAscIdAsc(owner)));
            model.addAttribute("selectedCategory", null);
            model.addAttribute("memos", memoMapper.toDtoList(memoRepository.search(owner, query)));
            model.addAttribute("selectedMemo", null);
            model.addAttribute("username", owner);
            model.addAttribute("searchQuery", query);
        }
        return "notes :: workspace";
    }

    /**
     * 새 노트 작성 폼을 우측 패널에 띄운다.
     */
    @GetMapping("/notes/category/{id}/new")
    public String newNoteForm(@PathVariable Long id, Model model, Authentication authentication) {
        String owner = owner(authentication);
        Category category = ownedCategory(id, owner);
        model.addAttribute("formAction", "/memos");
        model.addAttribute("formCategoryId", category.getId());
        model.addAttribute("formMemo", null);
        return "note-detail :: noteForm";
    }

    /**
     * 노트 수정 폼을 우측 패널에 띄운다.
     */
    @GetMapping("/notes/memo/{id}/edit")
    public String editNoteForm(@PathVariable Long id, Model model, Authentication authentication) {
        String owner = owner(authentication);
        Memo memo = ownedMemo(id, owner);
        model.addAttribute("formAction", "/memos/" + memo.getId());
        model.addAttribute("formMemo", memoMapper.toDto(memo));
        return "note-detail :: noteForm";
    }

    /**
     * 카테고리를 추가한다.
     */
    @PostMapping("/categories")
    public String addCategory(@RequestParam String name, Model model, Authentication authentication) {
        String owner = owner(authentication);
        int order = (int) categoryRepository.countByOwner(owner);
        Category category = categoryRepository.save(
                Category.builder().name(name).owner(owner).sortOrder(order).build());
        populate(model, owner, category, null);
        return "notes :: workspace";
    }

    /**
     * 카테고리 이름을 변경한다. 현재 열려 있던 카테고리 선택 상태를 유지한다.
     */
    @PostMapping("/categories/{id}/rename")
    public String renameCategory(@PathVariable Long id, @RequestParam String name,
                                 @RequestParam(required = false) Long selectedId,
                                 Model model, Authentication authentication) {
        String owner = owner(authentication);
        Category category = ownedCategory(id, owner);
        category.rename(name.trim());
        categoryRepository.save(category);
        Category selected = selectedId == null ? category : ownedCategory(selectedId, owner);
        populate(model, owner, selected, null);
        return "notes :: workspace";
    }

    /**
     * 카테고리 표시 순서를 드래그한 순서(ids)대로 저장한다. 화면은 그대로 둔다.
     */
    @PostMapping("/categories/reorder")
    @ResponseBody
    public void reorderCategories(@RequestParam String ids, Authentication authentication) {
        String owner = owner(authentication);
        List<Category> updated = new ArrayList<>();
        int order = 0;
        for (Long id : parseIds(ids)) {
            Category category = ownedCategory(id, owner);
            category.changeSortOrder(order++);
            updated.add(category);
        }
        categoryRepository.saveAll(updated);
    }

    /**
     * 카테고리를 삭제한다(소속 노트도 함께 삭제).
     */
    @DeleteMapping("/categories/{id}")
    public String deleteCategory(@PathVariable Long id, Model model, Authentication authentication) {
        String owner = owner(authentication);
        Category category = ownedCategory(id, owner);
        memoRepository.deleteByCategoryId(category.getId());
        categoryRepository.delete(category);
        populate(model, owner, firstCategory(owner), null);
        return "notes :: workspace";
    }

    /**
     * 노트를 등록한다.
     */
    @PostMapping("/memos")
    public String addMemo(@RequestParam Long categoryId,
                          @RequestParam String title,
                          @RequestParam String content,
                          Model model, Authentication authentication) {
        String owner = owner(authentication);
        Category category = ownedCategory(categoryId, owner);
        // 새 노트는 맨 위(sortOrder=0). 기존 노트는 한 칸씩 뒤로 민다.
        memoRepository.shiftSortOrder(category.getId());
        Memo memo = memoRepository.save(Memo.builder()
                .title(title).content(content).category(category).sortOrder(0).build());
        linkAttachments(content, memo, owner);
        populate(model, owner, category, memo);
        return "notes :: workspace";
    }

    /**
     * 노트 표시 순서를 드래그한 순서(ids)대로 저장한다. 화면은 그대로 둔다.
     */
    @PostMapping("/memos/reorder")
    @ResponseBody
    public void reorderMemos(@RequestParam String ids, Authentication authentication) {
        String owner = owner(authentication);
        List<Memo> updated = new ArrayList<>();
        int order = 0;
        for (Long id : parseIds(ids)) {
            Memo memo = ownedMemo(id, owner);
            memo.changeSortOrder(order++);
            updated.add(memo);
        }
        memoRepository.saveAll(updated);
    }

    /**
     * 노트를 수정한다.
     */
    @PostMapping("/memos/{id}")
    public String updateMemo(@PathVariable Long id,
                             @RequestParam String title,
                             @RequestParam String content,
                             Model model, Authentication authentication) {
        String owner = owner(authentication);
        Memo memo = ownedMemo(id, owner);
        memo.update(title, content);
        memoRepository.save(memo);
        linkAttachments(content, memo, owner);
        populate(model, owner, memo.getCategory(), memo);
        return "notes :: workspace";
    }

    /**
     * 노트를 삭제한다.
     */
    @DeleteMapping("/memos/{id}")
    public String deleteMemo(@PathVariable Long id, Model model, Authentication authentication) {
        String owner = owner(authentication);
        Memo memo = ownedMemo(id, owner);
        Category category = memo.getCategory();
        deleteAttachmentsOf(memo);
        memoRepository.delete(memo);
        populate(model, owner, category, null);
        return "notes :: workspace";
    }

    /**
     * 좌(카테고리)·중(노트 목록)·우(상세) 패널에 필요한 데이터를 DTO 로 변환해 모델에 담는다.
     */
    private void populate(Model model, String owner, Category selected, Memo selectedMemo) {
        model.addAttribute("categories",
                categoryMapper.toDtoList(categoryRepository.findByOwnerOrderBySortOrderAscIdAsc(owner)));
        model.addAttribute("selectedCategory", selected == null ? null : categoryMapper.toDto(selected));
        model.addAttribute("memos", selected == null ? List.of()
                : memoMapper.toDtoList(memoRepository.findByCategoryIdOrderBySortOrderAscIdDesc(selected.getId())));
        model.addAttribute("selectedMemo", selectedMemo == null ? null : memoMapper.toDto(selectedMemo));
        model.addAttribute("username", owner);
        model.addAttribute("searchQuery", null);
    }

    /**
     * 해당 사용자의 첫 번째 카테고리. 없으면 null.
     */
    private Category firstCategory(String owner) {
        List<Category> categories = categoryRepository.findByOwnerOrderBySortOrderAscIdAsc(owner);
        return categories.isEmpty() ? null : categories.get(0);
    }

    /**
     * 카테고리를 조회하되, 요청자의 소유가 아니면 403 으로 막는다.
     */
    private Category ownedCategory(Long id, String owner) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!owner.equals(category.getOwner())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return category;
    }

    /**
     * 노트를 조회하되, 요청자의 소유가 아니면 403 으로 막는다.
     */
    private Memo ownedMemo(Long id, String owner) {
        Memo memo = memoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!owner.equals(memo.getCategory().getOwner())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return memo;
    }

    /**
     * 현재 로그인 사용자의 표시 이름(소유자 키).
     */
    private String owner(Authentication authentication) {
        return LoginUsername.of(authentication);
    }

    /**
     * 본문에 들어 있는 첨부 토큰(/attachments/{id})을 찾아 해당 첨부를 이 노트로 연결한다.
     * 소유자가 다른 첨부는 무시한다. 연결해 두면 노트 삭제 시 함께 정리할 수 있다.
     */
    private void linkAttachments(String content, Memo memo, String owner) {
        if (content == null || content.isEmpty()) {
            return;
        }
        Matcher matcher = ATTACHMENT_REF.matcher(content);
        List<Attachment> toLink = new ArrayList<>();
        while (matcher.find()) {
            Long attachmentId = Long.valueOf(matcher.group(1));
            attachmentRepository.findById(attachmentId).ifPresent(attachment -> {
                if (owner.equals(attachment.getOwner())
                        && (attachment.getMemo() == null || !memo.getId().equals(attachment.getMemo().getId()))) {
                    attachment.linkTo(memo);
                    toLink.add(attachment);
                }
            });
        }
        if (!toLink.isEmpty()) {
            attachmentRepository.saveAll(toLink);
        }
    }

    /**
     * 노트에 연결된 첨부의 파일과 메타데이터를 모두 지운다.
     */
    private void deleteAttachmentsOf(Memo memo) {
        List<Attachment> attachments = attachmentRepository.findByMemoId(memo.getId());
        for (Attachment attachment : attachments) {
            fileStorage.delete(attachment.getStoredName());
        }
        attachmentRepository.deleteAll(attachments);
    }

    /**
     * "3,1,2" 형태의 콤마 구분 문자열을 Long 목록으로 변환한다(빈 값 무시).
     */
    private List<Long> parseIds(String ids) {
        List<Long> result = new ArrayList<>();
        if (ids == null) {
            return result;
        }
        for (String token : ids.split(",")) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                result.add(Long.valueOf(trimmed));
            }
        }
        return result;
    }
}
