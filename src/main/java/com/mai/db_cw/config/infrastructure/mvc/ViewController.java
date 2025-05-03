package com.mai.db_cw.config.infrastructure.mvc;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@Controller
public class ViewController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/signup")
    public String sighUp() {
        return "signup";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin")
    public String admin() {
        return "admin";
    }

    @GetMapping("/home")
    public String home() {
        return "home";
    }

    @GetMapping("/profile")
    public String profile() {
        return "profile";
    }

    @GetMapping("/bookings")
    public String bookings() { return "bookings"; }

    @GetMapping("/report-issue")
    public String reportIssue() {
        return "report-form";
    }

    @GetMapping("/courses/{courseId}/articles/{articleId}")
    public String articleDetails(@PathVariable UUID courseId, @PathVariable UUID articleId, Model model) {
        return "articles-details";
    }

    @GetMapping("/courses/{courseId}/testings/{testId}")
    public String testingDetails(@PathVariable UUID courseId, @PathVariable UUID testId, Model model) {
        return "testing-details";
    }

    @GetMapping("/agreement")
    public String agreement() {
        return "UserAgreement";
    }

    @GetMapping("/questions")
    public String questions() {
        return "questions";
    }
}
