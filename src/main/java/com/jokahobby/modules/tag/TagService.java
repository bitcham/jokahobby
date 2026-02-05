package com.jokahobby.modules.tag;

import com.jokahobby.infra.exception.BusinessException;
import com.jokahobby.infra.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;

    @Transactional
    public Tag findOrCreateNew(String tagTitle) {
        return tagRepository.findByTitle(tagTitle)
                .orElseGet(() -> tagRepository.save(Tag.builder().title(tagTitle).build()));
    }

    public Tag findByTitle(String tagTitle) {
        return tagRepository.findByTitle(tagTitle)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT, "Tag not found."));
    }
}
