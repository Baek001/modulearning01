package kr.or.ddit.approval.bookmark.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.or.ddit.approval.bookmark.service.CustomLineBookmarkService;
import kr.or.ddit.vo.CustomLineBookmarkVO;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/rest/approval-customline")
@RequiredArgsConstructor
public class CustomLineBookmarkRestController {

    private final CustomLineBookmarkService service;

    @GetMapping
    public List<CustomLineBookmarkVO> readCustomLineBookmarkList(Principal principal) {
        return service.readCustomLineBookmarkList(principal.getName());
    }

    @PostMapping
    public boolean createCustomLineBookmark(
        @RequestBody List<CustomLineBookmarkVO> voList,
        Principal principal
    ) {
        String userId = principal.getName();
        for (CustomLineBookmarkVO vo : voList) {
            vo.setUserId(userId);
            service.createCustomLineBookmark(vo);
        }
        return true;
    }

    @DeleteMapping("/{cstmLineBmNm}")
    public boolean removeCustomLineBookmark(
        @PathVariable String cstmLineBmNm,
        Principal principal
    ) {
        return service.removeCustomLineBookmark(principal.getName(), cstmLineBmNm);
    }
}
