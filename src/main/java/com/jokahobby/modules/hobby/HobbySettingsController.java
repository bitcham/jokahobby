package com.jokahobby.modules.hobby;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jokahobby.modules.account.CurrentAccount;
import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.tag.Tag;
import com.jokahobby.modules.zone.Zone;
import com.jokahobby.modules.hobby.form.HobbyDescriptionForm;
import com.jokahobby.modules.tag.TagForm;
import com.jokahobby.modules.zone.ZoneForm;
import com.jokahobby.modules.tag.TagRepository;
import com.jokahobby.modules.tag.TagService;
import com.jokahobby.modules.zone.ZoneRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/hobby/{path}/settings/")
@RequiredArgsConstructor
public class HobbySettingsController {

    private final HobbyService hobbyService;
    private final HobbyRepository hobbyRepository;
    private final ModelMapper modelMapper;
    private final ObjectMapper objectMapper;
    private final TagService tagService;
    private final TagRepository tagRepository;
    private final ZoneRepository zoneRepository;

    static final String DESCRIPTION = "description";
    static final String ROOT = "/";
    static final String SETTINGS = "settings";
    static final String HOBBY = "hobby";
    static final String BANNER = "banner";
    static final String TAGS = "tags";
    static final String ZONES = "zones";
    static final String PATH = "path";
    static final String TITLE = "title";
    static final String RECRUIT = "recruit";


    @GetMapping(DESCRIPTION)
    public String viewHobbySetting(@CurrentAccount Account account, @PathVariable String path, Model model) {
        Hobby hobby = hobbyService.getHobbyToUpdate(account,path);
        model.addAttribute(account);
        model.addAttribute(hobby);
        model.addAttribute(modelMapper.map(hobby, HobbyDescriptionForm.class));
        return HOBBY + ROOT + SETTINGS  + ROOT + DESCRIPTION;
    }

    @PostMapping(DESCRIPTION)
    public String updateHobbyInfo(
            @CurrentAccount Account account, @PathVariable String path,
            @Valid HobbyDescriptionForm hobbyDescriptionForm, Errors errors,
            Model model, RedirectAttributes redirectAttributes) {

        Hobby hobby = hobbyService.getHobbyToUpdate(account, path);
        if (errors.hasErrors()) {
            model.addAttribute(account);
            model.addAttribute(hobby);
            return HOBBY + ROOT + SETTINGS + ROOT + DESCRIPTION;
        }
        hobbyService.updateHobbyDescription(hobby, hobbyDescriptionForm);
        redirectAttributes.addFlashAttribute("message", "Hobby information updated successfully.");
        return "redirect:/" + HOBBY + "/" + hobby.getEncodedPath() + ROOT + SETTINGS + ROOT + DESCRIPTION;
    }

    @GetMapping(BANNER)
    public String hobbyImageForm(@CurrentAccount Account account, @PathVariable String path, Model model) {
        Hobby hobby = hobbyService.getHobbyToUpdate(account,path);
        model.addAttribute(account);
        model.addAttribute(hobby);
        return HOBBY + ROOT + SETTINGS + ROOT + BANNER;
    }

    @PostMapping(BANNER)
    public String hobbyImageSubmit(@CurrentAccount Account account, @PathVariable String path, String image, RedirectAttributes redirectAttributes) {
        Hobby hobby = hobbyService.getHobbyToUpdate(account,path);
        hobbyService.updateHobbyImage(hobby, image);
        redirectAttributes.addFlashAttribute("message", "Hobby banner updated successfully.");
        return "redirect:/" + HOBBY + ROOT + hobby.getEncodedPath() + ROOT + SETTINGS + ROOT + BANNER;
    }

    @PostMapping(BANNER + "/enable")
    public String enableHobbyBanner(@CurrentAccount Account account, @PathVariable String path){
        Hobby hobby = hobbyService.getHobbyToUpdate(account,path);
        hobbyService.enableHobbyBanner(hobby);
        return "redirect:/" + HOBBY + ROOT + hobby.getEncodedPath() + ROOT + SETTINGS + ROOT + BANNER;
    }

    @PostMapping(BANNER + "/disable")
    public String disableHobbyBanner(@CurrentAccount Account account, @PathVariable String path){
        Hobby hobby = hobbyService.getHobbyToUpdate(account,path);
        hobbyService.disableHobbyBanner(hobby);
        return "redirect:/" + HOBBY + ROOT + hobby.getEncodedPath() + ROOT + SETTINGS + ROOT + BANNER;
    }

    @GetMapping(TAGS)
    public String hobbyTagsForm(@CurrentAccount Account account, @PathVariable String path, Model model) throws JsonProcessingException {
        Hobby hobby = hobbyService.getHobbyToUpdate(account,path);
        model.addAttribute(account);
        model.addAttribute(hobby);

        model.addAttribute("tags", hobby.getTags().stream().map(Tag::getTitle).toList());
        List<String> allTagTitles = tagRepository.findAll().stream().map(Tag::getTitle).toList();
        model.addAttribute("chamlist", objectMapper.writeValueAsString(allTagTitles));
        return HOBBY + ROOT + SETTINGS + ROOT + TAGS;
    }

    @PostMapping(TAGS + "/add")
    @ResponseBody
    public ResponseEntity addTag(@CurrentAccount Account account, @PathVariable String path, @RequestBody TagForm tagForm){
        Hobby hobby = hobbyService.getHobbyToUpdateTag(account,path);
        Tag tag = tagService.findOrCreateNew(tagForm.getTagTitle());
        hobbyService.addTag(hobby, tag);
        return ResponseEntity.ok().build();
    }

    @PostMapping(TAGS + "/remove")
    @ResponseBody
    public ResponseEntity removeTag(@CurrentAccount Account account, @PathVariable String path, @RequestBody TagForm tagForm) {
        Hobby hobby = hobbyService.getHobbyToUpdateTag(account,path);
        Optional<Tag> tag = tagRepository.findByTitle(tagForm.getTagTitle());
        if (tag.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        hobbyService.removeTag(hobby, tag.get());
        return ResponseEntity.ok().build();
    }

    @GetMapping(ZONES)
    public String hobbyZonesForm(@CurrentAccount Account account, @PathVariable String path, Model model) throws JsonProcessingException {
        Hobby hobby = hobbyService.getHobbyToUpdate(account,path);
        model.addAttribute(account);
        model.addAttribute(hobby);

        model.addAttribute("zones", hobby.getZones().stream().map(Zone::toString).toList());
        List<String> allZones = zoneRepository.findAll().stream().map(Zone::toString).toList();
        model.addAttribute("chamlist", objectMapper.writeValueAsString(allZones));
        return HOBBY + ROOT + SETTINGS + ROOT + ZONES;
    }

    @PostMapping(ZONES + "/add")
    @ResponseBody
    public ResponseEntity addZone(@CurrentAccount Account account, @PathVariable String path, @RequestBody ZoneForm zoneForm) {
        Hobby hobby = hobbyService.getHobbyToUpdateZone(account,path);
        Optional<Zone> zone = zoneRepository.findByCityAndProvince(zoneForm.getCityName(), zoneForm.getProvinceName());
        if(zone.isEmpty()){
            return ResponseEntity.badRequest().build();
        }
        hobbyService.addZone(hobby, zone.get());
        return ResponseEntity.ok().build();
    }

    @PostMapping(ZONES + "/remove")
    @ResponseBody
    public ResponseEntity removeZone(@CurrentAccount Account account, @PathVariable String path, @RequestBody ZoneForm zoneForm) {
        Hobby hobby = hobbyService.getHobbyToUpdateZone(account,path);
        Optional<Zone> zone = zoneRepository.findByCityAndProvince(zoneForm.getCityName(), zoneForm.getProvinceName());
        if(zone.isEmpty()){
            return ResponseEntity.badRequest().build();
        }
        hobbyService.removeZone(hobby, zone.get());
        return ResponseEntity.ok().build();
    }

    @GetMapping(HOBBY)
    public String hobbySettingsForm(@CurrentAccount Account account, @PathVariable String path, Model model) {
        Hobby hobby = hobbyService.getHobbyToUpdate(account,path);
        model.addAttribute(account);
        model.addAttribute(hobby);
        return HOBBY + ROOT + SETTINGS + ROOT + HOBBY;
    }
    
    @PostMapping(HOBBY + "/publish")
    public String publishHobby(@CurrentAccount Account account, @PathVariable String path,
                               RedirectAttributes attributes) {
        Hobby hobby = hobbyService.getHobbyToUpdateStatus(account, path);
        hobbyService.publish(hobby);
        attributes.addFlashAttribute("message", "Hobby has been published.");
        return "redirect:/" + HOBBY + ROOT + hobby.getEncodedPath() + ROOT + SETTINGS + ROOT + HOBBY;
    }

    @PostMapping(HOBBY + "/close")
    public String closehobby(@CurrentAccount Account account, @PathVariable String path,
                             RedirectAttributes attributes) {
        Hobby hobby = hobbyService.getHobbyToUpdateStatus(account, path);
        hobbyService.close(hobby);
        attributes.addFlashAttribute("message", "Hobby has been closed.");
        return "redirect:/" + HOBBY + ROOT + hobby.getEncodedPath() + ROOT + SETTINGS + ROOT + HOBBY;
    }

    @PostMapping(HOBBY + "/remove")
    public String removeHobby(@CurrentAccount Account account, @PathVariable String path, Model model){
        Hobby hobby = hobbyService.getHobbyToUpdateStatus(account, path);
        hobbyService.remove(hobby);
        model.addAttribute("message", "Hobby has been removed successfully.");
        return "redirect:/";
    }

    @PostMapping(RECRUIT + "/start")
    public String startRecruit(@CurrentAccount Account account, @PathVariable String path, Model model,
                               RedirectAttributes attributes) {
        Hobby hobby = hobbyService.getHobbyToUpdateStatus(account, path);
        if (!hobby.canUpdateRecruiting()) {
            attributes.addFlashAttribute("message", "Within 1 hour of the last update, you cannot change the recruiting status.");
            return "redirect:/" + HOBBY + ROOT + hobby.getEncodedPath() + ROOT + SETTINGS + ROOT + HOBBY;
        }

        hobbyService.startRecruit(hobby);
        attributes.addFlashAttribute("message", "Recruiting has started.");
        return "redirect:/" + HOBBY + ROOT + hobby.getEncodedPath() + ROOT + SETTINGS + ROOT + HOBBY;
    }

    @PostMapping(HOBBY + ROOT + PATH)
    public String updateHobbyPath(@CurrentAccount Account account, @PathVariable String path, String newPath,
            Model model, RedirectAttributes redirectAttributes) {
        Hobby hobby = hobbyService.getHobbyToUpdateStatus(account, path);
        if(!hobbyService.isValidPath(newPath)){
            model.addAttribute(account);
            model.addAttribute(hobby);
            model.addAttribute("hobbyPathError", "This path is not valid. Please use a different path.");
            return HOBBY + ROOT + SETTINGS + ROOT + HOBBY;
        }
        hobbyService.updateHobbyPath(hobby, newPath);
        redirectAttributes.addFlashAttribute("message", "Hobby path updated successfully.");
        return "redirect:/" + HOBBY + ROOT + hobby.getEncodedPath() + ROOT + SETTINGS + ROOT + HOBBY;
    }

    @PostMapping(HOBBY + ROOT + TITLE)
    public String updateHobbyTitle(@CurrentAccount Account account, @PathVariable String path, String newTitle,
            Model model, RedirectAttributes redirectAttributes) {
        Hobby hobby = hobbyService.getHobbyToUpdateStatus(account, path);
        if(!hobbyService.isValidTitle(newTitle)){
            model.addAttribute(account);
            model.addAttribute(hobby);
            model.addAttribute("hobbyTitleError", "This title is not valid. Please use a different title.");
            return HOBBY + ROOT + SETTINGS + ROOT + HOBBY;
        }

        if (hobbyService.isDuplicatedTitle(newTitle)){
            model.addAttribute(account);
            model.addAttribute(hobby);
            model.addAttribute("hobbyTitleError", "This title is already taken. Please use a different title.");
            return HOBBY + ROOT + SETTINGS + ROOT + HOBBY;
        }

        hobbyService.updateHobbyTitle(hobby, newTitle);
        redirectAttributes.addFlashAttribute("message", "Hobby title updated successfully.");
        return "redirect:/" + HOBBY + ROOT + hobby.getEncodedPath() + ROOT + SETTINGS + ROOT + HOBBY;
    }





}
