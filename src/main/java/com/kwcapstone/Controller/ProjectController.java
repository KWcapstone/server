package com.kwcapstone.Controller;

import com.kwcapstone.Service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/main/project")
public class ProjectController {
    private final ProjectService projectService;
}
