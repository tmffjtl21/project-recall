package recall.controller;

import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import recall.domain.Memo;
import recall.repository.MemoRepository;

@Controller
public class BoardController {

    private final MemoRepository memoRepository;

    public BoardController(MemoRepository memoRepository) {
        this.memoRepository = memoRepository;
    }

    @GetMapping("/")
    public String board(Model model, Principal principal) {
        model.addAttribute("memos", memoRepository.findAllByOrderByIdDesc());
        model.addAttribute("username", principal.getName());
        return "board";
    }

    @PostMapping("/memos")
    public String addMemo(@RequestParam String title,
                          @RequestParam String content,
                          Principal principal) {
        memoRepository.save(Memo.builder()
                .title(title)
                .content(content)
                .writer(principal.getName())
                .build());
        return "redirect:/";
    }
}
