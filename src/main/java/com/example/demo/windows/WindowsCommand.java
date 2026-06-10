package com.example.demo.windows;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Collectors;

public class WindowsCommand {

    public String run(List<String> command) {
        try {
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();

            String output = readOutput(process);
            int exitCode = process.waitFor();

            System.out.println("COMMAND: " + String.join(" ", command));
            System.out.println("EXIT: " + exitCode);

            if (!output.isBlank()) {
                System.out.println(output);
            }

            return output;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void runIgnoreError(List<String> command) {
        try {
            run(command);
        } catch (Exception e) {
            System.out.println("COMMAND IGNORED ERROR: " + String.join(" ", command));
            System.out.println(e.getMessage());
        }
    }

    private String readOutput(Process process) throws Exception {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), Charset.defaultCharset())
        )) {
            return reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }
}
