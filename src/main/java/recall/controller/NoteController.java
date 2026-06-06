package recall.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import recall.domain.Category;
import recall.domain.Memo;
import recall.mapper.CategoryMapper;
import recall.mapper.MemoMapper;
import recall.repository.CategoryRepository;
import recall.repository.MemoRepository;
import recall.support.LoginUsername;

/**
 * 노트 3분할 화면과 카테고리/노트 CRUD, 검색을 처리하는 컨트롤러.
 * 응답은 화면 일부를 교체하는 HTMX fragment 또는 전체 페이지다.
 */
@Controller
public class NoteController {

    private final CategoryRepository categoryRepository;
    private final MemoRepository memoRepository;
    private final CategoryMapper categoryMapper;
    private final MemoMapper memoMapper;

    public NoteController(CategoryRepository categoryRepository, MemoRepository memoRepository,
                          CategoryMapper categoryMapper, MemoMapper memoMapper) {
        this.categoryRepository = categoryRepository;
        this.memoRepository = memoRepository;
        this.categoryMapper = categoryMapper;
        this.memoMapper = memoMapper;
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
        Memo memo = memoRepository.save(Memo.builder()
                .title(title).content(content).category(category).build());
        populate(model, owner, category, memo);
        return "notes :: workspace";
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
                : memoMapper.toDtoList(memoRepository.findByCategoryIdOrderByIdDesc(selected.getId())));
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
}
