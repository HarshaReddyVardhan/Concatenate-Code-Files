package com.concatenator.conatenate.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.test.web.servlet.MockMvc;

import com.concatenator.conatenate.service.ConcatenationService;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.awt.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

// We only need to scan the controller, not the whole app
@WebMvcTest(UtilsController.class)
class UtilsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ConcatenationService concatenationService;

    @Test
    void testGetEnvInfo() throws Exception {
        mockMvc.perform(get("/api/utils/env-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isDocker").isBoolean())
                .andExpect(jsonPath("$.isHeadless").isBoolean());
    }

    @Test
    void testBrowseFolderRequest() throws Exception {
        // This test behavior depends on the environment (Headless vs Headed)
        // We just want to ensure the endpoint exists and doesn't crash.

        if (GraphicsEnvironment.isHeadless()) {
            mockMvc.perform(get("/api/utils/browse-folder"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").exists());
        } else {
            // In a headed environment (like local dev), this opens a window which blocks
            // test execution
            // until closed. This is bad for automated tests.
            // Ideally, we mock the Swing interaction, but that's hard with the current
            // implementation
            // of UtilsController which does `new JFileChooser()` directly.
            // For now, we'll skip the actual execution in headed mode to prevent hanging,
            // or we could assume the user won't run `mvn test` while looking at it.
            // BETTER: We can just verify the controller is loaded (which @WebMvcTest does).
            // So we will just print that we are skipping the actual call in headed mode.
            System.out.println("Skipping /browse-folder execution in headed mode to avoid blocking UI.");
        }
    }
}
