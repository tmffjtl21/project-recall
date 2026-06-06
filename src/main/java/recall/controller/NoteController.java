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

    // 전체 3분할 화면
    @GetMapping({"/", "/notes"})
    public String notes(Model model, Authentication authentication) {
        String owner = owner(authentication);
        if (categoryRepository.countByOwner(owner) == 0) {
            categoryRepository.save(Category.builder().name("기본").owner(owner).sortOrder(0).build());
        }
        populate(model, owner, firstCategory(owner), null);
        return "notes";
    }

    // 카테고리 선택
    @GetMapping("/notes/category/{id}")
    public String selectCategory(@PathVariable Long id, Model model, Authentication authentication) {
        String owner = owner(authentication);
        populate(model, owner, ownedCategory(id, owner), null);
        return "notes :: workspace";
    }

    // 노트 선택 -> 목록 하이라이트 + 상세 (workspace 전체 갱신)
    @GetMapping("/notes/memo/{id}")
    public String selectMemo(@PathVariable Long id, Model model, Authentication authentication) {
        String owner = owner(authentication);
        Memo memo = ownedMemo(id, owner);
        populate(model, owner, memo.getCategory(), memo);
        return "notes :: workspace";
    }

    // 제목/내용 검색
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

    // 새 노트 작성 폼
    @GetMapping("/notes/category/{id}/new")
    public String newNoteForm(@PathVariable Long id, Model model, Authentication authentication) {
        String owner = owner(authentication);
        Category category = ownedCategory(id, owner);
        model.addAttribute("formAction", "/memos");
        model.addAttribute("formCategoryId", category.getId());
        model.addAttribute("formMemo", null);
        return "note-detail :: noteForm";
    }

    // 노트 수정 폼
    @GetMapping("/notes/memo/{id}/edit")
    public String editNoteForm(@PathVariable Long id, Model model, Authentication authentication) {
        String owner = owner(authentication);
        Memo memo = ownedMemo(id, owner);
        model.addAttribute("formAction", "/memos/" + memo.getId());
        model.addAttribute("formMemo", memoMapper.toDto(memo));
        return "note-detail :: noteForm";
    }

    @PostMapping("/categories")
    public String addCategory(@RequestParam String name, Model model, Authentication authentication) {
        String owner = owner(authentication);
        int order = (int) categoryRepository.countByOwner(owner);
        Category category = categoryRepository.save(
                Category.builder().name(name).owner(owner).sortOrder(order).build());
        populate(model, owner, category, null);
        return "notes :: workspace";
    }

    @DeleteMapping("/categories/{id}")
    public String deleteCategory(@PathVariable Long id, Model model, Authentication authentication) {
        String owner = owner(authentication);
        Category category = ownedCategory(id, owner);
        memoRepository.deleteByCategoryId(category.getId());
        categoryRepository.delete(category);
        populate(model, owner, firstCategory(owner), null);
        return "notes :: workspace";
    }

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

    @DeleteMapping("/memos/{id}")
    public String deleteMemo(@PathVariable Long id, Model model, Authentication authentication) {
        String owner = owner(authentication);
        Memo memo = ownedMemo(id, owner);
        Category category = memo.getCategory();
        memoRepository.delete(memo);
        populate(model, owner, category, null);
        return "notes :: workspace";
    }

    // ---- helpers ----

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

    private Category firstCategory(String owner) {
        List<Category> categories = categoryRepository.findByOwnerOrderBySortOrderAscIdAsc(owner);
        return categories.isEmpty() ? null : categories.get(0);
    }

    private Category ownedCategory(Long id, String owner) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!owner.equals(category.getOwner())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return category;
    }

    private Memo ownedMemo(Long id, String owner) {
        Memo memo = memoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!owner.equals(memo.getCategory().getOwner())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return memo;
    }

    private String owner(Authentication authentication) {
        return LoginUsername.of(authentication);
    }
}
