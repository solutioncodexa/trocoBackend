package ma.codexa.troco.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import ma.codexa.troco.service.StoreSeoService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class StoreSeoController {

    private final StoreSeoService storeSeoService;

    @GetMapping(value = {"/sitemap.xml", "/seo/sitemap.xml"}, produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> sitemap(HttpServletRequest request) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(storeSeoService.buildSitemapXml(request));
    }

    @GetMapping(value = {"/robots.txt", "/seo/robots.txt"}, produces = "text/plain;charset=UTF-8")
    public ResponseEntity<String> robots(HttpServletRequest request) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/plain;charset=UTF-8"))
                .body(storeSeoService.buildRobotsTxt(request));
    }
}
