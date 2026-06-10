package com.example.demo.windows;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Collectors;

public class WindowsCommand {

    public void run(List<String> command) {
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
        } catch (Exception e) {
            throw new RuntimeException(e);
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
