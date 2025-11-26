package com.concatenator.conatenate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Web Controller for serving HTML pages.
 *
 * Endpoints:
 * - GET / - Home page
 * - GET /settings - Settings page
 */
@Slf4j
@Controller
public class WebController {

    /**
     * Home page with concatenation form.
     *
     * GET /
     *
     * @return index.html template
     */
    @GetMapping("/")
    public String index() {
        log.info("Serving home page");
        return "index";
    }

    /**
     * Settings management page.
     *
     * GET /settings
     *
     * @return settings.html template
     */
    @GetMapping("/settings")
    public String settings() {
        log.info("Serving settings page");
        return "settings";
    }
}
