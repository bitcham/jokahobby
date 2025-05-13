package com.jokahobby.tag;

import com.jokahobby.domain.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;

    public Tag findOrCreateNew(String tagTitle) {
        return tagRepository.findByTitle(tagTitle)
                .orElseGet(() -> tagRepository.save(Tag.builder().title(tagTitle).build()));
    }
}
